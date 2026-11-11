package meteordevelopment.meteorclient.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;

/**
 * 原字节码在 Vineflower 中反编译失败（NPE），此实现按上游 v0.5.8 源码重写，
 * 仅把 NBT 类型名换成 mojmap（NbtCompound -&gt; CompoundTag）。
 */
public class EnumSetting<T extends Enum<?>> extends Setting<T> {
   private final T[] values;
   private final List<String> suggestions;

   @SuppressWarnings("unchecked")
   public EnumSetting(String name, String description, T defaultValue, Consumer<T> onChanged, Consumer<Setting<T>> onModuleActivated, IVisible visible) {
      super(name, description, defaultValue, onChanged, onModuleActivated, visible);

      this.values = (T[]) defaultValue.getDeclaringClass().getEnumConstants();
      this.suggestions = new ArrayList<>(this.values.length);
      for (T value : this.values) this.suggestions.add(value.toString());
   }

   @Override
   protected T parseImpl(String str) {
      for (T possibleValue : this.values) {
         if (str.equalsIgnoreCase(possibleValue.toString())) return possibleValue;
      }

      return null;
   }

   @Override
   protected boolean isValueValid(T value) {
      return true;
   }

   @Override
   public List<String> getSuggestions() {
      return this.suggestions;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      tag.putString("value", this.get().toString());
      return tag;
   }

   @Override
   public T load(CompoundTag tag) {
      this.parse(tag.getString("value"));
      return this.get();
   }

   public static class Builder<T extends Enum<?>> extends SettingBuilder<Builder<T>, T, EnumSetting<T>> {
      public Builder() {
         super(null);
      }

      @Override
      public EnumSetting<T> build() {
         return new EnumSetting<>(this.name, this.description, this.defaultValue, this.onChanged, this.onModuleActivated, this.visible);
      }
   }
}
