package meteordevelopment.meteorclient.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Fullbright;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess", remap = false)
public abstract class SodiumLightDataAccessMixin {
    @Shadow protected BlockAndTintGetter level;
    @Shadow @Final private BlockPos.MutableBlockPos pos;

    @ModifyReturnValue(method = "compute(III)I", at = @At("RETURN"))
    private int meteor$oreLight(int original) {
        Modules modules = Modules.get();
        if (modules == null) return original;
        Xray xray = modules.get(Xray.class);
        if (xray.isActive() && !xray.isBlocked(level.getBlockState(pos).getBlock(), pos)) {
            // Sodium 0.8 packs block/sky/emitted light into the low three nibbles.
            // Keep its geometry/occlusion flags and ambient-occlusion value intact.
            return original | 0xfff;
        }
        Fullbright fullbright = modules.get(Fullbright.class);
        int block = Math.max(original & 15, fullbright.getLuminance(LightLayer.BLOCK));
        int sky = Math.max((original >>> 4) & 15, fullbright.getLuminance(LightLayer.SKY));
        return (original & ~0xff) | block | (sky << 4);
    }
}
