package com.swampd.superficialtrauma;

import com.swampd.superficialtrauma.common.body.*;
import com.swampd.superficialtrauma.common.config.SeriousTraumaConfig;
import com.swampd.superficialtrauma.common.damage.*;
import com.swampd.superficialtrauma.common.wound.WoundType;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(SuperficialTrauma.MOD_ID)
@PrefixGameTestTemplate(false)
public class ArmorPenetrationGameTests {
    private static void equip(ServerPlayer player, Item head, Item chest, Item legs, Item feet) {
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(head));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(chest));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(legs));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(feet));
        refreshArmor(player);
    }

    private static void refreshArmor(ServerPlayer player) {
        var profile = ArmorPenetration.snapshot(player);
        // FakePlayers aren't ticked: mirror the armor attributes equipment would supply on the next tick.
        player.getAttribute(Attributes.ARMOR).setBaseValue(profile.head().armor() + profile.chest().armor()
                + profile.legs().armor() + profile.feet().armor());
        player.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(profile.head().toughness() + profile.chest().toughness()
                + profile.legs().toughness() + profile.feet().toughness());
    }

    @GameTest(template = "migration_empty")
    public static void vanillaEquipmentRatings(GameTestHelper helper) throws Exception {
        var player = SeriousTraumaGameTests.victim(helper);
        Item[][] equipment = {
                {Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS},
                {Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS},
                {Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS},
                {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS},
                {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS},
                {Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS}};
        double[] expected = {2.8, 4.4, 4.8, 6, 12, 14};
        for (int i = 0; i < equipment.length; i++) {
            var e = equipment[i];
            equip(player, e[0], e[1], e[2], e[3]);
            helper.assertTrue(Math.abs(ArmorPenetration.snapshot(player).overallRating() - expected[i]) < 1e-6,
                    "Real equipment components must match the reviewed armor table, row " + i);
        }
        helper.succeed();
    }

    @GameTest(template = "migration_empty")
    public static void cgmWeakSmgAndMixedRegions(GameTestHelper helper) throws Exception {
        if (!ModList.get().isLoaded("cgm")) { helper.succeed(); return; }
        var player = SeriousTraumaGameTests.victim(helper);
        equip(player, Items.AIR, Items.NETHERITE_CHESTPLATE, Items.AIR, Items.AIR);
        var shots = new SeriousTraumaGameTests.CgmShots(helper, player, "cgm:machine_pistol");
        var state = BodyStateCapability.get(player).orElseThrow();
        float head = shots.fire(1.65), chest = shots.fire(1.1), foot = shots.fire(.1);
        helper.assertTrue(head > 0 && head < 4 && foot > 0 && foot < 4, "Must exercise low-damage SMG hits");
        // Repeat stopped hits if the first did not cross the blunt minimum, without changing the gunshot pool.
        for (int i = 0; i < 3; i++) chest += shots.fire(1.1);
        helper.assertTrue(state.wounds().size() == 2, "Mixed exposed/armored regions must make exactly gunshot + blunt");
        var gunshot = state.wounds().stream().filter(w -> w.type().isGunshot()).findFirst().orElseThrow();
        helper.assertTrue(Math.abs(gunshot.accumulatedDamage() - head - foot) < .001,
                "Stopped chest damage cannot inflate the gunshot instance");
        helper.assertTrue(gunshot.gunshotRegions().equals(java.util.Set.of(GunshotRegion.HEAD)),
                "Only the penetrating head impact supplies regional metadata");
        helper.assertTrue(Math.abs(state.seriousTrauma().damage(GunshotRegion.HEAD) - head) < .001
                && state.seriousTrauma().damage(GunshotRegion.CHEST) == 0, "Serious pools use only penetrating region damage");
        helper.succeed();
    }

    @GameTest(template = "migration_empty")
    public static void cgmShotgunSeparatesArmorPerPellet(GameTestHelper helper) throws Exception {
        if (!ModList.get().isLoaded("cgm")) { helper.succeed(); return; }
        var player = SeriousTraumaGameTests.victim(helper);
        equip(player, Items.AIR, Items.NETHERITE_CHESTPLATE, Items.AIR, Items.AIR);
        var shots = new SeriousTraumaGameTests.CgmShots(helper, player, "cgm:shotgun");
        var state = BodyStateCapability.get(player).orElseThrow();
        float head = shots.fire(1.65), chest = 0, foot = shots.fire(.1);
        for (int i = 0; i < 3; i++) chest += shots.fire(1.1);
        ShotgunVolleyAggregator.resolveReady(player, state, helper.getLevel().getGameTime() + 2);
        helper.assertTrue(state.wounds().size() == 2, "One mixed volley produces separate gunshot and blunt instances");
        var wound = state.wounds().stream().filter(w -> w.type() == WoundType.GUNSHOT_SHOTGUN).findFirst().orElseThrow();
        helper.assertTrue(Math.abs(wound.accumulatedDamage() - head - foot) < .001, "Only exposed pellets accumulate gunshot");
        var blunt = state.wounds().stream().filter(w -> w.type() == WoundType.BLUNT).findFirst().orElseThrow();
        helper.assertTrue(Math.abs(blunt.accumulatedDamage() - chest) < .001, "Stopped pellet damage must be conserved");
        helper.assertTrue(state.seriousTrauma().damage(GunshotRegion.CHEST) == 0, "Blocked pellets cannot cause pneumothorax");
        helper.succeed();
    }

    @GameTest(template = "migration_empty")
    public static void cgmStrongArmorDoesNotImmediatelyMakeGradeThree(GameTestHelper helper) throws Exception {
        if (!ModList.get().isLoaded("cgm")) { helper.succeed(); return; }
        var player = SeriousTraumaGameTests.victim(helper);
        equip(player, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
        var shots = new SeriousTraumaGameTests.CgmShots(helper, player, "cgm:rifle");
        var state = BodyStateCapability.get(player).orElseThrow();
        float damage = shots.fire(1.1);
        var wound = state.wounds().getFirst();
        helper.assertTrue(wound.type() == WoundType.GUNSHOT_HIGH_VELOCITY && wound.fragmentationEligible(),
                "High-velocity ammo penetrates diamond chest armor and records eligibility");
        helper.assertTrue(damage < 12 && wound.severity() < 3, "Penetration alone must not force grade three");
        for (int i = 0; i < 20 && damage < 12; i++) damage += shots.fire(1.1);
        helper.assertTrue(wound.severity() == 3 && Math.abs(wound.accumulatedDamage() - damage) < .001,
                "Grade three begins only once final-damage accumulation reaches 12");
        helper.succeed();
    }

    @GameTest(template = "migration_empty")
    public static void cgmConfigOffAndUnsupportedPose(GameTestHelper helper) throws Exception {
        if (!ModList.get().isLoaded("cgm")) { helper.succeed(); return; }
        var setting = (net.neoforged.neoforge.common.ModConfigSpec.BooleanValue) SeriousTraumaConfig.SPEC.getValues().get("serioustrauma");
        boolean previous = setting.get();
        try {
            setting.set(false);
            var player = SeriousTraumaGameTests.victim(helper);
            equip(player, Items.AIR, Items.NETHERITE_CHESTPLATE, Items.AIR, Items.AIR);
            var shots = new SeriousTraumaGameTests.CgmShots(helper, player, "cgm:pistol");
            for (int i = 0; i < 6; i++) shots.fire(1.65);
            var state = BodyStateCapability.get(player).orElseThrow();
            helper.assertTrue(state.seriousTrauma().conditions().isEmpty() && state.seriousTrauma().damage(GunshotRegion.HEAD) == 0,
                    "Config off must disable serious consequences and pools");
            helper.assertTrue(state.wounds().getFirst().gunshotRegions().contains(GunshotRegion.HEAD),
                    "Config off must keep exposed-head penetration and location metadata");
        } finally { setting.set(previous); }
        var swimming = SeriousTraumaGameTests.victim(helper);
        equip(swimming, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
        swimming.setPose(Pose.SWIMMING);
        new SeriousTraumaGameTests.CgmShots(helper, swimming, "cgm:pistol").fire(.5);
        var state = BodyStateCapability.get(swimming).orElseThrow();
        helper.assertTrue(state.wounds().stream().noneMatch(w -> w.type().isGunshot()),
                "Unknown pose uses overall iron R=6, which stops low-velocity P=5");
        helper.assertTrue(state.seriousTrauma().conditions().isEmpty(), "Unknown pose cannot invent serious locations");
        helper.succeed();
    }

    @GameTest(template = "migration_empty")
    public static void cgmStoppedLethalHeadHitKeepsForensicsWithoutBrainDeath(GameTestHelper helper) throws Exception {
        if (!ModList.get().isLoaded("cgm")) { helper.succeed(); return; }
        var player = SeriousTraumaGameTests.victim(helper);
        equip(player, Items.NETHERITE_HELMET, Items.AIR, Items.AIR, Items.AIR);
        player.setHealth(1);
        new SeriousTraumaGameTests.CgmShots(helper, player, "cgm:rifle").fire(1.65);
        var state = BodyStateCapability.get(player).orElseThrow();
        var hit = state.downingHitRecord().orElseThrow();
        helper.assertTrue(hit.headFinalDamage() > 5 && hit.bulletLocation() == BulletHitLocation.HEAD,
                "Stopped rifle headshot fixture must still exceed 5 final damage and retain forensic location");
        helper.assertTrue(state.lifeState() == BodyLifeState.INCAPACITATED && !hit.fatalBrainInjury()
                && state.seriousTrauma().conditions().isEmpty(), "Unpenetrated helmet excludes fatal roll and concussion");
        helper.assertTrue(state.wounds().stream().allMatch(w -> w.type() == WoundType.BLUNT), "Stopped fatal hit is blunt, not gunshot");
        helper.succeed();
    }

    @GameTest(template = "migration_empty")
    public static void addonSmgAndVanillaProtectionStillWork(GameTestHelper helper) throws Exception {
        if (!ModList.get().isLoaded("nzgmaddon")) { helper.succeed(); return; }
        var plain = SeriousTraumaGameTests.victim(helper);
        var protectedPlayer = SeriousTraumaGameTests.victim(helper);
        equip(plain, Items.LEATHER_HELMET, Items.AIR, Items.AIR, Items.AIR);
        equip(protectedPlayer, Items.LEATHER_HELMET, Items.AIR, Items.AIR, Items.AIR);
        protectedPlayer.getItemBySlot(EquipmentSlot.HEAD).enchant(helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION), 4);
        float normal = new SeriousTraumaGameTests.CgmShots(helper, plain, "nzgmaddon:rapid_smg").fire(1.65);
        float reduced = new SeriousTraumaGameTests.CgmShots(helper, protectedPlayer, "nzgmaddon:rapid_smg").fire(1.65);
        helper.assertTrue(Math.abs(reduced - normal * .84F) < .001,
                "Protection IV must preserve vanilla 16% reduction for addon bullets");
        helper.assertTrue(reduced > 0 && reduced < 4 && BodyStateCapability.get(protectedPlayer).orElseThrow()
                .wounds().getFirst().type() == WoundType.GUNSHOT_LOW_VELOCITY,
                "Low-damage addon SMG remains a gunshot after enchantment reduction");
        helper.succeed();
    }

    @GameTest(template = "migration_empty")
    public static void tridentHasDistinctPenetrationPower(GameTestHelper helper) throws Exception {
        for (boolean diamond : new boolean[]{false, true}) {
            var player = SeriousTraumaGameTests.victim(helper);
            equip(player, Items.AIR, diamond ? Items.DIAMOND_CHESTPLATE : Items.IRON_CHESTPLATE, Items.AIR, Items.AIR);
            var trident = new ThrownTrident(EntityType.TRIDENT, helper.getLevel());
            trident.moveTo(player.position().add(0, 1.1, .3));
            NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(trident, new EntityHitResult(player, trident.position())));
            player.hurt(player.damageSources().trident(trident, null), 12);
            var wounds = BodyStateCapability.get(player).orElseThrow().wounds();
            helper.assertTrue(diamond ? wounds.isEmpty() : wounds.size() == 1 && wounds.getFirst().type() == WoundType.PUNCTURE,
                    "Trident P7 penetrates iron R6 but not diamond R12");
        }
        helper.succeed();
    }

    /** Fires the same impact event and damage entry used by vanilla projectiles; no client-supplied location. */
    private static float arrow(ServerPlayer target, double height, float damage) {
        var arrow = new Arrow(EntityType.ARROW, target.level());
        arrow.moveTo(target.position().add(0, height, .3));
        NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(arrow, new EntityHitResult(target, arrow.position())));
        target.invulnerableTime = 0;
        target.hurt(target.damageSources().arrow(arrow, null), damage);
        return BodyStateCapability.get(target).orElseThrow().lastFinalDamage();
    }

    @GameTest(template = "migration_empty")
    public static void arrowsRespectRegionWhilePreservingVanillaHealthDamage(GameTestHelper helper) throws Exception {
        var player = SeriousTraumaGameTests.victim(helper);
        equip(player, Items.AIR, Items.IRON_CHESTPLATE, Items.AIR, Items.AIR);
        float before = player.getHealth();
        float stopped = arrow(player, 1.1, 8);
        var state = BodyStateCapability.get(player).orElseThrow();
        helper.assertTrue(state.wounds().isEmpty(), "Arrow P=4 must not produce ST trauma through iron chest R=6");
        helper.assertTrue(stopped > 0 && Math.abs(before - player.getHealth() - stopped) < .001,
                "Stopping ST puncture must NOT cancel vanilla HP damage");
        float head = arrow(player, 1.65, 8);
        helper.assertTrue(state.wounds().size() == 1 && state.wounds().getFirst().type() == WoundType.PUNCTURE,
                "Same arrow must puncture the unprotected head");
        helper.assertTrue(Math.abs(state.wounds().getFirst().accumulatedDamage() - head) < .001,
                "Stopped arrows must not enter later penetrating puncture accumulation");
        helper.succeed();
    }

    @GameTest(template = "migration_empty")
    public static void enchantmentsReduceDamageWithoutChangingPenetrationRating(GameTestHelper helper) throws Exception {
        var plain = SeriousTraumaGameTests.victim(helper);
        var enchanted = SeriousTraumaGameTests.victim(helper);
        equip(plain, Items.AIR, Items.IRON_CHESTPLATE, Items.AIR, Items.AIR);
        equip(enchanted, Items.AIR, Items.IRON_CHESTPLATE, Items.AIR, Items.AIR);
        enchanted.getItemBySlot(EquipmentSlot.CHEST).enchant(helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROJECTILE_PROTECTION), 4);
        helper.assertTrue(ArmorPenetration.snapshot(plain).equals(ArmorPenetration.snapshot(enchanted)),
                "Protection enchantments must not add armor rating");
        float ordinary = arrow(plain, 1.65, 16), reduced = arrow(enchanted, 1.65, 16);
        helper.assertTrue(Math.abs(reduced - ordinary * .68F) < .001,
                "Projectile protection IV on chest still reduces head arrow damage by vanilla 32%");
        helper.assertTrue(BodyStateCapability.get(enchanted).orElseThrow().wounds().getFirst().type() == WoundType.PUNCTURE,
                "Enchantment must not convert an exposed-head penetration into a stop");
        helper.succeed();
    }

    @GameTest(template = "migration_empty")
    public static void sharpMeleeUsesPreArmorDamageAndOverallRating(GameTestHelper helper) throws Exception {
        var player = SeriousTraumaGameTests.victim(helper);
        equip(player, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
        var attacker = EntityType.ZOMBIE.create(helper.getLevel());
        attacker.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        player.hurt(player.damageSources().mobAttack(attacker), 6); // 4.8 < R6: blunt.
        var state = BodyStateCapability.get(player).orElseThrow();
        helper.assertTrue(state.wounds().stream().noneMatch(w -> w.type() == WoundType.SHARP), "Weak sword hit must be stopped");
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().mobAttack(attacker), 8); // 6.4 >= R6, but final D <7.5.
        helper.assertTrue(state.lastFinalDamage() < 7.5F, "Fixture distinguishes pre-armor power from reduced damage");
        helper.assertTrue(state.wounds().stream().anyMatch(w -> w.type() == WoundType.SHARP), "Strong melee must penetrate based on pre-armor damage");
        helper.succeed();
    }

    @GameTest(template = "migration_empty")
    public static void armorBreakingOnHitStillProtectsThatHit(GameTestHelper helper) throws Exception {
        var player = SeriousTraumaGameTests.victim(helper);
        equip(player, Items.AIR, Items.IRON_CHESTPLATE, Items.AIR, Items.AIR);
        var chest = player.getItemBySlot(EquipmentSlot.CHEST);
        chest.setDamageValue(chest.getMaxDamage() - 1);
        arrow(player, 1.1, 20);
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "Fixture must break armor during this hit");
        helper.assertTrue(BodyStateCapability.get(player).orElseThrow().wounds().isEmpty(),
                "Use pre-hit equipment, not the now-empty slot, for this hit's ST penetration");
        refreshArmor(player);
        arrow(player, 1.1, 8);
        helper.assertTrue(BodyStateCapability.get(player).orElseThrow().wounds().getFirst().type() == WoundType.PUNCTURE,
                "The NEXT arrow must penetrate the newly exposed chest");
        helper.succeed();
    }
}
