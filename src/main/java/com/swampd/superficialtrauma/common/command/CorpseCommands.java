package com.swampd.superficialtrauma.common.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.entity.CorpseCleanupService;
import com.swampd.superficialtrauma.common.entity.CorpseEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.TreeSet;

@EventBusSubscriber(modid = SuperficialTrauma.MOD_ID)
public final class CorpseCommands {
    private CorpseCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("superficialtrauma")
                .then(Commands.literal("corpse")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("clear")
                                .executes(context -> {
                                    context.getSource().sendSuccess(() -> Component.translatable(
                                            "command.superficialtrauma.corpse_clear.usage"), false);
                                    return 0;
                                })
                                .then(Commands.literal("all").executes(context -> clear(context.getSource(),
                                        CorpseCleanupService.loadedCorpses(context.getSource().getServer()))))
                                .then(Commands.literal("nearest")
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                .executes(context -> clear(context.getSource(), CorpseCleanupService.nearest(
                                                        context.getSource().getLevel(), context.getSource().getPosition(),
                                                        IntegerArgumentType.getInteger(context, "count"))))))
                                .then(Commands.literal("target").executes(context -> clear(context.getSource(),
                                        CorpseCleanupService.targeted(context.getSource().getPlayerOrException())
                                                .stream().toList())))
                                .then(Commands.literal("player")
                                        .then(Commands.argument("owner", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    var names = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
                                                    names.addAll(List.of(context.getSource().getServer().getPlayerNames()));
                                                    CorpseCleanupService.loadedCorpses(context.getSource().getServer())
                                                            .forEach(corpse -> names.add(corpse.ownerName()));
                                                    return SharedSuggestionProvider.suggest(names, builder);
                                                })
                                                .executes(context -> clear(context.getSource(), CorpseCleanupService.latestForOwner(
                                                        context.getSource().getServer(), StringArgumentType.getString(context, "owner"))
                                                        .stream().toList())))))));
    }

    private static int clear(CommandSourceStack source, List<CorpseEntity> corpses) {
        int count = CorpseCleanupService.clear(corpses);
        if (count == 0) {
            source.sendFailure(Component.translatable("command.superficialtrauma.corpse_clear.none"));
        } else {
            source.sendSuccess(() -> Component.translatable("command.superficialtrauma.corpse_clear.done", count), true);
        }
        return count;
    }
}
