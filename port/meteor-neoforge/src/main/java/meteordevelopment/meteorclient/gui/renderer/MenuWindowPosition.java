package meteordevelopment.meteorclient.gui.renderer;

import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.util.Mth;

/** Shared session position for the module browser and its configuration screens. */
public final class MenuWindowPosition {
    private static double x=Double.NaN, y=Double.NaN;
    private MenuWindowPosition() {}

    public static Position place(double width, double height) {
        if (Double.isNaN(x)) { x=(Utils.getWindowWidth()-width)/2; y=(Utils.getWindowHeight()-height)/2; }
        x=Mth.clamp(x,0,Math.max(0,Utils.getWindowWidth()-width));
        y=Mth.clamp(y,0,Math.max(0,Utils.getWindowHeight()-height));
        return new Position(x,y);
    }

    public static void moved(double nextX,double nextY) { x=nextX; y=nextY; }
    public record Position(double x,double y) {}
}
