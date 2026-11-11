package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.BoatMoveEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.BoatFly;
import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Boat.class})
public abstract class BoatEntityMixin {
   @Shadow
   private boolean inputLeft;
   @Shadow
   private boolean inputRight;

   @Inject(
      method = {"Lnet/minecraft/world/entity/vehicle/Boat;tick()V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/vehicle/Boat;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"
      )}
   )
   private void onTickInvokeMove(CallbackInfo info) {
      MeteorClient.EVENT_BUS.post(BoatMoveEvent.get((Boat)(Object)this));
   }

   @Redirect(
      method = {"Lnet/minecraft/world/entity/vehicle/Boat;controlBoat()V"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/world/entity/vehicle/Boat;inputLeft:Z"
      )
   )
   private boolean onUpdatePaddlesPressingLeft(Boat boat) {
      return Modules.get().isActive(BoatFly.class) ? false : this.inputLeft;
   }

   @Redirect(
      method = {"Lnet/minecraft/world/entity/vehicle/Boat;controlBoat()V"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/world/entity/vehicle/Boat;inputRight:Z"
      )
   )
   private boolean onUpdatePaddlesPressingRight(Boat boat) {
      return Modules.get().isActive(BoatFly.class) ? false : this.inputRight;
   }
}

