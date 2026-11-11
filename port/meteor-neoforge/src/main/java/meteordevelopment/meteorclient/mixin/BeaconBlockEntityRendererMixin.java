package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BeaconRenderer.class})
public abstract class BeaconBlockEntityRendererMixin {
   @Inject(
      method = {"Lnet/minecraft/client/renderer/blockentity/BeaconRenderer;render(Lnet/minecraft/world/level/block/entity/BeaconBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRender(
      BeaconBlockEntity beaconBlockEntity, float f, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i, int j, CallbackInfo info
   ) {
      if (Modules.get().get(NoRender.class).noBeaconBeams()) {
         info.cancel();
      }
   }
}
