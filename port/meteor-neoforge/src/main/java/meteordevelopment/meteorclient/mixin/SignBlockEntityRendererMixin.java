package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({SignRenderer.class})
public abstract class SignBlockEntityRendererMixin {
   @ModifyExpressionValue(
      method = {"Lnet/minecraft/client/renderer/blockentity/SignRenderer;renderSignText(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/SignText;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IIIZ)V"},
      at = {@At(
         value = "CONSTANT",
         args = {"intValue=4", "ordinal=1"}
      )}
   )
   private int loopTextLengthProxy(int i) {
      return Modules.get().get(NoRender.class).noSignText() ? 0 : i;
   }
}
