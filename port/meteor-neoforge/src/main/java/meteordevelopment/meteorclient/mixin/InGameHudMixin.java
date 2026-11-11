package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin({Gui.class})
public abstract class InGameHudMixin {
   @Shadow
   @Final
   private Minecraft minecraft;

   @Shadow
   public abstract void onDisconnected();

   @Inject(
      method = {"Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"},
      at = {@At("TAIL")}
   )
   private void onRender(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
      this.minecraft.getProfiler().push("meteor-client_render_2d");
      Utils.unscaledProjection();
      MeteorClient.EVENT_BUS.post(Render2DEvent.get(context, context.guiWidth(), context.guiWidth(), tickCounter.getGameTimeDeltaPartialTick(true)));
      Utils.scaledProjection();
      RenderSystem.applyModelViewMatrix();
      this.minecraft.getProfiler().pop();
   }

   @Inject(
      method = {"Lnet/minecraft/client/gui/Gui;renderEffects(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderStatusEffectOverlay(CallbackInfo info) {
      if (Modules.get().get(NoRender.class).noPotionIcons()) {
         info.cancel();
      }
   }

   @Inject(
      method = {"Lnet/minecraft/client/gui/Gui;renderPortalOverlay(Lnet/minecraft/client/gui/GuiGraphics;F)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderPortalOverlay(GuiGraphics context, float nauseaStrength, CallbackInfo ci) {
      if (Modules.get().get(NoRender.class).noPortalOverlay()) {
         ci.cancel();
      }
   }

   @ModifyArgs(
      method = {"Lnet/minecraft/client/gui/Gui;renderCameraOverlays(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/Gui;renderTextureOverlay(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;F)V",
         ordinal = 0
      )
   )
   private void onRenderPumpkinOverlay(Args args) {
      if (Modules.get().get(NoRender.class).noPumpkinOverlay()) {
         args.set(2, 0.0F);
      }
   }

   @ModifyArgs(
      method = {"Lnet/minecraft/client/gui/Gui;renderCameraOverlays(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/Gui;renderTextureOverlay(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;F)V",
         ordinal = 1
      )
   )
   private void onRenderPowderedSnowOverlay(Args args) {
      if (Modules.get().get(NoRender.class).noPowderedSnowOverlay()) {
         args.set(2, 0.0F);
      }
   }

   @Inject(
      method = {"Lnet/minecraft/client/gui/Gui;renderVignette(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/Entity;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderVignetteOverlay(GuiGraphics context, Entity entity, CallbackInfo ci) {
      if (Modules.get().get(NoRender.class).noVignette()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"Lnet/minecraft/client/gui/Gui;displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderScoreboardSidebar(GuiGraphics context, Objective objective, CallbackInfo ci) {
      if (Modules.get().get(NoRender.class).noScoreboard()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"Lnet/minecraft/client/gui/Gui;renderScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderScoreboardSidebar(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
      if (Modules.get().get(NoRender.class).noScoreboard()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"Lnet/minecraft/client/gui/Gui;renderSpyglassOverlay(Lnet/minecraft/client/gui/GuiGraphics;F)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderSpyglassOverlay(GuiGraphics context, float scale, CallbackInfo ci) {
      if (Modules.get().get(NoRender.class).noSpyglassOverlay()) {
         ci.cancel();
      }
   }

   @ModifyExpressionValue(
      method = {"Lnet/minecraft/client/gui/Gui;renderCrosshair(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z"
      )}
   )
   private boolean alwaysRenderCrosshairInFreecam(boolean firstPerson) {
      return Modules.get().isActive(Freecam.class) || firstPerson;
   }

   @Inject(
      method = {"Lnet/minecraft/client/gui/Gui;renderCrosshair(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderCrosshair(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
      if (Modules.get().get(NoRender.class).noCrosshair()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"Lnet/minecraft/client/gui/Gui;renderTitle(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderTitle(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
      if (Modules.get().get(NoRender.class).noTitle()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"Lnet/minecraft/client/gui/Gui;renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderHeldItemTooltip(GuiGraphics context, CallbackInfo ci) {
      if (Modules.get().get(NoRender.class).noHeldItemName()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"Lnet/minecraft/client/gui/Gui;onDisconnected()V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/components/ChatComponent;clearMessages(Z)V"
      )},
      cancellable = true
   )
   private void onClear(CallbackInfo info) {
      if (Modules.get().get(BetterChat.class).keepHistory()) {
         info.cancel();
      }
   }
}
