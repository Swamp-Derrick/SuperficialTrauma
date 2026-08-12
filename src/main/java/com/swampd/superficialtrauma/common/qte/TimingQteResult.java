package com.swampd.superficialtrauma.common.qte;

public enum TimingQteResult {
    PERFECT("perfect"),
    SUCCESS("success"),
    EARLY_FAILURE("early_failure"),
    MISSED_FAILURE("missed_failure"),
    CANCELLED("cancelled");

    private final String serializedName;

    TimingQteResult(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean failed() {
        return this == EARLY_FAILURE || this == MISSED_FAILURE;
    }

    public static TimingQteResult fromSerializedName(String name) {
        for (TimingQteResult result : values()) {
            if (result.serializedName.equals(name)) {
                return result;
            }
        }
        return CANCELLED;
    }
}
