package meteordevelopment.meteorclient.mixin;

import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ClientboundPlayerPositionPacket.class})
public interface PlayerPositionLookS2CPacketAccessor {
   @Mutable
   @Accessor("yRot:F")
   void setYaw(float var1);

   @Mutable
   @Accessor("xRot:F")
   void setPitch(float var1);
}
