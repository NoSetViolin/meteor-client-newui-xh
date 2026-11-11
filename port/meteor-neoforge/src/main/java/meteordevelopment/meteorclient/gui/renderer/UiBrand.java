package meteordevelopment.meteorclient.gui.renderer;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.NeverloseUi;
import meteordevelopment.meteorclient.renderer.Fonts;
import meteordevelopment.meteorclient.renderer.text.*;
import meteordevelopment.meteorclient.utils.render.color.Color;

/** Brand typography is independent of menu controls and HUD fonts. */
public final class UiBrand {
    private static CustomTextRenderer bold, regular;
    private static boolean syntheticBold;

    private static void init() {
        if (bold != null) return;
        FontFamily family = Fonts.getFamily(Fonts.DEFAULT_FONT.info.family());
        for (String name : new String[] {"Segoe UI", "Inter", "Noto Sans", "Arial"}) {
            FontFamily candidate = Fonts.getFamily(name);
            if (candidate != null && candidate.hasType(FontInfo.Type.Bold) && candidate.hasType(FontInfo.Type.Regular)) {
                family=candidate; break;
            }
        }
        FontFace normal = family == null ? Fonts.DEFAULT_FONT : family.get(FontInfo.Type.Regular);
        if (normal == null) normal=Fonts.DEFAULT_FONT;
        FontFace heavy = family == null ? null : family.get(FontInfo.Type.Bold);
        syntheticBold = heavy == null;
        regular=new CustomTextRenderer(normal);
        bold=new CustomTextRenderer(heavy == null ? normal : heavy);
    }

    public static void render(GuiRenderer renderer, GuiTheme theme, double x, double y) {
        init();
        renderer.roundedImage(x+theme.scale(20), y+theme.scale(25), theme.scale(30), theme.scale(30), theme.scale(7), GuiRenderer.BRAND_LOGO, Color.WHITE);
        // Two em boxes (18 + 9) and a 2-unit gap fit within the original 30-unit logo.
        double size=18/(bold.getHeight(false)*theme.textScale());
        double labelX=x+theme.scale(60), top=y+theme.scale(25.5);
        renderer.brandText("XiaohSense", bold, labelX, top, NeverloseUi.text(theme), size, 0.9);
        if (syntheticBold) renderer.brandText("XiaohSense", bold, labelX+theme.scale(0.5), top, NeverloseUi.text(theme), size, 0.9);
        renderer.brandText("KENNYZ.CN", regular, labelX, top+theme.scale(20), NeverloseUi.muted(theme), size*0.5, 1);
    }
}
