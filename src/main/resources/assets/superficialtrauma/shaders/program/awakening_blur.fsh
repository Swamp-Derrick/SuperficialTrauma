#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 BlurDir;
uniform float Radius;

out vec4 fragColor;

void main() {
    vec4 blurred = vec4(0.0);
    float sampleCount = 0.0;
    for (float offset = -Radius; offset <= Radius; offset += 1.0) {
        blurred += texture(DiffuseSampler, texCoord + oneTexel * offset * BlurDir);
        sampleCount += 1.0;
    }
    fragColor = blurred / max(sampleCount, 1.0);
}
