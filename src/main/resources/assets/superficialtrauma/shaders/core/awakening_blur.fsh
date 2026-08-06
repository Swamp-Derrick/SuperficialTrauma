#version 150

uniform sampler2D Sampler0;
uniform vec2 InSize;
uniform float Radius;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec2 stepSize = vec2(Radius) / InSize;
    vec2 halfStep = stepSize * 0.5;

    vec4 color = texture(Sampler0, texCoord0) * 0.16;

    color += texture(Sampler0, texCoord0 + vec2(halfStep.x, 0.0)) * 0.09;
    color += texture(Sampler0, texCoord0 - vec2(halfStep.x, 0.0)) * 0.09;
    color += texture(Sampler0, texCoord0 + vec2(0.0, halfStep.y)) * 0.09;
    color += texture(Sampler0, texCoord0 - vec2(0.0, halfStep.y)) * 0.09;

    color += texture(Sampler0, texCoord0 + vec2(halfStep.x, halfStep.y)) * 0.06;
    color += texture(Sampler0, texCoord0 + vec2(halfStep.x, -halfStep.y)) * 0.06;
    color += texture(Sampler0, texCoord0 + vec2(-halfStep.x, halfStep.y)) * 0.06;
    color += texture(Sampler0, texCoord0 - vec2(halfStep.x, halfStep.y)) * 0.06;

    color += texture(Sampler0, texCoord0 + vec2(stepSize.x, 0.0)) * 0.06;
    color += texture(Sampler0, texCoord0 - vec2(stepSize.x, 0.0)) * 0.06;
    color += texture(Sampler0, texCoord0 + vec2(0.0, stepSize.y)) * 0.06;
    color += texture(Sampler0, texCoord0 - vec2(0.0, stepSize.y)) * 0.06;

    fragColor = vec4(color.rgb, 1.0);
}
