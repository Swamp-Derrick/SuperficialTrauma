package com.swampd.superficialtrauma;

import com.swampd.superficialtrauma.client.*;
import com.swampd.superficialtrauma.common.block.*;
import com.swampd.superficialtrauma.common.body.*;
import com.swampd.superficialtrauma.common.entity.*;
import com.swampd.superficialtrauma.common.forensics.AutopsyService;
import com.swampd.superficialtrauma.common.init.*;
import com.swampd.superficialtrauma.common.item.DefibrillatorItem;
import com.swampd.superficialtrauma.common.loot.LootTargetMenu;
import com.swampd.superficialtrauma.common.wound.WoundType;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Opt-in, development-only UI/packet smoke run. Never loaded from the distributed JAR. */
@EventBusSubscriber(modid = SuperficialTrauma.MOD_ID, value = Dist.CLIENT)
public final class MigrationClientSmoke {
    private static boolean started;
    private static int ticks;
    private static int stage;
    private static volatile int corpseId;
    private static String screenshot;
    private static Class<? extends Screen> expected;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("superficialtrauma.clientSmoke")) return;
        var mc = Minecraft.getInstance();
        if (!started && mc.getOverlay() == null && mc.screen != null) {
            SuperficialTrauma.LOGGER.info("Migration smoke begins from {}", mc.screen.getClass().getSimpleName());
            started = true;
            mc.options.pauseOnLostFocus = false;
            mc.getWindow().setWindowed(1920, 1080);
            mc.createWorldOpenFlows().createFreshLevel("migration-smoke-" + System.currentTimeMillis(),
                    new LevelSettings("Migration smoke", GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT),
                    new WorldOptions(20261003L, false, false),
                    access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                            .value().createWorldDimensions(), new TitleScreen());
        }
        if (mc.player == null || mc.level == null || !ClientBodyState.hasReceivedSnapshot()) return;
        if (Boolean.getBoolean("superficialtrauma.worldAudioSmoke")) {
            WorldAudioClientSmoke.tick();
            return;
        }
        if (Boolean.getBoolean("superficialtrauma.voicechatSmoke")) {
            VoicechatClientSmoke.tick();
            return;
        }
        if (++ticks % 80 != 1) return;
        if (expected != null && !expected.isInstance(mc.screen)) {
            throw new AssertionError("Migration UI expected " + expected + ", got " + mc.screen);
        }
        expected = null;
        switch (stage++) {
            case 0 -> onServer(player -> {
                player.teleportTo(0.5, -60, 0.5);
                var state = BodyStateCapability.get(player).orElseThrow();
                state.unlockFirstAidSkill(); state.unlockSurgerySkill(); state.unlockForensicSkill();
                state.applyDamage(WoundType.BLUNT, 7, player.level().getGameTime());
                for (var item : List.of(ModItems.STETHOSCOPE.get(), ModItems.BANDAGE.get(), ModItems.SYRINGE.get(),
                        ModItems.MORPHINE_VIAL.get(), ModItems.PUPIL_PENLIGHT.get(), ModItems.CHECKLIST.get())) {
                    player.getInventory().add(new ItemStack(item));
                }
                var level = player.serverLevel();
                level.setBlockAndUpdate(new BlockPos(2, -60, 0), ModBlocks.DEFIBRILLATOR_STATION.get().defaultBlockState());
                var station = (DefibrillatorStationBlockEntity) level.getBlockEntity(new BlockPos(2, -60, 0));
                for (int slot = 0; slot < 2; slot++) {
                    var defib = new ItemStack(ModItems.DEFIBRILLATOR.get());
                    DefibrillatorItem.setEnergy(defib, 100 + slot * 500);
                    station.setItem(slot, defib);
                }
                level.setBlockAndUpdate(new BlockPos(2, -60, 1), ModBlocks.MEDICAL_WORKBENCH.get().defaultBlockState());
                var corpse = ModEntities.CORPSE.get().create(level);
                corpse.initialize(new CorpseSnapshot(UUID.randomUUID(), "Smoke corpse", "", "", level.getGameTime(),
                        new DownedPoseSnapshot(level.getGameTime(), 37, DownedPosture.UNSAFE, DownedFallDirection.FADE_ONLY),
                        List.of(), null, CollapseReason.NONE, false, false), -1, -60, 1);
                corpse.setItem(0, new ItemStack(Items.DIAMOND));
                level.addFreshEntity(corpse);
                corpseId = corpse.getId();
                ModNetworking.syncBodyState(player);
            });
            case 1 -> health(2, 0);
            case 2 -> health(3, 1);
            case 3 -> health(4, 2);
            case 4 -> {
                mc.setScreen(null);
                onServer(player -> player.openMenu((DefibrillatorStationBlockEntity) player.level().getBlockEntity(new BlockPos(2, -60, 0)), new BlockPos(2, -60, 0)));
                expected = DefibrillatorStationScreen.class;
            }
            case 5 -> {
                mc.player.closeContainer();
                scale(2);
                onServer(player -> player.openMenu((MedicalWorkbenchBlockEntity) player.level().getBlockEntity(new BlockPos(2, -60, 1)), new BlockPos(2, -60, 1)));
                expected = MedicalWorkbenchScreen.class;
            }
            case 6 -> {
                mc.player.closeContainer();
                scale(3);
                onServer(player -> {
                    var corpse = (CorpseEntity) player.level().getEntity(corpseId);
                    player.openMenu(new SimpleMenuProvider((id, inv, who) -> new LootTargetMenu(id, inv, corpse),
                                    Component.literal("Smoke corpse")),
                            buffer -> { buffer.writeVarInt(corpseId); buffer.writeBoolean(true); });
                });
                expected = LootTargetScreen.class;
            }
            case 7 -> {
                mc.player.closeContainer();
                onServer(player -> AutopsyService.open(player, corpseId));
                expected = AutopsyScreen.class;
            }
            case 8 -> {
                mc.screen.onClose();
                mc.player.setYRot(90); mc.player.setXRot(25);
            }
            case 9 -> {
                SuperficialTrauma.LOGGER.info("MIGRATION CLIENT SMOKE PASSED: health x2/x3/x4, station, workbench, corpse loot, autopsy, entity rendering, client/server payloads");
                mc.stop();
            }
        }
        SuperficialTrauma.LOGGER.info("Migration UI stage {}", stage);
    }

    private static void health(int scale, int panel) throws Exception {
        scale(scale);
        var mc = Minecraft.getInstance();
        var screen = new HealthScreen();
        mc.setScreen(screen);
        var type = Class.forName(HealthScreen.class.getName() + "$PanelMode");
        var method = HealthScreen.class.getDeclaredMethod("switchPanelMode", type);
        method.setAccessible(true);
        method.invoke(screen, type.getEnumConstants()[panel]);
        expected = HealthScreen.class;
    }

    private static void scale(int value) {
        var mc = Minecraft.getInstance();
        mc.options.guiScale().set(value);
        mc.resizeDisplay();
    }

    private static void onServer(Consumer<ServerPlayer> action) {
        var mc = Minecraft.getInstance();
        var id = mc.player.getUUID();
        mc.getSingleplayerServer().execute(() -> action.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(id)));
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) {
        if (!Boolean.getBoolean("superficialtrauma.clientSmoke") || ticks % 80 != 55) return;
        String name = "migration-ui-" + stage + ".png";
        if (name.equals(screenshot)) return;
        screenshot = name;
        var mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> {});
    }
}
