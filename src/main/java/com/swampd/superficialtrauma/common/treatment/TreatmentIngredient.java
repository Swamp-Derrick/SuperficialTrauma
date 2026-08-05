package com.swampd.superficialtrauma.common.treatment;

public record TreatmentIngredient(TreatmentType type, int count) {
    public TreatmentIngredient {
        if (type == null) {
            throw new IllegalArgumentException("Treatment ingredient type cannot be null");
        }
        count = Math.max(1, count);
    }
}
