package com.swampd.superficialtrauma.common.init;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.FatigueEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(
            ForgeRegistries.MOB_EFFECTS, SuperficialTrauma.MOD_ID);
    public static final RegistryObject<MobEffect> FATIGUE = EFFECTS.register("fatigue", FatigueEffect::new);

    private ModEffects() { }

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }
}
