package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.FinishUsingItemEvent;
import meteordevelopment.meteorclient.events.entity.player.StoppedUsingItemEvent;
import meteordevelopment.meteorclient.events.game.ItemStackTooltipEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ItemStack.class})
public abstract class ItemStackMixin {
   @ModifyReturnValue(
      method = {"Lnet/minecraft/world/item/ItemStack;getTooltipLines(Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/TooltipFlag;)Ljava/util/List;"},
      at = {@At("RETURN")}
   )
   private List<Component> onGetTooltip(List<Component> original) {
      if (Utils.canUpdate()) {
         ItemStackTooltipEvent event = MeteorClient.EVENT_BUS.post(new ItemStackTooltipEvent((ItemStack)(Object)this, original));
         return event.list();
      } else {
         return original;
      }
   }

   @ModifyExpressionValue(
      method = {"Lnet/minecraft/world/item/ItemStack;getTooltipLines(Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/TooltipFlag;)Ljava/util/List;"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/item/AdventureModePredicate;showInTooltip()Z",
         ordinal = 0
      )}
   )
   private boolean modifyCanBreakText(boolean original) {
      BetterTooltips bt = Modules.get().get(BetterTooltips.class);
      return bt.isActive() && bt.canDestroy.get() || original;
   }

   @ModifyExpressionValue(
      method = {"Lnet/minecraft/world/item/ItemStack;getTooltipLines(Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/TooltipFlag;)Ljava/util/List;"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/item/AdventureModePredicate;showInTooltip()Z",
         ordinal = 1
      )}
   )
   private boolean modifyCanPlaceText(boolean original) {
      BetterTooltips bt = Modules.get().get(BetterTooltips.class);
      return bt.isActive() && bt.canPlaceOn.get() || original;
   }

   @ModifyExpressionValue(
      method = {"Lnet/minecraft/world/item/ItemStack;getTooltipLines(Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/TooltipFlag;)Ljava/util/List;"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/item/ItemStack;has(Lnet/minecraft/core/component/DataComponentType;)Z",
         ordinal = 0
      )}
   )
   private boolean modifyContainsTooltip(boolean original) {
      BetterTooltips bt = Modules.get().get(BetterTooltips.class);
      return (!bt.isActive() || !bt.tooltip.get()) && original;
   }

   @ModifyExpressionValue(
      method = {"Lnet/minecraft/world/item/ItemStack;getTooltipLines(Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/TooltipFlag;)Ljava/util/List;"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/item/ItemStack;has(Lnet/minecraft/core/component/DataComponentType;)Z",
         ordinal = 3
      )}
   )
   private boolean modifyContainsAdditional(boolean original) {
      BetterTooltips bt = Modules.get().get(BetterTooltips.class);
      return (!bt.isActive() || !bt.additional.get()) && original;
   }

   @Inject(
      method = {"Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"},
      at = {@At("HEAD")}
   )
   private void onFinishUsing(Level world, LivingEntity user, CallbackInfoReturnable<ItemStack> info) {
      if (user == MeteorClient.mc.player) {
         MeteorClient.EVENT_BUS.post(FinishUsingItemEvent.get((ItemStack)(Object)this));
      }
   }

   @Inject(
      method = {"Lnet/minecraft/world/item/ItemStack;releaseUsing(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)V"},
      at = {@At("HEAD")}
   )
   private void onStoppedUsing(Level world, LivingEntity user, int remainingUseTicks, CallbackInfo info) {
      if (user == MeteorClient.mc.player) {
         MeteorClient.EVENT_BUS.post(StoppedUsingItemEvent.get((ItemStack)(Object)this));
      }
   }

   @ModifyExpressionValue(
      method = {"Lnet/minecraft/world/item/ItemStack;addAttributeTooltips(Ljava/util/function/Consumer;Lnet/minecraft/world/entity/player/Player;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/item/component/ItemAttributeModifiers;showInTooltip()Z"
      )}
   )
   private boolean modifyShowInTooltip(boolean original) {
      BetterTooltips bt = Modules.get().get(BetterTooltips.class);
      return bt.isActive() && bt.modifiers.get() || original;
   }
}

