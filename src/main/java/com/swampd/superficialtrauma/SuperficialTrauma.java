package com.swampd.superficialtrauma;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(SuperficialTrauma.MOD_ID)
public final class SuperficialTrauma {
    public static final String MOD_ID = "superficialtrauma";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SuperficialTrauma(FMLJavaModLoadingContext loadingContext) {
        IEventBus modEventBus = loadingContext.getModEventBus();
        modEventBus.addListener(this::onCommonSetup);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Superficial Trauma common setup complete");
    }
}
