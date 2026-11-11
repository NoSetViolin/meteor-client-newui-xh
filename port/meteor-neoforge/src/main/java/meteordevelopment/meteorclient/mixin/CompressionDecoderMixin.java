package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AntiPacketKick;
import net.minecraft.network.CompressionDecoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Bypass only the upper decompressed-size check, then continue vanilla inflation. */
@Mixin(CompressionDecoder.class)
public abstract class CompressionDecoderMixin {
    @ModifyConstant(method = "decode", constant = @Constant(intValue = 8388608))
    private int meteor$maximumSize(int original) {
        Modules modules = Modules.get();
        return modules != null && modules.isActive(AntiPacketKick.class) ? Integer.MAX_VALUE : original;
    }
}
