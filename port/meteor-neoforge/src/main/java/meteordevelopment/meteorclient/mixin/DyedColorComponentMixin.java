package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import net.minecraft.world.item.component.DyedItemColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({DyedItemColor.class})
public abstract class DyedColorComponentMixin {
   @ModifyExpressionValue(
      method = {"Lnet/minecraft/world/item/component/DyedItemColor;addToTooltip(Lnet/minecraft/world/item/Item$TooltipContext;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/world/item/component/DyedItemColor;showInTooltip:Z"
      )}
   )
   private boolean modifyShowInTooltip(boolean original) {
      BetterTooltips bt = Modules.get().get(BetterTooltips.class);
      return bt.isActive() && bt.dye.get() || original;
   }
}
