package meteordevelopment.meteorclient;

import java.nio.file.Path;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforgespi.language.IModInfo;

/**
 * NeoForge 平台适配层。
 *
 * <p>原版 Meteor 直接调用 FabricLoader 的 15 个调用点全部收敛到这里，
 * 其余 900+ 个类保持与平台无关。</p>
 */
public final class Platform {
   public static final String MOD_ID = "meteor_client";  // NeoForge 的 modId 不允许 "-"

   private Platform() {
   }

   /** 游戏目录（.minecraft 根） */
   public static Path gameDir() {
      return FMLPaths.GAMEDIR.get();
   }

   /** 配置目录 */
   public static Path configDir() {
      return FMLPaths.CONFIGDIR.get();
   }

   public static boolean isModLoaded(String id) {
      // mixin 配置选择阶段 ModList 还没初始化，这里必须容错，否则 NPE 会让整份 mixin 配置被丢弃
      ModList list = ModList.get();
      if (list != null) return list.isLoaded(id);
      LoadingModList loading = LoadingModList.get();
      return loading != null && loading.getMods().stream().anyMatch(mod -> mod.getModId().equals(id));
   }

   public static String modName() {
      IModInfo info = modInfo();
      return info == null ? "XiaohSense" : info.getDisplayName();
   }

   public static String modVersion() {
      IModInfo info = modInfo();
      return info == null ? "0.5.8" : info.getVersion().toString();
   }

   private static IModInfo modInfo() {
      return ModList.get().getModContainerById(MOD_ID).map(c -> c.getModInfo()).orElse(null);
   }
}


