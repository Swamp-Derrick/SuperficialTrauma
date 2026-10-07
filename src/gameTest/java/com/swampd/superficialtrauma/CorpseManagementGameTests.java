package com.swampd.superficialtrauma;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.swampd.superficialtrauma.common.body.*;
import com.swampd.superficialtrauma.common.drag.BodyDragService;
import com.swampd.superficialtrauma.common.entity.*;
import com.swampd.superficialtrauma.common.forensics.AutopsyAction;
import com.swampd.superficialtrauma.common.forensics.AutopsyService;
import com.swampd.superficialtrauma.common.init.ModEntities;
import com.swampd.superficialtrauma.common.init.ModItems;
import com.swampd.superficialtrauma.common.item.DefibrillatorItem;
import com.swampd.superficialtrauma.common.loot.LootTargetMenu;
import com.swampd.superficialtrauma.common.loot.LootingService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder(SuperficialTrauma.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CorpseManagementGameTests {
    @GameTest(template = "migration_empty", batch = "corpse_management")
    public static void dragClosesMenusAndRejectsStaleClicks(GameTestHelper helper) throws Exception {
        try (var fixture = new Fixture(helper)) {
            var dragger = fixture.player("Dragger", 2, 2, 2);
            var looter = fixture.player("Looter", 2, 2, 2);
            var patient = fixture.player("Patient", 3, 2, 2);
            BodyStateCapability.get(patient).orElseThrow().forceCardiacRhythmForDebug(
                    BodyLifeState.VENTRICULAR_FIBRILLATION, helper.getLevel().getGameTime());
            var corpse = fixture.corpse(UUID.randomUUID(), "Owner", 1, 3, 2, 2);
            dragger.setShiftKeyDown(true);
            for (Entity target : List.of(corpse, patient)) {
                var inventory = target instanceof CorpseEntity body ? body : patient.getInventory();
                inventory.setItem(9, new ItemStack(Items.DIAMOND, 3));
                var menu = target instanceof CorpseEntity body
                        ? new LootTargetMenu(1, looter.getInventory(), body)
                        : new LootTargetMenu(1, looter.getInventory(), patient);
                helper.assertTrue(menu.stillValid(looter), "Stationary body must be lootable");
                looter.containerMenu = menu;
                // A previously picked-up stack must return to the viewer, not disappear on forced close.
                menu.setCarried(new ItemStack(Items.EMERALD, 2));
                int emeralds = looter.getInventory().countItem(Items.EMERALD);
                BodyDragService.setHolding(dragger, target.getId(), true);
                helper.assertTrue(BodyDragService.isBeingDragged(target), "Fixture must start dragging");
                helper.assertTrue(looter.containerMenu == looter.inventoryMenu, "Starting drag must immediately close loot menu");
                helper.assertTrue(looter.getInventory().countItem(Items.EMERALD) == emeralds + 2,
                        "Forced close must return the cursor stack exactly once");
                helper.assertTrue(!LootingService.tryOpen(looter, target.getId()), "Dragged target must reject reopening");
                helper.assertTrue(!menu.stillValid(looter), "Old menu must become invalid during dragging");
                menu.clicked(0, 0, ClickType.PICKUP, looter);
                menu.clicked(0, 0, ClickType.SWAP, looter);
                menu.clicked(0, 1, ClickType.THROW, looter);
                helper.assertTrue(menu.quickMoveStack(looter, 0).isEmpty(), "Stale quick move must be rejected");
                helper.assertTrue(!menu.clickMenuButton(looter, LootTargetMenu.TAKE_ALL_BUTTON_ID), "Stale take-all must be rejected");
                helper.assertTrue(inventory.getItem(9).getCount() == 3 && menu.getCarried().isEmpty(),
                        "Rejected clicks cannot move or duplicate target items");
                BodyDragService.setHolding(dragger, target.getId(), false);
                helper.assertTrue(menu.stillValid(looter), "Releasing drag must restore loot eligibility");
                helper.assertTrue(LootingService.tryOpen(looter, target.getId()), "Target can be opened again after dragging");
                LootingService.release(target.getUUID(), looter.getUUID());
            }
        }
        helper.succeed();
    }

    @GameTest(template = "migration_empty", batch = "corpse_management")
    public static void cleanupDropsAllSlotsWithComponentsOnlyOnce(GameTestHelper helper) throws Exception {
        try (var fixture = new Fixture(helper)) {
            var dragger = fixture.player("Dragger", 2, 2, 2);
            var looter = fixture.player("Looter", 2, 2, 2);
            var examiner = fixture.player("Examiner", 2, 2, 2);
            var corpse = fixture.corpse(UUID.randomUUID(), "OfflineOwner", 20, 3, 2, 2);
            List<ItemStack> expected = new ArrayList<>();
            for (int slot = 0; slot < corpse.getContainerSize(); slot++) {
                var stack = new ItemStack(Items.GOLD_NUGGET, slot + 1);
                if (slot == 0) {
                    stack = new ItemStack(ModItems.DEFIBRILLATOR.get(), 2);
                    DefibrillatorItem.setEnergy(stack, 123);
                } else if (slot == 36) {
                    stack = new ItemStack(Items.DIAMOND_BOOTS);
                    stack.setDamageValue(12);
                    stack.set(DataComponents.CUSTOM_NAME, Component.literal("Named equipment"));
                } else if (slot == 40) {
                    stack = new ItemStack(Items.SHIELD);
                    stack.setDamageValue(20);
                }
                corpse.setItem(slot, stack);
                expected.add(stack.copy());
            }
            var alreadyTaken = corpse.removeItem(9, 4);
            expected.get(9).shrink(4);
            looter.getInventory().add(alreadyTaken);
            looter.containerMenu = new LootTargetMenu(1, looter.getInventory(), corpse);
            helper.assertTrue(AutopsyService.open(examiner, corpse.getId()), "Autopsy must open before cleanup");
            BodyStateCapability.get(examiner).orElseThrow().unlockForensicSkill();
            examiner.getInventory().add(new ItemStack(ModItems.PUPIL_PENLIGHT.get()));
            AutopsyService.start(examiner, corpse.getId(), AutopsyAction.PENLIGHT);
            dragger.setShiftKeyDown(true);
            BodyDragService.setHolding(dragger, corpse.getId(), true);
            // Simulate a stale viewer still present when the administrative command arrives.
            looter.containerMenu = new LootTargetMenu(2, looter.getInventory(), corpse);
            boolean oldDrops = helper.getLevel().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS);
            try {
                helper.getLevel().getGameRules().getRule(GameRules.RULE_DOENTITYDROPS).set(false, helper.getLevel().getServer());
                helper.assertTrue(CorpseCleanupService.clear(corpse), "Cleanup must remove a live corpse");
            } finally {
                helper.getLevel().getGameRules().getRule(GameRules.RULE_DOENTITYDROPS).set(oldDrops, helper.getLevel().getServer());
            }
            helper.assertTrue(corpse.isRemoved() && corpse.isEmpty(), "Removed corpse must retain no inventory");
            helper.assertTrue(!BodyDragService.isBeingDragged(corpse), "Cleanup must end drag session immediately");
            helper.assertTrue(looter.containerMenu == looter.inventoryMenu, "Cleanup must close loot menu immediately");
            var sessionsField = AutopsyService.class.getDeclaredField("SESSIONS");
            sessionsField.setAccessible(true);
            helper.assertTrue(!((Map<?, ?>) sessionsField.get(null)).containsKey(examiner.getUUID()), "Cleanup must end autopsy immediately");
            List<ItemStack> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    corpse.getBoundingBox().inflate(1)).stream().map(ItemEntity::getItem).toList();
            for (ItemStack expectedStack : expected) {
                int expectedCount = expected.stream().filter(stack -> ItemStack.isSameItemSameComponents(stack, expectedStack))
                        .mapToInt(ItemStack::getCount).sum();
                int actualCount = dropped.stream().filter(stack -> ItemStack.isSameItemSameComponents(stack, expectedStack))
                        .mapToInt(ItemStack::getCount).sum();
                helper.assertTrue(expectedCount == actualCount, "Every slot/component must drop exactly once: " + expectedStack);
            }
            helper.assertTrue(expected.stream().mapToInt(ItemStack::getCount).sum() == dropped.stream().mapToInt(ItemStack::getCount).sum(),
                    "No extra equipment drops or missing items");
            helper.assertTrue(looter.getInventory().countItem(Items.GOLD_NUGGET) == 4, "Previously taken items remain with looter");
            helper.assertTrue(!CorpseCleanupService.clear(corpse), "Repeated cleanup must be idempotent");
        }
        helper.succeed();
    }

    @GameTest(template = "migration_empty", batch = "corpse_management")
    public static void commandSelectionPermissionsAndTargetObstruction(GameTestHelper helper) throws Exception {
        try (var fixture = new Fixture(helper)) {
            var admin = fixture.player("Admin", 3, 2, 1);
            var server = helper.getLevel().getServer();
            var source = admin.createCommandSourceStack().withPermission(2).withSuppressedOutput();
            UUID owner = UUID.randomUUID();
            var old = fixture.corpse(owner, "OfflineOwner", 10, 2, 2, 4);
            var latest = fixture.corpse(owner, "OfflineOwner", 30, 4, 2, 4);
            var near = fixture.corpse(UUID.randomUUID(), "Other", 20, 3, 2, 2);
            var dispatcher = server.getCommands().getDispatcher();
            boolean rejected = false;
            try { dispatcher.execute("superficialtrauma corpse clear all", source.withPermission(0)); }
            catch (CommandSyntaxException expected) { rejected = true; }
            helper.assertTrue(rejected && !old.isRemoved() && !near.isRemoved(), "Non-admin cleanup must be denied");
            rejected = false;
            try { dispatcher.execute("superficialtrauma corpse clear nearest 0", source); }
            catch (CommandSyntaxException expected) { rejected = true; }
            helper.assertTrue(rejected, "Zero/negative nearest counts must be rejected");
            helper.assertTrue(dispatcher.execute("superficialtrauma corpse clear nearest 1", source) == 1 && near.isRemoved()
                    && !old.isRemoved() && !latest.isRemoved(), "Nearest must use distance rather than creation time");
            helper.assertTrue(dispatcher.execute("superficialtrauma corpse clear player offlineowner", source) == 1
                    && latest.isRemoved() && !old.isRemoved(), "Offline owner lookup must remove only newest corpse, ignoring name case");
            helper.assertTrue(CorpseCleanupService.latestForOwner(server, owner.toString()).orElseThrow() == old,
                    "Owner UUID lookup must remain available");
            var aimed = fixture.corpse(UUID.randomUUID(), "Aimed", 40, 3, 2, 3);
            Vec3 aim = aimed.position().add(0, 0.3, 0).subtract(admin.getEyePosition());
            admin.setYRot((float) Math.toDegrees(Math.atan2(-aim.x, aim.z)));
            admin.setXRot((float) -Math.toDegrees(Math.atan2(aim.y, Math.sqrt(aim.x * aim.x + aim.z * aim.z))));
            helper.assertTrue(CorpseCleanupService.targeted(admin).orElseThrow() == aimed, "Crosshair ray must select corpse");
            BlockPos wall = helper.absolutePos(new BlockPos(3, 3, 2));
            helper.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
            helper.assertTrue(CorpseCleanupService.targeted(admin).isEmpty(), "Cannot select corpse through a wall");
            helper.getLevel().setBlockAndUpdate(wall, Blocks.AIR.defaultBlockState());
            helper.assertTrue(dispatcher.execute("superficialtrauma corpse clear target", source) == 1 && aimed.isRemoved(),
                    "Target command must remove the selected corpse only");
            helper.assertTrue(dispatcher.execute("superficialtrauma corpse clear player " + owner, source) == 1 && old.isRemoved(),
                    "Player command must accept UUID");
            helper.assertTrue(dispatcher.execute("superficialtrauma corpse clear player MissingOwner", source) == 0,
                    "No matching owner must not delete another corpse");
            var remaining = fixture.corpse(UUID.randomUUID(), "All", 1, 3, 2, 4);
            int count = CorpseCleanupService.loadedCorpses(server).size();
            helper.assertTrue(dispatcher.execute("superficialtrauma corpse clear all", source) == count && remaining.isRemoved()
                    && CorpseCleanupService.loadedCorpses(server).isEmpty(), "All command must clear every loaded corpse");
            helper.assertTrue(!admin.isRemoved() && admin.isAlive(), "Cleanup must never delete a player");
            helper.assertTrue(dispatcher.execute("superficialtrauma corpse clear all", source) == 0, "Empty cleanup must be harmless");
        }
        helper.succeed();
    }

    private static final class Fixture implements AutoCloseable {
        private final GameTestHelper helper;
        private final List<Entity> entities = new ArrayList<>();
        private final Map<UUID, ServerPlayer> playerLookup;
        private final List<ServerPlayer> connectedPlayers;

        @SuppressWarnings("unchecked")
        private Fixture(GameTestHelper helper) throws Exception {
            this.helper = helper;
            var field = net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");
            field.setAccessible(true);
            playerLookup = (Map<UUID, ServerPlayer>) field.get(helper.getLevel().getServer().getPlayerList());
            var playersField = net.minecraft.server.players.PlayerList.class.getDeclaredField("players");
            playersField.setAccessible(true);
            connectedPlayers = (List<ServerPlayer>) playersField.get(helper.getLevel().getServer().getPlayerList());
        }

        private FakePlayer player(String name, int x, int y, int z) {
            var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
            player.moveTo(helper.absoluteVec(new Vec3(x + 0.5, y, z + 0.5)));
            helper.getLevel().addNewPlayer(player);
            connectedPlayers.add(player);
            playerLookup.put(player.getUUID(), player);
            entities.add(player);
            return player;
        }

        private CorpseEntity corpse(UUID owner, String name, long time, int x, int y, int z) {
            var corpse = ModEntities.CORPSE.get().create(helper.getLevel());
            Vec3 pos = helper.absoluteVec(new Vec3(x + 0.5, y, z + 0.5));
            corpse.initialize(new CorpseSnapshot(owner, name, "", "", time,
                    new DownedPoseSnapshot(time, 0, DownedPosture.UNSAFE, DownedFallDirection.FADE_ONLY),
                    List.of(), null, CollapseReason.NONE, false, false), pos.x, pos.y, pos.z);
            helper.getLevel().addFreshEntity(corpse);
            entities.add(corpse);
            return corpse;
        }

        @Override
        public void close() {
            for (Entity entity : entities) {
                if (entity instanceof ServerPlayer player) {
                    player.closeContainer();
                    BodyDragService.forgetPlayer(player.getUUID());
                    LootingService.forgetPlayer(player.getUUID());
                    AutopsyService.forgetPlayer(player.getUUID());
                    connectedPlayers.remove(player);
                    playerLookup.remove(player.getUUID());
                }
                entity.discard();
            }
        }
    }
}
