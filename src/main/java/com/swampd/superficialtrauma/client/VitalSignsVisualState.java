package com.swampd.superficialtrauma.client;

/** Visual-only, tick-driven filters; never writes back to the synchronized body state. */
public final class VitalSignsVisualState {
    private final Channel redness = new Channel();
    private final Channel veins = new Channel();
    private final Channel respiratoryEdge = new Channel();
    private final Channel respiratoryDim = new Channel();

    public void tick(int heartRateLevel, float respiratoryDistress) {
        redness.tick(unit(heartRateLevel / 3.0F));
        veins.tick(heartRateLevel >= 3 ? 1.0F : 0.0F);
        respiratoryEdge.tick(unit((respiratoryDistress - 3.0F) / 17.0F));
        respiratoryDim.tick(unit((respiratoryDistress - 15.0F) / 5.0F));
    }

    public Frame frame(float partialTick) {
        float portion = unit(partialTick);
        return new Frame(
                redness.sample(portion), veins.sample(portion),
                respiratoryEdge.sample(portion), respiratoryDim.sample(portion)
        );
    }

    public void clear() {
        redness.clear();
        veins.clear();
        respiratoryEdge.clear();
        respiratoryDim.clear();
    }

    private static float unit(float value) {
        return Float.isFinite(value) ? Math.max(0.0F, Math.min(1.0F, value)) : 0.0F;
    }

    public record Frame(float redness, float veins, float respiratoryEdge, float respiratoryDim) {
        public boolean visible() {
            return redness > 0.0001F || veins > 0.0001F
                    || respiratoryEdge > 0.0001F || respiratoryDim > 0.0001F;
        }
    }

    private static final class Channel {
        // Two cascaded filters ease both the onset and the end of a sudden change.
        // At 20 ticks/s, about 95% of the transition takes 1.0s in / 1.4s out.
        private static final float FADE_IN = 0.22F;
        private static final float FADE_OUT = 0.16F;
        private float filtered;
        private float current;
        private float previous;

        private void tick(float target) {
            previous = current;
            float speed = target > current ? FADE_IN : FADE_OUT;
            filtered += (target - filtered) * speed;
            current += (filtered - current) * speed;
            if (Math.abs(target - current) < 0.0001F
                    && Math.abs(target - filtered) < 0.0001F) {
                filtered = target;
                current = target;
            }
        }

        private float sample(float partialTick) {
            return previous + (current - previous) * partialTick;
        }

        private void clear() {
            filtered = 0.0F;
            current = 0.0F;
            previous = 0.0F;
        }
    }
}
