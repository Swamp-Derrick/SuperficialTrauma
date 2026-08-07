package com.swampd.superficialtrauma.common.loot;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.compat.CgmGunDetector;
import com.swampd.superficialtrauma.common.entity.CorpseEntity;
import com.swampd.superficialtrauma.common.forensics.AutopsyService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SuperficialTrauma.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LootInteractionEvents {
    private LootInteractionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer looter)) {
            return;
        }

        if (event.getTarget() instanceof CorpseEntity corpse) {
            if (looter.isShiftKeyDown()) {
                AutopsyService.open(looter, corpse.getId());
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }
            if (CgmGunDetector.isGun(looter.getMainHandItem())) {
                return;
            }
        } else if (looter.isShiftKeyDown() || !LootingService.isLootable(event.getTarget())) {
            return;
        }

        if (event.getTarget() instanceof ServerPlayer target) {
            LootingService.tryOpen(looter, target);
        } else if (event.getTarget() instanceof CorpseEntity corpse) {
            LootingService.tryOpen(looter, corpse);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
