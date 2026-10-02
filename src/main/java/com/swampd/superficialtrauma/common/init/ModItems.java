package com.swampd.superficialtrauma.common.init;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.item.DefibrillatorItem;
import com.swampd.superficialtrauma.common.item.DdvpInsecticideItem;
import com.swampd.superficialtrauma.common.item.FirstAidSkillBookItem;
import com.swampd.superficialtrauma.common.item.ForensicSkillBookItem;
import com.swampd.superficialtrauma.common.item.SurgerySkillBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            BuiltInRegistries.ITEM,
            SuperficialTrauma.MOD_ID
    );

    public static final DeferredHolder<Item, Item> BANDAGE = ITEMS.register(
            "bandage",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredHolder<Item, Item> MEDICAL_TAPE = ITEMS.register(
            "medical_tape",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredHolder<Item, Item> SELF_ADHESIVE_BANDAGE = ITEMS.register(
            "self_adhesive_bandage",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredHolder<Item, Item> MEDICAL_GAUZE = ITEMS.register(
            "medical_gauze",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredHolder<Item, Item> ICE_PACK = ITEMS.register(
            "ice_pack",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredHolder<Item, Item> TOURNIQUET = ITEMS.register(
            "tourniquet",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredHolder<Item, Item> POVIDONE_IODINE = ITEMS.register(
            "povidone_iodine",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final DeferredHolder<Item, Item> MEDICAL_ALCOHOL = ITEMS.register(
            "medical_alcohol",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final DeferredHolder<Item, Item> SALINE_SOLUTION = ITEMS.register(
            "saline_solution",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final DeferredHolder<Item, Item> BLOOD_BAG = ITEMS.register(
            "blood_bag",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final DeferredHolder<Item, Item> SYRINGE = ITEMS.register(
            "syringe",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final DeferredHolder<Item, Item> PARACETAMOL = ITEMS.register(
            "paracetamol",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final DeferredHolder<Item, Item> MORPHINE_VIAL = ITEMS.register(
            "morphine_vial",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final DeferredHolder<Item, Item> REMIFENTANIL_INJECTION = ITEMS.register(
            "remifentanil_injection",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final DeferredHolder<Item, Item> NALOXONE = ITEMS.register(
            "naloxone",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final DeferredHolder<Item, Item> EPINEPHRINE_INJECTION = ITEMS.register(
            "epinephrine_injection",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final DeferredHolder<Item, Item> METOPROLOL = ITEMS.register(
            "metoprolol",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final DeferredHolder<Item, Item> DDVP_INSECTICIDE = ITEMS.register(
            "ddvp_insecticide",
            () -> new DdvpInsecticideItem(new Item.Properties().stacksTo(16))
    );
    public static final DeferredHolder<Item, Item> ATROPINE_SULFATE_INJECTION = ITEMS.register(
            "atropine_sulfate_injection",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final DeferredHolder<Item, Item> PRALIDOXIME_CHLORIDE_INJECTION = ITEMS.register(
            "pralidoxime_chloride_injection",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final DeferredHolder<Item, Item> AMOXICILLIN = ITEMS.register(
            "amoxicillin",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final DeferredHolder<Item, Item> CEFTRIAXONE = ITEMS.register(
            "ceftriaxone",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final DeferredHolder<Item, Item> STETHOSCOPE = ITEMS.register(
            "stethoscope",
            () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final DeferredHolder<Item, Item> MANUAL_RESUSCITATOR = ITEMS.register(
            "manual_resuscitator",
            () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final DeferredHolder<Item, Item> DEFIBRILLATOR = ITEMS.register(
            "defibrillator",
            () -> new DefibrillatorItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredHolder<Item, Item> SURGICAL_KIT = ITEMS.register(
            "surgical_kit",
            () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final DeferredHolder<Item, Item> ARTIFICIAL_DERMIS = ITEMS.register(
            "artificial_dermis",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final DeferredHolder<Item, Item> PUPIL_PENLIGHT = ITEMS.register(
            "pupil_penlight",
            () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final DeferredHolder<Item, Item> CHECKLIST = ITEMS.register(
            "checklist",
            () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final DeferredHolder<Item, Item> SURGERY_SKILL_BOOK = ITEMS.register(
            "surgery_skill_book",
            () -> new SurgerySkillBookItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredHolder<Item, Item> FIRST_AID_SKILL_BOOK = ITEMS.register(
            "first_aid_skill_book",
            () -> new FirstAidSkillBookItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredHolder<Item, Item> FORENSIC_SKILL_BOOK = ITEMS.register(
            "forensic_skill_book",
            () -> new ForensicSkillBookItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredHolder<Item, Item> DEFIBRILLATOR_STATION = ITEMS.register(
            "defibrillator_station",
            () -> new BlockItem(ModBlocks.DEFIBRILLATOR_STATION.get(), new Item.Properties())
    );
    public static final DeferredHolder<Item, Item> MEDICAL_WORKBENCH = ITEMS.register(
            "medical_workbench",
            () -> new BlockItem(ModBlocks.MEDICAL_WORKBENCH.get(), new Item.Properties())
    );

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
