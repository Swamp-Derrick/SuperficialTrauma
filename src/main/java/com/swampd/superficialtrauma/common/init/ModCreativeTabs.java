package com.swampd.superficialtrauma.common.init;

import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(
            Registries.CREATIVE_MODE_TAB,
            SuperficialTrauma.MOD_ID
    );

    public static final RegistryObject<CreativeModeTab> SUPERFICIAL_TRAUMA = CREATIVE_MODE_TABS.register(
            "superficial_trauma",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.superficialtrauma"))
                    .icon(() -> new ItemStack(ModItems.BANDAGE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.BANDAGE.get());
                        output.accept(ModItems.MEDICAL_TAPE.get());
                        output.accept(ModItems.SELF_ADHESIVE_BANDAGE.get());
                    })
                    .build()
    );

    private ModCreativeTabs() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
