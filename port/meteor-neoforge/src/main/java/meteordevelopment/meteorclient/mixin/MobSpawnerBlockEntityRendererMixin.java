package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.renderer.blockentity.SpawnerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SpawnerRenderer.class})
public abstract class MobSpawnerBlockEntityRendererMixin {
   @Inject(
      method = {"Lnet/minecraft/client/renderer/blockentity/SpawnerRenderer;render(Lnet/minecraft/world/level/block/entity/SpawnerBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRender(CallbackInfo ci) {
      if (Modules.get().get(NoRender.class).noMobInSpawner()) {
         ci.cancel();
      }
   }
}
