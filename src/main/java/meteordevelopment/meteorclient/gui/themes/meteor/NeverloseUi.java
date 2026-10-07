/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Blur;
import meteordevelopment.meteorclient.utils.render.color.Color;
import java.util.Locale;
import static meteordevelopment.meteorclient.MeteorClient.mc;

/** Clean-room rendering primitives for the 830 x 580 Neverlose layout. */
public final class NeverloseUi {
    public static final double DESIGN_WIDTH = 830;
    public static final double DESIGN_HEIGHT = 580;
    public static final double SIDEBAR_WIDTH = 192;
    public static final double HEADER_HEIGHT = 70;
    public static final double WINDOW_RADIUS = 12;

    public static final Color WINDOW = new Color(18, 23, 34, 252);
    public static final Color FRAME = new Color(35, 43, 59, 255);
    public static final Color FRAME_ACTIVE = new Color(45, 55, 74, 255);
    public static final Color GROUP = new Color(27, 33, 46, 255);
    public static final Color BUTTON = new Color(32, 39, 53, 255);
    public static final Color BUTTON_HOVER = new Color(43, 53, 71, 255);
    public static final Color BUTTON_ACTIVE = new Color(51, 63, 86, 255);
    public static final Color ACCENT = new Color(77, 125, 255, 255);
    public static final Color ACCENT_SOFT = new Color(77, 125, 255, 48);
    public static final Color TEXT = new Color(232, 237, 247, 255);
    public static final Color MUTED = new Color(142, 154, 174, 255);
    public static final Color BORDER = new Color(255, 255, 255, 8);
    public static final Color BORDER_HOVER = new Color(255, 255, 255, 18);
    public static final Color SEPARATOR = new Color(255, 255, 255, 14);

    private static final Color LIGHT_WINDOW = new Color(246, 248, 252, 252);
    private static final Color LIGHT_SIDEBAR = new Color(235, 240, 249, 230);
    private static final Color LIGHT_FRAME = new Color(222, 229, 240, 255);
    private static final Color LIGHT_FRAME_ACTIVE = new Color(211, 220, 240, 255);
    private static final Color LIGHT_GROUP = new Color(255, 255, 255, 255);
    private static final Color LIGHT_BUTTON = new Color(255, 255, 255, 235);
    private static final Color LIGHT_BUTTON_HOVER = new Color(239, 244, 253, 255);
    private static final Color LIGHT_BUTTON_ACTIVE = new Color(214, 222, 236, 255);
    private static final Color LIGHT_TEXT = new Color(28, 31, 42, 255);
    private static final Color LIGHT_MUTED = new Color(117, 128, 149, 255);
    private static final Color LIGHT_BORDER = new Color(69, 89, 125, 17);
    private static final Color LIGHT_BORDER_HOVER = new Color(45, 56, 82, 42);
    private static final Color LIGHT_SEPARATOR = new Color(69, 89, 125, 17);

    private NeverloseUi() {}

    public static double s(GuiTheme theme, double value) {
        return theme.scale(value);
    }

    public static boolean isLight(GuiTheme theme) {
        return theme instanceof MeteorGuiTheme meteorTheme && meteorTheme.modernLightMode.get();
    }

    public static Color windowColor(GuiTheme theme) { return isLight(theme) ? LIGHT_WINDOW : WINDOW; }
    public static Color sidebarColor(GuiTheme theme) { return isLight(theme) ? LIGHT_SIDEBAR : new Color(14, 19, 29, 225); }
    public static Color frame(GuiTheme theme) { return isLight(theme) ? LIGHT_FRAME : FRAME; }
    public static Color frameActive(GuiTheme theme) { return isLight(theme) ? LIGHT_FRAME_ACTIVE : FRAME_ACTIVE; }
    public static Color groupColor(GuiTheme theme) { return isLight(theme) ? LIGHT_GROUP : GROUP; }
    public static Color button(GuiTheme theme) { return isLight(theme) ? LIGHT_BUTTON : BUTTON; }
    public static Color buttonHover(GuiTheme theme) { return isLight(theme) ? LIGHT_BUTTON_HOVER : BUTTON_HOVER; }
    public static Color buttonActive(GuiTheme theme) { return isLight(theme) ? LIGHT_BUTTON_ACTIVE : BUTTON_ACTIVE; }
    public static Color text(GuiTheme theme) { return isLight(theme) ? LIGHT_TEXT : TEXT; }
    public static Color muted(GuiTheme theme) { return isLight(theme) ? LIGHT_MUTED : MUTED; }
    public static Color border(GuiTheme theme) { return isLight(theme) ? LIGHT_BORDER : BORDER; }
    public static Color borderHover(GuiTheme theme) { return isLight(theme) ? LIGHT_BORDER_HOVER : BORDER_HOVER; }
    public static Color separator(GuiTheme theme) { return isLight(theme) ? LIGHT_SEPARATOR : SEPARATOR; }

    public static void brand(GuiRenderer renderer, GuiTheme theme, double x, double y) {
        renderer.roundedGradient(x + s(theme, 20), y + s(theme, 25), s(theme, 30), s(theme, 30), s(theme, 8), ACCENT,
            new Color(96, 99, 235));
        renderer.textScaled("X", x + s(theme, 35) - theme.textWidth("X") * 1.05 / 2,
            y + s(theme, 40) - theme.textHeight() * 1.05 / 2, Color.WHITE, 1.05);
        renderer.textScaled("XiaohSense", x + s(theme, 60), y + s(theme, 32), text(theme), 0.93);
        renderer.textScaled("CLIENT WORKSPACE", x + s(theme, 20), y + s(theme, 75), muted(theme), 0.65);
    }

    public static void profile(GuiRenderer renderer, GuiTheme theme, double x, double y, String status) {
        String username = mc.getUser().getName();
        String initial = username.isEmpty() ? "U" : username.substring(0, username.offsetByCodePoints(0, 1)).toUpperCase(Locale.ROOT);
        renderer.roundedQuad(x + s(theme, 20), y + s(theme, 536), s(theme, 28), s(theme, 28), s(theme, 14), ACCENT_SOFT);
        renderer.textScaled(initial, x + s(theme, 34) - theme.textWidth(initial) * 0.9 / 2,
            y + s(theme, 550) - theme.textHeight() * 0.9 / 2, ACCENT, 0.9);
        renderer.textScaled(fit(theme, username, s(theme, 110), 0.9), x + s(theme, 60), y + s(theme, 534), text(theme), 0.9);
        renderer.textScaled(fit(theme, status, s(theme, 110), 0.73), x + s(theme, 60), y + s(theme, 553), muted(theme), 0.73);
    }

    public static void window(GuiRenderer renderer, GuiTheme theme, double x, double y, double width, double height) {
        double radius = s(theme, WINDOW_RADIUS);
        boolean effects = effects(theme);
        boolean high = theme instanceof MeteorGuiTheme meteor && meteor.uiEffects.get() == MeteorGuiTheme.UiEffects.High;
        if (effects) renderer.roundedShadow(x, y + s(theme, 5), width, height, radius,
            s(theme, high ? 16 : 9), new Color(0, 0, 0, isLight(theme) ? 52 : 105));
        Blur blur = Modules.get().get(Blur.class);
        var backdrop = effects && blur != null ? blur.getGuiBlurTexture() : null;
        if (backdrop != null) renderer.roundedBackdrop(x, y, width, height, radius, backdrop, Color.WHITE);
        Color base = windowColor(theme);
        int opacity = backdrop != null ? (isLight(theme) ? 242 : 228) : 255;
        renderer.roundedGradient(x, y, width, height, radius,
            new Color(isLight(theme) ? 253 : base.r + 5, isLight(theme) ? 254 : base.g + 6, isLight(theme) ? 255 : base.b + 9, opacity),
            new Color(base.r, base.g, base.b, opacity));
        outline(renderer, theme, x, y, width, height, radius, border(theme));
    }

    public static void group(GuiRenderer renderer, GuiTheme theme, double x, double y, double width, double height) {
        Color fill = groupColor(theme);
        renderer.roundedGradient(x, y, width, height, s(theme, 10),
            new Color(fill.r, fill.g, fill.b, 250), new Color(fill.r, fill.g, fill.b, 245));
        outline(renderer, theme, x, y, width, height, s(theme, 10), border(theme));
    }

    public static void control(GuiRenderer renderer, GuiTheme theme, double x, double y, double width, double height,
                               Color fill, boolean hovered) {
        double radius = s(theme, 8);
        renderer.roundedGradient(x, y, width, height, radius,
            new Color(Math.min(255, fill.r + 4), Math.min(255, fill.g + 4), Math.min(255, fill.b + 5), fill.a), fill);
        outline(renderer, theme, x, y, width, height, radius, hovered ? borderHover(theme) : border(theme));
    }

    public static void outline(GuiRenderer renderer, GuiTheme theme, double x, double y, double width, double height,
                               double radius, Color color) {
        double line = Math.max(0.75, s(theme, 0.75));
        renderer.roundedOutline(x, y, width, height, radius, line, color);
    }

    public static boolean effects(GuiTheme theme) {
        return !(theme instanceof MeteorGuiTheme meteor) || meteor.uiEffects.get() != MeteorGuiTheme.UiEffects.Off;
    }

    public static double animate(GuiTheme theme, double current, double target, double delta) {
        if (theme instanceof MeteorGuiTheme meteor && !meteor.uiAnimations.get()) return target;
        double next = current + (target - current) * (1 - Math.exp(-14 * Math.max(0, delta)));
        return Math.abs(next - target) < 0.001 ? target : next;
    }

    public static Color mix(Color from, Color to, double progress) {
        progress = Math.max(0, Math.min(1, progress));
        return new Color((int) Math.round(from.r + (to.r - from.r) * progress),
            (int) Math.round(from.g + (to.g - from.g) * progress),
            (int) Math.round(from.b + (to.b - from.b) * progress),
            (int) Math.round(from.a + (to.a - from.a) * progress));
    }

    /** Ellipsis by actual font metrics, including surrogate pairs. */
    public static String fit(GuiTheme theme, String text, double width, double size) {
        if (theme.textWidth(text) * size <= width) return text;
        String suffix = "...";
        if (theme.textWidth(suffix) * size > width) return "";
        int end = text.length();
        while (end > 0 && theme.textWidth(text.substring(0, end) + suffix) * size > width)
            end = text.offsetByCodePoints(end, -1);
        return text.substring(0, end) + suffix;
    }

    /** Consistent 16px line icons; no font or emoji fallback. */
    public static void icon(GuiRenderer r, String name, double x, double y, double size, Color color) {
        double u = size / 16;
        switch (name) {
            case "Combat" -> {
                r.line(x + 3*u, y + 2*u, x + 13*u, y + 14*u, color);
                r.line(x + 13*u, y + 2*u, x + 3*u, y + 14*u, color);
                r.line(x + u, y + 10*u, x + 6*u, y + 14*u, color);
                r.line(x + 10*u, y + 14*u, x + 15*u, y + 10*u, color);
            }
            case "Player" -> {
                r.roundedOutline(x + 5*u, y + u, 6*u, 6*u, 3*u, u, color);
                r.roundedOutline(x + 2*u, y + 9*u, 12*u, 6*u, 3*u, u, color);
            }
            case "Movement" -> {
                r.line(x + 2*u, y + 2*u, x + 14*u, y + 8*u, color);
                r.line(x + 14*u, y + 8*u, x + 2*u, y + 14*u, color);
                r.line(x + 2*u, y + 14*u, x + 5*u, y + 8*u, color);
                r.line(x + 5*u, y + 8*u, x + 2*u, y + 2*u, color);
            }
            case "Render" -> {
                r.roundedOutline(x + u, y + 4*u, 14*u, 8*u, 4*u, u, color);
                r.roundedQuad(x + 6*u, y + 6*u, 4*u, 4*u, 2*u, color);
            }
            case "World" -> {
                r.roundedOutline(x + u, y + u, 14*u, 14*u, 7*u, u, color);
                r.roundedOutline(x + 5*u, y + u, 6*u, 14*u, 3*u, u, color);
                r.line(x + u, y + 8*u, x + 15*u, y + 8*u, color);
            }
            case "Favorites" -> {
                for (int i = 0; i < 10; i++) {
                    double a = -Math.PI/2 + i*Math.PI/5, b = a + Math.PI/5;
                    double ra = (i%2 == 0 ? 7 : 3)*u, rb = (i%2 == 0 ? 3 : 7)*u;
                    r.line(x + 8*u + Math.cos(a)*ra, y + 8*u + Math.sin(a)*ra,
                        x + 8*u + Math.cos(b)*rb, y + 8*u + Math.sin(b)*rb, color);
                }
            }
            case "Misc" -> {
                for (int i = 0; i < 3; i++) {
                    r.line(x + u, y + (3 + i*5)*u, x + 15*u, y + (3 + i*5)*u, color);
                    r.roundedQuad(x + (i == 1 ? 9 : 4)*u, y + (1 + i*5)*u, 3*u, 4*u, u, color);
                }
            }
            case "Search" -> {
                r.roundedOutline(x + 2*u, y + u, 9*u, 9*u, 4.5*u, u, color);
                r.line(x + 10*u, y + 9*u, x + 14*u, y + 14*u, color);
            }
            default -> {
                for (int i = 0; i < 4; i++) r.roundedOutline(x + (i%2)*8*u + u,
                    y + (i/2)*8*u + u, 5*u, 5*u, u, u, color);
            }
        }
    }
}
