package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import net.minecraft.world.item.armortrim.ArmorTrim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({ArmorTrim.class})
public abstract class ArmorTrimMixin {
   @ModifyExpressionValue(
      method = {"Lnet/minecraft/world/item/armortrim/ArmorTrim;addToTooltip(Lnet/minecraft/world/item/Item$TooltipContext;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/world/item/armortrim/ArmorTrim;showInTooltip:Z"
      )}
   )
   private boolean modifyShowInTooltip(boolean original) {
      BetterTooltips bt = Modules.get().get(BetterTooltips.class);
      return bt.isActive() && bt.upgrades.get() || original;
   }
}
