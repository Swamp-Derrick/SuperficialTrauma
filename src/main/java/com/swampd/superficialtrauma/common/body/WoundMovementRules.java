package com.swampd.superficialtrauma.common.body;

public final class WoundMovementRules {
    private WoundMovementRules() { }

    public static boolean isMoving(double dx, double dy, double dz, double horizontalVelocitySquared,
                                   double verticalVelocity, boolean sprinting) {
        // Include walking, sneaking, dragging and swimming without treating gravity at rest as movement.
        return sprinting || dx * dx + dy * dy + dz * dz > 0.000001D
                || horizontalVelocitySquared > 0.000001D || verticalVelocity > 0.01D;
    }
}
