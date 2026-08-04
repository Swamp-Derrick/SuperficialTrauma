package com.swampd.superficialtrauma.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.BodyState;
import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.common.body.BodyProgressionResult;
import com.swampd.superficialtrauma.common.body.BodyLifeState;
import com.swampd.superficialtrauma.common.body.CollapseReason;
import com.swampd.superficialtrauma.common.body.WoundUpdateResult;
import com.swampd.superficialtrauma.common.damage.CgmAmmoTags;
import com.swampd.superficialtrauma.common.damage.DamageKind;
import com.swampd.superficialtrauma.common.wound.WoundInstance;
import com.swampd.superficialtrauma.common.wound.WoundType;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.concurrent.atomic.AtomicInteger;

@Mod.EventBusSubscriber(modid = SuperficialTrauma.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DebugCommands {
    private DebugCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("superficialtrauma")
                .then(Commands.literal("status")
                        .executes(DebugCommands::showOwnStatus)
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> showStatus(
                                        context,
                                        EntityArgument.getPlayer(context, "player")
                                )))
                )
                .then(Commands.literal("classifyammo").executes(DebugCommands::classifyHeldAmmo))
                .then(Commands.literal("selftest").executes(DebugCommands::runSelfTest))
                .then(Commands.literal("recover")
                        .requires(source -> source.hasPermission(2))
                        .executes(DebugCommands::recoverSelf)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> recoverPlayer(
                                        context,
                                        EntityArgument.getPlayer(context, "player")
                                )))
                )
        );
    }

    private static int showOwnStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return showStatus(context, player);
    }

    private static int showStatus(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        AtomicInteger result = new AtomicInteger(0);
        long gameTime = player.serverLevel().getGameTime();
        BodyStateCapability.get(player).ifPresent(bodyState -> {
            context.getSource().sendSuccess(() -> Component.literal(
                    player.getGameProfile().getName()
                            + " BodyState v" + bodyState.dataVersion()
                            + " revision=" + bodyState.revision()
                            + " lifeState=" + bodyState.lifeState().serializedName()
                            + " collapseReason=" + bodyState.collapseReason().serializedName()
                            + " wounds=" + bodyState.wounds().size()
                            + " pain=" + bodyState.pain()
                            + " basePain=" + bodyState.basePain()
                            + " woundPain=" + bodyState.woundPainContribution()
                            + " stress=" + bodyState.stressRemainingTicks(gameTime) + "t"
                            + " shockWarning=" + bodyState.shockWarningRemainingTicks(gameTime) + "t"
                            + " danger=" + bodyState.downedDangerRemainingTicks(gameTime) + "t"
                            + " oxygen=" + bodyState.bloodOxygen()
                            + " movementBleeding=" + bodyState.movementBleedingActive()
                            + " lastD=" + bodyState.lastFinalDamage()
                            + " type=" + bodyState.lastDamageType()
                            + " class=" + bodyState.lastDamageKind().serializedName()
                            + " ammo=" + bodyState.lastAmmoId()
                            + " weapon=" + bodyState.lastWeaponId()
            ), false);
            for (WoundInstance wound : bodyState.wounds()) {
                context.getSource().sendSuccess(() -> Component.literal(
                        wound.type().serializedName()
                                + " severity=" + wound.severity()
                                + " A=" + wound.accumulatedDamage()
                                + " H=" + wound.healingProgress()
                                + " natural=" + wound.baseHealingPerSecond() + "/s"
                                + " bleeding=" + wound.bleedingLevel(bodyState.movementBleedingActive())
                                + " nextBleed=" + wound.nextBleedingGameTime()
                ), false);
            }
            result.set(1);
        });
        return result.get();
    }

    private static int classifyHeldAmmo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack heldStack = player.getMainHandItem();
        if (heldStack.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Hold an ammunition item in your main hand first."));
            return 0;
        }

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(heldStack.getItem());
        DamageKind kind = CgmAmmoTags.classify(heldStack);
        context.getSource().sendSuccess(() -> Component.literal(
                "Ammo " + (itemId == null ? "unknown" : itemId)
                        + " classified=" + kind.serializedName()
        ), false);
        return kind == DamageKind.CGM_UNCLASSIFIED ? 0 : 1;
    }

    private static int recoverSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return recoverPlayer(context, context.getSource().getPlayerOrException());
    }

    private static int recoverPlayer(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        AtomicInteger result = new AtomicInteger(0);
        BodyStateCapability.get(player).ifPresent(bodyState -> {
            boolean changed = bodyState.forceRecoverForDebug();
            String playerName = player.getGameProfile().getName();
            if (changed) {
                ModNetworking.syncBodyState(player);
                context.getSource().sendSuccess(
                        () -> Component.translatable(
                                "command.superficialtrauma.recover.success",
                                playerName
                        ),
                        true
                );
                result.set(1);
            } else {
                context.getSource().sendFailure(Component.translatable(
                        "command.superficialtrauma.recover.unchanged",
                        playerName
                ));
            }
        });
        return result.get();
    }

    private static int runSelfTest(CommandContext<CommandSourceStack> context) {
        BodyState original = new BodyState();
        WoundUpdateResult pending = original.applyBluntDamage(1.0F, 100L);
        WoundUpdateResult created = original.applyBluntDamage(0.5F, 101L);
        WoundUpdateResult upgraded = original.applyBluntDamage(2.5F, 102L);

        BodyState restored = new BodyState();
        restored.deserializeNBT(original.serializeNBT());

        BodyState classificationState = new BodyState();
        WoundUpdateResult explosionPending = classificationState.applyDamage(WoundType.EXPLOSION, 3.0F, 200L);
        WoundUpdateResult explosionCreated = classificationState.applyDamage(WoundType.EXPLOSION, 1.0F, 201L);
        WoundUpdateResult sharpCreated = classificationState.applyDamage(WoundType.SHARP, 0.5F, 202L);
        WoundUpdateResult burnCreated = classificationState.applyDamage(WoundType.BURN, 0.1F, 203L);

        BodyState progressionState = new BodyState();
        progressionState.applyDamage(WoundType.BLUNT, 1.5F, 300L);
        progressionState.resumeBodyProgression(300L);
        BodyProgressionResult progression = progressionState.advanceBodyProgression(320L);

        BodyState painState = new BodyState();
        painState.applyDamage(WoundType.SHARP, 5.0F, 0L);
        painState.resumeBodyProgression(0L);
        BodyProgressionResult painRecovery = painState.advanceBodyProgression(430L);

        BodyState bleedingState = new BodyState();
        bleedingState.applyDamage(WoundType.SHARP, 15.0F, 0L);
        bleedingState.resumeBodyProgression(0L);
        BodyProgressionResult bleedingPulse = bleedingState.advanceBodyProgression(100L);

        BodyState shockState = new BodyState();
        shockState.applyDamage(WoundType.SHARP, 16.0F, 0L);
        shockState.resumeBodyProgression(0L);
        BodyProgressionResult shockWarning = shockState.advanceBodyProgression(400L);
        BodyProgressionResult incapacitated = shockState.advanceBodyProgression(600L);

        BodyState downedState = new BodyState();
        boolean enteredDowned = downedState.incapacitate(CollapseReason.LETHAL_DAMAGE, 1_000L);
        var downedHit = downedState.applyDownedDamage(2.5F, 1_000L);
        BodyProgressionResult cardiacArrest = downedState.advanceBodyProgression(4_100L);

        boolean passed = pending.status() == WoundUpdateResult.Status.PENDING
                && created.status() == WoundUpdateResult.Status.CREATED
                && created.wound() != null
                && upgraded.status() == WoundUpdateResult.Status.UPDATED
                && upgraded.wound() != null
                && upgraded.wound().severity() == 2
                && restored.dataVersion() == BodyState.CURRENT_DATA_VERSION
                && restored.wounds().size() == 1
                && restored.wounds().get(0).severity() == 2
                && Math.abs(restored.wounds().get(0).accumulatedDamage() - 4.0F) < 0.0001F
                && explosionPending.status() == WoundUpdateResult.Status.PENDING
                && explosionCreated.status() == WoundUpdateResult.Status.CREATED
                && sharpCreated.status() == WoundUpdateResult.Status.CREATED
                && burnCreated.status() == WoundUpdateResult.Status.CREATED
                && classificationState.wounds().size() == 3
                && progression.changed()
                && progression.progressedWounds() == 1
                && progression.healedWounds() == 0
                && Math.abs(progressionState.wounds().get(0).healingProgress() - 99.0F) < 0.0001F
                && Math.abs(progressionState.basePain() - 1.5F) < 0.0001F
                && Math.abs(painRecovery.recoveredBasePain() - 1.0F) < 0.0001F
                && Math.abs(painState.basePain() - 4.0F) < 0.0001F
                && Math.abs(painState.pain() - 5.0F) < 0.0001F
                && Math.abs(bleedingPulse.bleedingDamage() - 1.0F) < 0.0001F
                && shockWarning.shockWarningStarted()
                && incapacitated.becameIncapacitated()
                && shockState.lifeState() == BodyLifeState.INCAPACITATED
                && shockState.collapseReason() == CollapseReason.TRAUMATIC_SHOCK
                && enteredDowned
                && downedHit.applied()
                && downedHit.shortenedTicks() == 500L
                && downedHit.remainingTicks() == 3_100L
                && cardiacArrest.becameCardiacArrest()
                && downedState.lifeState() == BodyLifeState.CARDIAC_ARREST;

        if (passed) {
            context.getSource().sendSuccess(
                    () -> Component.literal("Superficial Trauma BodyState/NBT self-test passed."),
                    false
            );
            return 1;
        }

        context.getSource().sendFailure(Component.literal("Superficial Trauma BodyState/NBT self-test failed."));
        return 0;
    }
}
