package com.swampd.superficialtrauma;

import com.mojang.logging.LogUtils;
import com.swampd.superficialtrauma.common.body.BodyState;
import com.swampd.superficialtrauma.common.init.ModCreativeTabs;
import com.swampd.superficialtrauma.common.init.ModEntities;
import com.swampd.superficialtrauma.common.init.ModMenus;
import com.swampd.superficialtrauma.common.init.ModItems;
import com.swampd.superficialtrauma.common.init.ModSounds;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
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
        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModSounds.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModMenus.register(modEventBus);
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onRegisterCapabilities);
        ModNetworking.register();
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Superficial Trauma common setup complete");
    }

    private void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(BodyState.class);
    }
}
