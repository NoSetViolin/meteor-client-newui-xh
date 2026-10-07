#version 330 core

layout(location = 0) in vec2 Position;
layout(location = 1) in vec2 Local;
layout(location = 2) in vec2 Size;
layout(location = 3) in vec4 Radii;
layout(location = 4) in vec2 Parameters;
layout(location = 5) in vec4 ColorTop;
layout(location = 6) in vec4 ColorBottom;

layout(std140) uniform MeshData {
    mat4 u_Proj;
    mat4 u_ModelView;
};

out vec2 v_Local;
flat out vec2 v_Size;
flat out vec4 v_Radii;
flat out vec2 v_Parameters;
flat out vec4 v_Top;
flat out vec4 v_Bottom;

void main() {
    gl_Position = u_Proj * u_ModelView * vec4(Position, 0.0, 1.0);
    v_Local = Local;
    v_Size = Size;
    v_Radii = Radii;
    v_Parameters = Parameters;
    v_Top = ColorTop;
    v_Bottom = ColorBottom;
}
