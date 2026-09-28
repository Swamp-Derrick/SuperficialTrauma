package com.swampd.superficialtrauma.common.medication;

public enum MedicationRoute {
    ORAL,
    INJECTION;

    public boolean allowsPatient(boolean self, boolean patientCanAct) {
        return this == INJECTION || (self && patientCanAct);
    }
}
