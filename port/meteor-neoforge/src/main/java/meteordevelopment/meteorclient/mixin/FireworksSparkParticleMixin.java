package meteordevelopment.meteorclient.mixin;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.particle.FireworkParticles.SparkParticle;
import net.minecraft.client.particle.FireworkParticles.Starter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin({Starter.class})
public abstract class FireworksSparkParticleMixin {
   @Inject(
      method = {"Lnet/minecraft/client/particle/FireworkParticles$Starter;createParticle(DDDDDDLit/unimi/dsi/fastutil/ints/IntList;Lit/unimi/dsi/fastutil/ints/IntList;ZZ)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/particle/FireworkParticles$SparkParticle;setTrail(Z)V"
      )},
      cancellable = true,
      locals = LocalCapture.CAPTURE_FAILSOFT
   )
   private void onAddExplosion(
      double x,
      double y,
      double z,
      double velocityX,
      double velocityY,
      double velocityZ,
      IntList colors,
      IntList targetColors,
      boolean trail,
      boolean flicker,
      CallbackInfo info,
      SparkParticle explosion
   ) {
      if (explosion == null) {
         info.cancel();
      }
   }
}
