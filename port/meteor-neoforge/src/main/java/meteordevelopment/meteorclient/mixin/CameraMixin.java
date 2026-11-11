package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.ICamera;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.CameraTweaks;
import meteordevelopment.meteorclient.systems.modules.render.ThirdPerson;
import meteordevelopment.meteorclient.systems.modules.render.FreeLook;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.systems.modules.world.HighwayBuilder;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin({Camera.class})
public abstract class CameraMixin implements ICamera {
   @Shadow
   private boolean detached;
   @Shadow
   private float yRot;
   @Shadow
   private float xRot;
   @Unique
   private float tickDelta;

   @Shadow
   protected abstract void setRotation(float var1, float var2);

   @Inject(
      method = {"Lnet/minecraft/client/Camera;getFluidInCamera()Lnet/minecraft/world/level/material/FogType;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void getSubmergedFluidState(CallbackInfoReturnable<FogType> ci) {
      if (Modules.get().get(NoRender.class).noLiquidOverlay()) {
         ci.setReturnValue(FogType.NONE);
      }
   }

   @ModifyVariable(
      method = {"Lnet/minecraft/client/Camera;getMaxZoom(F)F"},
      at = @At("HEAD"),
      ordinal = 0,
      argsOnly = true
   )
   private float modifyClipToSpace(float d) {
      if (Modules.get().isActive(Freecam.class)) return 0;
      ThirdPerson thirdPerson = Modules.get().get(ThirdPerson.class);
      if (thirdPerson.applies()) return (float) (d * thirdPerson.distance.get() / 4.0);
      CameraTweaks tweaks = Modules.get().get(CameraTweaks.class);
      return tweaks.isActive() ? (float) tweaks.getDistance() : d;
   }

   @Inject(
      method = {"Lnet/minecraft/client/Camera;getMaxZoom(F)F"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onClipToSpace(float desiredCameraDistance, CallbackInfoReturnable<Float> info) {
      if (!Modules.get().get(ThirdPerson.class).applies() && Modules.get().get(CameraTweaks.class).clip()) {
         info.setReturnValue(desiredCameraDistance);
      }
   }

   @Inject(
      method = {"Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V"},
      at = {@At("HEAD")}
   )
   private void onUpdateHead(BlockGetter area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo info) {
      this.tickDelta = tickDelta;
   }

   @Inject(
      method = {"Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V"},
      at = {@At("TAIL")}
   )
   private void onUpdateTail(BlockGetter area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo info) {
      if (Modules.get().isActive(Freecam.class)) {
         this.detached = true;
      }
   }

   @ModifyArgs(
      method = {"Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"
      )
   )
   private void onUpdateSetPosArgs(Args args) {
      Freecam freecam = Modules.get().get(Freecam.class);
      if (freecam.isActive()) {
         args.set(0, freecam.getX(this.tickDelta));
         args.set(1, freecam.getY(this.tickDelta));
         args.set(2, freecam.getZ(this.tickDelta));
      } else {
         ThirdPerson thirdPerson = Modules.get().get(ThirdPerson.class);
         if (thirdPerson.applies()) args.set(1, (double) args.get(1) + thirdPerson.height.get());
      }
   }

   @ModifyArgs(
      method = {"Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Camera;setRotation(FF)V"
      )
   )
   private void onUpdateSetRotationArgs(Args args) {
      Freecam freecam = Modules.get().get(Freecam.class);
      FreeLook freeLook = Modules.get().get(FreeLook.class);
      if (freecam.isActive()) {
         args.set(0, (float)freecam.getYaw(this.tickDelta));
         args.set(1, (float)freecam.getPitch(this.tickDelta));
      } else if (Modules.get().isActive(HighwayBuilder.class)) {
         args.set(0, this.yRot);
         args.set(1, this.xRot);
      } else if (freeLook.isActive()) {
         args.set(0, freeLook.cameraYaw);
         args.set(1, freeLook.cameraPitch);
      }
   }

   @Override
   public void setRot(double yaw, double pitch) {
      this.setRotation((float)yaw, (float)Mth.clamp(pitch, -90.0, 90.0));
   }
}
