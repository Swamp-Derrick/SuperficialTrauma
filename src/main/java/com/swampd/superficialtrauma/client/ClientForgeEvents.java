package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = SuperficialTrauma.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class ClientForgeEvents {
    private ClientForgeEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ClientDownedInput.suppressKeyActions();
            return;
        }
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
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
                || !(event.getTarget() instanceof Player target)
                || event.getEntity() != Minecraft.getInstance().player
                || event.getEntity() == target
                || ClientDownedPoses.get(event.getEntity().getId()).isPresent()) {
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
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        if (!minecraft.options.hideGui && !ClientDownedOverlay.isVisible()) {
            ClientBloodLossOverlay.render(
                    event.getGuiGraphics(),
                    width,
                    height,
                    event.getPartialTick()
            );
            ClientTreatmentOverlay.render(event.getGuiGraphics(), width, height);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        if (!ClientDownedOverlay.isVisible()) {
            return;
        }
        if (event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())
                || event.getOverlay().id().equals(VanillaGuiOverlay.CHAT_PANEL.id())
                || event.getOverlay().id().equals(VanillaGuiOverlay.ITEM_NAME.id())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientBodyState.clear();
        ClientInspectionState.clear();
        ClientTreatmentState.clear();
        ClientDownedPoses.clear();
        ClientBloodLossOverlay.clear();
    }
}
