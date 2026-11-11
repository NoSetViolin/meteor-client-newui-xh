package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ScreenEffectRenderer.class})
public abstract class InGameOverlayRendererMixin {
   @Inject(
      method = {"Lnet/minecraft/client/renderer/ScreenEffectRenderer;renderFire(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void onRenderFireOverlay(Minecraft minecraftClient, PoseStack matrixStack, CallbackInfo info) {
      if (Modules.get().get(NoRender.class).noFireOverlay()) {
         info.cancel();
      }
   }

   @Inject(
      method = {"Lnet/minecraft/client/renderer/ScreenEffectRenderer;renderWater(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void onRenderUnderwaterOverlay(Minecraft minecraftClient, PoseStack matrixStack, CallbackInfo info) {
      if (Modules.get().get(NoRender.class).noLiquidOverlay()) {
         info.cancel();
      }
   }

   @Inject(
      method = {"Lnet/minecraft/client/renderer/ScreenEffectRenderer;renderTex(Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lcom/mojang/blaze3d/vertex/PoseStack;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void render(TextureAtlasSprite sprite, PoseStack matrices, CallbackInfo info) {
      if (Modules.get().get(NoRender.class).noInWallOverlay()) {
         info.cancel();
      }
   }
}
