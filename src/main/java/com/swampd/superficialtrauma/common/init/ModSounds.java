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
    public static final float MEDICAL_ACTION_SOUND_RANGE = 8.0F;
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
    public static final RegistryObject<SoundEvent> PICKING_UP_BANDAGE = registerMedicalAction(
            "picking_up_bandage"
    );
    public static final RegistryObject<SoundEvent> CLOTH_WRAPPING = registerMedicalAction(
            "cloth_wrapping"
    );
    public static final RegistryObject<SoundEvent> PACKING = registerMedicalAction("packing");
    public static final RegistryObject<SoundEvent> LIQUID_POUCH = registerMedicalAction("liquid_pouch");
    public static final RegistryObject<SoundEvent> START_SURGERY = registerMedicalAction("start_surgery");
    public static final RegistryObject<SoundEvent> FLASHLIGHT_CLICK = registerMedicalAction("flashlight_click");
    public static final RegistryObject<SoundEvent> PAPER_WORK = registerMedicalAction("paper_work");
    public static final RegistryObject<SoundEvent> TOURNIQUET = registerMedicalAction("tourniquet");
    public static final RegistryObject<SoundEvent> ICE_BAG = registerMedicalAction("ice_bag");
    public static final RegistryObject<SoundEvent> TABLETS = registerMedicalAction("tablets");
    public static final RegistryObject<SoundEvent> VIAL = registerMedicalAction("vial");
    public static final RegistryObject<SoundEvent> SYRINGE_START = registerMedicalAction("syringe_start");

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

    private static RegistryObject<SoundEvent> registerMedicalAction(String name) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, name);
        return SOUND_EVENTS.register(
                name,
                () -> SoundEvent.createFixedRangeEvent(location, MEDICAL_ACTION_SOUND_RANGE)
        );
    }
}
