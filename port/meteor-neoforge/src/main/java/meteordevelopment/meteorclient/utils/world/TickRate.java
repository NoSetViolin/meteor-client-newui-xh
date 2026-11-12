package meteordevelopment.meteorclient.utils.world;

import java.util.Arrays;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Mth;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.neoforged.neoforge.network.payload.ClientboundCustomSetTimePayload;

public class TickRate {
   public static TickRate INSTANCE = new TickRate();
   private final float[] tickRates = new float[20];
   private int nextIndex = 0;
   private long timeLastTimeUpdate = -1L;
   private long timeGameJoined;

   private TickRate() {
      MeteorClient.EVENT_BUS.subscribe(this);
   }

   /** Start listening before the first world joins, even when the editable HUD is disabled. */
   @PostInit
   public static void init() {
   }

   @EventHandler
   private void onReceivePacket(PacketEvent.Receive event) {
      // NeoForge 21.1.265 uses an extended time payload instead of the vanilla packet.
      if (event.packet instanceof ClientboundSetTimePacket
          || event.packet instanceof ClientboundCustomPayloadPacket packet && packet.payload() instanceof ClientboundCustomSetTimePayload) {
         long now = System.currentTimeMillis();
         if (this.timeLastTimeUpdate < 0) {
            this.timeLastTimeUpdate = now;
            return;
         }
         float timeElapsed = (float)(now - this.timeLastTimeUpdate) / 1000.0F;
         this.tickRates[this.nextIndex] = Mth.clamp(20.0F / timeElapsed, 0.0F, 20.0F);
         this.nextIndex = (this.nextIndex + 1) % this.tickRates.length;
         this.timeLastTimeUpdate = now;
      }
   }

   @EventHandler
   private void onGameJoined(GameJoinedEvent event) {
      Arrays.fill(this.tickRates, 0.0F);
      this.nextIndex = 0;
      this.timeGameJoined = this.timeLastTimeUpdate = System.currentTimeMillis();
   }

   public float getTickRate() {
      if (!Utils.canUpdate()) {
         return 0.0F;
      } else if (System.currentTimeMillis() - this.timeGameJoined < 4000L) {
         return 20.0F;
      } else {
         int numTicks = 0;
         float sumTickRates = 0.0F;

         for (float tickRate : this.tickRates) {
            if (tickRate > 0.0F) {
               sumTickRates += tickRate;
               numTicks++;
            }
         }

         return numTicks == 0 ? 0.0F : sumTickRates / (float)numTicks;
      }
   }

   public float getTimeSinceLastTick() {
      long now = System.currentTimeMillis();
      return now - this.timeGameJoined < 4000L ? 0.0F : (float)(now - this.timeLastTimeUpdate) / 1000.0F;
   }
}
