/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */
package meteordevelopment.meteorclient.utils.misc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.screens.ModulesScreen;
import meteordevelopment.meteorclient.gui.screens.ModuleScreen;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.Criticals;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.orbit.listeners.ConsumerListener;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;

import java.nio.file.Files;
import java.nio.file.Path;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.glfw.GLFW.*;

/** Opt-in visual regression runner; inactive unless explicitly enabled by JVM property. */
public final class UiPreview {
    private static boolean opened;

    @PostInit
    public static void init() {
        if (!Boolean.getBoolean("meteor.ui.preview")) return;
        MeteorClient.EVENT_BUS.subscribe(new ConsumerListener<>(TickEvent.Post.class, _ -> {
            if (opened || mc.gui.overlay() != null || mc.gui.screen() == null) return;
            if (!(GuiThemes.get() instanceof MeteorGuiTheme theme)) return;
            opened = true;
            theme.modernLightMode.set(true);
            theme.applyModernPalette();
            mc.gui.setScreen(new PreviewScreen(theme));
        }));
    }

    private static final class PreviewScreen extends ModulesScreen {
        private int frames;
        private final MeteorGuiTheme meteorTheme;

        private PreviewScreen(MeteorGuiTheme theme) {
            super(theme);
            meteorTheme = theme;
        }

        @Override
        public void renderCustom(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            super.renderCustom(graphics, -100, -100, delta);
            frames++;
            if (frames == 60) capture("light", false);
            if (frames == 120) capture("dark", false);
            if (frames == 61) {
                meteorTheme.modernLightMode.set(false);
                meteorTheme.applyModernPalette();
            }
            if (frames == 121) search("crit");
            if (frames == 180) capture("search", false);
            if (frames == 181) search("no_matching_module");
            if (frames == 240) capture("empty", false);
            if (frames == 241) mc.gui.setScreen(new PreviewDetailsScreen(meteorTheme));
        }

        private void search(String query) {
            if (!keyPressed(new KeyEvent(GLFW_KEY_F, 0, GLFW_MOD_CONTROL)))
                throw new IllegalStateException("Search focus shortcut was not handled");
            keyPressed(new KeyEvent(GLFW_KEY_A, 0, GLFW_MOD_CONTROL));
            query.codePoints().forEach(cp -> {
                if (!charTyped(new CharacterEvent(cp))) throw new IllegalStateException("Search text input was not handled");
            });
        }
    }

    private static final class PreviewDetailsScreen extends ModuleScreen {
        private int frames;

        private PreviewDetailsScreen(MeteorGuiTheme theme) {
            super(theme, Modules.get().get(Criticals.class));
        }

        @Override
        public void renderCustom(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            super.renderCustom(graphics, -100, -100, delta);
            if (++frames == 60) capture("settings", true);
        }
    }

    private static void capture(String name, boolean last) {
        Path directory = Path.of(System.getProperty("meteor.ui.preview.dir", "ui-preview"));
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try {
                Files.createDirectories(directory);
                image.writeToFile(directory.resolve(name + ".png"));
                MeteorClient.LOG.info("UI visual regression captured: {}", directory.resolve(name + ".png"));
            } catch (Exception e) {
                MeteorClient.LOG.error("UI visual regression capture failed", e);
            } finally {
                image.close();
                if (last) mc.execute(mc::stop);
            }
        });
    }
}
