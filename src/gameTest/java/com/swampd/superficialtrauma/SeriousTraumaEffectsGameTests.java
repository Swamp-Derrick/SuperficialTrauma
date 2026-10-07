package com.swampd.superficialtrauma;

import com.swampd.superficialtrauma.common.body.*;
import com.swampd.superficialtrauma.common.damage.BloodLossDamage;
import com.swampd.superficialtrauma.common.init.ModItems;
import com.swampd.superficialtrauma.common.crafting.MedicalWorkbenchRecipes;
import com.swampd.superficialtrauma.common.wound.WoundType;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(SuperficialTrauma.MOD_ID)
@PrefixGameTestTemplate(false)
public class SeriousTraumaEffectsGameTests {
    @GameTest(template = "migration_empty")
    public static void realCgmLegAndChestReinjury(GameTestHelper helper) throws Exception {
        if (!ModList.get().isLoaded("cgm")) { helper.succeed(); return; }
        var patient = SeriousTraumaGameTests.victim(helper);
        var state = BodyStateCapability.get(patient).orElseThrow();
        var shots = new SeriousTraumaGameTests.CgmShots(helper, patient, "cgm:pistol");
        for (int i = 0; i < 5; i++) shots.fire(1.1);
        helper.assertTrue(state.hasOpenPneumothorax(), "Real penetrating chest rounds must create functional pneumothorax");
        var host = state.wounds().stream().filter(w -> w.pneumothoraxWound()).findFirst().orElseThrow();
        state.applyChestSeal(host.id(), helper.getLevel().getGameTime());
        shots.fire(.1);
        helper.assertTrue(host.chestSealApplied() && state.seriousTrauma().sealed(), "Leg shot must not remove chest seal");
        shots.fire(1.1);
        helper.assertTrue(!host.chestSealApplied() && state.hasOpenPneumothorax(), "Related chest penetration must reopen the same condition");
        helper.assertTrue(state.wounds().stream().filter(w -> w.pneumothoraxWound()).count() == 1, "One operable host only");
        helper.succeed();
    }
    @GameTest(template = "migration_empty")
    public static void internalBleedingDoesNotResetConcussionOrDislodgeGauze(GameTestHelper helper) throws Exception {
        var patient = SeriousTraumaGameTests.victim(helper);
        var state = BodyStateCapability.get(patient).orElseThrow();
        long now = helper.getLevel().getGameTime();
        state.configureSeriousTrauma(true, now);
        state.seriousTrauma().addDowningConcussion(Math.max(0, now - 20));
        var wound = state.applyDamage(WoundType.SHARP, 12, now).wound();
        state.applyWoundPacking(wound.id(), now);
        long deadline = state.seriousTrauma().concussionEndsAt();
        BloodLossDamage.apply(patient, 2);
        helper.assertTrue(wound.woundPackingApplied(), "Internal medical bleeding must not drop gauze");
        helper.assertTrue(state.seriousTrauma().concussionEndsAt() == deadline, "Internal bleeding must not reset quiet period");
        patient.invulnerableTime = 0;
        patient.hurt(patient.damageSources().generic(), 2);
        helper.assertTrue(!wound.woundPackingApplied(), "External damage greater than one must dislodge unsecured gauze");
        helper.assertTrue(state.seriousTrauma().concussionEndsAt() == now + 3600, "External trauma must reset quiet period");
        helper.succeed();
    }
    @GameTest(template = "migration_empty")
    public static void chestSealRegistrationAndWorkbenchRecipe(GameTestHelper helper) {
        var stack = new ItemStack(ModItems.CHEST_SEAL.get());
        helper.assertTrue(stack.getMaxStackSize() == 16, "Chest seal stacks to 16 like other treatment consumables");
        var recipe = MedicalWorkbenchRecipes.byKey("chest_seal");
        helper.assertTrue(recipe != null, "New consumable must be obtainable from the workbench");
        helper.succeed();
    }
}
