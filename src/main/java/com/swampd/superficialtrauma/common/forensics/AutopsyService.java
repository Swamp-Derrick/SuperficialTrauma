package com.swampd.superficialtrauma.common.forensics;

import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.common.entity.CorpseEntity;
import com.swampd.superficialtrauma.common.init.ModItems;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AutopsyService {
    private static final double MAX_OPEN_DISTANCE_SQUARED = 4.5D * 4.5D;
    private static final double MAX_CONTINUE_DISTANCE_SQUARED = 6.0D * 6.0D;
    private static final double ACTION_MOVEMENT_TOLERANCE_SQUARED = 0.12D * 0.12D;
    private static final long PERIODIC_SYNC_TICKS = 10L;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private AutopsyService() {
    }

    public static boolean open(ServerPlayer examiner, int corpseEntityId) {
        if (!(examiner.serverLevel().getEntity(corpseEntityId) instanceof CorpseEntity corpse)
                || !canInspect(examiner, corpse, MAX_OPEN_DISTANCE_SQUARED, true)) {
            return false;
        }
        if (examiner.containerMenu != examiner.inventoryMenu) {
            examiner.closeContainer();
        }
        Session session = SESSIONS.get(examiner.getUUID());
        if (session == null || !session.corpseId.equals(corpse.getUUID())) {
            session = new Session(corpse.getUUID(), corpse.getId());
            SESSIONS.put(examiner.getUUID(), session);
        } else {
            session.corpseEntityId = corpse.getId();
        }
        sendReport(examiner, corpse, session, true);
        return true;
    }

    public static void start(ServerPlayer examiner, int corpseEntityId, AutopsyAction action) {
        Session session = SESSIONS.get(examiner.getUUID());
        CorpseEntity corpse = examiner.serverLevel().getEntity(corpseEntityId) instanceof CorpseEntity found
                ? found
                : null;
        if (corpse == null
                || action == null
                || action == AutopsyAction.NONE
                || !canInspect(examiner, corpse, MAX_OPEN_DISTANCE_SQUARED, true)) {
            examiner.displayClientMessage(Component.translatable("message.superficialtrauma.autopsy.invalid"), true);
            return;
        }
        if (session == null || !session.corpseId.equals(corpse.getUUID())) {
            session = new Session(corpse.getUUID(), corpse.getId());
            SESSIONS.put(examiner.getUUID(), session);
        }
        if (session.activeAction != AutopsyAction.NONE) {
            return;
        }
        boolean knowsForensics = BodyStateCapability.get(examiner)
                .map(bodyState -> bodyState.hasForensicSkill())
                .orElse(false);
        if (!knowsForensics) {
            examiner.displayClientMessage(Component.translatable("message.superficialtrauma.autopsy.skill_missing"), true);
            return;
        }
        if (isAlreadyRevealed(corpse, action)) {
            sendReport(examiner, corpse, session, false);
            return;
        }
        Item requiredItem = requiredItem(action);
        if (requiredItem == null || !hasItem(examiner, requiredItem)) {
            examiner.displayClientMessage(Component.translatable("message.superficialtrauma.autopsy.item_missing"), true);
            return;
        }
        if (SESSIONS.entrySet().stream().anyMatch(entry ->
                !entry.getKey().equals(examiner.getUUID())
                        && entry.getValue().corpseId.equals(corpse.getUUID())
                        && entry.getValue().activeAction != AutopsyAction.NONE)) {
            examiner.displayClientMessage(Component.translatable("message.superficialtrauma.autopsy.busy"), true);
            return;
        }

        session.activeAction = action;
        session.actionStartPosition = examiner.position();
        session.actionEndGameTime = examiner.serverLevel().getGameTime() + action.durationTicks();
        sendReport(examiner, corpse, session, false);
        examiner.displayClientMessage(
                Component.translatable("message.superficialtrauma.autopsy.started." + action.serializedName()),
                true
        );
    }

    public static void close(ServerPlayer examiner, int corpseEntityId) {
        Session session = SESSIONS.get(examiner.getUUID());
        if (session != null && session.corpseEntityId == corpseEntityId) {
            SESSIONS.remove(examiner.getUUID());
        }
    }

    public static void tick(ServerPlayer examiner) {
        Session session = SESSIONS.get(examiner.getUUID());
        if (session == null) {
            return;
        }
        CorpseEntity corpse = findCorpse(examiner, session.corpseId);
        if (corpse == null || !canInspect(examiner, corpse, MAX_CONTINUE_DISTANCE_SQUARED, false)) {
            SESSIONS.remove(examiner.getUUID());
            ModNetworking.closeAutopsy(examiner, session.corpseEntityId);
            return;
        }
        session.corpseEntityId = corpse.getId();
        long gameTime = examiner.serverLevel().getGameTime();
        if (session.activeAction != AutopsyAction.NONE) {
            boolean moved = session.actionStartPosition == null
                    || examiner.position().distanceToSqr(session.actionStartPosition) > ACTION_MOVEMENT_TOLERANCE_SQUARED;
            boolean missingSkill = BodyStateCapability.get(examiner)
                    .map(bodyState -> !bodyState.hasForensicSkill())
                    .orElse(true);
            Item requiredItem = requiredItem(session.activeAction);
            if (moved || missingSkill || requiredItem == null || !hasItem(examiner, requiredItem)
                    || !examiner.hasLineOfSight(corpse)) {
                cancelAction(examiner, corpse, session);
                return;
            }
            if (gameTime >= session.actionEndGameTime) {
                AutopsyAction completed = session.activeAction;
                if (completed == AutopsyAction.PENLIGHT) {
                    corpse.revealDeathTime();
                } else if (completed == AutopsyAction.CHECKLIST) {
                    corpse.revealDetailedAutopsy();
                }
                session.clearAction();
                sendReport(examiner, corpse, session, false);
                examiner.displayClientMessage(
                        Component.translatable("message.superficialtrauma.autopsy.completed." + completed.serializedName()),
                        true
                );
                return;
            }
        }
        if (gameTime - session.lastSyncGameTime >= PERIODIC_SYNC_TICKS) {
            sendReport(examiner, corpse, session, false);
        }
    }

    public static void forgetPlayer(UUID playerId) {
        SESSIONS.remove(playerId);
    }

    public static void clearAll() {
        SESSIONS.clear();
    }

    private static void cancelAction(ServerPlayer examiner, CorpseEntity corpse, Session session) {
        session.clearAction();
        sendReport(examiner, corpse, session, false);
        examiner.displayClientMessage(Component.translatable("message.superficialtrauma.autopsy.cancelled"), true);
    }

    private static void sendReport(ServerPlayer examiner, CorpseEntity corpse, Session session, boolean openScreen) {
        long gameTime = examiner.serverLevel().getGameTime();
        boolean knowsForensics = BodyStateCapability.get(examiner)
                .map(bodyState -> bodyState.hasForensicSkill())
                .orElse(false);
        AutopsyReport report = AutopsyReport.create(
                corpse,
                gameTime,
                knowsForensics,
                hasItem(examiner, ModItems.PUPIL_PENLIGHT.get()),
                hasItem(examiner, ModItems.CHECKLIST.get()),
                session.activeAction,
                session.actionEndGameTime
        );
        session.lastSyncGameTime = gameTime;
        ModNetworking.sendAutopsyReport(examiner, report, openScreen);
    }

    private static boolean canInspect(
            ServerPlayer examiner,
            CorpseEntity corpse,
            double maximumDistanceSquared,
            boolean requireLineOfSight
    ) {
        if (!examiner.isAlive()
                || examiner.isRemoved()
                || examiner.isSpectator()
                || corpse.isRemoved()
                || examiner.serverLevel() != corpse.level()
                || examiner.distanceToSqr(corpse) > maximumDistanceSquared
                || (requireLineOfSight && !examiner.hasLineOfSight(corpse))) {
            return false;
        }
        return BodyStateCapability.get(examiner).map(bodyState -> bodyState.canAct()).orElse(false);
    }

    private static boolean isAlreadyRevealed(CorpseEntity corpse, AutopsyAction action) {
        return action == AutopsyAction.PENLIGHT
                ? corpse.deathTimeRevealed()
                : action == AutopsyAction.CHECKLIST && corpse.detailedAutopsyRevealed();
    }

    private static Item requiredItem(AutopsyAction action) {
        return switch (action) {
            case PENLIGHT -> ModItems.PUPIL_PENLIGHT.get();
            case CHECKLIST -> ModItems.CHECKLIST.get();
            case NONE -> null;
        };
    }

    private static boolean hasItem(ServerPlayer player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    private static CorpseEntity findCorpse(ServerPlayer examiner, UUID corpseId) {
        return examiner.serverLevel().getEntity(corpseId) instanceof CorpseEntity corpse ? corpse : null;
    }

    private static final class Session {
        private final UUID corpseId;
        private int corpseEntityId;
        private AutopsyAction activeAction = AutopsyAction.NONE;
        private Vec3 actionStartPosition;
        private long actionEndGameTime = -1L;
        private long lastSyncGameTime = Long.MIN_VALUE;

        private Session(UUID corpseId, int corpseEntityId) {
            this.corpseId = corpseId;
            this.corpseEntityId = corpseEntityId;
        }

        private void clearAction() {
            activeAction = AutopsyAction.NONE;
            actionStartPosition = null;
            actionEndGameTime = -1L;
        }
    }
}
