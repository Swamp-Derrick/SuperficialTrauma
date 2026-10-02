package com.swampd.superficialtrauma.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CorpseServerConfig {
    public static final boolean DEFAULT_COLLISION_ENABLED = false;
    public static final boolean DEFAULT_CORPSE_ENTITY_PUSHING_ENABLED = false;
    public static final boolean DEFAULT_DOWNED_COLLISION_ENABLED = false;
    public static final boolean DEFAULT_DOWNED_ENTITY_PUSHING_ENABLED = false;
    public static final boolean DEFAULT_EMPTY_REMOVAL_ENABLED = true;
    public static final int DEFAULT_EMPTY_LIFETIME_MINUTES = 15;

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue COLLISION_ENABLED;
    private static final ModConfigSpec.BooleanValue CORPSE_ENTITY_PUSHING_ENABLED;
    private static final ModConfigSpec.BooleanValue DOWNED_COLLISION_ENABLED;
    private static final ModConfigSpec.BooleanValue DOWNED_ENTITY_PUSHING_ENABLED;
    private static final ModConfigSpec.BooleanValue EMPTY_REMOVAL_ENABLED;
    private static final ModConfigSpec.IntValue EMPTY_LIFETIME_MINUTES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("corpse");
        COLLISION_ENABLED = builder
                .comment(
                        "Whether corpses physically block other entities.",
                        "The interaction hitbox remains available when this is false."
                )
                .define("collisionEnabled", DEFAULT_COLLISION_ENABLED);
        CORPSE_ENTITY_PUSHING_ENABLED = builder
                .comment(
                        "Whether corpses can push nearby entities or be pushed by them.",
                        "Gravity, floor collision, interaction and dragging remain available when this is false."
                )
                .define("entityPushingEnabled", DEFAULT_CORPSE_ENTITY_PUSHING_ENABLED);
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

        builder.push("downed");
        DOWNED_COLLISION_ENABLED = builder
                .comment(
                        "Whether downed players physically block other entities.",
                        "The low attack and interaction hitbox remains available when this is false."
                )
                .define("collisionEnabled", DEFAULT_DOWNED_COLLISION_ENABLED);
        DOWNED_ENTITY_PUSHING_ENABLED = builder
                .comment(
                        "Whether downed players can push nearby entities or be pushed by them.",
                        "Dragging, gravity, floor collision and the low attack hitbox remain available when this is false."
                )
                .define("entityPushingEnabled", DEFAULT_DOWNED_ENTITY_PUSHING_ENABLED);
        builder.pop();
        SPEC = builder.build();
    }

    private CorpseServerConfig() {
    }

    public static boolean collisionEnabled() {
        return COLLISION_ENABLED.get();
    }

    public static boolean corpseEntityPushingEnabled() {
        return CORPSE_ENTITY_PUSHING_ENABLED.get();
    }

    public static boolean downedCollisionEnabled() {
        return DOWNED_COLLISION_ENABLED.get();
    }

    public static boolean downedEntityPushingEnabled() {
        return DOWNED_ENTITY_PUSHING_ENABLED.get();
    }

    public static boolean emptyRemovalEnabled() {
        return EMPTY_REMOVAL_ENABLED.get();
    }

    public static long emptyLifetimeTicks() {
        return EMPTY_LIFETIME_MINUTES.get() * 60L * 20L;
    }
}
