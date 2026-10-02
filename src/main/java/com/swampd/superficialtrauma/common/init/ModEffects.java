package com.swampd.superficialtrauma.common.init;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.FatigueEffect;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(
            BuiltInRegistries.MOB_EFFECT, SuperficialTrauma.MOD_ID);
    public static final DeferredHolder<MobEffect, MobEffect> FATIGUE = EFFECTS.register("fatigue", FatigueEffect::new);

    private ModEffects() { }

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }
}
