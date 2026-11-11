package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.RegistryAccess.Frozen;
import net.minecraft.network.chat.LastSeenMessagesTracker;
import net.minecraft.network.chat.SignedMessageChain.Encoder;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ClientPacketListener.class})
public interface ClientPlayNetworkHandlerAccessor {
   @Accessor("serverChunkRadius:I")
   int getChunkLoadDistance();

   @Accessor("signedMessageEncoder:Lnet/minecraft/network/chat/SignedMessageChain$Encoder;")
   Encoder getMessagePacker();

   @Accessor("lastSeenMessages:Lnet/minecraft/network/chat/LastSeenMessagesTracker;")
   LastSeenMessagesTracker getLastSeenMessagesCollector();

   @Accessor("registryAccess:Lnet/minecraft/core/RegistryAccess$Frozen;")
   Frozen getCombinedDynamicRegistries();

   @Accessor("enabledFeatures:Lnet/minecraft/world/flag/FeatureFlagSet;")
   FeatureFlagSet getEnabledFeatures();
}
