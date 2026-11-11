package meteordevelopment.meteorclient.gui.renderer;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.*;
import meteordevelopment.meteorclient.utils.render.color.Color;
/** UI-only OpenGL implementation of the upstream SDF surface batch. */
public final class UiShapeRenderer {
    private ShaderMesh mesh, glass, image;
    private Shader imageShader;
    private meteordevelopment.meteorclient.gui.renderer.packer.TextureRegion imageRegion;
    private double alpha = 1;
    private void init() {
       if (mesh != null) return;
       mesh = create("ui_shape.frag"); glass = create("ui_glass.frag");
       imageShader=new Shader("ui_shape.vert","ui_image.frag");
       image=new ShaderMesh(imageShader, DrawMode.Triangles,
          Mesh.Attrib.Vec2, Mesh.Attrib.Vec2, Mesh.Attrib.Vec2, Mesh.Attrib.Vec2,
          Mesh.Attrib.Vec2, Mesh.Attrib.Vec2, Mesh.Attrib.Color, Mesh.Attrib.Color);
    }
    private ShaderMesh create(String fragment) {
       return new ShaderMesh(new Shader("ui_shape.vert", fragment), DrawMode.Triangles,
          Mesh.Attrib.Vec2, Mesh.Attrib.Vec2, Mesh.Attrib.Vec2, Mesh.Attrib.Vec2,
          Mesh.Attrib.Vec2, Mesh.Attrib.Vec2, Mesh.Attrib.Color, Mesh.Attrib.Color);
    }
    public void begin() { init(); mesh.begin(); glass.begin(); image.begin(); mesh.alpha = glass.alpha = image.alpha = alpha; }
    public void end() { mesh.end(); glass.end(); image.end(); }
    public void setAlpha(double alpha) { this.alpha=alpha; if (mesh != null) mesh.alpha=glass.alpha=image.alpha=alpha; }
    public void render(PoseStack pose, int backdrop) {
       if (currentImage) {
          imageShader.bind(); imageShader.set("u_Texture",0);
          imageShader.set("u_UvMin",imageRegion.x1,imageRegion.y1);
          imageShader.set("u_UvMax",imageRegion.x2,imageRegion.y2);
          image.render(pose);
       } else if (backdrop != 0) { GL.bindTexture(backdrop); glass.render(pose); }
       else mesh.render(pose);
    }
    private Mesh target() { return currentImage ? image : currentGlass ? glass : mesh; }
    private boolean currentGlass;
    private boolean currentImage;
    public void setGlass(boolean value) { currentGlass=value; }
    public void setImage(boolean value) { currentImage=value; }
    public void imageRegion(meteordevelopment.meteorclient.gui.renderer.packer.TextureRegion region) { imageRegion=region; }
    // Radii are TL, TR, BR, BL. A negative stroke selects a soft shadow.
    public void rectangle(double x, double y, double width, double height,
                          double tl, double tr, double br, double bl, double stroke, double softness,
                          Color top, Color bottom) {
        if (!Double.isFinite(x + y + width + height + tl + tr + br + bl + stroke + softness)
            || width <= 0 || height <= 0) return;
        double max = Math.min(width, height) / 2;
        tl = Math.max(0, Math.min(tl, max));
        tr = Math.max(0, Math.min(tr, max));
        br = Math.max(0, Math.min(br, max));
        bl = Math.max(0, Math.min(bl, max));
        softness = Math.max(0.5, softness);
        double padding = stroke < 0 ? Math.ceil(softness * 3) : 1.5;

        int a = vertex(x, y, -padding, -padding, width, height, tl, tr, br, bl, stroke, softness, top, bottom);
        int b = vertex(x, y, -padding, height + padding, width, height, tl, tr, br, bl, stroke, softness, top, bottom);
        int c = vertex(x, y, width + padding, height + padding, width, height, tl, tr, br, bl, stroke, softness, top, bottom);
        int d = vertex(x, y, width + padding, -padding, width, height, tl, tr, br, bl, stroke, softness, top, bottom);
        target().quad(a, b, c, d);
    }

    private int vertex(double x, double y, double lx, double ly, double width, double height,
                       double tl, double tr, double br, double bl, double stroke, double softness,
                       Color top, Color bottom) {
        return target().vec2(x + lx, y + ly).vec2(lx, ly).vec2(width, height)
            .vec2(tl, tr).vec2(br, bl).vec2(stroke, softness).color(top).color(bottom).next();
    }
}
