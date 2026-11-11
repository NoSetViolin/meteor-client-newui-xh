package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Collisions;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({WorldBorder.class})
public abstract class WorldBorderMixin {
   @Inject(
      method = {"Lnet/minecraft/world/level/border/WorldBorder;isInsideCloseToBorder(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void canCollide(CallbackInfoReturnable<Boolean> info) {
      if (Modules.get().get(Collisions.class).ignoreBorder()) {
         info.setReturnValue(false);
      }
   }

   @Inject(
      method = {"Lnet/minecraft/world/level/border/WorldBorder;isWithinBounds(Lnet/minecraft/core/BlockPos;)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void contains(CallbackInfoReturnable<Boolean> info) {
      if (Modules.get().get(Collisions.class).ignoreBorder()) {
         info.setReturnValue(true);
      }
   }
}
