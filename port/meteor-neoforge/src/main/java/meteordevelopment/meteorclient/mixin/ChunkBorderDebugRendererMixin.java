package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.debug.ChunkBorderRenderer;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({ChunkBorderRenderer.class})
public abstract class ChunkBorderDebugRendererMixin {
   @Shadow
   @Final
   private Minecraft minecraft;

   @ModifyExpressionValue(
      method = {"Lnet/minecraft/client/renderer/debug/ChunkBorderRenderer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;DDD)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;chunkPosition()Lnet/minecraft/world/level/ChunkPos;"
      )}
   )
   private ChunkPos render$getChunkPos(ChunkPos chunkPos) {
      Freecam freecam = Modules.get().get(Freecam.class);
      if (!freecam.isActive()) {
         return chunkPos;
      } else {
         float delta = this.minecraft.getTimer().getGameTimeDeltaPartialTick(true);
         return new ChunkPos(SectionPos.blockToSectionCoord(Mth.floor(freecam.getX(delta))), SectionPos.blockToSectionCoord(Mth.floor(freecam.getZ(delta))));
      }
   }
}
