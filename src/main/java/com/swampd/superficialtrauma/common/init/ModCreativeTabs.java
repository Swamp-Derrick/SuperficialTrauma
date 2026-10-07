package com.swampd.superficialtrauma.common.init;

import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(
            Registries.CREATIVE_MODE_TAB,
            SuperficialTrauma.MOD_ID
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SUPERFICIAL_TRAUMA = CREATIVE_MODE_TABS.register(
            "superficial_trauma",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.superficialtrauma"))
                    .icon(() -> new ItemStack(ModItems.BANDAGE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.BANDAGE.get());
                        output.accept(ModItems.MEDICAL_TAPE.get());
                        output.accept(ModItems.SELF_ADHESIVE_BANDAGE.get());
                        output.accept(ModItems.MEDICAL_GAUZE.get());
                        output.accept(ModItems.ICE_PACK.get());
                        output.accept(ModItems.TOURNIQUET.get());
                        output.accept(ModItems.POVIDONE_IODINE.get());
                        output.accept(ModItems.MEDICAL_ALCOHOL.get());
                        output.accept(ModItems.SALINE_SOLUTION.get());
                        output.accept(ModItems.BLOOD_BAG.get());
                        output.accept(ModItems.SYRINGE.get());
                        output.accept(ModItems.PARACETAMOL.get());
                        output.accept(ModItems.MORPHINE_VIAL.get());
                        output.accept(ModItems.REMIFENTANIL_INJECTION.get());
                        output.accept(ModItems.NALOXONE.get());
                        output.accept(ModItems.EPINEPHRINE_INJECTION.get());
                        output.accept(ModItems.METOPROLOL.get());
                        output.accept(ModItems.DDVP_INSECTICIDE.get());
                        output.accept(ModItems.ATROPINE_SULFATE_INJECTION.get());
                        output.accept(ModItems.PRALIDOXIME_CHLORIDE_INJECTION.get());
                        output.accept(ModItems.AMOXICILLIN.get());
                        output.accept(ModItems.CEFTRIAXONE.get());
                        output.accept(ModItems.STETHOSCOPE.get());
                        output.accept(ModItems.MANUAL_RESUSCITATOR.get());
                        output.accept(ModItems.DEFIBRILLATOR.get());
                        output.accept(ModItems.DEFIBRILLATOR_STATION.get());
                        output.accept(ModItems.MEDICAL_WORKBENCH.get());
                        output.accept(ModItems.SURGICAL_KIT.get());
                        output.accept(ModItems.ARTIFICIAL_DERMIS.get());
                        output.accept(ModItems.CHEST_SEAL.get());
                        output.accept(ModItems.PUPIL_PENLIGHT.get());
                        output.accept(ModItems.CHECKLIST.get());
                        output.accept(ModItems.FIRST_AID_SKILL_BOOK.get());
                        output.accept(ModItems.SURGERY_SKILL_BOOK.get());
                        output.accept(ModItems.FORENSIC_SKILL_BOOK.get());
                    })
                    .build()
    );

    private ModCreativeTabs() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
