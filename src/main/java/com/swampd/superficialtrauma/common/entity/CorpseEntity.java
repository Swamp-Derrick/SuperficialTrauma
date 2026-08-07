package com.swampd.superficialtrauma.common.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.swampd.superficialtrauma.common.body.DownedGeometry;
import com.swampd.superficialtrauma.common.body.DownedPoseSnapshot;
import com.swampd.superficialtrauma.common.body.DownedPosture;
import com.swampd.superficialtrauma.common.body.DownedFallDirection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class CorpseEntity extends LivingEntity {
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

    public CorpseEntity(EntityType<? extends CorpseEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
        setInvulnerable(true);
        setHealth(1.0F);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(OWNER_ID, Optional.empty());
        entityData.define(OWNER_NAME, "Unknown");
        entityData.define(SKIN_TEXTURE_VALUE, "");
        entityData.define(SKIN_TEXTURE_SIGNATURE, "");
        entityData.define(DEATH_GAME_TIME, 0L);
        entityData.define(DOWNED_GAME_TIME, 0L);
        entityData.define(BODY_YAW, 0.0F);
        entityData.define(POSTURE, DownedPosture.UNSAFE.serializedName());
        entityData.define(FALL_DIRECTION, DownedFallDirection.FADE_ONLY.serializedName());
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
                downedPose()
        );
    }

    public Optional<UUID> ownerId() {
        return entityData.get(OWNER_ID);
    }

    public DownedPoseSnapshot downedPose() {
        return new DownedPoseSnapshot(
                entityData.get(DOWNED_GAME_TIME),
                entityData.get(BODY_YAW),
                DownedPosture.fromSerializedName(entityData.get(POSTURE)),
                DownedFallDirection.fromSerializedName(entityData.get(FALL_DIRECTION))
        );
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
        super.tick();
        setDeltaMovement(0.0D, 0.0D, 0.0D);
        setAirSupply(getMaxAirSupply());
        setHealth(1.0F);
        updateCorpseBoundingBox();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return List.of();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("CorpseSnapshot", snapshot().save());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CorpseSnapshot", CompoundTag.TAG_COMPOUND)) {
            CorpseSnapshot snapshot = CorpseSnapshot.load(tag.getCompound("CorpseSnapshot"));
            initialize(snapshot, getX(), getY(), getZ());
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void updateCorpseBoundingBox() {
        setBoundingBox(DownedGeometry.boundingBox(this, downedPose()));
    }
}
