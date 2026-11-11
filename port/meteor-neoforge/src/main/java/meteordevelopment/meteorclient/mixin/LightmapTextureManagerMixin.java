package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Fullbright;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin({LightTexture.class})
public abstract class LightmapTextureManagerMixin {
   @ModifyArgs(
      method = {"Lnet/minecraft/client/renderer/LightTexture;updateLightTexture(F)V"},
      at = @At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/platform/NativeImage;setPixelRGBA(III)V"
      )
   )
   private void update(Args args) {
      if (Modules.get().get(Fullbright.class).getGamma() || Modules.get().isActive(Xray.class)) {
         args.set(2, -1);
      }
   }

   @Inject(
      method = {"Lnet/minecraft/client/renderer/LightTexture;getDarknessGamma(F)F"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void getDarknessFactor(float tickDelta, CallbackInfoReturnable<Float> info) {
      if (Modules.get().get(NoRender.class).noDarkness()) {
         info.setReturnValue(0.0F);
      }
   }
}
