package com.swampd.superficialtrauma.client;

/** Frame-rate independent visual envelope; deliberately has no Minecraft/GL dependencies. */
public final class ConcussionVisualState {
    public static final float SUSTAINED_BLUR_RADIUS = 24;
    public static final float ONSET_BLUR_RADIUS = 64;
    private double speed, coverage, intensity, pulseAge = 1;

    public void onset() { pulseAge = 0; }
    public void clear() { speed = coverage = intensity = 0; pulseAge = 1; }
    public void update(double seconds, boolean active, boolean painControlled, double actualSpeed) {
        double dt = Math.max(0, Math.min(.1, seconds));
        speed = approach(speed, Math.max(0, actualSpeed), dt, .15);
        double target = targetCoverage(speed, painControlled);
        coverage = approach(coverage, target, dt, target > coverage ? .35 : .8);
        intensity = active ? approach(intensity, 1, dt, .35) : Math.max(0, intensity - dt);
        pulseAge += dt;
        if (!active) pulseAge = 1;
    }
    public float coverage() { return (float) coverage; }
    public float intensity() { return (float) intensity; }
    public float blurRadiusPixels(int screenHeight) {
        return (SUSTAINED_BLUR_RADIUS + (ONSET_BLUR_RADIUS - SUSTAINED_BLUR_RADIUS) * pulse()) * screenHeight / 1080f;
    }
    public float pulse() {
        if (pulseAge < .05) return (float) (pulseAge / .05);
        if (pulseAge < .15) return 1;
        return (float) Math.max(0, 1 - (pulseAge - .15) / .15);
    }
    public static double targetCoverage(double speed, boolean painControlled) {
        double still = painControlled ? .05 : .25, walking = painControlled ? .12 : .40;
        double fast = painControlled ? .20 : .80;
        if (speed <= .1) return still;
        if (speed <= 4.3) return still + (walking - still) * (speed - .1) / 4.2;
        return walking + (fast - walking) * Math.min(1, (speed - 4.3) / 1.3);
    }
    private static double approach(double current, double target, double dt, double tau) {
        return current + (target - current) * (1 - Math.exp(-dt / tau));
    }

    /** Circle/viewport intersection, not a border-width percentage. Units are the short screen side. */
    public static float clearRadius(double coverage, double aspect) {
        double a = Math.max(1, aspect) / 2, b = Math.max(1, 1 / aspect) / 2;
        double desiredArea = 4 * a * b * (1 - Math.max(0, Math.min(1, coverage)));
        double lo = 0, hi = Math.hypot(a, b);
        for (int i = 0; i < 24; i++) {
            double radius = (lo + hi) / 2;
            if (circleArea(radius, a, b) < desiredArea) lo = radius; else hi = radius;
        }
        return (float) ((lo + hi) / 2);
    }
    public static double circleArea(double radius, double halfWidth, double halfHeight) {
        if (radius <= 0) return 0;
        double x = Math.min(halfWidth, radius);
        double flat = Math.min(x, Math.sqrt(Math.max(0, radius * radius - halfHeight * halfHeight)));
        return 4 * (halfHeight * flat + primitive(x, radius) - primitive(flat, radius));
    }
    private static double primitive(double x, double r) {
        return .5 * (x * Math.sqrt(Math.max(0, r * r - x * x)) + r * r * Math.asin(Math.min(1, x / r)));
    }
}
