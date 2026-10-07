package com.swampd.superficialtrauma.common.init;

import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModSounds {
    public static final float DEFIBRILLATOR_SOUND_RANGE = 8.0F;
    public static final float MEDICAL_ACTION_SOUND_RANGE = 8.0F;
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(
            BuiltInRegistries.SOUND_EVENT,
            SuperficialTrauma.MOD_ID
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> DEFIBRILLATOR_CHARGING = registerFixedRange(
            "defibrillator_charging"
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> DEFIBRILLATOR_DISCHARGE = registerFixedRange(
            "defibrillator_discharge"
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> PICKING_UP_BANDAGE = registerMedicalAction(
            "picking_up_bandage"
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> CLOTH_WRAPPING = registerMedicalAction(
            "cloth_wrapping"
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> PACKING = registerMedicalAction("packing");
    public static final DeferredHolder<SoundEvent, SoundEvent> LIQUID_POUCH = registerMedicalAction("liquid_pouch");
    public static final DeferredHolder<SoundEvent, SoundEvent> START_SURGERY = registerMedicalAction("start_surgery");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLASHLIGHT_CLICK = registerMedicalAction("flashlight_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> PAPER_WORK = registerMedicalAction("paper_work");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOURNIQUET = registerMedicalAction("tourniquet");
    public static final DeferredHolder<SoundEvent, SoundEvent> ICE_BAG = registerMedicalAction("ice_bag");
    public static final DeferredHolder<SoundEvent, SoundEvent> TABLETS = registerMedicalAction("tablets");
    public static final DeferredHolder<SoundEvent, SoundEvent> VIAL = registerMedicalAction("vial");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMPOULE = registerMedicalAction("ampoule");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLASTIC_CONTAINER = registerMedicalAction("plastic_container");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHEST_SEAL = registerMedicalAction("chest_seal");
    public static final DeferredHolder<SoundEvent, SoundEvent> TINNITUS = registerMedicalAction("tinnitus");
    public static final DeferredHolder<SoundEvent, SoundEvent> SYRINGE_START = registerMedicalAction("syringe_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> RESUSCITATION_1 = registerMedicalAction("resuscitation_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> RESUSCITATION_2 = registerMedicalAction("resuscitation_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> CPR = registerMedicalAction("cpr");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEARTBEAT = registerVariableRange("heartbeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEAVY_BREATHING = registerVariableRange("heavy_breathing");
    public static final DeferredHolder<SoundEvent, SoundEvent> QTE_PERFECT = registerVariableRange("qte_perfect");
    public static final DeferredHolder<SoundEvent, SoundEvent> QTE_SUCCESS = registerVariableRange("qte_success");
    public static final DeferredHolder<SoundEvent, SoundEvent> QTE_FAILED = registerVariableRange("qte_failed");

    private ModSounds() {
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> registerFixedRange(String name) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, name);
        return SOUND_EVENTS.register(
                name,
                () -> SoundEvent.createFixedRangeEvent(location, DEFIBRILLATOR_SOUND_RANGE)
        );
    }

    private static DeferredHolder<SoundEvent, SoundEvent> registerMedicalAction(String name) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, name);
        return SOUND_EVENTS.register(
                name,
                () -> SoundEvent.createFixedRangeEvent(location, MEDICAL_ACTION_SOUND_RANGE)
        );
    }

    private static DeferredHolder<SoundEvent, SoundEvent> registerVariableRange(String name) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(location));
    }
}
