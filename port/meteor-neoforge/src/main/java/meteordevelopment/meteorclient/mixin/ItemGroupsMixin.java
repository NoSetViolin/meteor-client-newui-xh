package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({CreativeModeTabs.class})
public abstract class ItemGroupsMixin {
   @ModifyReturnValue(
      method = {"Lnet/minecraft/world/item/CreativeModeTabs;tryRebuildTabContents(Lnet/minecraft/world/flag/FeatureFlagSet;ZLnet/minecraft/core/HolderLookup$Provider;)Z"},
      at = {@At("RETURN")}
   )
   private static boolean modifyReturn(boolean original) {
      return original || Modules.get().get(BetterTooltips.class).updateTooltips();
   }
}
