package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class RenderUtils {
   public static Vec3 center;
   private static final Pool<RenderUtils.RenderBlock> renderBlockPool = new Pool<>(RenderUtils.RenderBlock::new);
   private static final List<RenderUtils.RenderBlock> renderBlocks = new ArrayList<>();

   private RenderUtils() {
   }

   @PostInit
   public static void init() {
      MeteorClient.EVENT_BUS.subscribe(RenderUtils.class);
   }

   public static void drawItem(GuiGraphics drawContext, ItemStack itemStack, int x, int y, float scale, boolean overlay, String countOverride) {
      PoseStack matrices = drawContext.pose();
      matrices.pushPose();
      matrices.scale(scale, scale, 1.0F);
      matrices.translate(0.0F, 0.0F, 401.0F);
      int scaledX = (int)((float)x / scale);
      int scaledY = (int)((float)y / scale);
      drawContext.renderItem(itemStack, scaledX, scaledY);
      if (overlay) {
         drawContext.renderItemDecorations(MeteorClient.mc.font, itemStack, scaledX, scaledY, countOverride);
      }

      matrices.popPose();
   }

   public static void drawItem(GuiGraphics drawContext, ItemStack itemStack, int x, int y, float scale, boolean overlay) {
      drawItem(drawContext, itemStack, x, y, scale, overlay, null);
   }

   public static void updateScreenCenter() {
      Minecraft mc = Minecraft.getInstance();
      Vector3f pos = new Vector3f(0.0F, 0.0F, 1.0F);
      if ((Boolean)mc.options.bobView().get()) {
         PoseStack bobViewMatrices = new PoseStack();
         bobView(bobViewMatrices);
         pos.mulPosition(bobViewMatrices.last().pose().invert());
      }

      center = new Vec3((double)pos.x, (double)(-pos.y), (double)pos.z)
         .xRot(-((float)Math.toRadians((double)mc.gameRenderer.getMainCamera().getXRot())))
         .yRot(-((float)Math.toRadians((double)mc.gameRenderer.getMainCamera().getYRot())))
         .add(mc.gameRenderer.getMainCamera().getPosition());
   }

   private static void bobView(PoseStack matrices) {
      if (Minecraft.getInstance().getCameraEntity() instanceof Player playerEntity) {
         float f = MeteorClient.mc.getTimer().getGameTimeDeltaPartialTick(true);
         float g = playerEntity.walkDist - playerEntity.walkDistO;
         float h = -(playerEntity.walkDist + g * f);
         float i = Mth.lerp(f, playerEntity.oBob, playerEntity.bob);
         matrices.translate(-((double)(Mth.sin(h * (float) Math.PI) * i) * 0.5), (double)Math.abs(Mth.cos(h * (float) Math.PI) * i), 0.0);
         matrices.mulPose(Axis.ZP.rotationDegrees(Mth.sin(h * (float) Math.PI) * i * 3.0F));
         matrices.mulPose(Axis.XP.rotationDegrees(Math.abs(Mth.cos(h * (float) Math.PI - 0.2F) * i) * 5.0F));
      }
   }

   public static void renderTickingBlock(
      BlockPos blockPos, Color sideColor, Color lineColor, ShapeMode shapeMode, int excludeDir, int duration, boolean fade, boolean shrink
   ) {
      Iterator<RenderUtils.RenderBlock> iterator = renderBlocks.iterator();

      while (iterator.hasNext()) {
         RenderUtils.RenderBlock next = iterator.next();
         if (next.pos.equals(blockPos)) {
            iterator.remove();
            renderBlockPool.free(next);
         }
      }

      renderBlocks.add(renderBlockPool.get().set(blockPos, sideColor, lineColor, shapeMode, excludeDir, duration, fade, shrink));
   }

   @EventHandler
   private static void onTick(TickEvent.Pre event) {
      if (!renderBlocks.isEmpty()) {
         renderBlocks.forEach(RenderUtils.RenderBlock::tick);
         Iterator<RenderUtils.RenderBlock> iterator = renderBlocks.iterator();

         while (iterator.hasNext()) {
            RenderUtils.RenderBlock next = iterator.next();
            if (next.ticks <= 0) {
               iterator.remove();
               renderBlockPool.free(next);
            }
         }
      }
   }

   @EventHandler
   private static void onRender(Render3DEvent event) {
      renderBlocks.forEach(block -> block.render(event));
   }

   public static class RenderBlock {
      public MutableBlockPos pos = new MutableBlockPos();
      public Color sideColor;
      public Color lineColor;
      public ShapeMode shapeMode;
      public int excludeDir;
      public int ticks;
      public int duration;
      public boolean fade;
      public boolean shrink;

      public RenderUtils.RenderBlock set(
         BlockPos blockPos, Color sideColor, Color lineColor, ShapeMode shapeMode, int excludeDir, int duration, boolean fade, boolean shrink
      ) {
         this.pos.set(blockPos);
         this.sideColor = sideColor;
         this.lineColor = lineColor;
         this.shapeMode = shapeMode;
         this.excludeDir = excludeDir;
         this.fade = fade;
         this.shrink = shrink;
         this.ticks = duration;
         this.duration = duration;
         return this;
      }

      public void tick() {
         this.ticks--;
      }

      public void render(Render3DEvent event) {
         int preSideA = this.sideColor.a;
         int preLineA = this.lineColor.a;
         double x1 = (double)this.pos.getX();
         double y1 = (double)this.pos.getY();
         double z1 = (double)this.pos.getZ();
         double x2 = (double)(this.pos.getX() + 1);
         double y2 = (double)(this.pos.getY() + 1);
         double z2 = (double)(this.pos.getZ() + 1);
         double d = (double)((float)this.ticks - event.tickDelta) / (double)this.duration;
         if (this.fade) {
            this.sideColor.a = (int)((double)this.sideColor.a * d);
            this.lineColor.a = (int)((double)this.lineColor.a * d);
         }

         if (this.shrink) {
            x1 += d;
            y1 += d;
            z1 += d;
            x2 -= d;
            y2 -= d;
            z2 -= d;
         }

         event.renderer.box(x1, y1, z1, x2, y2, z2, this.sideColor, this.lineColor, this.shapeMode, this.excludeDir);
         this.sideColor.a = preSideA;
         this.lineColor.a = preLineA;
      }
   }
}
