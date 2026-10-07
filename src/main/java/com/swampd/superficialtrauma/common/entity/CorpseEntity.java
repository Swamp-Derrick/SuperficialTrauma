package com.swampd.superficialtrauma.common.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.swampd.superficialtrauma.common.body.DownedGeometry;
import com.swampd.superficialtrauma.common.body.CollapseReason;
import com.swampd.superficialtrauma.common.body.DownedPoseSnapshot;
import com.swampd.superficialtrauma.common.body.DownedPosture;
import com.swampd.superficialtrauma.common.body.DownedFallDirection;
import com.swampd.superficialtrauma.common.body.DowningHitRecord;
import com.swampd.superficialtrauma.common.body.WoundHistoryEntry;
import com.swampd.superficialtrauma.common.config.CorpseServerConfig;
import com.swampd.superficialtrauma.common.drag.BodyDragService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class CorpseEntity extends LivingEntity implements Container {
    public static final int INVENTORY_SIZE = 41;
    private static final String TAG_INVENTORY = "CorpseInventory";
    private static final String TAG_EMPTY_SINCE_GAME_TIME = "EmptySinceGameTime";
    private static final String TAG_DEATH_TIME_REVEALED = "DeathTimeRevealed";
    private static final String TAG_DEATH_TIME_INSPECTION_GAME_TIME = "DeathTimeInspectionGameTime";
    private static final String TAG_PENLIGHT_COOLDOWN_END_GAME_TIME = "PenlightCooldownEndGameTime";
    private static final String TAG_DETAILED_AUTOPSY_REVEALED = "DetailedAutopsyRevealed";
    public static final long PENLIGHT_REEXAMINATION_COOLDOWN_TICKS = 30L * 20L;
    private static final EntityDataAccessor<Optional<UUID>> OWNER_ID = SynchedEntityData.defineId(
            CorpseEntity.class,
            EntityDataSerializers.OPTIONAL_UUID
    );
    private static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(
            CorpseEntity.class,
            EntityDataSerializers.STRING
    );
    private static final EntityDataAccessor<String> SKIN_TEXTURE_VALUE = SynchedEntityData.defineId(
            CorpseEntity.class,
            EntityDataSerializers.STRING
    );
    private static final EntityDataAccessor<String> SKIN_TEXTURE_SIGNATURE = SynchedEntityData.defineId(
            CorpseEntity.class,
            EntityDataSerializers.STRING
    );
    private static final EntityDataAccessor<Long> DEATH_GAME_TIME = SynchedEntityData.defineId(
            CorpseEntity.class,
            EntityDataSerializers.LONG
    );
    private static final EntityDataAccessor<Long> DOWNED_GAME_TIME = SynchedEntityData.defineId(
            CorpseEntity.class,
            EntityDataSerializers.LONG
    );
    private static final EntityDataAccessor<Float> BODY_YAW = SynchedEntityData.defineId(
            CorpseEntity.class,
            EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<String> POSTURE = SynchedEntityData.defineId(
            CorpseEntity.class,
            EntityDataSerializers.STRING
    );
    private static final EntityDataAccessor<String> FALL_DIRECTION = SynchedEntityData.defineId(
            CorpseEntity.class,
            EntityDataSerializers.STRING
    );
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private long emptySinceGameTime = EmptyCorpseLifecycle.NOT_EMPTY;
    private List<WoundHistoryEntry> forensicWoundHistory = List.of();
    private DowningHitRecord forensicDowningHit;
    private CollapseReason forensicCollapseReason = CollapseReason.NONE;
    private boolean voluntaryDeath;
    private boolean administrativeDeath;
    private boolean deathTimeRevealed;
    private long deathTimeInspectionGameTime = -1L;
    private long penlightCooldownEndGameTime = -1L;
    private boolean detailedAutopsyRevealed;

    public CorpseEntity(EntityType<? extends CorpseEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(false);
        setInvulnerable(true);
        setHealth(1.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_ID, Optional.empty());
        builder.define(OWNER_NAME, "Unknown");
        builder.define(SKIN_TEXTURE_VALUE, "");
        builder.define(SKIN_TEXTURE_SIGNATURE, "");
        builder.define(DEATH_GAME_TIME, 0L);
        builder.define(DOWNED_GAME_TIME, 0L);
        builder.define(BODY_YAW, 0.0F);
        builder.define(POSTURE, DownedPosture.UNSAFE.serializedName());
        builder.define(FALL_DIRECTION, DownedFallDirection.FADE_ONLY.serializedName());
    }

    public void initialize(CorpseSnapshot snapshot, double x, double y, double z) {
        entityData.set(OWNER_ID, Optional.of(snapshot.ownerId()));
        entityData.set(OWNER_NAME, snapshot.ownerName());
        entityData.set(SKIN_TEXTURE_VALUE, snapshot.skinTextureValue());
        entityData.set(SKIN_TEXTURE_SIGNATURE, snapshot.skinTextureSignature());
        entityData.set(DEATH_GAME_TIME, snapshot.deathGameTime());
        entityData.set(DOWNED_GAME_TIME, snapshot.downedPose().downedGameTime());
        entityData.set(BODY_YAW, snapshot.downedPose().bodyYaw());
        entityData.set(POSTURE, snapshot.downedPose().posture().serializedName());
        entityData.set(FALL_DIRECTION, snapshot.downedPose().fallDirection().serializedName());
        forensicWoundHistory = snapshot.woundHistory();
        forensicDowningHit = snapshot.downingHitRecord();
        forensicCollapseReason = snapshot.collapseReason();
        voluntaryDeath = snapshot.voluntaryDeath();
        administrativeDeath = snapshot.administrativeDeath();
        moveTo(x, y, z, snapshot.downedPose().bodyYaw(), 0.0F);
        setYBodyRot(snapshot.downedPose().bodyYaw());
        setYHeadRot(snapshot.downedPose().bodyYaw());
        updateCorpseBoundingBox();
    }

    public CorpseSnapshot snapshot() {
        return new CorpseSnapshot(
                ownerId().orElse(new UUID(0L, 0L)),
                entityData.get(OWNER_NAME),
                entityData.get(SKIN_TEXTURE_VALUE),
                entityData.get(SKIN_TEXTURE_SIGNATURE),
                entityData.get(DEATH_GAME_TIME),
                downedPose(),
                forensicWoundHistory,
                forensicDowningHit,
                forensicCollapseReason,
                voluntaryDeath,
                administrativeDeath
        );
    }

    public List<WoundHistoryEntry> forensicWoundHistory() {
        return forensicWoundHistory;
    }

    public Optional<DowningHitRecord> forensicDowningHit() {
        return Optional.ofNullable(forensicDowningHit);
    }

    public CollapseReason forensicCollapseReason() {
        return forensicCollapseReason;
    }

    public boolean voluntaryDeath() {
        return voluntaryDeath;
    }

    public boolean administrativeDeath() {
        return administrativeDeath;
    }

    public boolean deathTimeRevealed() {
        return deathTimeRevealed;
    }

    public boolean detailedAutopsyRevealed() {
        return detailedAutopsyRevealed;
    }

    public long deathAgeAtLastPupilInspection() {
        return deathTimeRevealed && deathTimeInspectionGameTime >= 0L
                ? Math.max(0L, deathTimeInspectionGameTime - entityData.get(DEATH_GAME_TIME))
                : -1L;
    }

    public long penlightCooldownEndGameTime() {
        return penlightCooldownEndGameTime;
    }

    public boolean canExaminePupils(long gameTime) {
        return penlightCooldownEndGameTime < 0L || gameTime >= penlightCooldownEndGameTime;
    }

    public boolean revealDeathTime(long gameTime) {
        if (!canExaminePupils(gameTime)) {
            return false;
        }
        deathTimeRevealed = true;
        deathTimeInspectionGameTime = Math.max(entityData.get(DEATH_GAME_TIME), gameTime);
        penlightCooldownEndGameTime = gameTime + PENLIGHT_REEXAMINATION_COOLDOWN_TICKS;
        setChanged();
        return true;
    }

    public boolean revealDetailedAutopsy() {
        if (detailedAutopsyRevealed) {
            return false;
        }
        detailedAutopsyRevealed = true;
        setChanged();
        return true;
    }

    public Optional<UUID> ownerId() {
        return entityData.get(OWNER_ID);
    }

    public String ownerName() {
        return entityData.get(OWNER_NAME);
    }

    public long deathGameTime() {
        return entityData.get(DEATH_GAME_TIME);
    }

    public DownedPoseSnapshot downedPose() {
        return new DownedPoseSnapshot(
                entityData.get(DOWNED_GAME_TIME),
                entityData.get(BODY_YAW),
                DownedPosture.fromSerializedName(entityData.get(POSTURE)),
                DownedFallDirection.fromSerializedName(entityData.get(FALL_DIRECTION))
        );
    }

    public void rotateBody(float clockwiseDegrees) {
        if (!Float.isFinite(clockwiseDegrees) || clockwiseDegrees == 0.0F) {
            return;
        }
        float bodyYaw = Mth.wrapDegrees(entityData.get(BODY_YAW) + clockwiseDegrees);
        entityData.set(BODY_YAW, bodyYaw);
        setYRot(bodyYaw);
        setYBodyRot(bodyYaw);
        setYHeadRot(bodyYaw);
        updateCorpseBoundingBox();
        setChanged();
    }

    public GameProfile createOwnerProfile() {
        GameProfile profile = new GameProfile(
                ownerId().orElse(getUUID()),
                entityData.get(OWNER_NAME)
        );
        String textureValue = entityData.get(SKIN_TEXTURE_VALUE);
        if (!textureValue.isBlank()) {
            String signature = entityData.get(SKIN_TEXTURE_SIGNATURE);
            Property property = signature.isBlank()
                    ? new Property("textures", textureValue)
                    : new Property("textures", textureValue, signature);
            profile.getProperties().put("textures", property);
        }
        return profile;
    }

    @Override
    public void tick() {
        // Older saved corpses may still carry the previous NoGravity NBT flag.
        setNoGravity(false);
        super.tick();
        if (!level().isClientSide) {
            Vec3 pull = BodyDragService.pullMovement(this);
            double verticalMovement = BodyDragService.bodyVerticalMovement(
                    this,
                    getDeltaMovement().y,
                    pull.y
            );
            setDeltaMovement(pull.x, verticalMovement, pull.z);
            if (BodyDragService.isBeingDragged(this) || isInWaterOrBubble()) {
                hurtMarked = true;
            }
        }
        setAirSupply(getMaxAirSupply());
        setHealth(1.0F);
        if (!level().isClientSide && advanceEmptyLifecycle()) {
            discard();
            return;
        }
        updateCorpseBoundingBox();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return CorpseServerConfig.corpseEntityPushingEnabled();
    }

    @Override
    public boolean canBeCollidedWith() {
        return CorpseServerConfig.collisionEnabled();
    }

    @Override
    protected void pushEntities() {
        if (CorpseServerConfig.corpseEntityPushingEnabled()) {
            super.pushEntities();
        }
    }

    @Override
    public void push(Entity entity) {
        if (CorpseServerConfig.corpseEntityPushingEnabled()) {
            super.push(entity);
        }
    }

    @Override
    public void push(double x, double y, double z) {
        if (CorpseServerConfig.corpseEntityPushingEnabled()) {
            super.push(x, y, z);
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public int getContainerSize() {
        return inventory.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < inventory.size() ? inventory.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(inventory, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = ContainerHelper.takeItem(inventory, slot);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= inventory.size()) {
            return;
        }
        inventory.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        if (!stack.isEmpty()) {
            resetEmptyRemovalTimer();
        }
        setChanged();
    }

    @Override
    public void setChanged() {
        int chunkX = blockPosition().getX() >> 4;
        int chunkZ = blockPosition().getZ() >> 4;
        if (!level().isClientSide && level().getChunkSource().hasChunk(chunkX, chunkZ)) {
            level().getChunkAt(blockPosition()).setUnsaved(true);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return !isRemoved()
                && player.level() == level()
                && player.distanceToSqr(this) <= 64.0D;
    }

    @Override
    public void clearContent() {
        inventory.clear();
        setChanged();
    }

    public void copyInventoryFrom(Player player) {
        clearContent();
        int slotsToCopy = Math.min(INVENTORY_SIZE, player.getInventory().getContainerSize());
        for (int slot = 0; slot < slotsToCopy; slot++) {
            inventory.set(slot, player.getInventory().getItem(slot).copy());
        }
        setChanged();
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return List.of(
                inventory.get(36),
                inventory.get(37),
                inventory.get(38),
                inventory.get(39)
        );
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        int inventorySlot = inventorySlot(slot);
        return inventorySlot < 0 ? ItemStack.EMPTY : inventory.get(inventorySlot);
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        int inventorySlot = inventorySlot(slot);
        if (inventorySlot >= 0) {
            setItem(inventorySlot, stack);
        }
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("CorpseSnapshot", snapshot().save());
        CompoundTag inventoryTag = new CompoundTag();
        ContainerHelper.saveAllItems(inventoryTag, inventory, registryAccess());
        tag.put(TAG_INVENTORY, inventoryTag);
        if (emptySinceGameTime >= 0L) {
            tag.putLong(TAG_EMPTY_SINCE_GAME_TIME, emptySinceGameTime);
        }
        tag.putBoolean(TAG_DEATH_TIME_REVEALED, deathTimeRevealed);
        if (deathTimeInspectionGameTime >= 0L) {
            tag.putLong(TAG_DEATH_TIME_INSPECTION_GAME_TIME, deathTimeInspectionGameTime);
        }
        if (penlightCooldownEndGameTime >= 0L) {
            tag.putLong(TAG_PENLIGHT_COOLDOWN_END_GAME_TIME, penlightCooldownEndGameTime);
        }
        tag.putBoolean(TAG_DETAILED_AUTOPSY_REVEALED, detailedAutopsyRevealed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CorpseSnapshot", CompoundTag.TAG_COMPOUND)) {
            CorpseSnapshot snapshot = CorpseSnapshot.load(tag.getCompound("CorpseSnapshot"));
            initialize(snapshot, getX(), getY(), getZ());
        }
        inventory.clear();
        if (tag.contains(TAG_INVENTORY, CompoundTag.TAG_COMPOUND)) {
            ContainerHelper.loadAllItems(tag.getCompound(TAG_INVENTORY), inventory, registryAccess());
        }
        emptySinceGameTime = tag.contains(TAG_EMPTY_SINCE_GAME_TIME, Tag.TAG_ANY_NUMERIC)
                ? Math.max(0L, tag.getLong(TAG_EMPTY_SINCE_GAME_TIME))
                : EmptyCorpseLifecycle.NOT_EMPTY;
        deathTimeRevealed = tag.getBoolean(TAG_DEATH_TIME_REVEALED);
        deathTimeInspectionGameTime = tag.contains(TAG_DEATH_TIME_INSPECTION_GAME_TIME, Tag.TAG_ANY_NUMERIC)
                ? Math.max(0L, tag.getLong(TAG_DEATH_TIME_INSPECTION_GAME_TIME))
                : deathTimeRevealed ? Math.max(entityData.get(DEATH_GAME_TIME), level().getGameTime()) : -1L;
        penlightCooldownEndGameTime = tag.contains(TAG_PENLIGHT_COOLDOWN_END_GAME_TIME, Tag.TAG_ANY_NUMERIC)
                ? Math.max(0L, tag.getLong(TAG_PENLIGHT_COOLDOWN_END_GAME_TIME))
                : -1L;
        detailedAutopsyRevealed = tag.getBoolean(TAG_DETAILED_AUTOPSY_REVEALED);
    }


    private void updateCorpseBoundingBox() {
        setBoundingBox(DownedGeometry.boundingBox(this, downedPose()));
    }

    private boolean advanceEmptyLifecycle() {
        EmptyCorpseLifecycle.Progression progression = EmptyCorpseLifecycle.advance(
                CorpseServerConfig.emptyRemovalEnabled(),
                isEmpty(),
                emptySinceGameTime,
                level().getGameTime(),
                CorpseServerConfig.emptyLifetimeTicks()
        );
        if (progression.emptySinceGameTime() != emptySinceGameTime) {
            emptySinceGameTime = progression.emptySinceGameTime();
            setChanged();
        }
        return progression.shouldRemove();
    }

    private void resetEmptyRemovalTimer() {
        if (emptySinceGameTime >= 0L) {
            emptySinceGameTime = EmptyCorpseLifecycle.NOT_EMPTY;
        }
    }

    private static int inventorySlot(EquipmentSlot slot) {
        if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            return 36 + slot.getIndex();
        }
        return switch (slot) {
            case MAINHAND -> 0;
            case OFFHAND -> 40;
            default -> -1;
        };
    }
}
