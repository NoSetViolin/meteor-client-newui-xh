package meteordevelopment.meteorclient.addons;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import meteordevelopment.meteorclient.MeteorClient;

/**
 * NeoForge 版 addon 发现：
 * <ul>
 *   <li>内置 addon（Meteor 自身）保持原样；</li>
 *   <li>第三方 addon 走 {@link ServiceLoader}（在 mods 目录里声明
 *       {@code META-INF/services/meteordevelopment.meteorclient.addons.MeteorAddon} 即可被 FML 的模块层加载到）。</li>
 * </ul>
 * Fabric 的 {@code meteor} entrypoint 机制在 NeoForge 不存在，已移除。
 */
public class AddonManager {
   public static final List<MeteorAddon> ADDONS = new ArrayList<>();

   public static void init() {
      MeteorClient.ADDON = new MeteorAddon() {
         @Override
         public void onInitialize() {
         }

         @Override
         public String getPackage() {
            return "meteordevelopment.meteorclient";
         }

         @Override
         public String getWebsite() {
            return "https://meteorclient.com";
         }

         @Override
         public GithubRepo getRepo() {
            return new GithubRepo("MeteorDevelopment", "meteor-client");
         }

         @Override
         public String getCommit() {
            return null;
         }
      };

      MeteorClient.ADDON.name = MeteorClient.NAME;
      MeteorClient.ADDON.authors = new String[]{"NoSetViolin"};
      MeteorClient.ADDON.color.parse("145,61,226");

      ADDONS.add(MeteorClient.ADDON);

      for (MeteorAddon addon : ServiceLoader.load(MeteorAddon.class, AddonManager.class.getClassLoader())) {
         if (addon == null || ADDONS.contains(addon)) continue;

         addon.name = switch (addon.getClass().getSimpleName()) {
            case "" -> "Unknown";
            default -> addon.getClass().getSimpleName().replace("Addon", "");
         };
         addon.authors = new String[]{"unknown"};

         ADDONS.add(addon);
      }
   }
}
