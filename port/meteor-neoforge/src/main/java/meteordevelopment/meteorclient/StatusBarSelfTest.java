package meteordevelopment.meteorclient;

import java.nio.file.*;
import java.util.Arrays;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.renderer.StatusBar;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.systems.hud.Hud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

/** Opt-in visual/integration check, guarded by a marker in a disposable test instance. */
public final class StatusBarSelfTest {
    private final Path report = Platform.gameDir().resolve("statusbar-selftest.txt");
    private int stage, ticks;

    public void tick(ClientTickEvent.Post event) {
        if (stage == 99) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (!Files.exists(Platform.gameDir().resolve(".statusbar-test"))) throw new IllegalStateException("Disposable instance marker required");
            if (++ticks > 2400) throw new IllegalStateException("Timed out at stage " + stage);
            switch (stage) {
                case 0 -> {
                    if (!(mc.screen instanceof TitleScreen) || mc.getOverlay() != null || ticks < 40) return;
                    log("START MC 1.21.1 / NeoForge 21.1.265 / " + Platform.modVersion());
                    mc.createWorldOpenFlows().createFreshLevel("statusbar-test",
                        new LevelSettings("Status Bar Test", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                        new WorldOptions(12345L, false, false),
                        access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL).value().createWorldDimensions(),
                        new TitleScreen());
                    next(1);
                }
                case 1 -> {
                    if (mc.player == null || mc.level == null || mc.getOverlay() != null || ticks < 120) return;
                    Hud.get().active = false;
                    var theme = (MeteorGuiTheme) GuiThemes.get();
                    require(theme.uiSize.get() == MeteorGuiTheme.UiSize.Default && theme.uiSize.get().statusBarScale == 0.8, "default status bar scale is 80 percent");
                    mc.options.hideGui = false;
                    mc.options.guiScale().set(2);
                    mc.setScreen(null);
                    GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1280, 720);
                    next(2);
                }
                case 2 -> {
                    if (ticks < 80) return;
                    String[] values = StatusBar.values();
                    log("VALUES " + Arrays.toString(values) + " HUD.active=" + Hud.get().active);
                    var rate = meteordevelopment.meteorclient.utils.world.TickRate.INSTANCE;
                    var index = rate.getClass().getDeclaredField("nextIndex");
                    index.setAccessible(true);
                    require(index.getInt(rate) > 0, "real server-time packets reached the existing TPS sampler");
                    capture("01-gameplay.png");
                    require(values[0].equals(mc.getUser().getName()), "live logged-in username");
                    require(values[1].equals(mc.getFps() + " FPS"), "live FPS");
                    require(values[2].equals("Singleplayer") && values[3].equals("1.21.1"), "singleplayer and actual MC version");
                    require(values[4].matches("[0-9]+ ms"), "player-list ping");
                    Tabs.get().getFirst().openScreen(GuiThemes.get());
                    next(3);
                }
                case 3 -> {
                    if (ticks < 50) return;
                    require(Math.abs(GuiThemes.get().scale(100) - 100) < 0.001, "default menu scale is 100 percent");
                    capture("02-menu.png");
                    mc.setScreen(null);
                    mc.options.hideGui = true;
                    next(4);
                }
                case 4 -> {
                    if (ticks < 30) return;
                    capture("03-f1-hidden.png");
                    mc.options.hideGui = false;
                    GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 854, 480);
                    next(5);
                }
                case 5 -> {
                    if (ticks < 60) return;
                    capture("04-small-window.png");
                    mc.options.guiScale().set(3);
                    mc.resizeDisplay();
                    GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1600, 900);
                    next(6);
                }
                case 6 -> {
                    if (ticks < 60) return;
                    capture("05-large-window-scale3.png");
                    require(StatusBar.values()[5].matches("[0-9]+\\.[0-9] TPS"), "sampled TPS after world updates");
                    require(org.lwjgl.opengl.GL11.glGetError() == 0, "no pending OpenGL errors");
                    var theme = (MeteorGuiTheme) GuiThemes.get();
                    Tabs.get().getFirst().openScreen(theme);
                    next(7);
                }
                case 7 -> {
                    if (ticks == 1) ((MeteorGuiTheme) GuiThemes.get()).uiSize.set(MeteorGuiTheme.UiSize.Large);
                    if (ticks < 50) return;
                    var theme = (MeteorGuiTheme) GuiThemes.get();
                    require(Math.abs(theme.scale(100) - 125) < 0.001, "125 percent option scales the entire menu to 125 percent");
                    require(theme.uiSize.get().statusBarScale == 1.1, "125 percent option scales the status bar to 110 percent");
                    require(mc.screen != null, "menu remains open while changing UI size");
                    capture("06-menu-125-status-110-live-switch.png");
                    theme.uiSize.set(MeteorGuiTheme.UiSize.Default);
                    next(8);
                }
                case 8 -> {
                    if (ticks < 50) return;
                    var theme = (MeteorGuiTheme) GuiThemes.get();
                    require(theme.uiSize.get() == MeteorGuiTheme.UiSize.Default, "menu returns to 100 percent without reopening");
                    capture("07-menu-100-status-80-live-switch.png");
                    var saved = theme.toTag();
                    theme.uiSize.set(MeteorGuiTheme.UiSize.Large);
                    theme.fromTag(saved);
                    require(theme.uiSize.get() == MeteorGuiTheme.UiSize.Large, "UI size survives settings serialization");
                    GuiThemes.save();
                    mc.setScreen(null);
                    next(9);
                }
                case 9 -> {
                    if (ticks < 40) return;
                    capture("08-gameplay-status-110.png");
                    log("PASS status bar integration completed; inspect screenshots for alignment, blur and rounding");
                    next(99);
                    mc.stop();
                }
                default -> throw new IllegalStateException("Unknown stage " + stage);
            }
        } catch (Throwable failure) {
            log("FAIL " + failure);
            failure.printStackTrace();
            next(99);
            mc.stop();
        }
    }

    private void next(int value) { stage = value; ticks = 0; }
    private void require(boolean condition, String label) {
        if (!condition) throw new IllegalStateException(label);
        log("PASS " + label);
    }
    private void capture(String name) {
        var mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "statusbar-" + name, mc.getMainRenderTarget(), text -> {});
    }
    private void log(String message) {
        System.out.println("[statusbar-selftest] " + message);
        try { Files.writeString(report, message + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND); }
        catch (Exception error) { throw new IllegalStateException(error); }
    }
}
