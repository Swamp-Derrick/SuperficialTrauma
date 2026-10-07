package com.swampd.superficialtrauma;

import com.mojang.authlib.GameProfile;
import com.swampd.superficialtrauma.common.body.*;
import com.swampd.superficialtrauma.common.config.SeriousTraumaConfig;
import com.swampd.superficialtrauma.common.damage.*;
import com.swampd.superficialtrauma.common.wound.WoundType;
import com.swampd.superficialtrauma.network.packet.InspectionSnapshotS2CPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(SuperficialTrauma.MOD_ID)
@PrefixGameTestTemplate(false)
public class SeriousTraumaGameTests {
    static FakePlayer victim(GameTestHelper helper) throws Exception {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ST_Regions")) {
            @Override public boolean isInvulnerableTo(DamageSource source) { return false; }
        };
        player.moveTo(helper.absolutePos(new BlockPos(3, 2, 3)).getCenter());
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        player.setHealth(1000);
        var field = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
        field.setAccessible(true);
        field.setInt(player, 0);
        return player;
    }

    @GameTest(template = "migration_empty")
    public static void snapshotAndServerConfig(GameTestHelper helper) {
        helper.assertTrue(!SeriousTraumaConfig.PRODUCTION_DEFAULT, "Production baseline must stay disabled");
        var state = new BodyState();
        state.configureSeriousTrauma(true, 0);
        var wound = state.applyGunshotDamage(WoundType.GUNSHOT_HIGH_VELOCITY, 12, 0, 10, false, 0);
        state.recordGunshotLocations(wound, 12, 12, 0, 0);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            var packet = new InspectionSnapshotS2CPacket(1, Component.literal("Patient"), 8, 20, state.serializeNBT(), true);
            InspectionSnapshotS2CPacket.STREAM_CODEC.encode(buffer, packet);
            var received = InspectionSnapshotS2CPacket.STREAM_CODEC.decode(buffer);
            BodyState clientCopy = new BodyState();
            clientCopy.deserializeNBT(received.bodyStateTag());
            helper.assertTrue(clientCopy.seriousTrauma().conditions().contains(SeriousTraumaState.Condition.CONCUSSION),
                    "Inspection snapshot must retain the global condition");
            helper.assertTrue(clientCopy.wounds().getFirst().gunshotRegions().contains(GunshotRegion.HEAD),
                    "Inspection snapshot must retain the wound's location metadata");
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(template = "migration_empty")
    public static void actualCgmBulletLocations(GameTestHelper helper) throws Exception {
        if (!ModList.get().isLoaded("cgm")) { helper.succeed(); return; }
        helper.assertTrue(SeriousTraumaConfig.enabled(), "Run this experimental integration suite with serioustrauma=true");
        var patient = victim(helper);
        var state = BodyStateCapability.get(patient).orElseThrow();
        var shots = new CgmShots(helper, patient, "cgm:pistol");
        for (int i = 0; i < 4; i++) shots.fire(.1);
        helper.assertTrue(state.seriousTrauma().damage(GunshotRegion.HEAD) == 0
                && state.seriousTrauma().damage(GunshotRegion.CHEST) == 0,
                "Real CGM foot impacts must not contaminate either regional pool");
        float headDamage = 0, chestDamage = 0;
        for (int i = 0; i < 8; i++) headDamage += shots.fire(1.65);
        for (int i = 0; i < 8; i++) chestDamage += shots.fire(1.1);
        helper.assertTrue(headDamage > 10 && chestDamage > 15, "Fixture must exceed both thresholds");
        helper.assertTrue(Math.abs(state.seriousTrauma().damage(GunshotRegion.HEAD) - headDamage) < .001F,
                "Head pool must equal only real head impacts after final-damage resolution");
        helper.assertTrue(Math.abs(state.seriousTrauma().damage(GunshotRegion.CHEST) - chestDamage) < .001F,
                "Chest pool must equal only real chest impacts");
        helper.assertTrue(state.seriousTrauma().conditions().size() == 2, "Both unique conditions may coexist");
        helper.succeed();
    }

    @GameTest(template = "migration_empty")
    public static void actualCgmMixedShotgunLocations(GameTestHelper helper) throws Exception {
        if (!ModList.get().isLoaded("cgm")) { helper.succeed(); return; }
        var patient = victim(helper);
        var state = BodyStateCapability.get(patient).orElseThrow();
        var shots = new CgmShots(helper, patient, "cgm:shotgun");
        float total = 0;
        for (int i = 0; i < 8; i++) total += shots.fire(.1);
        float head = shots.fire(1.65), chest = shots.fire(1.1);
        total += head + chest;
        ShotgunVolleyAggregator.resolveReady(patient, state, helper.getLevel().getGameTime() + 2);
        helper.assertTrue(state.wounds().stream().anyMatch(w -> w.type() == WoundType.GUNSHOT_SHOTGUN),
                "Existing shotgun wound aggregation must remain intact");
        helper.assertTrue(Math.abs(state.seriousTrauma().damage(GunshotRegion.HEAD) - head) < .001F,
                "Mixed volley: head gets only its own pellet");
        helper.assertTrue(Math.abs(state.seriousTrauma().damage(GunshotRegion.CHEST) - chest) < .001F,
                "Mixed volley: chest gets only its own pellet");
        helper.assertTrue(total > head + chest, "Fixture must include ignored lower-body damage");
        helper.succeed();
    }

    @GameTest(template = "migration_empty")
    public static void actualCgmDowningBulletEvidence(GameTestHelper helper) throws Exception {
        if (!ModList.get().isLoaded("cgm")) { helper.succeed(); return; }
        for (var location : new BulletHitLocation[]{BulletHitLocation.HEAD, BulletHitLocation.CHEST, BulletHitLocation.LIMBS}) {
            for (boolean fatalRoll : new boolean[]{true, false}) {
                var patient = victim(helper);
                patient.setHealth(2);
                // DamageEvents draws once for debridement, then once for the brain-death roll.
                long seed = 0;
                for (;; seed++) {
                    var random = net.minecraft.util.RandomSource.create(seed);
                    random.nextFloat();
                    if ((random.nextFloat() < .5F) == fatalRoll) break;
                }
                patient.getRandom().setSeed(seed);
                var shots = new CgmShots(helper, patient, "cgm:pistol");
                shots.fire(location == BulletHitLocation.HEAD ? 1.65 : location == BulletHitLocation.CHEST ? 1.1 : .1);
                var state = BodyStateCapability.get(patient).orElseThrow();
                var evidence = state.downingHitRecord().orElseThrow();
                helper.assertTrue(evidence.bulletLocation() == location, "Actual lethal CGM impact must freeze its location");
                boolean fatal = location == BulletHitLocation.HEAD && fatalRoll;
                helper.assertTrue(evidence.fatalBrainInjury() == fatal, "Actual CGM brain-death roll must use server final damage");
                helper.assertTrue((state.lifeState() == BodyLifeState.BRAIN_DEAD) == fatal, "Death state must match the forensic flag");
                helper.assertTrue(state.seriousTrauma().conditions().contains(SeriousTraumaState.Condition.CONCUSSION)
                        == (location == BulletHitLocation.HEAD), "A downing head hit >5 must cause concussion even below 10");
                shots.fire(.1);
                helper.assertTrue(state.downingHitRecord().orElseThrow().equals(evidence), "Later execution must not replace evidence");
                if (fatal) {
                    helper.assertTrue(com.swampd.superficialtrauma.common.entity.CorpseService.spawn(patient, state),
                            "Brain injury must use the normal corpse snapshot path");
                    var corpses = helper.getLevel().getEntitiesOfClass(
                            com.swampd.superficialtrauma.common.entity.CorpseEntity.class, patient.getBoundingBox().inflate(2),
                            corpse -> corpse.ownerId().filter(patient.getUUID()::equals).isPresent());
                    helper.assertTrue(corpses.size() == 1, "One corpse must be created");
                    var corpse = corpses.getFirst();
                    var basic = com.swampd.superficialtrauma.common.forensics.AutopsyReport.create(corpse, true, true, true,
                            com.swampd.superficialtrauma.common.forensics.AutopsyAction.NONE, -1);
                    helper.assertTrue(basic.downingHit() == null, "Basic autopsy must not reveal brain injury");
                    corpse.revealDetailedAutopsy();
                    var detailed = com.swampd.superficialtrauma.common.forensics.AutopsyReport.create(corpse, true, true, true,
                            com.swampd.superficialtrauma.common.forensics.AutopsyAction.NONE, -1);
                    helper.assertTrue(detailed.downingHit().equals(evidence), "Detailed corpse autopsy must retain lethal head evidence");
                    corpse.discard();
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "migration_empty")
    public static void actualCgmArmorAndHorizontalPoseExcludeBrainDeath(GameTestHelper helper) throws Exception {
        if (!ModList.get().isLoaded("cgm")) { helper.succeed(); return; }
        var armored = victim(helper);
        armored.setHealth(1);
        armored.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.NETHERITE_HELMET));
        armored.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(net.minecraft.world.item.Items.NETHERITE_CHESTPLATE));
        armored.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(net.minecraft.world.item.Items.NETHERITE_LEGGINGS));
        armored.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(net.minecraft.world.item.Items.NETHERITE_BOOTS));
        // FakePlayer is not ticked in this fixture, so equipment attribute refresh has not happened yet.
        armored.getAttribute(Attributes.ARMOR).setBaseValue(20);
        armored.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(12);
        new CgmShots(helper, armored, "cgm:pistol").fire(1.65);
        var state = BodyStateCapability.get(armored).orElseThrow();
        var hit = state.downingHitRecord().orElseThrow();
        helper.assertTrue(hit.headFinalDamage() > 0 && hit.headFinalDamage() <= 5,
                "Armor fixture must reduce a head bullet below the brain-death threshold: " + hit.headFinalDamage());
        helper.assertTrue(state.lifeState() == BodyLifeState.INCAPACITATED && state.seriousTrauma().conditions().isEmpty(),
                "No brain death or forced concussion below the final-damage threshold");
        var swimming = victim(helper);
        swimming.setPose(net.minecraft.world.entity.Pose.SWIMMING);
        swimming.setHealth(2);
        new CgmShots(helper, swimming, "cgm:pistol").fire(.5);
        state = BodyStateCapability.get(swimming).orElseThrow();
        helper.assertTrue(state.lifeState() == BodyLifeState.INCAPACITATED && state.seriousTrauma().conditions().isEmpty(),
                "Horizontal pose must not generate serious trauma or direct brain death");
        helper.assertTrue(state.downingHitRecord().orElseThrow().bulletLocation() == BulletHitLocation.UNKNOWN,
                "Unsupported pose must not invent limb evidence");
        helper.succeed();
    }

    @GameTest(template = "migration_empty")
    public static void actualCgmDowningShotgunPrefersHeadRegardlessOfPelletOrder(GameTestHelper helper) throws Exception {
        if (!ModList.get().isLoaded("cgm")) { helper.succeed(); return; }
        var patients = new java.util.ArrayList<ServerPlayer>();
        for (boolean headFirst : new boolean[]{true, false}) {
            var patient = victim(helper);
            patient.setHealth(2); // The FIRST pellet downs, but later pellets of this discharge still matter.
            var state = BodyStateCapability.get(patient).orElseThrow();
            var shots = new CgmShots(helper, patient, "cgm:shotgun");
            shots.fire(headFirst ? 1.65 : 1.1);
            helper.assertTrue(ShotgunVolleyAggregator.awaitingDowningPellets(patient), "Fatal volley must wait until tick end");
            helper.assertTrue(patient.getBoundingBox().getYsize() > 1, "Do not shrink the box halfway through one discharge");
            shots.fire(headFirst ? 1.1 : 1.65);
            shots.fire(1.65);
            shots.fire(.1);
            // Another shooter's volley in the same tick is an execution, not part of this downing shot.
            new CgmShots(helper, patient, "cgm:shotgun").fire(1.65);
            patients.add(patient);
        }
        helper.runAfterDelay(1, () -> {
            for (var patient : patients) {
                var state = BodyStateCapability.get(patient).orElseThrow();
                var evidence = state.downingHitRecord().orElseThrow();
                helper.assertTrue(evidence.bulletLocation() == BulletHitLocation.HEAD, "Both pellet orders must prefer head");
                helper.assertTrue(Math.abs(evidence.headFinalDamage() - 7.2F) < .001F,
                        "Only the two head pellets count, not the chest, feet or later shooter: " + evidence.headFinalDamage());
                helper.assertTrue(state.seriousTrauma().conditions().contains(SeriousTraumaState.Condition.CONCUSSION),
                        "Downing head damage >5 guarantees a concussion if the player survives");
                helper.assertTrue(!ShotgunVolleyAggregator.awaitingDowningPellets(patient)
                        && patient.getBoundingBox().getYsize() < 1, "Normal downed hitbox must be restored by the end of the tick");
                helper.assertTrue(state.wounds().stream().filter(w -> w.type() == WoundType.GUNSHOT_SHOTGUN).count() == 1,
                        "Exactly one shotgun wound must be resolved");
            }
            helper.succeed();
        });
    }

    /** Invokes the actual onHit dispatcher, including CGM's pre-damage hit event and damage pipeline. */
    static final class CgmShots {
        private final GameTestHelper helper;
        private final ServerPlayer patient;
        private final LivingEntity shooter;
        private final ItemStack weapon;
        private final Class<?> projectileType, entityResultType;
        private final Object gunData;
        private final java.lang.reflect.Constructor<?> constructor, hitConstructor, resultConstructor;
        private final java.lang.reflect.Method onHit;

        CgmShots(GameTestHelper helper, ServerPlayer patient, String weaponId) throws Exception {
            this.helper = helper;
            this.patient = patient;
            shooter = EntityType.ZOMBIE.create(helper.getLevel());
            shooter.moveTo(patient.position().add(0, 0, 2));
            weapon = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(weaponId)));
            var gunClass = Class.forName("com.mrcrayfish.guns.item.GunItem");
            var dataClass = Class.forName("com.mrcrayfish.guns.common.Gun");
            projectileType = Class.forName("com.mrcrayfish.guns.entity.ProjectileEntity");
            entityResultType = Class.forName("com.mrcrayfish.guns.entity.ProjectileEntity$EntityResult");
            gunData = gunClass.getMethod("getModifiedGun", ItemStack.class).invoke(weapon.getItem(), weapon);
            constructor = projectileType.getConstructor(EntityType.class, Level.class, LivingEntity.class,
                    ItemStack.class, gunClass, dataClass);
            resultConstructor = entityResultType.getConstructor(Entity.class, Vec3.class, boolean.class);
            hitConstructor = Class.forName("com.mrcrayfish.guns.util.math.ExtendedEntityRayTraceResult")
                    .getConstructor(entityResultType);
            onHit = projectileType.getDeclaredMethod("onHit", HitResult.class, Vec3.class, Vec3.class);
            onHit.setAccessible(true);
        }

        void renameWeapon(net.minecraft.network.chat.Component name) {
            weapon.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, name);
        }

        float fire(double height) throws Exception {
            Object bullet = constructor.newInstance(BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("cgm:projectile")),
                    helper.getLevel(), shooter, weapon, weapon.getItem(), gunData);
            projectileType.getMethod("setWeapon", ItemStack.class).invoke(bullet, weapon);
            Vec3 point = patient.position().add(0, height, .3);
            Object result = resultConstructor.newInstance(patient, point, false);
            Object hit = hitConstructor.newInstance(result);
            patient.invulnerableTime = 0;
            onHit.invoke(bullet, hit, shooter.position().add(0, height, 0), point.add(0, 0, -1));
            return BodyStateCapability.get(patient).orElseThrow().lastFinalDamage();
        }
    }
}
