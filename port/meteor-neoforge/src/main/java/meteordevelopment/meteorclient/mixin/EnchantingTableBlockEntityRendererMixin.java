package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({EnchantTableRenderer.class})
public abstract class EnchantingTableBlockEntityRendererMixin {
   @WrapWithCondition(
      method = {"Lnet/minecraft/client/renderer/blockentity/EnchantTableRenderer;render(Lnet/minecraft/world/level/block/entity/EnchantingTableBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/model/BookModel;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"
      )}
   )
   private boolean onRenderBookModelRenderProxy(BookModel instance, PoseStack matrices, VertexConsumer vertices, int light, int overlay, int i) {
      return !Modules.get().get(NoRender.class).noEnchTableBook();
   }
}
