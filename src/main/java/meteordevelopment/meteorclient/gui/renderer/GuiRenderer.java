/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.operations.TextOperation;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.renderer.packer.TexturePacker;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

public class GuiRenderer {
    private static final Color WHITE = new Color(255, 255, 255);

    private static final TexturePacker TEXTURE_PACKER = new TexturePacker();
    private static Texture TEXTURE;

    public static GuiTexture CIRCLE;
    public static GuiTexture TRIANGLE;
    public static GuiTexture EDIT;
    public static GuiTexture RESET;
    public static GuiTexture FAVORITE_NO, FAVORITE_YES;
    public static GuiTexture COPY, PASTE;

    public GuiTheme theme;

    private final Renderer2D r = new Renderer2D(false);
    private final Renderer2D rTex = new Renderer2D(true);
    private final UiShapeRenderer shapes = new UiShapeRenderer();
    private enum Batch { Color, Line, Texture, Shape, Glass, Text }
    private Batch batch;
    private GpuTextureView batchTexture;
    private GpuSampler batchSampler;
    private boolean building;
    private double alpha = 1;
    private double textSize = 1;

    private final Pool<Scissor> scissorPool = new Pool<>(Scissor::new);
    private final Stack<Scissor> scissorStack = new ObjectArrayList<>();

    private final Pool<TextOperation> textPool = new Pool<>(TextOperation::new);
    private final List<TextOperation> texts = new ObjectArrayList<>();

    private final List<Runnable> postTasks = new ObjectArrayList<>();

    public String tooltip, lastTooltip;
    public WWidget tooltipWidget;
    private double tooltipAnimProgress;

    private GuiGraphicsExtractor graphics;

    public static GuiTexture addTexture(Identifier id) {
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

        COPY = addTexture(MeteorClient.identifier("textures/icons/gui/copy.png"));
        PASTE = addTexture(MeteorClient.identifier("textures/icons/gui/paste.png"));

        TEXTURE = TEXTURE_PACKER.pack();
    }

    public void begin(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
        this.graphics.nextStratum();

        var matrices = graphics.pose();
        matrices.pushMatrix();
        matrices.scale(1.0f / mc.getWindow().getGuiScale());

        scissorStart(0, 0, getWindowWidth(), getWindowHeight());
    }

    public void end() {
        scissorEnd();

        for (Runnable task : postTasks) task.run();
        postTasks.clear();

        graphics.pose().popMatrix();
        graphics.nextStratum();
    }

    public void beginRender() {
        r.begin();
        rTex.begin();
        shapes.begin();
        batch = null;
        batchTexture = null;
        batchSampler = null;
        building = true;
    }

    private void selectBatch(Batch next) {
        selectBatch(next, null, null);
    }

    private void selectBatch(Batch next, GpuTextureView texture, GpuSampler sampler) {
        if (batch != null && (batch != next || batchTexture != texture || batchSampler != sampler)) {
            endRender(scissorStack.top());
            beginRender();
        }
        batch = next;
        batchTexture = texture;
        batchSampler = sampler;
    }

    public void endRender() {
        endRender(null);
    }

    public void endRender(Scissor scissor) {
        if (scissor != null) scissor.push();

        r.end();
        rTex.end();
        shapes.end();
        building = false;

        r.render();
        boolean externalTexture = batch == Batch.Texture && batchTexture != null;
        rTex.render("u_Texture", externalTexture ? batchTexture : TEXTURE.getTextureView(),
            externalTexture ? batchSampler : TEXTURE.getSampler());
        shapes.render(batch == Batch.Glass ? batchTexture : null);

        if (!texts.isEmpty()) {
            theme.textRenderer().begin(graphics, theme.scale(theme.textScale() * textSize));
            for (TextOperation text : texts) text.run(textPool);
            theme.textRenderer().end();
            texts.clear();
        }

        if (scissor != null) scissor.pop();
    }

    public void scissorStart(double x, double y, double width, double height) {
        if (!scissorStack.isEmpty()) {
            Scissor parent = scissorStack.top();
            double right = Math.min(x + Math.max(0, width), parent.x + parent.width);
            double bottom = Math.min(y + Math.max(0, height), parent.y + parent.height);
            x = Math.max(x, parent.x);
            y = Math.max(y, parent.y);
            width = Math.max(0, right - x);
            height = Math.max(0, bottom - y);

            endRender(parent);
        }

        Scissor current = scissorPool.get().set(x, y, width, height);
        scissorStack.push(current);
        graphics.enableScissor(current.x, current.y, current.x + current.width, current.y + current.height);

        beginRender();
    }

    public void scissorEnd() {
        Scissor scissor = scissorStack.pop();

        endRender(scissor);

        scissor.push();
        for (Runnable task : scissor.postTasks) task.run();
        scissor.pop();

        graphics.disableScissor();
        if (!scissorStack.isEmpty()) beginRender();

        scissorPool.free(scissor);
    }

    public boolean renderTooltip(GuiGraphicsExtractor graphics, double mouseX, double mouseY, double delta) {
        tooltipAnimProgress += (tooltip != null ? 1 : -1) * delta * 14;
        tooltipAnimProgress = Mth.clamp(tooltipAnimProgress, 0, 1);

        boolean toReturn = false;

        if (tooltipAnimProgress > 0) {
            if (tooltip != null && !tooltip.equals(lastTooltip)) {
                tooltipWidget = theme.tooltip(tooltip);
                tooltipWidget.init();
            }

            double deltaX = -tooltipWidget.x + mouseX + 12;
            double deltaY = -tooltipWidget.y + mouseY + 12;

            if (mouseX + 12 + tooltipWidget.width > getWindowWidth())
                deltaX = -tooltipWidget.x + getWindowWidth() - tooltipWidget.width;
            if (mouseY + 12 + tooltipWidget.height > getWindowHeight())
                deltaY = -tooltipWidget.y + getWindowHeight() - tooltipWidget.height;

            tooltipWidget.move(deltaX, deltaY);

            setAlpha(tooltipAnimProgress);

            begin(graphics);
            tooltipWidget.render(this, mouseX, mouseY, delta);
            end();

            setAlpha(1);

            lastTooltip = tooltip;
            toReturn = true;
        }

        tooltip = null;
        return toReturn;
    }

    public void setAlpha(double a) {
        a = Double.isFinite(a) ? Mth.clamp(a, 0, 1) : 1;
        // Pending text must use the alpha of the batch that queued it.
        if (building && a != alpha) {
            endRender(scissorStack.top());
            beginRender();
        }
        alpha = a;
        r.setAlpha(a);
        rTex.setAlpha(a);
        shapes.setAlpha(a);

        theme.textRenderer().setAlpha(a);
    }

    public void tooltip(String text) {
        tooltip = text;
    }

    public void quad(double x, double y, double width, double height, Color cTopLeft, Color cTopRight, Color cBottomRight, Color cBottomLeft) {
        if (width <= 0 || height <= 0) return;
        selectBatch(Batch.Color);
        r.quad(x, y, width, height, cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void quad(double x, double y, double width, double height, Color colorLeft, Color colorRight) {
        quad(x, y, width, height, colorLeft, colorRight, colorRight, colorLeft);
    }

    public void quad(double x, double y, double width, double height, Color color) {
        quad(x, y, width, height, color, color);
    }

    public void quad(WWidget widget, Color color) {
        quad(widget.x, widget.y, widget.width, widget.height, color);
    }

    public void line(double x1, double y1, double x2, double y2, Color color) {
        selectBatch(Batch.Line);
        r.line(x1, y1, x2, y2, color);
    }

    public void quad(double x, double y, double width, double height, GuiTexture texture, Color color) {
        selectBatch(Batch.Texture);
        rTex.texQuad(x, y, width, height, texture.get(width, height), color);
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

    /** Samples a full-window backdrop through a rounded mask in one draw. */
    public void roundedBackdrop(double x, double y, double width, double height, double radius, GpuTextureView texture, Color tint) {
        if (texture == null) return;
        selectBatch(Batch.Glass, texture, null);
        shapes.rectangle(x, y, width, height, radius, radius, radius, radius, 0, 1, tint, tint);
    }

    public void rotatedQuad(double x, double y, double width, double height, double rotation, GuiTexture texture, Color color) {
        selectBatch(Batch.Texture);
        rTex.texQuad(x, y, width, height, rotation, texture.get(width, height), color);
    }

    public void triangle(double x1, double y1, double x2, double y2, double x3, double y3, Color color) {
        selectBatch(Batch.Color);
        r.triangle(x1, y1, x2, y2, x3, y3, color);
    }

    public void text(String text, double x, double y, Color color, boolean title) {
        textScaled(text, x, y, color, title ? 1.25 : 1);
    }

    public void textScaled(String text, double x, double y, Color color, double size) {
        if (size <= 0 || !Double.isFinite(size)) return;
        if (batch == Batch.Text && textSize != size) {
            endRender(scissorStack.top());
            beginRender();
        }
        selectBatch(Batch.Text);
        textSize = size;
        texts.add(getOp(textPool, x, y, color).set(text, theme.textRenderer(), false));
    }

    public void texture(double x, double y, double width, double height, double rotation, Texture texture) {
        selectBatch(Batch.Texture, texture.getTextureView(), texture.getSampler());
        rTex.texQuad(x, y, width, height, rotation, 0, 0, 1, 1, WHITE);
    }

    public void texture(double x, double y, double width, double height, double rotation, GpuTextureView texture) {
        selectBatch(Batch.Texture, texture, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        // Framebuffer textures use the opposite vertical origin from GUI assets.
        rTex.texQuad(x, y, width, height, rotation, 0, 1, 1, 0, WHITE);
    }

    public void post(Runnable task) {
        scissorStack.top().postTasks.add(task);
    }

    public void item(ItemStack itemStack, int x, int y, float scale, boolean overlay) {
        RenderUtils.drawItem(graphics, itemStack, x, y, scale, overlay, null, false);
    }

    public void absolutePost(Runnable task) {
        postTasks.add(task);
    }

    private <T extends GuiRenderOperation<T>> T getOp(Pool<T> pool, double x, double y, Color color) {
        T op = pool.get();
        op.set(x, y, color);
        return op;
    }
}
