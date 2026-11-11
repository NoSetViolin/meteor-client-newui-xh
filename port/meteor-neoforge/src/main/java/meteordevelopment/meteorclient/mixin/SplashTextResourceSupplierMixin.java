package meteordevelopment.meteorclient.mixin;

import java.util.List;
import java.util.Random;
import meteordevelopment.meteorclient.systems.config.Config;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.resources.SplashManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({SplashManager.class})
public abstract class SplashTextResourceSupplierMixin {
   @Unique
   private boolean override = true;
   @Unique
   private static final Random random = new Random();
   @Unique
   private final List<String> meteorSplashes = getMeteorSplashes();

   @Inject(
      method = {"Lnet/minecraft/client/resources/SplashManager;getSplash()Lnet/minecraft/client/gui/components/SplashRenderer;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onApply(CallbackInfoReturnable<SplashRenderer> cir) {
      if (Config.get() != null && Config.get().titleScreenSplashes.get()) {
         if (this.override) {
            cir.setReturnValue(new SplashRenderer(this.meteorSplashes.get(random.nextInt(this.meteorSplashes.size()))));
         }

         this.override = !this.override;
      }
   }

   @Unique
   private static List<String> getMeteorSplashes() {
      return List.of(
         "XiaohSense!",
         "Star XiaohSense on GitHub!",
         "Based utility mod.",
         "§6NoSetViolin §fbased god",
         "§4KENNYZ.CN",
         "§4XiaohSense!",
         "§6XiaohSense!"
      );
   }
}
