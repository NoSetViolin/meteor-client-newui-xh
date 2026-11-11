package meteordevelopment.meteorclient.mixin.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.neoforge.render.FluidRendererImpl", remap = false)
public abstract class SodiumFluidRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void meteor$hideFluid(CallbackInfo ci, @Local(argsOnly = true) FluidState fluid,
                                  @Local(argsOnly = true, ordinal = 0) BlockPos pos) {
        if (Modules.get() != null && Xray.getAlpha(fluid.createLegacyBlock(), pos) == 0) ci.cancel();
    }
}
