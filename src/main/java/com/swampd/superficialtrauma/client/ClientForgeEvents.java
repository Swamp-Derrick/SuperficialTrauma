package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.entity.CorpseEntity;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
        modid = SuperficialTrauma.MOD_ID,
        value = Dist.CLIENT
)
public final class ClientForgeEvents {
    private ClientForgeEvents() {
    }

    @SubscribeEvent
    public static void onClientTickStart(ClientTickEvent.Pre event) {
            ClientGiveUpInput.tick();
            ClientBodyDragInput.tick();
            ClientBodyRotationInput.tick();
            ClientDownedInput.suppressKeyActions();
    }

    @SubscribeEvent
    public static void onClientTickEnd(ClientTickEvent.Post event) {

        Minecraft minecraft = Minecraft.getInstance();
        ClientHeartRateSounds.tick();
        ClientVitalSignsOverlay.tick();
        ClientBloodLossOverlay.tick();
        ClientAwakeningRecovery.tick();
        ClientDownedInput.enforceMovementLock();
        ClientDownedHitbox.tick();
        while (ClientModEvents.OPEN_HEALTH_HUD.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                ModNetworking.requestOwnBodyState();
                minecraft.setScreen(new HealthScreen());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!event.getLevel().isClientSide
                || event.getHand() != InteractionHand.MAIN_HAND
                || event.getEntity() != Minecraft.getInstance().player
                || ClientDownedPoses.get(event.getEntity().getId()).isPresent()) {
            return;
        }

        if (event.getTarget() instanceof CorpseEntity corpse) {
            if (!event.getEntity().isShiftKeyDown()) {
                return;
            }
            ModNetworking.requestAutopsy(corpse.getId());
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (!(event.getTarget() instanceof Player target) || event.getEntity() == target) {
            return;
        }

        if (event.getEntity().isShiftKeyDown()) {
            ModNetworking.requestInspection(target.getId());
        } else if (ClientDownedPoses.get(target.getId()).isPresent()) {
            ModNetworking.requestLootTarget(target.getId());
        } else {
            return;
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        if (!minecraft.options.hideGui && !ClientDownedOverlay.isVisible()) {
            ClientBloodLossOverlay.render(
                    event.getGuiGraphics(),
                    width,
                    height,
                    event.getPartialTick().getGameTimeDeltaPartialTick(false)
            );
            ClientTreatmentOverlay.render(event.getGuiGraphics(), width, height);
        }
        ClientMedicalInspectionNotice.render(event.getGuiGraphics(), width, height);
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiLayerEvent.Pre event) {
        if (!ClientDownedOverlay.isVisible()) {
            return;
        }
        if (event.getName().equals(VanillaGuiLayers.HOTBAR)
                || event.getName().equals(VanillaGuiLayers.CHAT)
                || event.getName().equals(VanillaGuiLayers.SELECTED_ITEM_NAME)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientBodyState.clear();
        ClientInspectionState.clear();
        ClientAutopsyState.clear();
        ClientTreatmentState.clear();
        ClientMedicationState.clear();
        ClientGiveUpInput.clear();
        ClientBodyDragInput.clear(false);
        ClientBodyRotationInput.clear(false);
        ClientBodyDragState.clear();
        ClientDownedPoses.clear();
        ClientBloodLossOverlay.clear();
        ClientHeartRateSounds.clear();
        ClientVitalSignsOverlay.clear();
        ClientMedicalInspectionNotice.clear();
    }
}
