package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import net.minecraft.world.item.BundleItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({BundleItem.class})
public abstract class BundleItemMixin {
   @ModifyExpressionValue(
      method = {"Lnet/minecraft/world/item/BundleItem;getTooltipImage(Lnet/minecraft/world/item/ItemStack;)Ljava/util/Optional;"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/item/ItemStack;has(Lnet/minecraft/core/component/DataComponentType;)Z",
         ordinal = 0
      )}
   )
   private boolean modifyContains1(boolean original) {
      BetterTooltips bt = Modules.get().get(BetterTooltips.class);
      return (!bt.isActive() || !bt.tooltip.get()) && original;
   }

   @ModifyExpressionValue(
      method = {"Lnet/minecraft/world/item/BundleItem;getTooltipImage(Lnet/minecraft/world/item/ItemStack;)Ljava/util/Optional;"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/item/ItemStack;has(Lnet/minecraft/core/component/DataComponentType;)Z",
         ordinal = 1
      )}
   )
   private boolean modifyContains2(boolean original) {
      BetterTooltips bt = Modules.get().get(BetterTooltips.class);
      return (!bt.isActive() || !bt.additional.get()) && original;
   }
}
