package com.swampd.superficialtrauma;

import com.mojang.logging.LogUtils;
import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.common.config.CorpseServerConfig;
import com.swampd.superficialtrauma.common.init.ModBlocks;
import com.swampd.superficialtrauma.common.init.ModBlockEntities;
import com.swampd.superficialtrauma.common.init.ModCreativeTabs;
import com.swampd.superficialtrauma.common.init.ModEntities;
import com.swampd.superficialtrauma.common.init.ModMenus;
import com.swampd.superficialtrauma.common.init.ModItems;
import com.swampd.superficialtrauma.common.init.ModSounds;
import com.swampd.superficialtrauma.common.init.ModEffects;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.ModContainer;
import org.slf4j.Logger;

@Mod(SuperficialTrauma.MOD_ID)
public final class SuperficialTrauma {
    public static final String MOD_ID = "superficialtrauma";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SuperficialTrauma(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, CorpseServerConfig.SPEC);
        BodyStateCapability.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModSounds.register(modEventBus);
        ModEffects.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModMenus.register(modEventBus);
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(ModNetworking::register);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(com.swampd.superficialtrauma.common.treatment.TreatmentEvents::registerOptionalGunEvents);
        LOGGER.info("Superficial Trauma common setup complete");
    }

}
