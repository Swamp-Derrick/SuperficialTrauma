package com.swampd.superficialtrauma.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(
        modid = SuperficialTrauma.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class ClientModEvents {
    public static final KeyMapping OPEN_HEALTH_HUD = new KeyMapping(
            "key.superficialtrauma.open_health_hud",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.superficialtrauma"
    );

    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_HEALTH_HUD);
    }

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll(
                "downed_overlay",
                (gui, graphics, partialTick, width, height) -> ClientDownedOverlay.render(graphics, width, height)
        );
    }
}
