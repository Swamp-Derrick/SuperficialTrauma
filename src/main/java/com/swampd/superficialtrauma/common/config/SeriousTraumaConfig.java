package com.swampd.superficialtrauma.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.io.IOException;
import java.util.Properties;

public final class SeriousTraumaConfig {
    public static final boolean PRODUCTION_DEFAULT = false;
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLED;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        ENABLED = builder.comment(
                "Experimental head/chest bullet trauma and a 50% brain-death roll for downing head damage >5.",
                "Production default: false. Development builds default to true for testing.",
                "False clears serious conditions/pools and disables brain-death rolls, not armor penetration or wound locations.",
                "Enables functional open pneumothorax and concussion. Packing instability remains active regardless."
        ).define("serioustrauma", developmentBuild() || PRODUCTION_DEFAULT);
        SPEC = builder.build();
    }

    private SeriousTraumaConfig() { }

    public static boolean enabled() { return ENABLED.get(); }

    private static boolean developmentBuild() {
        Properties profile = new Properties();
        try (var input = SeriousTraumaConfig.class.getResourceAsStream("/superficialtrauma-build.properties")) {
            if (input != null) profile.load(input);
        } catch (IOException ignored) { return false; }
        return Boolean.parseBoolean(profile.getProperty("development", "false"));
    }
}
