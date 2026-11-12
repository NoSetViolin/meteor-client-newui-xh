package meteordevelopment.meteorclient.gui.renderer;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.renderer.*;

/** Own framebuffer chain, captured before menu drawing. Does not enable or change Blur. */
final class UiBackdrop {
    private final Framebuffer[] buffers = new Framebuffer[4];
    private Shader down, up;
    private boolean ready;
    Integer texture() { return ready ? buffers[0].texture : null; }
    void capture() {
        ready=false;
        if (GuiThemes.get() instanceof MeteorGuiTheme theme && theme.uiEffects.get()==MeteorGuiTheme.UiEffects.Off) return;
        capture(1.0);
    }
    /** Independent HUD capture; menu effect settings do not control the status bar. */
    void capture(double strength) {
        ready=false;
        if (down==null) { down=new Shader("blur.vert","blur_down.frag"); up=new Shader("blur.vert","blur_up.frag"); }
        for (int i=0;i<buffers.length;i++) {
            if (buffers[i]==null) buffers[i]=new Framebuffer(1.0/Math.pow(2,i));
            else if (buffers[i].width!=(int)(MeteorClient.mc.getWindow().getWidth()*buffers[i].sizeMulti)
                || buffers[i].height!=(int)(MeteorClient.mc.getWindow().getHeight()*buffers[i].sizeMulti)) buffers[i].resize();
        }
        PostProcessRenderer.beginRender();
        try {
            render(buffers[0],MeteorClient.mc.getMainRenderTarget().getColorTextureId(),down,strength);
            for (int i=1;i<buffers.length;i++) render(buffers[i],buffers[i-1].texture,down,strength);
            for (int i=buffers.length-1;i>0;i--) render(buffers[i-1],buffers[i].texture,up,strength);
            ready=true;
        } finally {
            MeteorClient.mc.getMainRenderTarget().bindWrite(true);
            PostProcessRenderer.endRender();
        }
    }
    private void render(Framebuffer target,int source,Shader shader,double strength) {
        target.bind(); target.setViewport(); shader.bind(); GL.bindTexture(source);
        shader.set("uTexture",0); shader.set("uHalfTexelSize",0.5/target.width,0.5/target.height); shader.set("uOffset",2.0*strength);
        PostProcessRenderer.render();
    }
}
