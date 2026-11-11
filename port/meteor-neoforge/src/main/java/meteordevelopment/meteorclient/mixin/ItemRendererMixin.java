package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.TransparentBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin({ItemRenderer.class})
public abstract class ItemRendererMixin {
   @ModifyArgs(
      method = {"Lnet/minecraft/client/renderer/entity/ItemRenderer;render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderModelLists(Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/item/ItemStack;IILcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V"
      )
   )
   private void modifyEnchant(
      Args args,
      ItemStack stack,
      ItemDisplayContext renderMode,
      boolean leftHanded,
      PoseStack matrices,
      MultiBufferSource vertexConsumers,
      int light,
      int overlay,
      BakedModel model
   ) {
      if (Modules.get().get(NoRender.class).noEnchantGlint()) {
         boolean bl = renderMode == ItemDisplayContext.GUI
            || renderMode.firstPerson()
            || !(stack.getItem() instanceof BlockItem blockItem)
            || !(blockItem.getBlock() instanceof TransparentBlock) && !(blockItem.getBlock() instanceof StainedGlassPaneBlock);
         args.set(5, vertexConsumers.getBuffer(ItemBlockRenderTypes.getRenderType(stack, bl)));
      }
   }
}
