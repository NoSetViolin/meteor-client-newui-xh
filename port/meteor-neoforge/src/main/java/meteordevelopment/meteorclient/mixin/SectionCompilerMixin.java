package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** NeoForge section compilation uses model render-type sets instead of getChunkRenderType. */
@Mixin(SectionCompiler.class)
public abstract class SectionCompilerMixin {
    @WrapOperation(method = "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/BakedModel;getRenderTypes(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;Lnet/neoforged/neoforge/client/model/data/ModelData;)Lnet/neoforged/neoforge/client/ChunkRenderTypeSet;"))
    private ChunkRenderTypeSet meteor$renderLayers(BakedModel model, BlockState state, RandomSource random, ModelData data, Operation<ChunkRenderTypeSet> original) {
        ChunkRenderTypeSet layers = original.call(model, state, random, data);
        int alpha = Xray.getAlpha(state, null);
        return alpha > 0 && alpha < 255 ? ChunkRenderTypeSet.of(RenderType.translucent()) : layers;
    }
}
