package com.swampd.superficialtrauma.common.init;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.block.DefibrillatorStationMenu;
import com.swampd.superficialtrauma.common.block.MedicalWorkbenchMenu;
import com.swampd.superficialtrauma.common.loot.LootTargetMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(
            BuiltInRegistries.MENU,
            SuperficialTrauma.MOD_ID
    );
    public static final DeferredHolder<MenuType<?>, MenuType<LootTargetMenu>> LOOT_TARGET = MENUS.register(
            "loot_target",
            () -> IMenuTypeExtension.create(LootTargetMenu::fromNetwork)
    );
    public static final DeferredHolder<MenuType<?>, MenuType<DefibrillatorStationMenu>> DEFIBRILLATOR_STATION = MENUS.register(
            "defibrillator_station",
            () -> IMenuTypeExtension.create(DefibrillatorStationMenu::fromNetwork)
    );
    public static final DeferredHolder<MenuType<?>, MenuType<MedicalWorkbenchMenu>> MEDICAL_WORKBENCH = MENUS.register(
            "medical_workbench",
            () -> IMenuTypeExtension.create(MedicalWorkbenchMenu::fromNetwork)
    );

    private ModMenus() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
