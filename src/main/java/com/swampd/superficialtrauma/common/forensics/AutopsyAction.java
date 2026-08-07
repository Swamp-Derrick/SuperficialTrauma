package com.swampd.superficialtrauma.common.forensics;

public enum AutopsyAction {
    NONE("none", 0L),
    PENLIGHT("penlight", 10L * 20L),
    CHECKLIST("checklist", 30L * 20L);

    private final String serializedName;
    private final long durationTicks;

    AutopsyAction(String serializedName, long durationTicks) {
        this.serializedName = serializedName;
        this.durationTicks = durationTicks;
    }

    public String serializedName() {
        return serializedName;
    }

    public long durationTicks() {
        return durationTicks;
    }

    public static AutopsyAction fromSerializedName(String name) {
        for (AutopsyAction action : values()) {
            if (action.serializedName.equals(name)) {
                return action;
            }
        }
        return NONE;
    }
}
