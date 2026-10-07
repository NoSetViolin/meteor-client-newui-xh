#version 330 core

in vec2 v_Local;
flat in vec2 v_Size;
flat in vec4 v_Radii;
flat in vec2 v_Parameters;
flat in vec4 v_Top;
flat in vec4 v_Bottom;
out vec4 fragColor;

float distanceToSurface() {
    vec2 p = v_Local - v_Size * 0.5;
    float radius = p.y < 0.0
        ? (p.x < 0.0 ? v_Radii.x : v_Radii.y)
        : (p.x < 0.0 ? v_Radii.w : v_Radii.z);
    vec2 q = abs(p) - v_Size * 0.5 + radius;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
}

void main() {
    float d = distanceToSurface();
    float aa = max(fwidth(d), 0.75);
    float coverage;
    if (v_Parameters.x < 0.0) {
        float sigma = max(v_Parameters.y, 0.5);
        float outside = max(d, 0.0);
        coverage = exp(-0.5 * outside * outside / (sigma * sigma));
    } else {
        coverage = 1.0 - smoothstep(-aa * 0.5, aa * 0.5, d);
        if (v_Parameters.x > 0.0)
            coverage *= smoothstep(-aa * 0.5, aa * 0.5, d + v_Parameters.x);
    }
    vec4 color = mix(v_Top, v_Bottom, clamp(v_Local.y / v_Size.y, 0.0, 1.0));
    fragColor = vec4(color.rgb, color.a * coverage);
}
