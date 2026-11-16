#version 330 core

in vec2 texCoord;
in vec2 oneTexel;
in float shaderTime;

uniform sampler2D u_Texture;
uniform sampler2D u_EntityDepth;
uniform sampler2D u_MainDepth;
uniform sampler2D u_Bloom;

layout(std140) uniform OutlineData {
    vec4 OutlineParams;
    vec4 FillColor;
    vec4 ExtraParams;
    vec4 ObscuredColor;
    vec4 GlowParams;
    vec4 StyleParams;
    vec4 OutlineSecond;
    vec4 FillSecond;
    vec4 StrengthParams;
};

out vec4 fragColor;

bool isObscured(float entityDepth, vec2 uv) {
    return entityDepth < texture(u_MainDepth, uv).r - 0.00015;
}

bool depthVisible(float entityDepth, vec2 uv) {
    return OutlineParams.w > 0.5 || !isObscured(entityDepth, uv);
}

// Equal-lightness OKLab palette, shared with RainbowColors.java.
const vec2 RAINBOW_AB[8] = vec2[8](
    vec2(.14500925, .06761892), vec2(.05472322, .15035082),
    vec2(-.06761892, .14500925), vec2(-.15035082, .05472322),
    vec2(-.14500925, -.06761892), vec2(-.05472322, -.15035082),
    vec2(.06761892, -.14500925), vec2(.15035082, -.05472322)
);

vec3 rainbow(float phase) {
    float position = fract(phase) * 8.0;
    int i = int(position);
    float t = fract(position);
    vec2 p0 = RAINBOW_AB[(i + 7) % 8], p1 = RAINBOW_AB[i];
    vec2 p2 = RAINBOW_AB[(i + 1) % 8], p3 = RAINBOW_AB[(i + 2) % 8];
    vec2 ab = .5 * (2.0 * p1 + t * (-p0 + p2 + t * (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3
        + t * (-p0 + 3.0 * p1 - 3.0 * p2 + p3))));
    vec3 lms = vec3(.76) + vec3(
        .3963377774 * ab.x + .2158037573 * ab.y,
        -.1055613458 * ab.x - .0638541728 * ab.y,
        -.0894841775 * ab.x - 1.2914855480 * ab.y);
    lms = lms * lms * lms;
    vec3 linear = clamp(vec3(
        dot(vec3(4.0767416621, -3.3077115913, .2309699292), lms),
        dot(vec3(-1.2684380046, 2.6097574011, -.3413193965), lms),
        dot(vec3(-.0041960863, -.7034186147, 1.7076147010), lms)), 0.0, 1.0);
    return mix(1.055 * pow(linear, vec3(1.0 / 2.4)) - .055,
        12.92 * linear, lessThanEqual(linear, vec3(.0031308)));
}

vec4 styled(vec4 base, bool fill) {
    if (StyleParams.x > 0.5 && StyleParams.x < 1.5) {
        vec2 pixels = texCoord / oneTexel;
        vec2 dimensions = 1.0 / oneTexel;
        float phase = .75 * (pixels.x + pixels.y) / (dimensions.x + dimensions.y) + StrengthParams.z;
        return vec4(mix(base.rgb, rainbow(phase), fill ? StrengthParams.y : StrengthParams.x), base.a);
    }
    if (StyleParams.x > 1.5 && StyleParams.x < 2.5) {
        float amount = 0.5 + 0.5 * sin((texCoord.y / max(StyleParams.z, 0.05) + shaderTime * StyleParams.y * 0.2) * 6.2831853);
        if (StyleParams.w < 0.5) amount = step(0.5, amount);
        vec4 second = fill ? FillSecond : OutlineSecond;
        // Coverage belongs to the mask, never to the secondary colour.
        return vec4(mix(base.rgb, second.rgb, amount), base.a * mix(1.0, second.a, amount));
    }
    return base;
}

void main() {
    vec4 center = texture(u_Texture, texCoord);

    if (center.a > 0.0) {
        if (OutlineParams.z < 0.5) discard;
        float entityDepth = texture(u_EntityDepth, texCoord).r;
        if (!depthVisible(entityDepth, texCoord)) discard;

        vec4 fill = ExtraParams.z > 0.5 ? center : FillColor;
        fill = styled(fill, true);
        if (ExtraParams.w > 0.5 && isObscured(entityDepth, texCoord)) fill = ObscuredColor;
        float fillAlpha = fill.a * ExtraParams.x;
        if (GlowParams.z > 0.5 && (GlowParams.x > 0.5 || GlowParams.y > 0.5)) {
            float inner = clamp((1.0 - texture(u_Bloom, texCoord).a) * ExtraParams.y, 0.0, 1.0);
            fillAlpha = mix(fillAlpha, fill.a, inner);
        }
        fragColor = vec4(fill.rgb, fillAlpha);
        return;
    }

    bool bloomEnabled = GlowParams.x > 0.5 || GlowParams.y > 0.5;
    if (OutlineParams.y <= 0.0 && !bloomEnabled) discard;

    float edgeAlpha = 0.0;
    vec4 outlineColor = vec4(0.0);
    if (OutlineParams.y > 0.0) {
        int width = int(clamp(ceil(OutlineParams.x), 1.0, 8.0));
        float nearestDistance = 1000.0;
        float outlineDepth = 1.0;
        vec2 outlineUv = texCoord;

        for (int x = -width; x <= width; x++) {
            for (int y = -width; y <= width; y++) {
                if (GlowParams.w > 0.5 && x != 0 && y != 0 && abs(x) != abs(y)) continue;
                vec2 offset = vec2(x, y);
                vec2 uv = texCoord + oneTexel * offset;
                vec4 sampleColor = texture(u_Texture, uv);

                if (sampleColor.a > 0.0) {
                    float distance = dot(offset, offset);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        outlineColor = sampleColor;
                        outlineDepth = texture(u_EntityDepth, uv).r;
                        outlineUv = uv;
                    }
                }
            }
        }

        float coverage = clamp(OutlineParams.x + 1.0 - sqrt(nearestDistance), 0.0, 1.0);
        if (coverage > 0.0 && outlineColor.a > 0.0 && depthVisible(outlineDepth, outlineUv)) {
            outlineColor = styled(outlineColor, false);
            if (ExtraParams.w > 0.5 && isObscured(outlineDepth, outlineUv)) outlineColor = ObscuredColor;
            edgeAlpha = outlineColor.a * OutlineParams.y * coverage;
        }
    }

    vec4 halo = bloomEnabled ? texture(u_Bloom, texCoord) : vec4(0.0);
    if (bloomEnabled && ExtraParams.w < 0.5) halo = styled(halo, false);
    float bloomAlpha = clamp(halo.a * ExtraParams.y * (GlowParams.x > 0.5 ? 2.0 : 1.5), 0.0, 0.9);
    float alpha = edgeAlpha + bloomAlpha * (1.0 - edgeAlpha);
    if (alpha <= 0.001) discard;

    vec3 rgb = (outlineColor.rgb * edgeAlpha + halo.rgb * bloomAlpha * (1.0 - edgeAlpha)) / alpha;
    fragColor = vec4(rgb, alpha);
}
