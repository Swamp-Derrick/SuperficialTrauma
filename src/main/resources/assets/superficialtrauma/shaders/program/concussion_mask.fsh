#version 150
uniform sampler2D DiffuseSampler;
uniform sampler2D OriginalSampler;
uniform vec2 OutSize;
uniform float ClearRadius;
uniform float Intensity;
uniform float Pulse;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec2 point = (texCoord - 0.5) * OutSize / min(OutSize.x, OutSize.y);
    float peripheral = smoothstep(ClearRadius - 0.04, ClearRadius + 0.04, length(point));
    float amount = max(Pulse, peripheral * Intensity);
    fragColor = vec4(mix(texture(OriginalSampler, texCoord).rgb, texture(DiffuseSampler, texCoord).rgb, amount), 1.0);
}
