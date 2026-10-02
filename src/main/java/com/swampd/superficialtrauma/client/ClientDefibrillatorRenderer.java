package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.init.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(
        modid = SuperficialTrauma.MOD_ID,
        value = Dist.CLIENT
)
public final class ClientDefibrillatorRenderer {
    private static final Map<Integer, ItemStack> HIDDEN_OFFHAND_BY_PLAYER = new HashMap<>();

    private ClientDefibrillatorRenderer() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderFirstPersonHand(RenderHandEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player != null
                && event.getHand() == InteractionHand.OFF_HAND
                && player.getMainHandItem().is(ModItems.DEFIBRILLATOR.get())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        ItemStack offhand = player.getOffhandItem();
        if (!player.getMainHandItem().is(ModItems.DEFIBRILLATOR.get()) || offhand.isEmpty()) {
            return;
        }
        HIDDEN_OFFHAND_BY_PLAYER.put(player.getId(), offhand);
        player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        ItemStack hidden = HIDDEN_OFFHAND_BY_PLAYER.remove(event.getEntity().getId());
        if (hidden != null) {
            event.getEntity().setItemSlot(EquipmentSlot.OFFHAND, hidden);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES
                || HIDDEN_OFFHAND_BY_PLAYER.isEmpty()
                || Minecraft.getInstance().level == null) {
            return;
        }
        for (Map.Entry<Integer, ItemStack> entry : Map.copyOf(HIDDEN_OFFHAND_BY_PLAYER).entrySet()) {
            if (Minecraft.getInstance().level.getEntity(entry.getKey()) instanceof Player player) {
                player.setItemSlot(EquipmentSlot.OFFHAND, entry.getValue());
            }
            HIDDEN_OFFHAND_BY_PLAYER.remove(entry.getKey());
        }
    }
}
