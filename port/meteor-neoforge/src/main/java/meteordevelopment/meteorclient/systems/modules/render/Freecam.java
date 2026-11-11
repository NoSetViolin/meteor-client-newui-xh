package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.entity.DamageEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.meteor.MouseScrollEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.ChunkOcclusionEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.GUIMove;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class Freecam extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> speed = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("speed")
            .description("Your speed while in freecam.")
            .onChanged(aDouble -> this.speedValue = aDouble)
            .defaultValue(1.0)
            .min(0.0)
            .build()
      );
   private final Setting<Double> speedScrollSensitivity = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("speed-scroll-sensitivity")
            .description("Allows you to change speed value using scroll wheel. 0 to disable.")
            .defaultValue(0.0)
            .min(0.0)
            .sliderMax(2.0)
            .build()
      );
   private final Setting<Boolean> toggleOnDamage = this.sgGeneral
      .add(
         new BoolSetting.Builder().name("toggle-on-damage").description("Disables freecam when you take damage.").defaultValue(Boolean.valueOf(false)).build()
      );
   private final Setting<Boolean> toggleOnDeath = this.sgGeneral
      .add(new BoolSetting.Builder().name("toggle-on-death").description("Disables freecam when you die.").defaultValue(Boolean.valueOf(false)).build());
   private final Setting<Boolean> toggleOnLog = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("toggle-on-log")
            .description("Disables freecam when you disconnect from a server.")
            .defaultValue(Boolean.valueOf(true))
            .build()
      );
   private final Setting<Boolean> reloadChunks = this.sgGeneral
      .add(new BoolSetting.Builder().name("reload-chunks").description("Disables cave culling.").defaultValue(Boolean.valueOf(true)).build());
   private final Setting<Boolean> renderHands = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("show-hands")
            .description("Whether or not to render your hands in freecam.")
            .defaultValue(Boolean.valueOf(true))
            .build()
      );
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("rotate")
            .description("Rotates to the block or entity you are looking at.")
            .defaultValue(Boolean.valueOf(false))
            .build()
      );
   private final Setting<Boolean> staticView = this.sgGeneral
      .add(new BoolSetting.Builder().name("static").description("Disables settings that move the view.").defaultValue(Boolean.valueOf(true)).build());
   public final Vector3d pos = new Vector3d();
   public final Vector3d prevPos = new Vector3d();
   private CameraType perspective;
   private double speedValue;
   public float yaw;
   public float pitch;
   public float prevYaw;
   public float prevPitch;
   private double fovScale;
   private boolean bobView;
   private boolean forward;
   private boolean backward;
   private boolean right;
   private boolean left;
   private boolean up;
   private boolean down;

   public Freecam() {
      super(Categories.Render, "freecam", "Allows the camera to move away from the player.");
   }

   @Override
   public void onActivate() {
      this.fovScale = (Double)this.mc.options.fovEffectScale().get();
      this.bobView = (Boolean)this.mc.options.bobView().get();
      if (this.staticView.get()) {
         this.mc.options.fovEffectScale().set(0.0);
         this.mc.options.bobView().set(false);
      }

      this.yaw = this.mc.player.getYRot();
      this.pitch = this.mc.player.getXRot();
      this.perspective = this.mc.options.getCameraType();
      this.speedValue = this.speed.get();
      Utils.set(this.pos, this.mc.gameRenderer.getMainCamera().getPosition());
      Utils.set(this.prevPos, this.mc.gameRenderer.getMainCamera().getPosition());
      if (this.mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT) {
         this.yaw += 180.0F;
         this.pitch *= -1.0F;
      }

      this.prevYaw = this.yaw;
      this.prevPitch = this.pitch;
      this.forward = this.mc.options.keyUp.isDown();
      this.backward = this.mc.options.keyDown.isDown();
      this.right = this.mc.options.keyRight.isDown();
      this.left = this.mc.options.keyLeft.isDown();
      this.up = this.mc.options.keyJump.isDown();
      this.down = this.mc.options.keyShift.isDown();
      this.unpress();
      if (this.reloadChunks.get()) {
         this.mc.levelRenderer.allChanged();
      }
   }

   @Override
   public void onDeactivate() {
      if (this.reloadChunks.get()) {
         this.mc.levelRenderer.allChanged();
      }

      this.mc.options.setCameraType(this.perspective);
      if (this.staticView.get()) {
         this.mc.options.fovEffectScale().set(this.fovScale);
         this.mc.options.bobView().set(this.bobView);
      }
   }

   @EventHandler
   private void onOpenScreen(OpenScreenEvent event) {
      this.unpress();
      this.prevPos.set(this.pos);
      this.prevYaw = this.yaw;
      this.prevPitch = this.pitch;
   }

   private void unpress() {
      this.mc.options.keyUp.setDown(false);
      this.mc.options.keyDown.setDown(false);
      this.mc.options.keyRight.setDown(false);
      this.mc.options.keyLeft.setDown(false);
      this.mc.options.keyJump.setDown(false);
      this.mc.options.keyShift.setDown(false);
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.mc.cameraEntity.isInWall()) {
         this.mc.getCameraEntity().noPhysics = true;
      }

      if (!this.perspective.isFirstPerson()) {
         this.mc.options.setCameraType(CameraType.FIRST_PERSON);
      }

      Vec3 forward = Vec3.directionFromRotation(0.0F, this.yaw);
      Vec3 right = Vec3.directionFromRotation(0.0F, this.yaw + 90.0F);
      double velX = 0.0;
      double velY = 0.0;
      double velZ = 0.0;
      if (this.rotate.get()) {
         if (this.mc.hitResult instanceof EntityHitResult) {
            BlockPos crossHairPos = ((EntityHitResult)this.mc.hitResult).getEntity().blockPosition();
            Rotations.rotate(Rotations.getYaw(crossHairPos), Rotations.getPitch(crossHairPos), 0, null);
         } else {
            Vec3 crossHairPosition = this.mc.hitResult.getLocation();
            BlockPos crossHairPos = ((BlockHitResult)this.mc.hitResult).getBlockPos();
            if (!this.mc.level.getBlockState(crossHairPos).isAir()) {
               Rotations.rotate(Rotations.getYaw(crossHairPosition), Rotations.getPitch(crossHairPosition), 0, null);
            }
         }
      }

      double s = 0.5;
      if (this.mc.options.keySprint.isDown()) {
         s = 1.0;
      }

      boolean a = false;
      if (this.forward) {
         velX += forward.x * s * this.speedValue;
         velZ += forward.z * s * this.speedValue;
         a = true;
      }

      if (this.backward) {
         velX -= forward.x * s * this.speedValue;
         velZ -= forward.z * s * this.speedValue;
         a = true;
      }

      boolean b = false;
      if (this.right) {
         velX += right.x * s * this.speedValue;
         velZ += right.z * s * this.speedValue;
         b = true;
      }

      if (this.left) {
         velX -= right.x * s * this.speedValue;
         velZ -= right.z * s * this.speedValue;
         b = true;
      }

      if (a && b) {
         double diagonal = 1.0 / Math.sqrt(2.0);
         velX *= diagonal;
         velZ *= diagonal;
      }

      if (this.up) {
         velY += s * this.speedValue;
      }

      if (this.down) {
         velY -= s * this.speedValue;
      }

      this.prevPos.set(this.pos);
      this.pos.set(this.pos.x + velX, this.pos.y + velY, this.pos.z + velZ);
   }

   @EventHandler
   public void onKey(KeyEvent event) {
      if (!Input.isKeyPressed(292)) {
         if (!this.checkGuiMove()) {
            boolean cancel = true;
            if (this.mc.options.keyUp.matches(event.key, 0)) {
               this.forward = event.action != KeyAction.Release;
               this.mc.options.keyUp.setDown(false);
            } else if (this.mc.options.keyDown.matches(event.key, 0)) {
               this.backward = event.action != KeyAction.Release;
               this.mc.options.keyDown.setDown(false);
            } else if (this.mc.options.keyRight.matches(event.key, 0)) {
               this.right = event.action != KeyAction.Release;
               this.mc.options.keyRight.setDown(false);
            } else if (this.mc.options.keyLeft.matches(event.key, 0)) {
               this.left = event.action != KeyAction.Release;
               this.mc.options.keyLeft.setDown(false);
            } else if (this.mc.options.keyJump.matches(event.key, 0)) {
               this.up = event.action != KeyAction.Release;
               this.mc.options.keyJump.setDown(false);
            } else if (this.mc.options.keyShift.matches(event.key, 0)) {
               this.down = event.action != KeyAction.Release;
               this.mc.options.keyShift.setDown(false);
            } else {
               cancel = false;
            }

            if (cancel) {
               event.cancel();
            }
         }
      }
   }

   @EventHandler
   private void onMouseButton(MouseButtonEvent event) {
      if (!this.checkGuiMove()) {
         boolean cancel = true;
         if (this.mc.options.keyUp.matchesMouse(event.button)) {
            this.forward = event.action != KeyAction.Release;
            this.mc.options.keyUp.setDown(false);
         } else if (this.mc.options.keyDown.matchesMouse(event.button)) {
            this.backward = event.action != KeyAction.Release;
            this.mc.options.keyDown.setDown(false);
         } else if (this.mc.options.keyRight.matchesMouse(event.button)) {
            this.right = event.action != KeyAction.Release;
            this.mc.options.keyRight.setDown(false);
         } else if (this.mc.options.keyLeft.matchesMouse(event.button)) {
            this.left = event.action != KeyAction.Release;
            this.mc.options.keyLeft.setDown(false);
         } else if (this.mc.options.keyJump.matchesMouse(event.button)) {
            this.up = event.action != KeyAction.Release;
            this.mc.options.keyJump.setDown(false);
         } else if (this.mc.options.keyShift.matchesMouse(event.button)) {
            this.down = event.action != KeyAction.Release;
            this.mc.options.keyShift.setDown(false);
         } else {
            cancel = false;
         }

         if (cancel) {
            event.cancel();
         }
      }
   }

   @EventHandler(
      priority = -100
   )
   private void onMouseScroll(MouseScrollEvent event) {
      if (this.speedScrollSensitivity.get() > 0.0 && this.mc.screen == null) {
         this.speedValue = this.speedValue + event.value * 0.25 * this.speedScrollSensitivity.get() * this.speedValue;
         if (this.speedValue < 0.1) {
            this.speedValue = 0.1;
         }

         event.cancel();
      }
   }

   @EventHandler
   private void onChunkOcclusion(ChunkOcclusionEvent event) {
      event.cancel();
   }

   @EventHandler
   private void onDamage(DamageEvent event) {
      if (event.entity.getUUID() != null) {
         if (event.entity.getUUID().equals(this.mc.player.getUUID())) {
            if (this.toggleOnDamage.get()) {
               this.toggle();
               this.info("Toggled off because you took damage.", new Object[0]);
            }
         }
      }
   }

   @EventHandler
   private void onGameLeft(GameLeftEvent event) {
      if (this.toggleOnLog.get()) {
         this.toggle();
      }
   }

   @EventHandler
   private void onPacketReceive(PacketEvent.Receive event) {
      if (event.packet instanceof ClientboundPlayerCombatKillPacket packet) {
         Entity entity = this.mc.level.getEntity(packet.playerId());
         if (entity == this.mc.player && this.toggleOnDeath.get()) {
            this.toggle();
            this.info("Toggled off because you died.", new Object[0]);
         }
      }
   }

   private boolean checkGuiMove() {
      GUIMove guiMove = Modules.get().get(GUIMove.class);
      return this.mc.screen != null && !guiMove.isActive() ? true : this.mc.screen != null && guiMove.isActive() && guiMove.skip();
   }

   public void changeLookDirection(double deltaX, double deltaY) {
      this.prevYaw = this.yaw;
      this.prevPitch = this.pitch;
      this.yaw = (float)((double)this.yaw + deltaX);
      this.pitch = (float)((double)this.pitch + deltaY);
      this.pitch = Mth.clamp(this.pitch, -90.0F, 90.0F);
   }

   public boolean renderHands() {
      return !this.isActive() || this.renderHands.get();
   }

   public double getX(float tickDelta) {
      return Mth.lerp((double)tickDelta, this.prevPos.x, this.pos.x);
   }

   public double getY(float tickDelta) {
      return Mth.lerp((double)tickDelta, this.prevPos.y, this.pos.y);
   }

   public double getZ(float tickDelta) {
      return Mth.lerp((double)tickDelta, this.prevPos.z, this.pos.z);
   }

   public double getYaw(float tickDelta) {
      return (double)Mth.lerp(tickDelta, this.prevYaw, this.yaw);
   }

   public double getPitch(float tickDelta) {
      return (double)Mth.lerp(tickDelta, this.prevPitch, this.pitch);
   }
}
