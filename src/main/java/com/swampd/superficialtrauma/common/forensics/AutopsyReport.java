package com.swampd.superficialtrauma.common.forensics;

import com.swampd.superficialtrauma.common.body.DowningHitRecord;
import com.swampd.superficialtrauma.common.body.WoundHistoryEntry;
import com.swampd.superficialtrauma.common.entity.CorpseEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Client-safe view of a corpse. Unrevealed evidence is omitted, rather than merely hidden by the screen. */
public record AutopsyReport(
        int corpseEntityId,
        String ownerName,
        long deathAgeTicks,
        List<WoundHistoryEntry> visibleWounds,
        boolean detailedAutopsyRevealed,
        DowningHitRecord downingHit,
        boolean suspectedMyocardialInfarction,
        boolean examinerKnowsForensics,
        boolean examinerHasPenlight,
        boolean examinerHasChecklist,
        long penlightCooldownEndGameTime,
        AutopsyAction activeAction,
        long actionEndGameTime
) {
    private static final String TAG_CORPSE_ENTITY_ID = "CorpseEntityId";
    private static final String TAG_OWNER_NAME = "OwnerName";
    private static final String TAG_DEATH_AGE_TICKS = "DeathAgeTicks";
    private static final String TAG_VISIBLE_WOUNDS = "VisibleWounds";
    private static final String TAG_DETAILED = "DetailedAutopsyRevealed";
    private static final String TAG_DOWNING_HIT = "DowningHit";
    private static final String TAG_SUSPECTED_MYOCARDIAL_INFARCTION = "SuspectedMyocardialInfarction";
    private static final String TAG_KNOWS_FORENSICS = "ExaminerKnowsForensics";
    private static final String TAG_HAS_PENLIGHT = "ExaminerHasPenlight";
    private static final String TAG_HAS_CHECKLIST = "ExaminerHasChecklist";
    private static final String TAG_PENLIGHT_COOLDOWN_END = "PenlightCooldownEndGameTime";
    private static final String TAG_ACTIVE_ACTION = "ActiveAction";
    private static final String TAG_ACTION_END = "ActionEndGameTime";

    public AutopsyReport {
        ownerName = ownerName == null || ownerName.isBlank() ? "Unknown" : ownerName;
        deathAgeTicks = deathAgeTicks < 0L ? -1L : deathAgeTicks;
        visibleWounds = visibleWounds == null ? List.of() : List.copyOf(visibleWounds);
        penlightCooldownEndGameTime = Math.max(-1L, penlightCooldownEndGameTime);
        activeAction = activeAction == null ? AutopsyAction.NONE : activeAction;
        actionEndGameTime = activeAction == AutopsyAction.NONE ? -1L : Math.max(0L, actionEndGameTime);
        if (!detailedAutopsyRevealed) {
            downingHit = null;
            suspectedMyocardialInfarction = false;
        }
    }

    public static AutopsyReport create(
            CorpseEntity corpse,
            boolean examinerKnowsForensics,
            boolean examinerHasPenlight,
            boolean examinerHasChecklist,
            AutopsyAction activeAction,
            long actionEndGameTime
    ) {
        boolean detailed = corpse.detailedAutopsyRevealed();
        int maximumVisible = detailed ? 4 : 2;
        List<WoundHistoryEntry> chronological = corpse.forensicWoundHistory();
        int firstVisible = Math.max(0, chronological.size() - maximumVisible);
        List<WoundHistoryEntry> newestFirst = new ArrayList<>(chronological.subList(firstVisible, chronological.size()));
        Collections.reverse(newestFirst);
        long deathAge = corpse.deathAgeAtLastPupilInspection();
        return new AutopsyReport(
                corpse.getId(),
                corpse.ownerName(),
                deathAge,
                newestFirst,
                detailed,
                detailed ? corpse.forensicDowningHit().orElse(null) : null,
                detailed && corpse.voluntaryDeath(),
                examinerKnowsForensics,
                examinerHasPenlight,
                examinerHasChecklist,
                corpse.penlightCooldownEndGameTime(),
                activeAction,
                actionEndGameTime
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_CORPSE_ENTITY_ID, corpseEntityId);
        tag.putString(TAG_OWNER_NAME, ownerName);
        tag.putLong(TAG_DEATH_AGE_TICKS, deathAgeTicks);
        ListTag wounds = new ListTag();
        for (WoundHistoryEntry entry : visibleWounds) {
            wounds.add(entry.serializeNBT());
        }
        tag.put(TAG_VISIBLE_WOUNDS, wounds);
        tag.putBoolean(TAG_DETAILED, detailedAutopsyRevealed);
        if (downingHit != null) {
            tag.put(TAG_DOWNING_HIT, downingHit.serializeNBT());
        }
        tag.putBoolean(TAG_SUSPECTED_MYOCARDIAL_INFARCTION, suspectedMyocardialInfarction);
        tag.putBoolean(TAG_KNOWS_FORENSICS, examinerKnowsForensics);
        tag.putBoolean(TAG_HAS_PENLIGHT, examinerHasPenlight);
        tag.putBoolean(TAG_HAS_CHECKLIST, examinerHasChecklist);
        tag.putLong(TAG_PENLIGHT_COOLDOWN_END, penlightCooldownEndGameTime);
        tag.putString(TAG_ACTIVE_ACTION, activeAction.serializedName());
        tag.putLong(TAG_ACTION_END, actionEndGameTime);
        return tag;
    }

    public static AutopsyReport load(CompoundTag tag) {
        List<WoundHistoryEntry> wounds = new ArrayList<>();
        ListTag woundTags = tag.getList(TAG_VISIBLE_WOUNDS, Tag.TAG_COMPOUND);
        for (int index = 0; index < woundTags.size(); index++) {
            try {
                wounds.add(WoundHistoryEntry.deserializeNBT(woundTags.getCompound(index)));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed network evidence and keep the screen usable.
            }
        }
        boolean detailed = tag.getBoolean(TAG_DETAILED);
        DowningHitRecord hit = detailed && tag.contains(TAG_DOWNING_HIT, Tag.TAG_COMPOUND)
                ? DowningHitRecord.deserializeNBT(tag.getCompound(TAG_DOWNING_HIT))
                : null;
        return new AutopsyReport(
                tag.getInt(TAG_CORPSE_ENTITY_ID),
                tag.getString(TAG_OWNER_NAME),
                tag.contains(TAG_DEATH_AGE_TICKS, Tag.TAG_ANY_NUMERIC) ? tag.getLong(TAG_DEATH_AGE_TICKS) : -1L,
                wounds,
                detailed,
                hit,
                detailed && tag.getBoolean(TAG_SUSPECTED_MYOCARDIAL_INFARCTION),
                tag.getBoolean(TAG_KNOWS_FORENSICS),
                tag.getBoolean(TAG_HAS_PENLIGHT),
                tag.getBoolean(TAG_HAS_CHECKLIST),
                tag.contains(TAG_PENLIGHT_COOLDOWN_END, Tag.TAG_ANY_NUMERIC)
                        ? tag.getLong(TAG_PENLIGHT_COOLDOWN_END)
                        : -1L,
                AutopsyAction.fromSerializedName(tag.getString(TAG_ACTIVE_ACTION)),
                tag.contains(TAG_ACTION_END, Tag.TAG_ANY_NUMERIC) ? tag.getLong(TAG_ACTION_END) : -1L
        );
    }
}
