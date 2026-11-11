package meteordevelopment.meteorclient;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * NeoForge 版 Mixin 插件。
 *
 * <p>与 Fabric 版的差异：删除了通过反射替换 Fabric 类加载器 mixinTransformer 的整段逻辑
 * （原逻辑用于挂载 Meteor 自研 ASM Transformer，该功能在 NeoForge 版改用 mixin 实现），
 * mod 是否加载改由 {@link Platform#isModLoaded(String)} 查询。</p>
 */
public class MixinPlugin implements IMixinConfigPlugin {
   private static final String mixinPackage = "meteordevelopment.meteorclient.mixin";

   private static boolean loaded;

   private static boolean isOriginsPresent;
   private static boolean isIndigoPresent;
   public static boolean isSodiumPresent;
   private static boolean isLithiumPresent;
   public static boolean isIrisPresent;
   private static boolean isVFPPresent;

   @Override
   public void onLoad(String mixinPackage) {
      // NeoForge 在选择 mixin 配置时 ModList 尚未初始化，探测推迟到 shouldApplyMixin 首次调用
   }

   private static void ensureProbed() {
      if (loaded) return;

      isIndigoPresent = Platform.isModLoaded("fabric-renderer-indigo");
      isOriginsPresent = Platform.isModLoaded("origins");
      isSodiumPresent = Platform.isModLoaded("sodium");
      isLithiumPresent = Platform.isModLoaded("lithium");
      isIrisPresent = Platform.isModLoaded("iris");
      isVFPPresent = Platform.isModLoaded("viafabricplus");

      loaded = true;
   }

   /** Iris is optional. Resolve its public API lazily, after mod discovery. */
   public static boolean isIrisShaderPackInUse() {
      if (!Platform.isModLoaded("iris")) return false;
      try {
         return (boolean) IrisApiBridge.query.invoke(IrisApiBridge.api);
      } catch (ReflectiveOperationException e) {
         throw new IllegalStateException("Cannot query Iris shader-pack state", e);
      }
   }

   private static final class IrisApiBridge {
      static final Object api;
      static final java.lang.reflect.Method query;
      static {
         try {
            Class<?> apiType = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            api = apiType.getMethod("getInstance").invoke(null);
            query = apiType.getMethod("isShaderPackInUse");
         } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unsupported Iris API", e);
         }
      }
   }

   @Override
   public String getRefMapperConfig() {
      return null;
   }

   @Override
   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      ensureProbed();

      if (mixinClassName.startsWith(mixinPackage + ".baritone.")) {
         return Platform.isModLoaded("baritone");
      }

      if (!mixinClassName.startsWith(mixinPackage)) {
         throw new RuntimeException("Mixin " + mixinClassName + " is not in the mixin package");
      } else if (mixinClassName.endsWith("PlayerEntityRendererMixin")) {
         return !isOriginsPresent;
      } else if (mixinClassName.startsWith(mixinPackage + ".sodium")) {
         return isSodiumPresent;
      } else if (mixinClassName.startsWith(mixinPackage + ".indigo")) {
         return isIndigoPresent;
      } else if (mixinClassName.startsWith(mixinPackage + ".lithium")) {
         return isLithiumPresent;
      } else if (mixinClassName.startsWith(mixinPackage + ".viafabricplus")) {
         return isVFPPresent;
      }

      return true;
   }

   @Override
   public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
   }

   @Override
   public List<String> getMixins() {
      return null;
   }

   @Override
   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   @Override
   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }
}



