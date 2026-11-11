package meteordevelopment.meteorclient.mixin;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({LivingEntity.class})
public interface LivingEntityAccessor {
   @Invoker("jumpInLiquid(Lnet/minecraft/tags/TagKey;)V")
   void swimUpwards(TagKey<Fluid> var1);

   @Accessor("jumping:Z")
   boolean isJumping();

   @Accessor("noJumpDelay:I")
   int getJumpCooldown();

   @Accessor("noJumpDelay:I")
   void setJumpCooldown(int var1);
}
