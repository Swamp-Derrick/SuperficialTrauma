package com.swampd.superficialtrauma.common.init;

import com.swampd.superficialtrauma.SuperficialTrauma;
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

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
