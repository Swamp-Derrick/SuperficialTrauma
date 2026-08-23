package com.swampd.superficialtrauma.common.toxicology;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.common.init.ModItems;
import com.swampd.superficialtrauma.common.treatment.InspectionService;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemStackedOnOtherEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SuperficialTrauma.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ToxicologyEvents {
    private ToxicologyEvents() {
    }

    @SubscribeEvent
    public static void onItemStackedOnOther(ItemStackedOnOtherEvent event) {
        ItemStack pesticide = event.getCarriedItem();
        ItemStack food = event.getStackedOnItem();
        if (event.getClickAction() != ClickAction.SECONDARY
                || !pesticide.is(ModItems.DDVP_INSECTICIDE.get())
                || food.isEmpty()) {
            return;
        }
        if (food.getFoodProperties(event.getPlayer()) == null) {
            return;
        }

        event.setCanceled(true);
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (food.getCount() != 1) {
            player.displayClientMessage(
                    Component.translatable("message.superficialtrauma.toxicology.single_food_required"),
                    true
            );
            return;
        }
        ItemStack contaminatedFood = food.copy();
        if (!FoodContamination.contaminateWithDdvp(contaminatedFood)) {
            player.displayClientMessage(
                    Component.translatable("message.superficialtrauma.toxicology.already_contaminated"),
                    true
            );
            return;
        }
        ItemStack remainingPesticide = pesticide.copy();
        if (!player.getAbilities().instabuild) {
            remainingPesticide.shrink(1);
        }
        // Both event stacks may be transient references owned by the click transaction. Write independent
        // copies back to both authoritative locations so cancelling the vanilla swap cannot discard either side.
        event.getSlot().set(contaminatedFood);
        event.getCarriedSlotAccess().set(remainingPesticide);
        event.getSlot().setChanged();
        player.containerMenu.broadcastChanges();
        player.displayClientMessage(
                Component.translatable("message.superficialtrauma.toxicology.food_contaminated"),
                true
        );
    }

    @SubscribeEvent
    public static void onUseItemFinished(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !FoodContamination.isDdvpContaminated(event.getItem())) {
            return;
        }
        BodyStateCapability.get(player).ifPresent(state -> {
            if (state.exposeToOrganophosphate(player.serverLevel().getGameTime())) {
                ModNetworking.syncBodyState(player);
                InspectionService.syncPatient(player);
            }
        });
    }
}
