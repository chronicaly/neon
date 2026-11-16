#version 330 core

in vec2 texCoord;
uniform sampler2D u_Texture;
uniform sampler2D u_EntityDepth;
uniform sampler2D u_MainDepth;

layout(std140) uniform BlurData {
    vec4 BlurParams;
    vec4 BlurFlags;
    vec4 ObscuredColor;
};

out vec4 fragColor;

vec4 maskedSample(vec2 uv) {
    vec4 color = texture(u_Texture, uv);
    if (color.a <= 0.001 || BlurParams.z < 0.5) return color;
    float entityDepth = texture(u_EntityDepth, uv).r;
    bool obscured = entityDepth < texture(u_MainDepth, uv).r - 0.00015;
    if (obscured && BlurParams.w < 0.5) return vec4(0.0);
    if (obscured && BlurFlags.x > 0.5) return ObscuredColor;
    return color;
}

void main() {
    // Gaussian weights are normalized after accumulation so translucent masks
    // retain their colour while the alpha fades smoothly into the world.
    const float weights[7] = float[7](0.182, 0.160, 0.120, 0.080, 0.040, 0.018, 0.006);
    vec4 sum = vec4(0.0);
    float total = 0.0;
    for (int i = -6; i <= 6; i++) {
        float weight = weights[abs(i)];
        vec4 color = maskedSample(texCoord + BlurParams.xy * float(i));
        sum += vec4(color.rgb * color.a, color.a) * weight;
        total += weight;
    }
    float alpha = sum.a / total;
    fragColor = alpha > 0.0001 ? vec4(sum.rgb / sum.a, alpha) : vec4(0.0);
}
