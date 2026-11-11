package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ModelBlockRenderer.class})
public abstract class BlockModelRendererMixin {
   @Unique
   private final ThreadLocal<Integer> alphas = ThreadLocal.withInitial(() -> -1);

   @Inject(
      method = {"Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateWithAO(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V", "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateWithoutAO(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderSmooth(
      BlockAndTintGetter world,
      BakedModel model,
      BlockState state,
      BlockPos pos,
      PoseStack matrices,
      VertexConsumer vertexConsumer,
      boolean cull,
      RandomSource random,
      long seed,
      int overlay,
      net.neoforged.neoforge.client.model.data.ModelData modelData,
      net.minecraft.client.renderer.RenderType renderType,
      CallbackInfo info
   ) {
      int alpha = Xray.getAlpha(state, pos);
      if (alpha == 0) {
         info.cancel();
      } else {
         this.alphas.set(alpha);
      }
   }

   @ModifyConstant(
      method = {"Lnet/minecraft/client/renderer/block/ModelBlockRenderer;putQuadData(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFIIIII)V"},
      constant = {@Constant(
         floatValue = 1.0F,
         ordinal = 3
      )}
   )
   private float renderQuad_modifyAlpha(float original) {
      int alpha = this.alphas.get();
      return alpha == -1 ? original : (float)alpha / 255.0F;
   }
}

