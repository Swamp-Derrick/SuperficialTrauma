package com.swampd.superficialtrauma.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.init.ModEntities;
import com.swampd.superficialtrauma.common.init.ModMenus;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(
        modid = SuperficialTrauma.MOD_ID,
        value = Dist.CLIENT
)
public final class ClientModEvents {
    public static final KeyMapping OPEN_HEALTH_HUD = new KeyMapping(
            "key.superficialtrauma.open_health_hud",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.superficialtrauma"
    );
    public static final KeyMapping DRAG_BODY = new KeyMapping(
            "key.superficialtrauma.drag_body",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_CONTROL,
            "key.categories.superficialtrauma"
    );
    public static final KeyMapping ROTATE_BODY = new KeyMapping(
            "key.superficialtrauma.rotate_body",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.superficialtrauma"
    );

    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onReloadListeners(net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)
                resources -> ClientConcussionEffects.reload());
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.LOOT_TARGET.get(), LootTargetScreen::new);
        event.register(ModMenus.DEFIBRILLATOR_STATION.get(), DefibrillatorStationScreen::new);
        event.register(ModMenus.MEDICAL_WORKBENCH.get(), MedicalWorkbenchScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CORPSE.get(), CorpseRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_HEALTH_HUD);
        event.register(DRAG_BODY);
        event.register(ROTATE_BODY);
    }

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS,
                ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "vital_signs_overlay"),
                (graphics, delta) -> ClientVitalSignsOverlay.render(graphics,
                        graphics.guiWidth(), graphics.guiHeight(), delta.getGameTimeDeltaPartialTick(false)));
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "downed_overlay"),
                (graphics, delta) -> ClientDownedOverlay.render(graphics, graphics.guiWidth(), graphics.guiHeight()));
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "awakening_recovery_overlay"),
                (graphics, delta) -> ClientAwakeningRecovery.render(graphics,
                        graphics.guiWidth(), graphics.guiHeight(), delta.getGameTimeDeltaPartialTick(false)));
    }
}
