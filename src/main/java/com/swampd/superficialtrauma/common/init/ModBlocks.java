package com.swampd.superficialtrauma.common.init;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.block.DefibrillatorStationBlock;
import com.swampd.superficialtrauma.common.block.MedicalWorkbenchBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS,
            SuperficialTrauma.MOD_ID
    );

    public static final RegistryObject<DefibrillatorStationBlock> DEFIBRILLATOR_STATION = BLOCKS.register(
            "defibrillator_station",
            () -> new DefibrillatorStationBlock(medicalEquipmentProperties())
    );
    public static final RegistryObject<MedicalWorkbenchBlock> MEDICAL_WORKBENCH = BLOCKS.register(
            "medical_workbench",
            () -> new MedicalWorkbenchBlock(medicalEquipmentProperties())
    );

    private ModBlocks() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    private static BlockBehaviour.Properties medicalEquipmentProperties() {
        return BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                .strength(3.5F, 6.0F)
                .noOcclusion();
    }
}
