package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.GetFovEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.CameraType;

/** Vanilla perspective controls with optional third-person camera parameters. */
public class ThirdPerson extends Module {
    public final Setting<Double> distance = settings.getDefaultGroup().add(new DoubleSetting.Builder()
        .name("thirdperson-distance").description("Camera distance in blocks. Vanilla wall collision is preserved.")
        .defaultValue(4).min(0).max(64).sliderRange(0, 16).build());
    public final Setting<Double> height = settings.getDefaultGroup().add(new DoubleSetting.Builder()
        .name("thirdperson-camera-height").description("Vertical offset from the vanilla eye-height camera anchor, in blocks.")
        .defaultValue(0).range(-16, 16).sliderRange(-4, 4).build());
    public final Setting<Double> fov = settings.getDefaultGroup().add(new DoubleSetting.Builder()
        .name("thirdperson-fov").description("Base third-person FOV in degrees, before dynamic FOV and Zoom.")
        .defaultValue(70).range(30, 110).sliderRange(30, 110).build());
    private CameraType previous;

    public ThirdPerson() {
        super(Categories.Render, "thirdperson", "Vanilla third person with adjustable distance, camera height and FOV.");
    }

    @Override public void onActivate() {
        previous = mc.options.getCameraType();
        if (previous.isFirstPerson()) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    @Override public void onDeactivate() {
        if (previous != null) mc.options.setCameraType(previous);
        previous = null;
    }

    public boolean applies() {
        return isActive() && !mc.options.getCameraType().isFirstPerson() && !Modules.get().isActive(Freecam.class);
    }

    // Set the base before Zoom's normal-priority multiplier.
    @EventHandler(priority = 1000)
    private void onFov(GetFovEvent event) {
        if (applies()) event.fov = fov.get();
    }
}
