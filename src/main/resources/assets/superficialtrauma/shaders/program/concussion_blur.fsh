#version 150
uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform vec2 BlurDir;
uniform float BlurRadius;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec2 stepUV = BlurDir * max(1.0, BlurRadius) / (16.0 * InSize);
    vec2 edge = 0.5 / InSize;
    vec3 result = vec3(0.0);
    float total = 0.0;
    // Dense normalized Gaussian samples avoid sparse duplicate edges at the stronger onset radius.
    for (int i = -16; i <= 16; i++) {
        float offset = float(i);
        float weight = exp(-4.5 * offset * offset / 256.0);
        result += texture(DiffuseSampler, clamp(texCoord + stepUV * offset, edge, 1.0-edge)).rgb * weight;
        total += weight;
    }
    fragColor = vec4(result / total, 1.0);
}
