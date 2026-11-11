package meteordevelopment.meteorclient.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({EntitySectionStorage.class})
public interface SectionedEntityCacheAccessor {
   @Accessor("sectionIds:Lit/unimi/dsi/fastutil/longs/LongSortedSet;")
   LongSortedSet getTrackedPositions();

   @Accessor("sections:Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;")
   <T extends EntityAccess> Long2ObjectMap<EntitySection<T>> getTrackingSections();
}
