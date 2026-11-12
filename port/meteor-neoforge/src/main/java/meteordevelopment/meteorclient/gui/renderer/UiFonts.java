package meteordevelopment.meteorclient.gui.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.Fonts;
import meteordevelopment.meteorclient.renderer.text.*;
import meteordevelopment.meteorclient.utils.render.color.Color;

/** Independent menu font: never changes the HUD or the user's configured font. */
public final class UiFonts implements TextRenderer {
    private static UiFonts instance;
    private static UiFonts status;
    private static TextRenderer legacyBase;
    private static UiFonts legacy;
    private final TextRenderer latin;
    private final UiFallbackTextRenderer fallback = new UiFallbackTextRenderer();
    private final double fallbackRatio;
    private boolean building;
    private static TextRenderer modernLatin() {
        FontFace face = Fonts.DEFAULT_FONT;
        for (String name : new String[] {"Segoe UI", "Inter", "Noto Sans", "Arial"}) {
            FontFamily family = Fonts.getFamily(name);
            if (family != null && family.get(FontInfo.Type.Regular) != null) { face=family.get(FontInfo.Type.Regular); break; }
        }
        return new CustomTextRenderer(face);
    }
    private UiFonts() { this(modernLatin()); }
    private UiFonts(TextRenderer base) {
        latin = base;
        fallbackRatio = latin.getHeight(false) / 18.0;
        fallback.scale = fallbackRatio * 2;
    }
    public static TextRenderer renderer() { if (instance == null) instance=new UiFonts(); return instance; }
    /** Dedicated status typography; menu animation/scale cannot change this renderer. */
    public static TextRenderer statusRenderer() {
        if (status == null) {
            FontFace face = Fonts.DEFAULT_FONT;
            for (String name : new String[] {"Segoe UI", "Inter", "Noto Sans", "Arial"}) {
                FontFamily family = Fonts.getFamily(name);
                if (family != null && family.get(FontInfo.Type.Bold) != null) { face=family.get(FontInfo.Type.Bold); break; }
            }
            status = new UiFonts(new CustomTextRenderer(face));
        }
        return status;
    }
    /** Preserve the selected Latin face on older menu pages while adding Unicode fallback. */
    public static TextRenderer withFallback(TextRenderer base) {
        if (base instanceof UiFonts) return base;
        if (legacyBase != base) { legacyBase=base; legacy=new UiFonts(base); }
        return legacy;
    }
    private boolean needsFallback(String text) { return text.codePoints().anyMatch(c -> c > 255); }
    public void setAlpha(double alpha) { latin.setAlpha(alpha); fallback.setAlpha(alpha); }
    public void begin(double scale, boolean scaleOnly, boolean big) {
        latin.begin(scale,scaleOnly,big); fallback.begin(scale*fallbackRatio,scaleOnly,big); building=true;
    }
    public double getWidth(String text, int length, boolean shadow) {
        return (needsFallback(text) ? fallback : latin).getWidth(text,length,shadow);
    }
    public double getHeight(boolean shadow) { return latin.getHeight(shadow); }
    public double render(String text,double x,double y,Color color,boolean shadow) {
        return (needsFallback(text) ? fallback : latin).render(text,x,y,color,shadow);
    }
    public boolean isBuilding() { return building; }
    public void end(PoseStack pose) { latin.end(pose); fallback.end(pose); fallback.scale=fallbackRatio*2; building=false; }
}
