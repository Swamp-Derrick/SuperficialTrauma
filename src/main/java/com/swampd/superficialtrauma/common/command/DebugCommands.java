package com.swampd.superficialtrauma.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.BodyState;
import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.common.body.WoundUpdateResult;
import com.swampd.superficialtrauma.common.wound.WoundInstance;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
                .then(Commands.literal("selftest").executes(DebugCommands::runSelfTest))
        );
    }

    private static int showOwnStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return showStatus(context, player);
    }

    private static int showStatus(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        AtomicInteger result = new AtomicInteger(0);
        BodyStateCapability.get(player).ifPresent(bodyState -> {
            context.getSource().sendSuccess(() -> Component.literal(
                    player.getGameProfile().getName()
                            + " BodyState v" + bodyState.dataVersion()
                            + " revision=" + bodyState.revision()
                            + " wounds=" + bodyState.wounds().size()
                            + " lastD=" + bodyState.lastFinalDamage()
                            + " type=" + bodyState.lastDamageType()
            ), false);
            for (WoundInstance wound : bodyState.wounds()) {
                context.getSource().sendSuccess(() -> Component.literal(
                        wound.type().serializedName()
                                + " severity=" + wound.severity()
                                + " A=" + wound.accumulatedDamage()
                                + " H=" + wound.healingProgress()
                ), false);
            }
            result.set(1);
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

        boolean passed = pending.status() == WoundUpdateResult.Status.PENDING
                && created.status() == WoundUpdateResult.Status.CREATED
                && created.wound() != null
                && upgraded.status() == WoundUpdateResult.Status.UPDATED
                && upgraded.wound() != null
                && upgraded.wound().severity() == 2
                && restored.dataVersion() == BodyState.CURRENT_DATA_VERSION
                && restored.wounds().size() == 1
                && restored.wounds().get(0).severity() == 2
                && Math.abs(restored.wounds().get(0).accumulatedDamage() - 4.0F) < 0.0001F;

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
