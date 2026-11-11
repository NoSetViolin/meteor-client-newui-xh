package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererFovAccessor {
    @Invoker("getFov")
    double meteor$getFov(Camera camera, float partialTick, boolean useSetting);
}
