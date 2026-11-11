#version 330 core
uniform sampler2D u_Texture;
uniform vec2 u_UvMin;
uniform vec2 u_UvMax;
in vec2 v_Local;
flat in vec2 v_Size;
flat in vec4 v_Radii;
flat in vec2 v_Parameters;
flat in vec4 v_Top;
flat in vec4 v_Bottom;
out vec4 fragColor;
void main() {
    vec2 p=v_Local-v_Size*0.5;
    float radius=p.y<0.0 ? (p.x<0.0?v_Radii.x:v_Radii.y) : (p.x<0.0?v_Radii.w:v_Radii.z);
    vec2 q=abs(p)-v_Size*0.5+radius;
    float d=min(max(q.x,q.y),0.0)+length(max(q,0.0))-radius;
    float aa=max(fwidth(d),0.75);
    float coverage=1.0-smoothstep(-aa*0.5,aa*0.5,d);
    vec2 uv=mix(u_UvMin,u_UvMax,clamp(v_Local/v_Size,0.0,1.0));
    vec4 source=texture(u_Texture,uv);
    fragColor=vec4(source.rgb*v_Top.rgb,source.a*v_Top.a*coverage);
}
