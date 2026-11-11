package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Sneak;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({KeyboardInput.class})
public abstract class KeyboardInputMixin extends Input {
   @Inject(
      method = {"Lnet/minecraft/client/player/KeyboardInput;tick(ZF)V"},
      at = {@At("TAIL")}
   )
   private void isPressed(boolean slowDown, float f, CallbackInfo ci) {
      if (Modules.get().get(Sneak.class).doVanilla()) {
         this.shiftKeyDown = true;
      }
   }
}
