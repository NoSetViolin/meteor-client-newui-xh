package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({MultiPlayerGameMode.class})
public interface ClientPlayerInteractionManagerAccessor {
   @Accessor("destroyProgress:F")
   float getBreakingProgress();

   @Accessor("destroyProgress:F")
   void setCurrentBreakingProgress(float var1);

   @Accessor("destroyBlockPos:Lnet/minecraft/core/BlockPos;")
   BlockPos getCurrentBreakingBlockPos();
}
