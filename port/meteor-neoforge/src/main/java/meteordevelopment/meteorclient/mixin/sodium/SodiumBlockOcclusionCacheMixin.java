package meteordevelopment.meteorclient.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache", remap = false)
public abstract class SodiumBlockOcclusionCacheMixin {
    @ModifyReturnValue(method = "shouldDrawSide", at = @At("RETURN"))
    private boolean meteor$showOreFace(boolean original, BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        Modules modules = Modules.get();
        if (modules == null) return original;
        Xray xray = modules.get(Xray.class);
        return xray.isActive() ? xray.modifyDrawSide(state, level, pos, side, original) : original;
    }
}
