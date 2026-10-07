package com.swampd.superficialtrauma;

import com.swampd.superficialtrauma.common.body.*;
import com.swampd.superficialtrauma.common.forensics.WeaponNameSnapshot;
import com.swampd.superficialtrauma.common.qte.*;
import com.swampd.superficialtrauma.network.packet.*;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(SuperficialTrauma.MOD_ID)
@PrefixGameTestTemplate(false)
public class QteAndWeaponNameGameTests {
    @GameTest(template = "migration_empty")
    public static void actualCgmAndAddonRenamedDowningWeapon(GameTestHelper helper) throws Exception {
        if (!ModList.get().isLoaded("cgm")) { helper.succeed(); return; }
        var guns = new java.util.ArrayList<>(java.util.List.of("cgm:pistol", "cgm:shotgun"));
        if (ModList.get().isLoaded("nzgmaddon")) guns.add("nzgmaddon:rapid_smg");
        for (String gun : guns) {
            var patient = SeriousTraumaGameTests.victim(helper);
            patient.setHealth(1);
            var shots = new SeriousTraumaGameTests.CgmShots(helper, patient, gun);
            var customName = Component.literal("致倒枪械：" + gun).withStyle(net.minecraft.ChatFormatting.GOLD);
            shots.renameWeapon(customName);
            shots.fire(.1); // Limbs: do not roll random fatal brain injury in this fixture.
            var body = BodyStateCapability.get(patient).orElseThrow();
            var hit = body.downingHitRecord().orElseThrow();
            var name = WeaponNameSnapshot.restore(hit.weaponDisplayNameJson(), helper.getLevel().registryAccess()).orElseThrow();
            helper.assertTrue(name.equals(customName), "Must use projectile weapon's exact display name: " + gun);
            shots.renameWeapon(Component.literal("后来改名"));
            shots.fire(.1);
            helper.assertTrue(body.downingHitRecord().orElseThrow().weaponDisplayNameJson().equals(hit.weaponDisplayNameJson()),
                    "Renaming or shooting again cannot overwrite the downing name");
            helper.runAfterDelay(1, () -> {
                helper.assertTrue(body.downingHitRecord().orElseThrow().weaponDisplayNameJson().equals(hit.weaponDisplayNameJson()),
                        "Shotgun tick-end evidence must preserve the name");
            });
        }
        helper.runAfterDelay(2, helper::succeed);
    }

    @GameTest(template = "migration_empty")
    public static void displayNamesPreserveTranslationAndMalformedLegacyFallsBack(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var stack = new ItemStack(Items.IRON_SWORD);
        var original = stack.getHoverName();
        helper.assertTrue(WeaponNameSnapshot.restore(WeaponNameSnapshot.capture(stack, registries), registries)
                .orElseThrow().equals(original), "Unrenamed items retain translation components");
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("\"双引号\"与中文枪名").withStyle(net.minecraft.ChatFormatting.AQUA));
        String snapshot = WeaponNameSnapshot.capture(stack, registries);
        helper.assertTrue(WeaponNameSnapshot.restore(snapshot, registries).orElseThrow().equals(stack.getHoverName()),
                "Quoted, Unicode and styled names round-trip");
        helper.assertTrue(WeaponNameSnapshot.restore("{bad json", registries).isEmpty(), "Malformed names fall back safely");
        helper.assertTrue(WeaponNameSnapshot.restore("", registries).isEmpty(), "Old corpses use default name fallback");
        helper.succeed();
    }

    @GameTest(template = "migration_empty")
    public static void qtePacketsPreserveFractionalInputAndLeadIn(GameTestHelper helper) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            var start = new TimingQteStartS2CPacket(new TimingQteSnapshot(123, 1234567890123L, 26, .28F, .34F, .52F), 10);
            TimingQteStartS2CPacket.STREAM_CODEC.encode(buffer, start);
            helper.assertTrue(start.equals(TimingQteStartS2CPacket.STREAM_CODEC.decode(buffer)), "Start packet preserves lead-in");
            var ready = new TimingQteReadyC2SPacket(123);
            TimingQteReadyC2SPacket.STREAM_CODEC.encode(buffer, ready);
            helper.assertTrue(ready.equals(TimingQteReadyC2SPacket.STREAM_CODEC.decode(buffer)), "Readiness packet round-trip");
            var input = new TimingQteSubmitC2SPacket(123, 7.3F, true);
            TimingQteSubmitC2SPacket.STREAM_CODEC.encode(buffer, input);
            helper.assertTrue(input.equals(TimingQteSubmitC2SPacket.STREAM_CODEC.decode(buffer)), "Do not quantize sub-tick input");
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(template = "migration_empty")
    public static void qteCannotResolveTwiceOrAffectAnotherSession(GameTestHelper helper) throws Exception {
        var player = SeriousTraumaGameTests.victim(helper);
        var results = new java.util.ArrayList<TimingQteResult>();
        helper.assertTrue(TimingQteService.start(player, MedicalTimingQte.DEFINITION, (p, r) -> results.add(r)), "Start succeeds");
        // Inspect just this test player's session id, without exposing a production testing API.
        var field = TimingQteService.class.getDeclaredField("ACTIVE");
        field.setAccessible(true);
        Object active = ((java.util.Map<?, ?>) field.get(null)).get(player.getUUID());
        var accessor = active.getClass().getDeclaredMethod("snapshot");
        accessor.setAccessible(true);
        int id = ((TimingQteSnapshot) accessor.invoke(active)).sessionId();
        TimingQteService.ready(player, id + 1);
        TimingQteService.submit(player, id + 1, 7.3F, true);
        helper.assertTrue(TimingQteService.hasActive(player) && results.isEmpty(), "Wrong session ignored");
        TimingQteService.ready(player, id);
        TimingQteService.submit(player, id, -10, true);
        TimingQteService.submit(player, id, -10, true);
        helper.assertTrue(results.equals(java.util.List.of(TimingQteResult.EARLY_FAILURE)), "Result settles exactly once");
        TimingQteService.start(player, MedicalTimingQte.DEFINITION, (p, r) -> results.add(r));
        TimingQteService.cancel(player);
        TimingQteService.submit(player, id, 8, true);
        helper.assertTrue(!TimingQteService.hasActive(player) && results.size() == 1, "Cancel and replay cannot apply rewards");
        helper.succeed();
    }
}
