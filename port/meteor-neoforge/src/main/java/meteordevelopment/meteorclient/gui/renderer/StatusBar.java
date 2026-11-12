package meteordevelopment.meteorclient.gui.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Locale;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.TickRate;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;

/** Always-on, top-right in-world status capsule, independent of the editable HUD. */
public final class StatusBar {
    private static final double HEIGHT = 32;
    private static final double MARGIN = 12;
    private static final double BLUR_STRENGTH = 0.70;
    private static final Color BACKGROUND = new Color(0, 0, 0, 179);
    private static final Color TEXT = new Color(218, 222, 231);
    private static final Color ICON = new Color(82, 136, 222);
    private static final Color SHADOW = new Color(0, 0, 0, 85);
    private static final String VERSION = SharedConstants.getCurrentVersion().getName();
    private static final GuiRenderer renderer = new GuiRenderer();
    private static final UiBackdrop backdrop = new UiBackdrop();

    private StatusBar() {}

    /** The same public MC counters used by the existing HUD, without mixin-only accessors. */
    public static String[] values() {
        var mc = MeteorClient.mc;
        var connection = mc.getConnection();
        var playerInfo = connection == null || mc.player == null ? null : connection.getPlayerInfo(mc.player.getUUID());
        var server = mc.getCurrentServer();
        float tps = TickRate.INSTANCE.getTickRate();
        return new String[] {
            mc.getUser().getName(),
            mc.getFps() + " FPS",
            mc.hasSingleplayerServer() ? "Singleplayer" : server == null ? "--" : server.ip,
            VERSION,
            playerInfo == null ? "-- ms" : Math.max(0, playerInfo.getLatency()) + " ms",
            Float.isFinite(tps) && tps > 0 ? String.format(Locale.ROOT, "%.1f TPS", tps) : "-- TPS"
        };
    }

    public static void render(GuiGraphics context) {
        var mc = MeteorClient.mc;
        if (mc.level == null || mc.player == null || mc.options.hideGui || Utils.isLoading()) return;
        int windowWidth = mc.getWindow().getWidth();
        int windowHeight = mc.getWindow().getHeight();
        if (windowWidth < 8 || windowHeight < 8) return;

        // Keep the compact reference proportions independent of Minecraft/menu GUI scale.
        double displayScale = Math.max(1, windowHeight / 1080.0);
        double scale = displayScale * (GuiThemes.get() instanceof MeteorGuiTheme theme ? theme.uiSize.get().statusBarScale : 0.8);
        String[] values = values();
        TextRenderer font = UiFonts.statusRenderer();
        double fontScale = 18.5 / font.getHeight(false);
        font.begin(fontScale, true, false);
        double[] widths = new double[values.length];
        double width = 12 + 25 + 12 + 12;
        try {
            values[2] = ellipsize(values[2], font, Math.min(224, windowWidth * 0.30 / scale));
            for (int i = 0; i < values.length; i++) {
                widths[i] = font.getWidth(values[i]);
                // Reserve counter widths so ordinary FPS/ping fluctuations do not move the bar.
                if (i == 1) widths[i] = Math.max(widths[i], font.getWidth("999 FPS"));
                if (i == 4) widths[i] = Math.max(widths[i], font.getWidth("999 ms"));
                if (i == 5) widths[i] = Math.max(widths[i], font.getWidth("20.0 TPS"));
                width += 16 + 5 + widths[i] + (i == values.length - 1 ? 0 : 12);
            }
        } finally {
            font.end();
        }
        double margin = Math.min(MARGIN * displayScale, windowWidth / 4.0);
        scale = Math.min(scale, (windowWidth - margin * 2) / width);
        double x = windowWidth - margin - width * scale;
        double y = margin;
        double height = HEIGHT * scale;

        context.flush();
        var projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        boolean rendering3D = Utils.rendering3D;
        try {
            Utils.unscaledProjection();
            backdrop.capture(BLUR_STRENGTH);
            renderer.theme = GuiThemes.get();
            renderer.begin(context, false);
            try {
                renderer.roundedShadow(x, y + scale, width * scale, height, height / 2, 3 * scale, SHADOW);
                renderer.roundedBackdrop(x, y, width * scale, height, height / 2, backdrop.texture(), Color.WHITE);
                renderer.roundedQuad(x, y, width * scale, height, height / 2, BACKGROUND);
                double cursor = x + 12 * scale;
                renderer.quad(cursor, y + 9 * scale, 25 * scale, 14 * scale, GuiRenderer.STATUS_LOGO, Color.WHITE);
                cursor += 37 * scale;
                for (int i = 0; i < values.length; i++) {
                    renderer.quad(cursor, y + 8 * scale, 16 * scale, 16 * scale, GuiRenderer.STATUS_ICONS[i], ICON);
                    cursor += (16 + 5 + widths[i] + 12) * scale;
                }
            } finally {
                renderer.end();
            }
            font.begin(fontScale * scale);
            try {
                double cursor = x + (12 + 25 + 12 + 16 + 5) * scale;
                double textY = y + (height - font.getHeight(false)) / 2 - 0.5 * scale;
                for (int i = 0; i < values.length; i++) {
                    font.render(values[i], cursor, textY, TEXT, false);
                    cursor += (widths[i] + 12 + 16 + 5) * scale;
                }
            } finally {
                font.end(context.pose());
            }
        } finally {
            RenderSystem.setProjectionMatrix(projection, sorting);
            Utils.rendering3D = rendering3D;
            RenderSystem.applyModelViewMatrix();
        }
    }

    private static String ellipsize(String value, TextRenderer font, double maxWidth) {
        if (font.getWidth(value) <= maxWidth) return value;
        int end = value.length();
        while (end > 0 && font.getWidth(value.substring(0, end) + "...") > maxWidth) {
            end = value.offsetByCodePoints(end, -1);
        }
        return value.substring(0, end) + "...";
    }
}
