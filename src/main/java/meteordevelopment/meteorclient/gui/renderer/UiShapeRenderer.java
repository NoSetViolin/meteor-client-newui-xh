/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */
package meteordevelopment.meteorclient.gui.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.utils.render.color.Color;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/** Batched, resolution-independent rounded surfaces, strokes and Gaussian shadows. */
public final class UiShapeRenderer {
    private final MeshBuilder mesh = new MeshBuilder(MeteorRenderPipelines.UI_SHAPE);

    public void begin() { mesh.begin(); }
    public void end() { mesh.end(); }
    public void setAlpha(double alpha) { mesh.alpha = alpha; }

    public void render(GpuTextureView backdrop) {
        MeshRenderer.begin()
            .attachments(mc.gameRenderer.mainRenderTarget())
            .pipeline(backdrop == null ? MeteorRenderPipelines.UI_SHAPE : MeteorRenderPipelines.UI_GLASS)
            .mesh(mesh)
            .sampler("u_Texture", backdrop, backdrop == null ? null : RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
            .end();
    }

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
        mesh.ensureQuadCapacity();
        int a = vertex(x, y, -padding, -padding, width, height, tl, tr, br, bl, stroke, softness, top, bottom);
        int b = vertex(x, y, -padding, height + padding, width, height, tl, tr, br, bl, stroke, softness, top, bottom);
        int c = vertex(x, y, width + padding, height + padding, width, height, tl, tr, br, bl, stroke, softness, top, bottom);
        int d = vertex(x, y, width + padding, -padding, width, height, tl, tr, br, bl, stroke, softness, top, bottom);
        mesh.quad(a, b, c, d);
    }

    private int vertex(double x, double y, double lx, double ly, double width, double height,
                       double tl, double tr, double br, double bl, double stroke, double softness,
                       Color top, Color bottom) {
        return mesh.vec2(x + lx, y + ly).vec2(lx, ly).vec2(width, height)
            .vec2(tl, tr).vec2(br, bl).vec2(stroke, softness).color(top).color(bottom).next();
    }
}
