package meteordevelopment.meteorclient.mixin;

import com.google.common.base.MoreObjects;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.ArmRenderEvent;
import meteordevelopment.meteorclient.events.render.HeldItemRendererEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.HandView;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemInHandRenderer.class})
public abstract class HeldItemRendererMixin {
   @Shadow
   private float mainHandHeight;
   @Shadow
   private float offHandHeight;
   @Shadow
   private ItemStack mainHandItem;
   @Shadow
   private ItemStack offHandItem;

   @ModifyVariable(
      method = {"Lnet/minecraft/client/renderer/ItemInHandRenderer;renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/player/LocalPlayer;I)V"},
      at = @At(
         value = "STORE",
         ordinal = 0
      ),
      index = 6
   )
   private float modifySwing(float swingProgress) {
      HandView module = Modules.get().get(HandView.class);
      InteractionHand hand = (InteractionHand)MoreObjects.firstNonNull(MeteorClient.mc.player.swingingArm, InteractionHand.MAIN_HAND);
      if (module.isActive()) {
         if (hand == InteractionHand.OFF_HAND && !MeteorClient.mc.player.getOffhandItem().isEmpty()) {
            return swingProgress + module.offSwing.get().floatValue();
         }

         if (hand == InteractionHand.MAIN_HAND && !MeteorClient.mc.player.getMainHandItem().isEmpty()) {
            return swingProgress + module.mainSwing.get().floatValue();
         }
      }

      return swingProgress;
   }

   @Redirect(
      method = {"Lnet/minecraft/client/renderer/ItemInHandRenderer;tick()V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/item/ItemStack;matches(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"
      )
   )
   private boolean redirectSwapping(ItemStack left, ItemStack right) {
      return this.showSwapping(left, right);
   }

   @ModifyArg(
      method = {"Lnet/minecraft/client/renderer/ItemInHandRenderer;tick()V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/Mth;clamp(FFF)F",
         ordinal = 2
      ),
      index = 0
   )
   private float modifyEquipProgressMainhand(float value) {
      float f = MeteorClient.mc.player.getAttackStrengthScale(1.0F);
      float modified = Modules.get().get(HandView.class).oldAnimations() ? 1.0F : f * f * f;
      return (this.showSwapping(this.mainHandItem, MeteorClient.mc.player.getMainHandItem()) ? modified : 0.0F) - this.mainHandHeight;
   }

   @ModifyArg(
      method = {"Lnet/minecraft/client/renderer/ItemInHandRenderer;tick()V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/Mth;clamp(FFF)F",
         ordinal = 3
      ),
      index = 0
   )
   private float modifyEquipProgressOffhand(float value) {
      return (float)(this.showSwapping(this.offHandItem, MeteorClient.mc.player.getOffhandItem()) ? 1 : 0) - this.offHandHeight;
   }

   @Inject(
      method = {"Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
      )}
   )
   private void onRenderItem(
      AbstractClientPlayer player,
      float tickDelta,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      PoseStack matrices,
      MultiBufferSource vertexConsumers,
      int light,
      CallbackInfo ci
   ) {
      MeteorClient.EVENT_BUS.post(HeldItemRendererEvent.get(hand, matrices));
   }

   @Inject(
      method = {"Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderPlayerArm(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IFFLnet/minecraft/world/entity/HumanoidArm;)V"
      )}
   )
   private void onRenderArm(
      AbstractClientPlayer player,
      float tickDelta,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      PoseStack matrices,
      MultiBufferSource vertexConsumers,
      int light,
      CallbackInfo ci
   ) {
      MeteorClient.EVENT_BUS.post(ArmRenderEvent.get(hand, matrices));
   }

   @Inject(
      method = {"Lnet/minecraft/client/renderer/ItemInHandRenderer;applyEatTransform(Lcom/mojang/blaze3d/vertex/PoseStack;FLnet/minecraft/world/entity/HumanoidArm;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/lang/Math;pow(DD)D",
         shift = Shift.BEFORE
      )},
      cancellable = true
   )
   private void cancelTransformations(PoseStack matrices, float tickDelta, HumanoidArm arm, ItemStack stack, Player player, CallbackInfo ci) {
      if (Modules.get().get(HandView.class).disableFoodAnimation()) {
         ci.cancel();
      }
   }

   @Unique
   private boolean showSwapping(ItemStack stack1, ItemStack stack2) {
      return !Modules.get().get(HandView.class).showSwapping() || ItemStack.matches(stack1, stack2);
   }
}
