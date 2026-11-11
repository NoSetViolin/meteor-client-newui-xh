package meteordevelopment.meteorclient;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.Deflater;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.events.render.GetFovEvent;
import meteordevelopment.meteorclient.mixin.GameRendererFovAccessor;
import meteordevelopment.meteorclient.utils.tooltip.*;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AntiPacketKick;
import meteordevelopment.meteorclient.systems.modules.player.PotionSpoof;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.CompressionDecoder;
import net.minecraft.network.VarInt;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Explicit opt-in, isolated world smoke test. Never runs during normal play. */
public final class PortSelfTest {
    private int stage, ticks;
    private CompletableFuture<?> setup;
    private final Path report = Platform.gameDir().resolve("meteor-selftest.txt");
    private Xray xray;
    private boolean priorXray;
    private int priorOpacity;
    private boolean priorHideGui;
    private double uiX,uiY;
    private Screen menuUnderTest;

    private void freecamTests() throws Exception {
        Minecraft mc = Minecraft.getInstance();
        var freecam = Modules.get().get(meteordevelopment.meteorclient.systems.modules.render.Freecam.class);
        boolean active = freecam.isActive();
        float savedYaw = mc.player.getYRot(), savedPitch = mc.player.getXRot();
        mc.player.setYRot(0); mc.player.setXRot(0);
        float yaw = 0, pitch = 0;
        var dx = net.minecraft.client.MouseHandler.class.getDeclaredField("accumulatedDX");
        var dy = net.minecraft.client.MouseHandler.class.getDeclaredField("accumulatedDY");
        var turn = net.minecraft.client.MouseHandler.class.getDeclaredMethod("turnPlayer", double.class);
        dx.setAccessible(true); dy.setAccessible(true); turn.setAccessible(true);
        double oldDx = dx.getDouble(mc.mouseHandler), oldDy = dy.getDouble(mc.mouseHandler);
        boolean smooth = mc.options.smoothCamera;
        double sensitivity = mc.options.sensitivity().get();
        boolean invert = mc.options.invertYMouse().get();
        try {
            mc.options.smoothCamera = false; mc.options.sensitivity().set(0.5); mc.options.invertYMouse().set(false);
            if (!active) freecam.toggle();
            float startYaw = freecam.yaw, startPitch = freecam.pitch;
            dx.setDouble(mc.mouseHandler, 80); dy.setDouble(mc.mouseHandler, 40);
            turn.invoke(mc.mouseHandler, 0.016);
            log("FREECAM mouse delta result yaw=" + (freecam.yaw-startYaw) + " pitch=" + (freecam.pitch-startPitch));
            require(Math.abs(freecam.yaw-startYaw-12) < 0.01, "Freecam mouse yaw");
            require(Math.abs(freecam.pitch-startPitch-6) < 0.01, "Freecam mouse pitch");
            require(mc.player.getYRot()==yaw && mc.player.getXRot()==pitch, "Freecam leaves player rotation unchanged");
            var camera = mc.gameRenderer.getMainCamera();
            camera.setup(mc.level, mc.player, false, false, 1);
            require(Math.abs(camera.getYRot()-freecam.yaw)<0.01 && Math.abs(camera.getXRot()-freecam.pitch)<0.01, "Freecam camera follows mouse");
            freecam.toggle();
            turn.invoke(mc.mouseHandler, 0.016);
            require(Math.abs(mc.player.getYRot()-yaw-12)<0.01, "Normal mouse rotation restored");
            log("PASS Freecam mouse yaw/pitch, camera rotation, stationary player and normal rotation after disable");
        } finally {
            if (freecam.isActive()!=active) freecam.toggle();
            mc.player.setYRot(savedYaw); mc.player.setXRot(savedPitch);
            dx.setDouble(mc.mouseHandler,oldDx); dy.setDouble(mc.mouseHandler,oldDy);
            mc.options.smoothCamera=smooth; mc.options.sensitivity().set(sensitivity); mc.options.invertYMouse().set(invert);
        }
    }

    private void log(String message) {
        System.out.println("[meteor-selftest] " + message);
        try { Files.writeString(report, message + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    private void advance(int next) { stage = next; ticks = 0; }

    public void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (Boolean.getBoolean("meteor.ui.interactive")) {
            if (stage == 99) return;
            if (mc.screen instanceof TitleScreen && mc.getOverlay() == null && ++ticks == 80) {
                require(meteordevelopment.meteorclient.utils.misc.input.KeyBinds.OPEN_GUI.matches(260,0), "Insert binding");
                require(!meteordevelopment.meteorclient.utils.misc.input.KeyBinds.OPEN_GUI.matches(344,0), "RightShift no longer bound");
                mc.keyboardHandler.keyPress(mc.getWindow().getWindow(),260,0,1,0);
                mc.keyboardHandler.keyPress(mc.getWindow().getWindow(),260,0,0,0);
                require(mc.screen instanceof meteordevelopment.meteorclient.gui.screens.ModulesScreen, "Insert callback opens module browser");
                log("PASS Insert callback opens module browser; RightShift unbound");
                stage=99;
            }
            return;
        }
        if (stage == 99) return;
        ticks++;
        try {
            switch (stage) {
                case 0 -> {
                    if (ticks == 40) {
                        log("waiting screen=" + (mc.screen == null ? "null" : mc.screen.getClass().getName()));
                        capture("00-startup.png");
                    }
                    if (mc.screen != null && mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")) {
                        mc.setScreen(new TitleScreen());
                    }
                    if (!(mc.screen instanceof TitleScreen) || mc.getOverlay() != null || ticks < 40) return;
                    Files.deleteIfExists(report);
                    log("START NeoForge=21.1.265 Meteor=" + Platform.modVersion() + " baritone=" + Platform.isModLoaded("baritone")
                        + " sodium=" + Platform.isModLoaded("sodium") + " iris=" + Platform.isModLoaded("iris"));
                    int count = 0;
                    require(MixinPlugin.isSodiumPresent == Platform.isModLoaded("sodium"), "early Sodium detection");
                    for (var module : Modules.get().getAll()) for (var group : module.settings) for (var setting : group) count++;
                    require(Modules.get().getAll().size() > 150 && count > 1000, "module/settings initialization");
                    log("modules=" + Modules.get().getAll().size() + " settings=" + count);
                    compressionTests();
                    Tabs.get().getFirst().openScreen(GuiThemes.get());
                    if (Boolean.getBoolean("meteor.ui.only")) {
                        ((meteordevelopment.meteorclient.gui.WidgetScreen)mc.screen).add(new meteordevelopment.meteorclient.gui.widgets.WWidget() {
                            @Override protected void onRender(meteordevelopment.meteorclient.gui.renderer.GuiRenderer r,double mx,double my,double dt) {
                                r.textScaled("中文测试：你好，世界。",10,mc.getWindow().getHeight()-48,Color.WHITE,1);
                                r.textScaled("Mixed 中文输入 123 / 简体與繁體",10,mc.getWindow().getHeight()-25,Color.WHITE,0.9);
                            }
                        });
                    }
                    advance(1);
                }
                case 1 -> {
                    if (ticks < 40) return;
                    capture("01-clickgui.png");
                    dragUiPanel(mc.screen,80,10);
                    var bounds=uiPanel(mc.screen); uiX=bounds.x; uiY=bounds.y;
                    mc.screen.keyPressed(70,0,2);
                    for (char c : "中文测试".toCharArray()) mc.screen.charTyped(c,0);
                    advance(15);
                }
                case 15 -> {
                    if (ticks < 30) return;
                    capture("10-ui-chinese.png");
                    menuUnderTest=mc.screen;
                    mc.setScreen(new meteordevelopment.meteorclient.gui.WidgetScreen(GuiThemes.get(),"中文编辑") {
                        @Override public void initWidgets() {
                            var window=add(theme.window("中文设置 / Chinese")).widget();
                            window.add(theme.label("中文测试：你好，世界。"));
                            window.add(theme.label("Mixed 中文输入 123 / 简体與繁體"));
                            window.add(theme.textBox("中文名称 Test 123")).minWidth(260);
                            window.add(theme.label("English text stays unchanged"));
                        }
                    });
                    advance(16);
                }
                case 16 -> {
                    if (ticks < 40) return;
                    capture("11-ui-chinese-editor.png");
                    mc.setScreen(menuUnderTest);
                    mc.screen.keyPressed(70,0,2);
                    mc.screen.keyPressed(65,0,2);
                    mc.screen.keyPressed(259,0,0);
                    for (char c : "xray".toCharArray()) mc.screen.charTyped(c,0);
                    advance(10);
                }
                case 10 -> {
                    if (ticks < 30) return;
                    capture("07-ui-search.png");
                    double s = GuiThemes.get().scale(1), scale=mc.getWindow().getGuiScale();
                    double x=uiX+260*s;
                    double y=uiY+176*s;
                    mc.screen.mouseMoved(x/scale,y/scale);
                    mc.screen.mouseClicked(x/scale,y/scale,1);
                    mc.screen.mouseReleased(x/scale,y/scale,1);
                    require(mc.screen instanceof meteordevelopment.meteorclient.gui.screens.ModuleScreen, "search result opens settings");
                    advance(11);
                }
                case 11 -> {
                    if (ticks < 30) return;
                    var panel=uiPanel(mc.screen);
                    require(Math.abs(panel.x-uiX)<1 && Math.abs(panel.y-uiY)<1,"configure inherits dragged browser position");
                    capture("08-ui-settings.png");
                    dragUiPanel(mc.screen,-35,-5);
                    panel=uiPanel(mc.screen); uiX=panel.x; uiY=panel.y;
                    double s=GuiThemes.get().scale(1), scale=mc.getWindow().getGuiScale();
                    double px=uiX;
                    double py=uiY;
                    double x=px+96*s,y=py+132*s;
                    mc.screen.mouseMoved(x/scale,y/scale);
                    mc.screen.mouseClicked(x/scale,y/scale,0);
                    mc.screen.mouseReleased(x/scale,y/scale,0);
                    require(mc.screen instanceof meteordevelopment.meteorclient.gui.screens.ModulesScreen,"Back to modules button");
                    advance(12);
                }
                case 12 -> {
                    if (ticks < 30) return;
                    var panel=uiPanel(mc.screen);
                    require(Math.abs(panel.x-uiX)<1 && Math.abs(panel.y-uiY)<1,"return inherits dragged configure position");
                    require(meteordevelopment.meteorclient.utils.misc.input.KeyBinds.OPEN_GUI.matches(260,0),"Insert default binding");
                    require(!meteordevelopment.meteorclient.utils.misc.input.KeyBinds.OPEN_GUI.matches(344,0),"RightShift unbound");
                    mc.keyboardHandler.keyPress(mc.getWindow().getWindow(),260,0,1,0);
                    mc.keyboardHandler.keyPress(mc.getWindow().getWindow(),260,0,0,0);
                    advance(13);
                }
                case 13 -> {
                    if (ticks < 30) return;
                    require(mc.screen instanceof TitleScreen,"Insert closes menu");
                    mc.keyboardHandler.keyPress(mc.getWindow().getWindow(),344,0,1,0);
                    mc.keyboardHandler.keyPress(mc.getWindow().getWindow(),344,0,0,0);
                    require(mc.screen instanceof TitleScreen,"RightShift does not open menu");
                    mc.keyboardHandler.keyPress(mc.getWindow().getWindow(),260,0,1,0);
                    mc.keyboardHandler.keyPress(mc.getWindow().getWindow(),260,0,0,0);
                    require(mc.screen instanceof meteordevelopment.meteorclient.gui.screens.ModulesScreen,"Insert opens menu");
                    log("PASS GUI search/right-click settings/back button/Insert open-close/RightShift unbound");
                    advance(14);
                }
                case 14 -> {
                    if (ticks < 30) return;
                    var panel=uiPanel(mc.screen);
                    require(Math.abs(panel.x-uiX)<1 && Math.abs(panel.y-uiY)<1,"reopen preserves window position");
                    log("PASS dragged browser/configure/back/reopen share position x="+uiX+" y="+uiY);
                    capture("09-ui-final.png");
                    if (Boolean.getBoolean("meteor.ui.only")) {
                        log("PASS smoke test completed; UI-only preview; screenshots require visual inspection");
                        advance(99); mc.stop(); return;
                    }
                    mc.setScreen(new TitleScreen());
                    advance(2);
                }
                case 2 -> {
                    if (ticks < 20) return;
                    String name = "meteor-neoforge-selftest";
                    advance(3);
                    if (Files.exists(Platform.gameDir().resolve("saves").resolve(name).resolve("level.dat"))) {
                        mc.createWorldOpenFlows().openWorld(name, () -> mc.setScreen(new TitleScreen()));
                    } else {
                        mc.createWorldOpenFlows().createFreshLevel(name,
                            new LevelSettings("Meteor NeoForge Selftest", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                            new WorldOptions(12345L, false, false),
                            access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            new TitleScreen());
                    }
                }
                case 3 -> {
                    if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null || ticks < 100) return;
                    freecamTests();
                    potionHolderTests();
                    log("RENDER BACKEND sodium=" + MixinPlugin.isSodiumPresent + " shaders=" + MixinPlugin.isIrisShaderPackInUse());
                    log("PASS entered singleplayer world");
                    xray = Modules.get().get(Xray.class);
                    priorXray = xray.isActive(); priorOpacity = xray.opacity.get(); priorHideGui = mc.options.hideGui;
                    if (priorXray) xray.toggle();
                    mc.options.hideGui = true;
                    var server = mc.getSingleplayerServer();
                    UUID uuid = mc.player.getUUID();
                    setup = server.submit(() -> {
                        var level = server.overworld();
                        level.setDayTime(6000); level.setWeatherParameters(6000, 0, false, false);
                        for (BlockPos pos : BlockPos.betweenClosed(-12, 80, -8, 12, 80, 14)) level.setBlock(pos, Blocks.BEDROCK.defaultBlockState(), 3);
                        for (BlockPos pos : BlockPos.betweenClosed(-5, 81, -2, 5, 87, 2)) level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
                        for (int y = 82; y <= 84; y++) {
                            level.setBlock(new BlockPos(-2, y, 0), Blocks.DIAMOND_ORE.defaultBlockState(), 3);
                            level.setBlock(new BlockPos(0, y, 0), Blocks.GOLD_ORE.defaultBlockState(), 3);
                            level.setBlock(new BlockPos(2, y, 0), Blocks.EMERALD_ORE.defaultBlockState(), 3);
                        }
                        server.getPlayerList().getPlayer(uuid).connection.teleport(0.5, 81, 10.5, 180, 0);
                    });
                    mc.setScreen(null);
                    advance(4);
                }
                case 4 -> {
                    if (!setup.isDone() || ticks < 160) return;
                    setup.join();
                    require(mc.level.getBlockState(new BlockPos(0, 83, 0)).is(Blocks.GOLD_ORE), "ore fixture received by client");
                    capture("02-xray-off.png");
                    xray.toggle();
                    require(xray.isActive(), "Xray enabled");
                    int expectedAlpha = MixinPlugin.isSodiumPresent || MixinPlugin.isIrisShaderPackInUse() ? 0 : xray.opacity.get();
                    require(Xray.getAlpha(Blocks.STONE.defaultBlockState(), new BlockPos(0,83,2)) == expectedAlpha, "backend stone opacity");
                    require(Xray.getAlpha(Blocks.DIAMOND_ORE.defaultBlockState(), new BlockPos(-2,83,0)) == -1, "ore visibility");
                    advance(5);
                }
                case 5 -> {
                    if (ticks < 200) return;
                    capture("03-xray-default-opacity.png");
                    log("PASS Xray active for 200 world ticks at default opacity=" + xray.opacity.get());
                    xray.opacity.set(0);
                    advance(6);
                }
                case 6 -> {
                    if (ticks < 160) return;
                    capture("04-xray-zero-opacity.png");
                    xray.toggle();
                    xray.opacity.set(priorOpacity);
                    advance(7);
                }
                case 7 -> {
                    if (ticks < 160) return;
                    capture("05-xray-disabled-again.png");
                    log("PASS Xray disabled and chunks rebuilt");
                    mc.options.hideGui = priorHideGui;
                    if (priorXray) xray.toggle();
                    thirdPersonTests();
                    fovTests();
                    if (Platform.isModLoaded("baritone")) {
                        require(baritone.api.BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("help"), "Baritone help command");
                        log("PASS Baritone help command");
                    }
                    mc.setScreen(new TooltipTestScreen());
                    advance(9);
                }
                case 9 -> {
                    if (ticks < 60) return;
                    capture("06-six-tooltips.png");
                    log("PASS six native tooltip factories rendered");
                    Systems.save(); GuiThemes.save();
                    require(Files.exists(MeteorClient.FOLDER.toPath().resolve("modules.nbt")), "module config saved");
                    log("PASS config saved");
                    mc.level.disconnect(); mc.disconnect(new TitleScreen());
                    advance(8);
                }
                case 8 -> {
                    if (ticks < 40) return;
                    log("PASS smoke test completed; screenshots require visual inspection");
                    advance(99);
                    mc.stop();
                }
                default -> throw new IllegalStateException("Unknown selftest stage " + stage);
            }
        } catch (Throwable failure) {
            log("FAIL stage=" + stage + " " + failure);
            failure.printStackTrace();
            advance(99);
            mc.stop();
        }
    }

    private meteordevelopment.meteorclient.gui.widgets.WWidget uiPanel(Screen screen) throws ReflectiveOperationException {
        var field=screen.getClass().getDeclaredField("panel"); field.setAccessible(true);
        return (meteordevelopment.meteorclient.gui.widgets.WWidget)field.get(screen);
    }
    private void dragUiPanel(Screen screen,double dx,double dy) throws ReflectiveOperationException {
        var panel=uiPanel(screen);
        double s=GuiThemes.get().scale(1), scale=Minecraft.getInstance().getWindow().getGuiScale();
        double x=panel.x+300*s,y=panel.y+25*s;
        screen.mouseMoved(x/scale,y/scale); screen.mouseClicked(x/scale,y/scale,0);
        screen.mouseMoved((x+dx)/scale,(y+dy)/scale); screen.mouseReleased((x+dx)/scale,(y+dy)/scale,0);
    }

    private void capture(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "meteor-" + name, mc.getMainRenderTarget(), component -> log("screenshot=" + component.getString()));
    }

    private void thirdPersonTests() {
        Minecraft mc = Minecraft.getInstance();
        var module = Modules.get().get(meteordevelopment.meteorclient.systems.modules.render.ThirdPerson.class);
        var camera = mc.gameRenderer.getMainCamera();
        var originalPerspective = mc.options.getCameraType();
        var originalPos = mc.player.position();
        float originalYaw=mc.player.getYRot(), originalPitch=mc.player.getXRot();
        double oldDistance=module.distance.get(), oldHeight=module.height.get(), oldFov=module.fov.get();
        boolean active=module.isActive();
        if(active) module.toggle();
        try {
            mc.player.setPos(0, 120, 10); mc.player.setYRot(0); mc.player.setXRot(0);
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            module.distance.set(4.0); module.height.set(0.0); module.fov.set(70.0);
            camera.setup(mc.level,mc.player,true,false,1);
            var vanilla=camera.getPosition();
            module.toggle();
            require(mc.options.getCameraType()==net.minecraft.client.CameraType.THIRD_PERSON_BACK,"ThirdPerson activation");
            camera.setup(mc.level,mc.player,true,false,1);
            require(camera.getPosition().distanceTo(vanilla)<0.001,"ThirdPerson default matches vanilla position");
            module.distance.set(8.0); module.height.set(2.0);
            camera.setup(mc.level,mc.player,true,false,1);
            require(Math.abs(camera.getPosition().y-vanilla.y-2)<0.001,"ThirdPerson height");
            require(Math.abs(camera.getPosition().z-vanilla.z+4)<0.001,"ThirdPerson distance");
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
            camera.setup(mc.level,mc.player,true,true,1);
            require(Math.abs(camera.getPosition().z-18)<0.001,"ThirdPerson front view distance");
            module.fov.set(90.0);
            require(Math.abs(MeteorClient.EVENT_BUS.post(GetFovEvent.get(70)).fov-90)<0.001,"ThirdPerson FOV");
            class HalfFov { @EventHandler private void fov(GetFovEvent e) { e.fov/=2; } }
            var half=new HalfFov(); MeteorClient.EVENT_BUS.subscribe(half);
            try { require(Math.abs(MeteorClient.EVENT_BUS.post(GetFovEvent.get(70)).fov-45)<0.001,"ThirdPerson FOV precedes normal-priority Zoom multiplier"); }
            finally { MeteorClient.EVENT_BUS.unsubscribe(half); }
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            require(!module.applies() && MeteorClient.EVENT_BUS.post(GetFovEvent.get(70)).fov==70,"First person unaffected");
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            mc.player.setPos(0.5,83,5); module.height.set(0.0);
            camera.setup(mc.level,mc.player,true,false,1);
            require(camera.getPosition().z>2 && camera.getPosition().z<5,"ThirdPerson vanilla wall collision");
            var freecam=Modules.get().get(meteordevelopment.meteorclient.systems.modules.render.Freecam.class);
            freecam.toggle();
            try { require(!module.applies(),"Freecam takes precedence"); }
            finally { freecam.toggle(); }
            module.toggle();
            require(mc.options.getCameraType()==net.minecraft.client.CameraType.FIRST_PERSON,"ThirdPerson restores perspective");
            log("PASS ThirdPerson vanilla default, distance, height, front view, FOV multiplier ordering, first-person isolation, wall collision, Freecam precedence, disable restoration");
        } finally {
            if(module.isActive()) module.toggle();
            module.distance.set(oldDistance); module.height.set(oldHeight); module.fov.set(oldFov);
            mc.player.setPos(originalPos); mc.player.setYRot(originalYaw); mc.player.setXRot(originalPitch);
            mc.options.setCameraType(originalPerspective);
            if(active) module.toggle();
        }
    }

    private void fovTests() {
        Minecraft mc = Minecraft.getInstance();
        GameRendererFovAccessor renderer = (GameRendererFovAccessor)mc.gameRenderer;
        var camera = mc.gameRenderer.getMainCamera();
        double base = renderer.meteor$getFov(camera, 0.5f, true);
        double hand = renderer.meteor$getFov(camera, 0.5f, false);
        class Probe {
            int calls;
            @EventHandler private void fov(GetFovEvent event) { calls++; event.fov *= 0.5; }
        }
        Probe probe = new Probe();
        MeteorClient.EVENT_BUS.subscribe(probe);
        try {
            double changed = renderer.meteor$getFov(camera, 0.5f, true);
            require(base > 0 && Math.abs(changed - base * 0.5) < 1e-6 && probe.calls == 1, "base FOV event before dynamic effects");
            require(renderer.meteor$getFov(camera, 0.5f, false) == hand && probe.calls == 1, "hand FOV unchanged");
            log("PASS base FOV event base=" + base + " changed=" + changed + " hand=" + hand);
        } finally { MeteorClient.EVENT_BUS.unsubscribe(probe); }
    }

    private void potionHolderTests() {
        var entity = EntityType.PIG.create(Minecraft.getInstance().level);
        require(entity != null, "status effect test entity");
        DeferredHolder<MobEffect, MobEffect> unbound = DeferredHolder.create(Registries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath("petrolpark", "midas_touch"));
        if (unbound.isBound()) {
            unbound = DeferredHolder.create(Registries.MOB_EFFECT,
                ResourceLocation.fromNamespaceAndPath("meteor-selftest", "unregistered_effect"));
        }
        require(!unbound.isBound(), "unbound NeoForge DeferredHolder fixture");
        PotionSpoof spoof = Modules.get().get(PotionSpoof.class);
        boolean active = spoof.isActive();
        try {
            if (active) spoof.toggle();
            require(!entity.hasEffect(unbound), "disabled PotionSpoof preserves absent unbound holder");
            entity.getActiveEffectsMap().put(MobEffects.LEVITATION, new MobEffectInstance(MobEffects.LEVITATION, 200));
            require(entity.hasEffect(MobEffects.LEVITATION), "disabled PotionSpoof preserves present bound effect");
            spoof.toggle();
            require(!entity.hasEffect(unbound), "enabled PotionSpoof preserves absent unbound holder");
            // The entity is never added to the world. Seed the map to exercise a true
            // original result with an unbound key, without ticking an invalid effect.
            entity.getActiveEffectsMap().put(unbound, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200));
            require(entity.hasEffect(unbound), "enabled PotionSpoof preserves true result for unbound holder");
            require(!entity.hasEffect(MobEffects.LEVITATION), "enabled PotionSpoof still blocks configured bound effect");
            entity.getActiveEffectsMap().put(MobEffects.MOVEMENT_SPEED, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200));
            require(entity.hasEffect(MobEffects.MOVEMENT_SPEED), "enabled PotionSpoof preserves allowed bound effect");
            log("PASS PotionSpoof bound/unbound holders, enabled/disabled, original true/false");
        } finally {
            entity.getActiveEffectsMap().clear();
            if (spoof.isActive() != active) spoof.toggle();
        }
    }

    private static final class TooltipTestScreen extends Screen {
        private final List<MeteorTooltipData> components;
        TooltipTestScreen() {
            super(Component.literal("Meteor native tooltip test"));
            Minecraft mc = Minecraft.getInstance();
            mc.level.setMapData(new MapId(1), MapItemSavedData.createFresh(0,0,(byte)0,false,false,Level.OVERWORLD));
            ItemStack[] items = new ItemStack[27]; Arrays.fill(items, ItemStack.EMPTY); items[0] = new ItemStack(Items.DIAMOND);
            components = List.of(new TextTooltipComponent(Component.literal("Text factory OK")), new MapTooltipComponent(1),
                new EntityTooltipComponent(EntityType.PIG.create(mc.level)), new ContainerTooltipComponent(items, new Color(255,255,255)),
                new BookTooltipComponent(Component.literal("Book factory OK")), new BannerTooltipComponent(new ItemStack(Items.RED_BANNER)));
            for (var data : components) require(ClientTooltipComponent.create(data) == data.getComponent(), "native tooltip factory " + data.getClass().getSimpleName());
        }
        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0,0,width,height,0xff202020);
            graphics.drawString(font, title, 8, 8, 0xffffffff);
            for (int i=0;i<components.size();i++) {
                MeteorTooltipData data = components.get(i);
                int col = i%3, row = i/3;
                graphics.renderTooltip(font, List.of(Component.literal(data.getClass().getSimpleName().replace("TooltipComponent", ""))), Optional.of(data),
                    8 + col*(width/3), 48 + row*((height-48)/2));
            }
        }
        @Override public boolean isPauseScreen() { return false; }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private void compressionTests() {
        AntiPacketKick module = Modules.get().get(AntiPacketKick.class);
        boolean active = module.isActive();
        try {
            if (active) module.toggle();
            require(!decode(9 * 1024 * 1024, false), "oversized packet rejected when disabled");
            module.toggle();
            require(decode(9 * 1024 * 1024, false), "oversized packet still inflated when enabled");
            require(!decode(256, true), "corrupt compression rejected when enabled");
            log("PASS AntiPacketKick disabled/oversized/corrupt packet cases");
        } finally { if (module.isActive() != active) module.toggle(); }
    }

    private static boolean decode(int size, boolean corrupt) {
        ByteBuf packet = Unpooled.buffer();
        VarInt.write(packet, size);
        if (corrupt) packet.writeBytes(new byte[]{1,2,3,4,5});
        else {
            Deflater compressor = new Deflater();
            try {
                compressor.setInput(new byte[size]); compressor.finish(); byte[] bytes = new byte[8192];
                while (!compressor.finished()) { int n = compressor.deflate(bytes); packet.writeBytes(bytes,0,n); }
            } finally { compressor.end(); }
        }
        EmbeddedChannel channel = new EmbeddedChannel(new CompressionDecoder(128, true));
        try {
            channel.writeInbound(packet);
            ByteBuf result = channel.readInbound();
            if (result == null) return false;
            try { return result.readableBytes() == size; } finally { result.release(); }
        } catch (DecoderException expected) { return false; }
        finally {
            // Do not decode the remaining compressed bytes again after an expected rejection.
            channel.pipeline().remove(CompressionDecoder.class);
            channel.finishAndReleaseAll();
        }
    }
}

