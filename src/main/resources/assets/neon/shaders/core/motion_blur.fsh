#version 330 core
// Ported from chronicaly's Unity client; see NOTICE.
in vec2 texCoord;
uniform sampler2D LiveFrame;
uniform sampler2D HistoryFrame;
layout(std140) uniform BlurData { vec4 BlurParams; };
out vec4 fragColor;
void main() {
    vec3 currentColor = texture(LiveFrame, texCoord).rgb;
    vec3 historyColor = texture(HistoryFrame, texCoord).rgb;
    fragColor = vec4(mix(currentColor, historyColor, BlurParams.x), 1.0);
}
