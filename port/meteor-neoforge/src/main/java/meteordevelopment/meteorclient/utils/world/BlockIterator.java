package meteordevelopment.meteorclient.utils.world;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BlockIterator {
   private static final Pool<BlockIterator.Callback> callbackPool = new Pool<>(BlockIterator.Callback::new);
   private static final List<BlockIterator.Callback> callbacks = new ArrayList<>();
   private static final List<Runnable> afterCallbacks = new ArrayList<>();
   private static final MutableBlockPos blockPos = new MutableBlockPos();
   private static int hRadius;
   private static int vRadius;
   private static boolean disableCurrent;

   private BlockIterator() {
   }

   @PreInit
   public static void init() {
      MeteorClient.EVENT_BUS.subscribe(BlockIterator.class);
   }

   @EventHandler(
      priority = -201
   )
   private static void onTick(TickEvent.Pre event) {
      if (Utils.canUpdate()) {
         int px = MeteorClient.mc.player.getBlockX();
         int py = MeteorClient.mc.player.getBlockY();
         int pz = MeteorClient.mc.player.getBlockZ();

         for (int x = px - hRadius; x <= px + hRadius; x++) {
            for (int z = pz - hRadius; z <= pz + hRadius; z++) {
               for (int y = Math.max(MeteorClient.mc.level.getMinBuildHeight(), py - vRadius);
                  y <= py + vRadius && y <= MeteorClient.mc.level.getMaxBuildHeight();
                  y++
               ) {
                  blockPos.set(x, y, z);
                  BlockState blockState = MeteorClient.mc.level.getBlockState(blockPos);
                  int dx = Math.abs(x - px);
                  int dy = Math.abs(y - py);
                  int dz = Math.abs(z - pz);
                  Iterator<BlockIterator.Callback> it = callbacks.iterator();

                  while (it.hasNext()) {
                     BlockIterator.Callback callback = it.next();
                     if (dx <= callback.hRadius && dy <= callback.vRadius && dz <= callback.hRadius) {
                        disableCurrent = false;
                        callback.function.accept(blockPos, blockState);
                        if (disableCurrent) {
                           it.remove();
                        }
                     }
                  }
               }
            }
         }

         hRadius = 0;
         vRadius = 0;

         for (BlockIterator.Callback callback : callbacks) {
            callbackPool.free(callback);
         }

         callbacks.clear();

         for (Runnable callback : afterCallbacks) {
            callback.run();
         }

         afterCallbacks.clear();
      }
   }

   public static void register(int horizontalRadius, int verticalRadius, BiConsumer<BlockPos, BlockState> function) {
      hRadius = Math.max(hRadius, horizontalRadius);
      vRadius = Math.max(vRadius, verticalRadius);
      BlockIterator.Callback callback = callbackPool.get();
      callback.function = function;
      callback.hRadius = horizontalRadius;
      callback.vRadius = verticalRadius;
      callbacks.add(callback);
   }

   public static void disableCurrent() {
      disableCurrent = true;
   }

   public static void after(Runnable callback) {
      afterCallbacks.add(callback);
   }

   private static class Callback {
      public BiConsumer<BlockPos, BlockState> function;
      public int hRadius;
      public int vRadius;
   }
}
