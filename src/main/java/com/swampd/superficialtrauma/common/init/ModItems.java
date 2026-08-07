package com.swampd.superficialtrauma.common.init;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.item.DefibrillatorItem;
import com.swampd.superficialtrauma.common.item.FirstAidSkillBookItem;
import com.swampd.superficialtrauma.common.item.ForensicSkillBookItem;
import com.swampd.superficialtrauma.common.item.SurgerySkillBookItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS,
            SuperficialTrauma.MOD_ID
    );

    public static final RegistryObject<Item> BANDAGE = ITEMS.register(
            "bandage",
            () -> new Item(new Item.Properties())
    );
    public static final RegistryObject<Item> MEDICAL_TAPE = ITEMS.register(
            "medical_tape",
            () -> new Item(new Item.Properties())
    );
    public static final RegistryObject<Item> SELF_ADHESIVE_BANDAGE = ITEMS.register(
            "self_adhesive_bandage",
            () -> new Item(new Item.Properties())
    );
    public static final RegistryObject<Item> MEDICAL_GAUZE = ITEMS.register(
            "medical_gauze",
            () -> new Item(new Item.Properties())
    );
    public static final RegistryObject<Item> ICE_PACK = ITEMS.register(
            "ice_pack",
            () -> new Item(new Item.Properties())
    );
    public static final RegistryObject<Item> TOURNIQUET = ITEMS.register(
            "tourniquet",
            () -> new Item(new Item.Properties())
    );
    public static final RegistryObject<Item> SALINE_SOLUTION = ITEMS.register(
            "saline_solution",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final RegistryObject<Item> BLOOD_BAG = ITEMS.register(
            "blood_bag",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final RegistryObject<Item> SYRINGE = ITEMS.register(
            "syringe",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final RegistryObject<Item> PARACETAMOL = ITEMS.register(
            "paracetamol",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final RegistryObject<Item> MORPHINE_VIAL = ITEMS.register(
            "morphine_vial",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final RegistryObject<Item> MANUAL_RESUSCITATOR = ITEMS.register(
            "manual_resuscitator",
            () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final RegistryObject<Item> DEFIBRILLATOR = ITEMS.register(
            "defibrillator",
            () -> new DefibrillatorItem(new Item.Properties().stacksTo(1))
    );
    public static final RegistryObject<Item> SURGICAL_KIT = ITEMS.register(
            "surgical_kit",
            () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final RegistryObject<Item> PUPIL_PENLIGHT = ITEMS.register(
            "pupil_penlight",
            () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final RegistryObject<Item> CHECKLIST = ITEMS.register(
            "checklist",
            () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final RegistryObject<Item> SURGERY_SKILL_BOOK = ITEMS.register(
            "surgery_skill_book",
            () -> new SurgerySkillBookItem(new Item.Properties().stacksTo(1))
    );
    public static final RegistryObject<Item> FIRST_AID_SKILL_BOOK = ITEMS.register(
            "first_aid_skill_book",
            () -> new FirstAidSkillBookItem(new Item.Properties().stacksTo(1))
    );
    public static final RegistryObject<Item> FORENSIC_SKILL_BOOK = ITEMS.register(
            "forensic_skill_book",
            () -> new ForensicSkillBookItem(new Item.Properties().stacksTo(1))
    );

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
