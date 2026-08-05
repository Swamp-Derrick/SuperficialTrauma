package com.swampd.superficialtrauma.common.body;

public record AwakeningProgression(boolean changed, boolean started, boolean cancelled, boolean completed) {
    private static final AwakeningProgression UNCHANGED = new AwakeningProgression(false, false, false, false);

    public static AwakeningProgression unchanged() {
        return UNCHANGED;
    }

    public static AwakeningProgression startedNow() {
        return new AwakeningProgression(true, true, false, false);
    }

    public static AwakeningProgression cancelledNow() {
        return new AwakeningProgression(true, false, true, false);
    }

    public static AwakeningProgression completedNow() {
        return new AwakeningProgression(true, false, false, true);
    }
}
