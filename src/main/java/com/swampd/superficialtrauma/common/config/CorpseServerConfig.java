package com.swampd.superficialtrauma.common.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class CorpseServerConfig {
    public static final boolean DEFAULT_COLLISION_ENABLED = false;
    public static final boolean DEFAULT_EMPTY_REMOVAL_ENABLED = true;
    public static final int DEFAULT_EMPTY_LIFETIME_MINUTES = 15;

    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue COLLISION_ENABLED;
    private static final ForgeConfigSpec.BooleanValue EMPTY_REMOVAL_ENABLED;
    private static final ForgeConfigSpec.IntValue EMPTY_LIFETIME_MINUTES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("corpse");
        COLLISION_ENABLED = builder
                .comment(
                        "Whether corpses physically collide with and push living entities.",
                        "The interaction hitbox remains available when this is false."
                )
                .define("collisionEnabled", DEFAULT_COLLISION_ENABLED);
        EMPTY_REMOVAL_ENABLED = builder
                .comment("Whether completely empty corpses are removed after a delay.")
                .define("removeEmptyCorpses", DEFAULT_EMPTY_REMOVAL_ENABLED);
        EMPTY_LIFETIME_MINUTES = builder
                .comment("Loaded-world minutes an empty corpse remains before it is removed.")
                .defineInRange(
                        "emptyCorpseLifetimeMinutes",
                        DEFAULT_EMPTY_LIFETIME_MINUTES,
                        1,
                        10_080
                );
        builder.pop();
        SPEC = builder.build();
    }

    private CorpseServerConfig() {
    }

    public static boolean collisionEnabled() {
        return COLLISION_ENABLED.get();
    }

    public static boolean emptyRemovalEnabled() {
        return EMPTY_REMOVAL_ENABLED.get();
    }

    public static long emptyLifetimeTicks() {
        return EMPTY_LIFETIME_MINUTES.get() * 60L * 20L;
    }
}
