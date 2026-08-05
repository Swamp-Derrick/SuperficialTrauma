package com.swampd.superficialtrauma.common.body;

public record InfusionProgression(boolean changed, float healingAmount, boolean completed) {
    private static final InfusionProgression UNCHANGED = new InfusionProgression(false, 0.0F, false);

    public static InfusionProgression unchanged() {
        return UNCHANGED;
    }
}
