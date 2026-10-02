package com.swampd.superficialtrauma.common.init;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.block.DefibrillatorStationBlockEntity;
import com.swampd.superficialtrauma.common.block.MedicalWorkbenchBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            SuperficialTrauma.MOD_ID
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DefibrillatorStationBlockEntity>> DEFIBRILLATOR_STATION =
            BLOCK_ENTITY_TYPES.register(
                    "defibrillator_station",
                    () -> BlockEntityType.Builder.of(
                            DefibrillatorStationBlockEntity::new,
                            ModBlocks.DEFIBRILLATOR_STATION.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MedicalWorkbenchBlockEntity>> MEDICAL_WORKBENCH =
            BLOCK_ENTITY_TYPES.register(
                    "medical_workbench",
                    () -> BlockEntityType.Builder.of(
                            MedicalWorkbenchBlockEntity::new,
                            ModBlocks.MEDICAL_WORKBENCH.get()
                    ).build(null)
            );

    private ModBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
