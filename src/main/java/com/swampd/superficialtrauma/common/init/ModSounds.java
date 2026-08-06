package com.swampd.superficialtrauma.common.init;

import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final float DEFIBRILLATOR_SOUND_RANGE = 8.0F;
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(
            ForgeRegistries.SOUND_EVENTS,
            SuperficialTrauma.MOD_ID
    );

    public static final RegistryObject<SoundEvent> DEFIBRILLATOR_CHARGING = registerFixedRange(
            "defibrillator_charging"
    );
    public static final RegistryObject<SoundEvent> DEFIBRILLATOR_DISCHARGE = registerFixedRange(
            "defibrillator_discharge"
    );

    private ModSounds() {
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }

    private static RegistryObject<SoundEvent> registerFixedRange(String name) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, name);
        return SOUND_EVENTS.register(
                name,
                () -> SoundEvent.createFixedRangeEvent(location, DEFIBRILLATOR_SOUND_RANGE)
        );
    }
}
