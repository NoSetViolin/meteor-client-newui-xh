package meteordevelopment.meteorclient;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.DiscordPresence;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.meteorclient.utils.ReflectInit;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Version;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.misc.input.KeyBinds;
import meteordevelopment.meteorclient.utils.network.OnlinePlayers;
import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.IEventBus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NeoForge 版：原 {@code onInitializeClient()} 折叠成静态 {@code init()}，在 Minecraft 构造完成后调用。
 * 与 Fabric 版的差异仅在加载器层（目录 / 元数据），其余初始化顺序逐行保留。
 */
public class MeteorClient {
   public static final String MOD_ID = "meteor-client";
   public static final String NAME = "XiaohSense";
   public static final Version VERSION = new Version("0.5.8");
   public static final String DEV_BUILD = "";
   public static MeteorClient INSTANCE;
   public static MeteorAddon ADDON;
   public static Minecraft mc;
   public static final IEventBus EVENT_BUS = new EventBus();
   public static final File FOLDER = Platform.gameDir().resolve("meteor-client").toFile();
   public static final Logger LOG = LoggerFactory.getLogger(NAME);
   private boolean wasWidgetScreen;
   private boolean wasHudHiddenRoot;

   public static void init() {
      if (INSTANCE != null) return;
      INSTANCE = new MeteorClient();
      MeteorClient self = INSTANCE;
      LOG.info("Initializing {} (NeoForge)", NAME);
      mc = Minecraft.getInstance();

      if (!FOLDER.exists()) {
         FOLDER.getParentFile().mkdirs();
         FOLDER.mkdir();
         Systems.addPreLoadTask(() -> Modules.get().get(DiscordPresence.class).toggle());
      }

      AddonManager.init();
      AddonManager.ADDONS.forEach(addon -> {
         try {
            EVENT_BUS.registerLambdaFactory(
               addon.getPackage(), (lookupInMethod, klass) -> (Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup())
            );
         } catch (AbstractMethodError var2) {
            throw new RuntimeException("Addon \"%s\" is too old and cannot be ran.".formatted(addon.name), var2);
         }
      });
      ReflectInit.registerPackages();
      ReflectInit.init(PreInit.class);
      Categories.init();
      Systems.init();
      EVENT_BUS.subscribe(self);
      AddonManager.ADDONS.forEach(MeteorAddon::onInitialize);
      Modules.get().sortModules();
      Systems.load();
      ReflectInit.init(PostInit.class);

      if (Boolean.getBoolean("meteor.selftest")) {
         net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(new PortSelfTest()::tick);
      }

      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         OnlinePlayers.leave();
         Systems.save();
         GuiThemes.save();
      }));
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (mc.screen == null && mc.getOverlay() == null && KeyBinds.OPEN_COMMANDS.consumeClick()) {
         mc.setScreen(new ChatScreen(Config.get().prefix.get()));
      }
   }

   @EventHandler
   private void onKey(KeyEvent event) {
      if (event.action == KeyAction.Press && KeyBinds.OPEN_GUI.matches(event.key, 0)) {
         this.toggleGui();
      }
   }

   @EventHandler
   private void onMouseButton(MouseButtonEvent event) {
      if (event.action == KeyAction.Press && KeyBinds.OPEN_GUI.matchesMouse(event.button)) {
         this.toggleGui();
      }
   }

   private void toggleGui() {
      if (Utils.canCloseGui()) {
         mc.screen.onClose();
      } else if (Utils.canOpenGui()) {
         Tabs.get().getFirst().openScreen(GuiThemes.get());
      }
   }

   @EventHandler(priority = -200)
   private void onOpenScreen(OpenScreenEvent event) {
      boolean hideHud = GuiThemes.get().hideHUD();
      if (hideHud) {
         if (!this.wasWidgetScreen) {
            this.wasHudHiddenRoot = mc.options.hideGui;
         }

         if (event.screen instanceof WidgetScreen) {
            mc.options.hideGui = true;
         } else if (!this.wasHudHiddenRoot) {
            mc.options.hideGui = false;
         }
      }

      this.wasWidgetScreen = event.screen instanceof WidgetScreen;
   }

   public static ResourceLocation identifier(String path) {
      return ResourceLocation.fromNamespaceAndPath("meteor-client", path);
   }
}


