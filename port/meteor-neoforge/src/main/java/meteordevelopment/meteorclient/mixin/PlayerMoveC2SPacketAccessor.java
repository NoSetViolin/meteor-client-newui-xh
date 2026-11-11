package meteordevelopment.meteorclient.mixin;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ServerboundMovePlayerPacket.class})
public interface PlayerMoveC2SPacketAccessor {
   @Mutable
   @Accessor("y:D")
   void setY(double var1);

   @Mutable
   @Accessor("onGround:Z")
   void setOnGround(boolean var1);
}
