package meteordevelopment.meteorclient.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.MobBucketItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({MobBucketItem.class})
public interface EntityBucketItemAccessor {
   @Accessor("type:Lnet/minecraft/world/entity/EntityType;")
   EntityType<?> getEntityType();
}
