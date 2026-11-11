package meteordevelopment.meteorclient.gui.renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.operations.TextOperation;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.renderer.packer.TexturePacker;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.renderer.GL;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.ByteTexture;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class GuiRenderer {
   private static final Color WHITE = new Color(255, 255, 255);
   private static final TexturePacker TEXTURE_PACKER = new TexturePacker();
   private static ByteTexture TEXTURE;
   public static GuiTexture CIRCLE;
   public static GuiTexture TRIANGLE;
   public static GuiTexture EDIT;
   public static GuiTexture RESET;
   public static GuiTexture FAVORITE_NO;
   public static GuiTexture FAVORITE_YES;
   public static GuiTexture BRAND_LOGO;
   public GuiTheme theme;
   private final Renderer2D r = new Renderer2D(false);
   private final Renderer2D rTex = new Renderer2D(true);
   private final UiShapeRenderer shapes = new UiShapeRenderer();
   private enum Batch { Color, Line, Texture, Shape, Glass, Image, Text }
   private Batch batch;
   private int batchTexture;
   private boolean building;
   private double alpha = 1, textSize = 1;
   private final UiBackdrop backdrop = new UiBackdrop();
   public Integer backdropTexture() { return backdrop.texture(); }
   private void selectBatch(Batch next) { selectBatch(next, 0); }
   private void selectBatch(Batch next, int texture) {
      if (batch != null && (batch != next || batchTexture != texture)) {
         scissorStack.peek().apply(); endRender(); beginRender();
      }
      batch = next; batchTexture = texture; shapes.setGlass(next == Batch.Glass);
      shapes.setImage(next == Batch.Image);
   }
   private final Pool<Scissor> scissorPool = new Pool<>(Scissor::new);
   private final Stack<Scissor> scissorStack = new Stack<>();
   private final Pool<TextOperation> textPool = new Pool<>(TextOperation::new);
   private final List<TextOperation> texts = new ArrayList<>();
   private final List<Runnable> postTasks = new ArrayList<>();
   public String tooltip;
   public String lastTooltip;
   public WWidget tooltipWidget;
   private double tooltipAnimProgress;
   private GuiGraphics drawContext;

   public static GuiTexture addTexture(ResourceLocation id) {
      return TEXTURE_PACKER.add(id);
   }

   @PostInit
   public static void init() {
      CIRCLE = addTexture(MeteorClient.identifier("textures/icons/gui/circle.png"));
      TRIANGLE = addTexture(MeteorClient.identifier("textures/icons/gui/triangle.png"));
      EDIT = addTexture(MeteorClient.identifier("textures/icons/gui/edit.png"));
      RESET = addTexture(MeteorClient.identifier("textures/icons/gui/reset.png"));
      FAVORITE_NO = addTexture(MeteorClient.identifier("textures/icons/gui/favorite_no.png"));
      FAVORITE_YES = addTexture(MeteorClient.identifier("textures/icons/gui/favorite_yes.png"));
      BRAND_LOGO = addTexture(MeteorClient.identifier("textures/icons/gui/xiaohsense.jpg"));
      TEXTURE = TEXTURE_PACKER.pack();
   }

   public void begin(GuiGraphics drawContext) {
      this.drawContext = drawContext;
      if (meteordevelopment.meteorclient.gui.themes.meteor.ModernWidgetStyle.isModernScreen()) backdrop.capture();
      GL.enableBlend();
      GL.enableScissorTest();
      this.scissorStart(0.0, 0.0, (double)Utils.getWindowWidth(), (double)Utils.getWindowHeight());
   }

   public void end() {
      this.scissorEnd();

      for (Runnable task : this.postTasks) {
         task.run();
      }

      this.postTasks.clear();
      GL.disableScissorTest();
   }

   public void beginRender() {
      this.r.begin();
      this.rTex.begin();
      shapes.begin(); shapes.setGlass(false); shapes.setImage(false); batch = null; batchTexture = 0; building = true;
   }

   public void endRender() {
      r.end(); rTex.end(); shapes.end(); building = false;
      r.render(drawContext.pose());
      GL.bindTexture(batch == Batch.Texture && batchTexture != 0 ? batchTexture : TEXTURE.getId());
      rTex.render(drawContext.pose());
      shapes.render(drawContext.pose(), batch == Batch.Glass ? batchTexture : 0);
      if (!texts.isEmpty()) {
         theme.textRenderer().begin(theme.scale(theme.textScale() * textSize));
         for (TextOperation text : texts) text.run(textPool);
         theme.textRenderer().end(drawContext.pose()); texts.clear();
      }
   }

   public void scissorStart(double x, double y, double width, double height) {
      if (!this.scissorStack.isEmpty()) {
         Scissor parent = this.scissorStack.peek();
         double right = Math.min(x + Math.max(0, width), parent.x + parent.width);
         double bottom = Math.min(y + Math.max(0, height), parent.y + parent.height);
         x = Math.max(x, parent.x); y = Math.max(y, parent.y);
         width = Math.max(0, right - x); height = Math.max(0, bottom - y);
         parent.apply();
         this.endRender();
      }

      this.scissorStack.push(this.scissorPool.get().set(x, y, width, height));
      this.beginRender();
   }

   public void scissorEnd() {
      Scissor scissor = this.scissorStack.pop();
      scissor.apply();
      this.endRender();

      for (Runnable task : scissor.postTasks) {
         task.run();
      }

      if (!this.scissorStack.isEmpty()) {
         this.beginRender();
      }

      this.scissorPool.free(scissor);
   }

   public boolean renderTooltip(GuiGraphics drawContext, double mouseX, double mouseY, double delta) {
      this.tooltipAnimProgress = this.tooltipAnimProgress + (double)(this.tooltip != null ? 1 : -1) * delta * 14.0;
      this.tooltipAnimProgress = Mth.clamp(this.tooltipAnimProgress, 0.0, 1.0);
      boolean toReturn = false;
      if (this.tooltipAnimProgress > 0.0) {
         if (this.tooltip != null && !this.tooltip.equals(this.lastTooltip)) {
            this.tooltipWidget = this.theme.tooltip(this.tooltip);
            this.tooltipWidget.init();
         }

         this.tooltipWidget.move(-this.tooltipWidget.x + mouseX + 12.0, -this.tooltipWidget.y + mouseY + 12.0);
         this.setAlpha(this.tooltipAnimProgress);
         this.begin(drawContext);
         this.tooltipWidget.render(this, mouseX, mouseY, delta);
         this.end();
         this.setAlpha(1.0);
         this.lastTooltip = this.tooltip;
         toReturn = true;
      }

      this.tooltip = null;
      return toReturn;
   }

   public void setAlpha(double a) {
      if (building && alpha != a) { scissorStack.peek().apply(); endRender(); beginRender(); }
      alpha = a; shapes.setAlpha(a);
      this.r.setAlpha(a);
      this.rTex.setAlpha(a);
      this.theme.textRenderer().setAlpha(a);
   }

   public void tooltip(String text) {
      this.tooltip = text;
   }

   public void quad(double x, double y, double width, double height, Color cTopLeft, Color cTopRight, Color cBottomRight, Color cBottomLeft) {
      selectBatch(Batch.Color);
      this.r.quad(x, y, width, height, cTopLeft, cTopRight, cBottomRight, cBottomLeft);
   }

   public void quad(double x, double y, double width, double height, Color colorLeft, Color colorRight) {
      this.quad(x, y, width, height, colorLeft, colorRight, colorRight, colorLeft);
   }

   public void quad(double x, double y, double width, double height, Color color) {
      this.quad(x, y, width, height, color, color);
   }

   public void quad(WWidget widget, Color color) {
      this.quad(widget.x, widget.y, widget.width, widget.height, color);
   }

   public void quad(double x, double y, double width, double height, GuiTexture texture, Color color) {
      selectBatch(Batch.Texture);
      this.rTex.texQuad(x, y, width, height, texture.get(width, height), color);
   }

   public void rotatedQuad(double x, double y, double width, double height, double rotation, GuiTexture texture, Color color) {
      selectBatch(Batch.Texture);
      this.rTex.texQuad(x, y, width, height, rotation, texture.get(width, height), color);
   }

   public void triangle(double x1, double y1, double x2, double y2, double x3, double y3, Color color) {
      selectBatch(Batch.Color);
      this.r.triangle(x1, y1, x2, y2, x3, y3, color);
   }

    /** Analytic, anti-aliased rounded rectangle; independent of the icon atlas. */
    public void roundedQuad(double x, double y, double width, double height, double radius, Color color) {
        roundedGradient(x, y, width, height, radius, color, color);
    }

    /** Draws a rectangle with only its top two corners rounded. */
    public void roundedTopQuad(double x, double y, double width, double height, double radius, Color color) {
        selectBatch(Batch.Shape);
        shapes.rectangle(x, y, width, height, radius, radius, 0, 0, 0, 1, color, color);
    }

    /** Draws a rectangle with only its left two corners rounded. */
    public void roundedLeftQuad(double x, double y, double width, double height, double radius, Color color) {
        selectBatch(Batch.Shape);
        shapes.rectangle(x, y, width, height, radius, 0, 0, radius, 0, 1, color, color);
    }

    public void roundedGradient(double x, double y, double width, double height, double radius, Color top, Color bottom) {
        selectBatch(Batch.Shape);
        shapes.rectangle(x, y, width, height, radius, radius, radius, radius, 0, 1, top, bottom);
    }

    public void roundedOutline(double x, double y, double width, double height, double radius, double thickness, Color color) {
        if (thickness <= 0) return;
        selectBatch(Batch.Shape);
        shapes.rectangle(x, y, width, height, radius, radius, radius, radius, thickness, 1, color, color);
    }

    public void roundedShadow(double x, double y, double width, double height, double radius, double softness, Color color) {
        selectBatch(Batch.Shape);
        shapes.rectangle(x, y, width, height, radius, radius, radius, radius, -1, softness, color, color);
    }

    public void roundedLeftBackdrop(double x, double y, double width, double height, double radius, Integer texture, Color tint) {
        if (texture == null) return;
        selectBatch(Batch.Glass, texture);
        shapes.rectangle(x,y,width,height,radius,0,0,radius,0,1,tint,tint);
    }

    /** Samples a full-window backdrop through a rounded mask in one draw. */
    public void roundedBackdrop(double x, double y, double width, double height, double radius, Integer texture, Color tint) {
        if (texture == null) return;
        selectBatch(Batch.Glass, texture);
        shapes.rectangle(x, y, width, height, radius, radius, radius, radius, 0, 1, tint, tint);
    }

   public void line(double x1, double y1, double x2, double y2, Color color) {
      selectBatch(Batch.Line); r.line(x1, y1, x2, y2, color);
   }
   public void roundedImage(double x,double y,double width,double height,double radius,GuiTexture texture,Color tint) {
      if (batch == Batch.Image) { scissorStack.peek().apply(); endRender(); beginRender(); }
      selectBatch(Batch.Image);
      shapes.imageRegion(texture.get(width,height));
      shapes.rectangle(x,y,width,height,radius,radius,radius,radius,0,1,tint,tint);
   }
   public void text(String text, double x, double y, Color color, boolean title) { textScaled(text,x,y,color,title ? 1.25 : 1); }
   public void brandText(String text, meteordevelopment.meteorclient.renderer.text.TextRenderer font,
                         double x, double y, Color color, double size, double widthScale) {
      scissorStack.peek().apply(); endRender();
      // The 1.21 two-dimensional Mesh reads RenderSystem's model-view stack.
      var matrices=com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
      matrices.pushMatrix();
      try {
         matrices.translate((float)x,(float)y,0).scale((float)widthScale,1,1);
         font.setAlpha(alpha);
         font.begin(theme.scale(theme.textScale()*size));
         font.render(text,0,0,color,false);
         font.end(drawContext.pose());
      } finally {
         matrices.popMatrix();
      }
      beginRender();
   }
   public void textScaled(String text, double x, double y, Color color, double size) {
      if (!Double.isFinite(size) || size <= 0) return;
      if (batch == Batch.Text && textSize != size) { scissorStack.peek().apply(); endRender(); beginRender(); }
      selectBatch(Batch.Text); textSize = size;
      texts.add(getOp(textPool,x,y,color).set(text,theme.textRenderer(),false));
   }
   public void texture(double x, double y, double width, double height, double rotation, Texture texture) {
      texture.bind();
      selectBatch(Batch.Texture, org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D));
      rTex.texQuad(x,y,width,height,rotation,0,0,1,1,WHITE);
   }

   public void post(Runnable task) {
      this.scissorStack.peek().postTasks.add(task);
   }

   public void item(ItemStack itemStack, int x, int y, float scale, boolean overlay) {
      RenderUtils.drawItem(this.drawContext, itemStack, x, y, scale, overlay);
   }

   public void absolutePost(Runnable task) {
      this.postTasks.add(task);
   }

   private <T extends GuiRenderOperation<T>> T getOp(Pool<T> pool, double x, double y, Color color) {
      T op = (T)pool.get();
      op.set(x, y, color);
      return op;
   }
}
