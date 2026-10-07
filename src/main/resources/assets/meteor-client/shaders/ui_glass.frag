#version 330 core

uniform sampler2D u_Texture;
in vec2 v_Local;
flat in vec2 v_Size;
flat in vec4 v_Radii;
flat in vec2 v_Parameters;
flat in vec4 v_Top;
flat in vec4 v_Bottom;
out vec4 fragColor;

void main() {
    vec2 p = v_Local - v_Size * 0.5;
    float radius = p.y < 0.0
        ? (p.x < 0.0 ? v_Radii.x : v_Radii.y)
        : (p.x < 0.0 ? v_Radii.w : v_Radii.z);
    vec2 q = abs(p) - v_Size * 0.5 + radius;
    float d = min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
    float aa = max(fwidth(d), 0.75);
    float coverage = 1.0 - smoothstep(-aa * 0.5, aa * 0.5, d);
    // gl_FragCoord and render-target textures share the framebuffer origin.
    vec2 uv = gl_FragCoord.xy / vec2(textureSize(u_Texture, 0));
    vec4 tint = mix(v_Top, v_Bottom, clamp(v_Local.y / v_Size.y, 0.0, 1.0));
    fragColor = vec4(texture(u_Texture, uv).rgb * tint.rgb, tint.a * coverage);
}
