package com.swampd.superficialtrauma;

import com.mojang.authlib.GameProfile;
import com.swampd.superficialtrauma.common.block.*;
import com.swampd.superficialtrauma.common.body.*;
import com.swampd.superficialtrauma.common.crafting.MedicalWorkbenchRecipes;
import com.swampd.superficialtrauma.common.damage.*;
import com.swampd.superficialtrauma.common.entity.*;
import com.swampd.superficialtrauma.common.init.*;
import com.swampd.superficialtrauma.common.item.DefibrillatorItem;
import com.swampd.superficialtrauma.common.toxicology.FoodContamination;
import com.swampd.superficialtrauma.common.wound.WoundType;
import com.swampd.superficialtrauma.network.packet.StartMedicationC2SPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/** Development-only integration tests. This source set is never packaged into the mod JAR. */
@GameTestHolder(SuperficialTrauma.MOD_ID)
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid = SuperficialTrauma.MOD_ID)
public class MigrationGameTests {
    @SubscribeEvent
    public static void template(ServerStartingEvent event) throws Exception {
        event.getServer().getStructureManager().getOrCreate(id("migration_empty")).load(
                event.getServer().registryAccess().lookupOrThrow(Registries.BLOCK),
                TagParser.parseTag("{size:[8,8,8],entities:[],blocks:[],palette:[{Name:\"minecraft:air\"}],DataVersion:3955}"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, path);
    }

    private static FakePlayer player(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ST_Migration")) {
            @Override public boolean isInvulnerableTo(DamageSource source) { return false; }
        };
        player.moveTo(helper.absolutePos(new BlockPos(3, 2, 3)).getCenter());
        player.setHealth(20);
        // New players normally have 60 ticks of login protection; remove only in this test fixture.
        try {
            var field = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            field.setAccessible(true);
            field.setInt(player, 0);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        return player;
    }

    @GameTest(template = "migration_empty")
    public static void itemComponents(GameTestHelper helper) {
        var energy = new ItemStack(ModItems.DEFIBRILLATOR.get(), 2);
        helper.assertTrue(DefibrillatorItem.getEnergy(energy) == 900, "New defibrillators must start charged");
        DefibrillatorItem.consumeEnergy(energy, 250);
        var saved = ItemStack.parseOptional(helper.getLevel().registryAccess(), (CompoundTag) energy.save(helper.getLevel().registryAccess()));
        helper.assertTrue(saved.getCount() == 2 && DefibrillatorItem.getEnergy(saved) == 650, "Energy and stack count must persist");
        helper.assertTrue(DefibrillatorItem.getEnergy(saved.split(1)) == 650, "Splitting must preserve existing energy rules");
        var food = new ItemStack(Items.BREAD);
        FoodContamination.contaminateWithDdvp(food);
        var loaded = ItemStack.parseOptional(helper.getLevel().registryAccess(), (CompoundTag) food.save(helper.getLevel().registryAccess()));
        helper.assertTrue(FoodContamination.isDdvpContaminated(loaded), "Poison must survive item serialization");
        helper.assertTrue(!ItemStack.isSameItemSameComponents(loaded, new ItemStack(Items.BREAD)), "Poisoned food cannot merge with clean food");
        helper.assertTrue(loaded.getHoverName().equals(new ItemStack(Items.BREAD).getHoverName()), "Poison must not alter visible name");
        helper.succeed();
    }

    @GameTest(template = "migration_empty")
    public static void bodyAttachments(GameTestHelper helper) {
        var original = player(helper);
        var state = BodyStateCapability.get(original).orElseThrow();
        state.unlockFirstAidSkill();
        state.unlockSurgerySkill();
        state.applyDamage(WoundType.BLUNT, 7, helper.getLevel().getGameTime());
        CompoundTag saved = new CompoundTag();
        original.saveWithoutId(saved);
        var loaded = player(helper);
        loaded.load(saved);
        var loadedState = BodyStateCapability.get(loaded).orElseThrow();
        helper.assertTrue(loadedState.hasSurgerySkill() && !loadedState.wounds().isEmpty(), "Attachment must retain wounds and skills on reload");
        var respawn = player(helper);
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(respawn, original, true));
        var respawnState = BodyStateCapability.get(respawn).orElseThrow();
        helper.assertTrue(respawnState.hasFirstAidSkill() && respawnState.wounds().isEmpty(), "Death must keep learned skills but clear wounds");
        helper.succeed();
    }

    @GameTest(template = "migration_empty")
    public static void absorptionAndDowning(GameTestHelper helper) {
        var victim = player(helper);
        victim.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(20);
        victim.setAbsorptionAmount(12);
        victim.hurt(victim.damageSources().fall(), 8);
        helper.assertTrue(victim.getHealth() == 20 && victim.getAbsorptionAmount() == 4, "Absorption must be spent before health");
        helper.assertTrue(BodyStateCapability.get(victim).orElseThrow().wounds().isEmpty(), "Absorbed damage must not create trauma");
        victim.invulnerableTime = 0;
        victim.hurt(victim.damageSources().fall(), 8);
        helper.assertTrue(victim.getHealth() == 16, "Only the unabsorbed four damage may affect health");
        helper.assertTrue(!BodyStateCapability.get(victim).orElseThrow().wounds().isEmpty(), "Unabsorbed damage must create trauma");
        victim.invulnerableTime = 0;
        victim.hurt(victim.damageSources().fall(), 100);
        helper.assertTrue(victim.getHealth() > 0 && !BodyStateCapability.get(victim).orElseThrow().canAct(), "Lethal hits must down rather than vanilla-kill");
        helper.succeed();
    }

    @GameTest(template = "migration_empty")
    public static void administrativeKill(GameTestHelper helper) {
        var victim = player(helper);
        victim.hurt(victim.damageSources().genericKill(), Float.MAX_VALUE);
        helper.assertTrue(BodyStateCapability.get(victim).orElseThrow().lifeState() == BodyLifeState.BRAIN_DEAD, "/kill must directly mark brain death");
        helper.succeed();
    }

    @GameTest(template = "migration_empty")
    public static void damageInterruptsBeforeHealthClamp(GameTestHelper helper) {
        var victim = player(helper);
        helper.getLevel().addNewPlayer(victim);
        try {
            victim.getInventory().add(new ItemStack(ModItems.PARACETAMOL.get()));
            helper.assertTrue(com.swampd.superficialtrauma.common.medication.MedicationService.start(victim, victim.getId(),
                    com.swampd.superficialtrauma.common.medication.MedicationType.PARACETAMOL), "Medication must start");
            BodyStateCapability.get(victim).orElseThrow().incapacitateFromLastDamage(CollapseReason.HEMORRHAGIC_SHOCK,
                    helper.getLevel().getGameTime());
            victim.setHealth(1);
            victim.hurt(victim.damageSources().fall(), 4);
            helper.assertTrue(victim.getHealth() == 1, "Downed hit must preserve vanilla health");
            helper.assertTrue(!com.swampd.superficialtrauma.common.medication.MedicationService.isActorAdministering(victim.getUUID()),
                    "A hit clamped to zero health loss must still interrupt a medical action");
        } finally {
            victim.discard();
            com.swampd.superficialtrauma.common.medication.MedicationService.clearAll();
        }
        helper.succeed();
    }

    @GameTest(template = "migration_empty")
    public static void blockEntityPersistence(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var station = new DefibrillatorStationBlockEntity(pos, ModBlocks.DEFIBRILLATOR_STATION.get().defaultBlockState());
        station.setLevel(helper.getLevel());
        for (int slot = 0; slot < 2; slot++) {
            var stack = new ItemStack(ModItems.DEFIBRILLATOR.get());
            DefibrillatorItem.setEnergy(stack, 100);
            station.setItem(slot, stack);
        }
        for (int tick = 0; tick < 20; tick++) DefibrillatorStationBlockEntity.serverTick(helper.getLevel(), pos, station.getBlockState(), station);
        helper.assertTrue(DefibrillatorItem.getEnergy(station.getItem(0)) == 103 && DefibrillatorItem.getEnergy(station.getItem(1)) == 103, "Both charging slots must gain 3 J per second");
        var stationCopy = new DefibrillatorStationBlockEntity(pos, station.getBlockState());
        stationCopy.loadAdditional(station.saveWithoutMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(DefibrillatorItem.getEnergy(stationCopy.getItem(1)) == 103, "Charging inventory must persist");
        var crafter = player(helper);
        crafter.getInventory().add(new ItemStack(Items.STRING, 6));
        crafter.getInventory().add(new ItemStack(Items.WHITE_WOOL, 2));
        var bench = new MedicalWorkbenchBlockEntity(pos, ModBlocks.MEDICAL_WORKBENCH.get().defaultBlockState());
        bench.setLevel(helper.getLevel());
        helper.assertTrue(bench.enqueueRecipe(crafter, MedicalWorkbenchRecipes.byKey("bandage")), "First craft queues");
        helper.assertTrue(bench.enqueueRecipe(crafter, MedicalWorkbenchRecipes.byKey("bandage")), "Second craft queues");
        var benchCopy = new MedicalWorkbenchBlockEntity(pos, bench.getBlockState());
        benchCopy.setLevel(helper.getLevel());
        benchCopy.loadAdditional(bench.saveWithoutMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(benchCopy.cancelJob(0, crafter) && benchCopy.cancelJob(0, crafter), "Persisted queue must remain cancellable");
        helper.assertTrue(crafter.getInventory().countItem(Items.STRING) == 6 && crafter.getInventory().countItem(Items.WHITE_WOOL) == 2, "Cancellation must refund exact inputs after reload");
        helper.succeed();
    }

    @GameTest(template = "migration_empty")
    public static void corpsePersistence(GameTestHelper helper) {
        var corpse = ModEntities.CORPSE.get().create(helper.getLevel());
        var identity = new CorpseSnapshot(UUID.randomUUID(), "OfflineOwner", "texture-snapshot", "signature-snapshot", 40,
                new DownedPoseSnapshot(20, 37, DownedPosture.UNSAFE, DownedFallDirection.FADE_ONLY),
                List.of(), null, CollapseReason.NONE, false, false);
        corpse.initialize(identity, 1, 90, 1);
        var item = new ItemStack(ModItems.DEFIBRILLATOR.get());
        DefibrillatorItem.setEnergy(item, 123);
        corpse.setItem(0, item);
        var tag = new CompoundTag();
        corpse.saveWithoutId(tag);
        var loaded = ModEntities.CORPSE.get().create(helper.getLevel());
        loaded.load(tag);
        helper.assertTrue(loaded.snapshot().skinTextureValue().equals("texture-snapshot"), "Offline skin snapshot must persist");
        helper.assertTrue(loaded.downedPose().bodyYaw() == 37, "Corpse orientation must persist");
        helper.assertTrue(DefibrillatorItem.getEnergy(loaded.getItem(0)) == 123, "Corpse inventory components must persist");
        helper.succeed();
    }

    @GameTest(template = "migration_empty")
    public static void recipesTagsAndStrictPackets(GameTestHelper helper) {
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(id("medical_workbench")).isPresent(), "Workbench recipe must load from 1.21 singular path");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(id("defibrillator_station")).isPresent(), "Station recipe must load");
        if (ModList.get().isLoaded("cgm")) {
            var shell = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("cgm:shell")));
            helper.assertTrue(CgmAmmoTags.classify(shell) == DamageKind.CGM_SHOTGUN, "Actual CGM shells must use shotgun category");
        }
        if (ModList.get().isLoaded("nzgmaddon")) {
            var medium = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("nzgmaddon:medium_bullet")));
            helper.assertTrue(!medium.isEmpty() && CgmAmmoTags.classify(medium) == DamageKind.CGM_HIGH_VELOCITY,
                    "The supplied NineZero port uses nzgmaddon, not the old nzgexpansion namespace");
        }
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            buffer.writeVarInt(1);
            buffer.writeUtf("invalid_medication");
            helper.assertTrue(StartMedicationC2SPacket.STREAM_CODEC.decode(buffer).selectedType() == null, "Unknown network medicine ID must not default to paracetamol");
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(template = "migration_empty")
    public static void actualCgmShotgunHit(GameTestHelper helper) throws Exception {
        if (!ModList.get().isLoaded("cgm")) { helper.succeed(); return; }
        var victim = player(helper);
        var shooter = net.minecraft.world.entity.EntityType.ZOMBIE.create(helper.getLevel());
        shooter.moveTo(victim.position().add(0, 0, 2));
        var gunStack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("cgm:shotgun")));
        var gunClass = Class.forName("com.mrcrayfish.guns.item.GunItem");
        var gunDataClass = Class.forName("com.mrcrayfish.guns.common.Gun");
        var projectileClass = Class.forName("com.mrcrayfish.guns.entity.ProjectileEntity");
        var gunData = gunClass.getMethod("getModifiedGun", ItemStack.class).invoke(gunStack.getItem(), gunStack);
        var constructor = projectileClass.getConstructor(net.minecraft.world.entity.EntityType.class,
                net.minecraft.world.level.Level.class, net.minecraft.world.entity.LivingEntity.class,
                ItemStack.class, gunClass, gunDataClass);
        var hit = projectileClass.getDeclaredMethod("onHitEntity", net.minecraft.world.entity.Entity.class,
                net.minecraft.world.phys.Vec3.class, net.minecraft.world.phys.Vec3.class,
                net.minecraft.world.phys.Vec3.class, boolean.class);
        hit.setAccessible(true);
        // Execute the real supplied CGM damage callback, not a synthesized gun DamageSource.
        for (int pellet = 0; pellet < 12 && BodyStateCapability.get(victim).orElseThrow().canAct(); pellet++) {
            Object projectile = constructor.newInstance(BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("cgm:projectile")),
                    helper.getLevel(), shooter, gunStack, gunStack.getItem(), gunData);
            // CGM assigns the weapon after construction in its firing pipeline.
            projectileClass.getMethod("setWeapon", ItemStack.class).invoke(projectile, gunStack);
            victim.invulnerableTime = 0;
            hit.invoke(projectile, victim, victim.position(), shooter.position(), victim.position(), false);
        }
        var state = BodyStateCapability.get(victim).orElseThrow();
        helper.assertTrue(!state.canAct() && victim.getHealth() > 0, "Actual CGM shotgun hits must enter downed state");
        ShotgunVolleyAggregator.resolveReady(victim, state, helper.getLevel().getGameTime() + 3);
        helper.assertTrue(state.wounds().stream().anyMatch(wound -> wound.type() == WoundType.GUNSHOT_SHOTGUN),
                "A close-range lethal CGM volley must still produce shotgun trauma");
        helper.assertTrue(state.downingHitRecord().orElseThrow().weaponId().equals("cgm:shotgun"),
                "Forensic record must retain the real CGM weapon");
        helper.succeed();
    }
}
