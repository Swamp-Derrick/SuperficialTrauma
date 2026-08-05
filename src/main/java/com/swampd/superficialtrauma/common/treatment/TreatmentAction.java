package com.swampd.superficialtrauma.common.treatment;

import java.util.Locale;

public enum TreatmentAction {
    APPLY,
    REMOVE;

    public boolean consumesItem() {
        return this == APPLY;
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey(TreatmentProcedure procedure) {
        return "treatment_action.superficialtrauma."
                + serializedName()
                + "."
                + procedure.serializedName();
    }

    public static TreatmentAction fromSerializedName(String name) {
        for (TreatmentAction action : values()) {
            if (action.serializedName().equals(name)) {
                return action;
            }
        }
        return APPLY;
    }
}
