package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.platform.InputConstants.Key;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({KeyMapping.class})
public interface KeyBindingAccessor {
   @Accessor("CATEGORY_SORT_ORDER:Ljava/util/Map;")
   static Map<String, Integer> getCategoryOrderMap() {
      return null;
   }

   @Accessor("key:Lcom/mojang/blaze3d/platform/InputConstants$Key;")
   Key getKey();

   @Accessor("clickCount:I")
   int meteor$getTimesPressed();

   @Accessor("clickCount:I")
   void meteor$setTimesPressed(int var1);
}
