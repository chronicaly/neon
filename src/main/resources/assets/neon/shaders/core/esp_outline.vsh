#version 330 core

in vec3 Position;

layout(std140) uniform PostData {
    vec4 PostParams;
};

out vec2 texCoord;
out vec2 oneTexel;
out float shaderTime;

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);
    texCoord = (Position.xy + 1.0) * 0.5;
    shaderTime = PostParams.z;
    oneTexel = 1.0 / PostParams.xy;
}
