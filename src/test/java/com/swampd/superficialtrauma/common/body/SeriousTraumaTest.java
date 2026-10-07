package com.swampd.superficialtrauma.common.body;

import com.swampd.superficialtrauma.common.damage.*;
import com.swampd.superficialtrauma.common.wound.WoundType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.swampd.superficialtrauma.common.body.SeriousTraumaState.Condition.*;
import static com.swampd.superficialtrauma.common.damage.GunshotRegion.*;
import static org.junit.jupiter.api.Assertions.*;

class SeriousTraumaTest {
    @Test void configDefaultMatchesPackagedBuildProfile() throws Exception {
        var profile = new java.util.Properties();
        try (var stream = getClass().getResourceAsStream("/superficialtrauma-build.properties")) {
            assertNotNull(stream);
            profile.load(stream);
        }
        var value = (net.neoforged.neoforge.common.ModConfigSpec.BooleanValue)
                com.swampd.superficialtrauma.common.config.SeriousTraumaConfig.SPEC.getValues().get("serioustrauma");
        assertEquals(Boolean.parseBoolean(profile.getProperty("development")), value.getDefault());
        assertFalse(com.swampd.superficialtrauma.common.config.SeriousTraumaConfig.PRODUCTION_DEFAULT);
    }

    private BodyState enabled() {
        BodyState state = new BodyState();
        state.configureSeriousTrauma(true, 0);
        return state;
    }

    private WoundUpdateResult hit(BodyState state, GunshotRegion region, float damage, long time) {
        var result = state.applyGunshotDamage(WoundType.GUNSHOT_HIGH_VELOCITY, damage, 0, 10, false, time);
        state.recordGunshotLocations(result, damage, region == HEAD ? damage : 0, region == CHEST ? damage : 0, time);
        return result;
    }

    @Test void independentPoolsIgnoreFeetEvenInsideSameMergedWound() {
        BodyState state = enabled();
        var wound = hit(state, HEAD, 5, 100).wound();
        hit(state, CHEST, 8, 110);
        hit(state, null, 40, 115); // Same normal wound, but feet/unknown cannot enter either pool.
        hit(state, HEAD, 5, 120);
        assertEquals(1, state.wounds().size());
        assertTrue(wound.accumulatedDamage() > 50);
        assertEquals(10, state.seriousTrauma().damage(HEAD));
        assertEquals(8, state.seriousTrauma().damage(CHEST));
        assertTrue(state.seriousTrauma().conditions().isEmpty());
        hit(state, HEAD, 4, 121);
        assertEquals(java.util.Set.of(CONCUSSION), state.seriousTrauma().conditions());
        hit(state, CHEST, 7, 123);
        assertFalse(state.seriousTrauma().conditions().contains(PNEUMOTHORAX));
        hit(state, CHEST, 4, 125);
        hit(state, CHEST, 20, 126);
        assertEquals(java.util.Set.of(CONCUSSION, PNEUMOTHORAX), state.seriousTrauma().conditions());
        assertEquals(java.util.Set.of(HEAD, CHEST), wound.gunshotRegions());
    }

    @Test void eachRegionHasItsOwnFixedTwentySecondWindow() {
        BodyState state = enabled();
        hit(state, HEAD, 6, 0);
        hit(state, CHEST, 8, 200);
        hit(state, HEAD, 6, 400); // Boundary is excluded from the original head window.
        assertFalse(state.seriousTrauma().conditions().contains(CONCUSSION));
        hit(state, CHEST, 8, 599);
        assertTrue(state.seriousTrauma().conditions().contains(PNEUMOTHORAX));
        assertEquals(6, state.seriousTrauma().damage(HEAD));
        state.configureSeriousTrauma(true, 800);
        assertEquals(0, state.seriousTrauma().damage(HEAD));
        assertEquals(0, state.seriousTrauma().damage(CHEST));
        assertEquals(java.util.Set.of(PNEUMOTHORAX), state.seriousTrauma().conditions());
    }

    @Test void differentAmmoTypesShareRegionalPoolButDoNotMergeNormalWounds() {
        BodyState state = enabled();
        hit(state, HEAD, 6, 0);
        var low = state.applyGunshotDamage(WoundType.GUNSHOT_LOW_VELOCITY, 5, 0, 10, false, 10);
        state.recordGunshotLocations(low, 5, 5, 0, 10);
        assertEquals(2, state.wounds().size());
        assertEquals(11, state.seriousTrauma().damage(HEAD));
        assertTrue(state.seriousTrauma().conditions().contains(CONCUSSION));
    }

    @Test void delayedPelletResolutionUsesOriginalWindowNotNewerHeadDamage() {
        BodyState state = enabled();
        hit(state, HEAD, 6, 0);
        state.configureSeriousTrauma(true, 400);
        hit(state, HEAD, 4, 400);
        var shotgun = state.applyGunshotDamage(WoundType.GUNSHOT_SHOTGUN, 8, 0, 2, false, 401);
        state.recordGunshotLocations(shotgun, 8, 5, 0, 399);
        assertTrue(state.seriousTrauma().conditions().contains(CONCUSSION)); // Old pool is 6+5.
        assertEquals(4, state.seriousTrauma().damage(HEAD)); // New pool stays independent.
        state.configureSeriousTrauma(false, 402);
        state.configureSeriousTrauma(true, 403);
        hit(state, HEAD, 6, 404);
        assertTrue(state.seriousTrauma().conditions().isEmpty());
    }

    @Test void bluntFallbackNeverCountsEvenWhenItAccumulatesOverThreshold() {
        BodyState state = enabled();
        for (int index = 0; index < 12; index++) {
            var result = state.applyGunshotDamage(WoundType.GUNSHOT_HIGH_VELOCITY, 3, 14, 10, false, index);
            state.recordGunshotLocations(result, 3, 3, 0, index);
        }
        assertTrue(state.seriousTrauma().conditions().isEmpty());
        assertEquals(0, state.seriousTrauma().damage(HEAD));
        assertTrue(state.wounds().stream().allMatch(w -> w.gunshotRegions().isEmpty()));
    }

    @Test void disabledAndReenabledNeverBackfillOldHits() {
        BodyState state = new BodyState();
        hit(state, HEAD, 12, 0);
        assertTrue(state.seriousTrauma().conditions().isEmpty());
        assertEquals(java.util.Set.of(HEAD), state.wounds().getFirst().gunshotRegions());
        state.configureSeriousTrauma(true, 10);
        hit(state, HEAD, 4, 11);
        assertEquals(4, state.seriousTrauma().damage(HEAD));
        hit(state, HEAD, 8, 12);
        state.configureSeriousTrauma(false, 13);
        assertTrue(state.seriousTrauma().conditions().isEmpty());
        assertEquals(0, state.seriousTrauma().damage(HEAD));
        assertEquals(java.util.Set.of(HEAD), state.wounds().getFirst().gunshotRegions());
        state.configureSeriousTrauma(true, 14);
        hit(state, HEAD, 4, 15);
        assertTrue(state.seriousTrauma().conditions().isEmpty());
    }

    @Test void saveLoadAndOldSavesAndDebugRecovery() {
        BodyState state = enabled();
        hit(state, HEAD, 11, 0);
        hit(state, CHEST, 16, 1);
        BodyState copy = new BodyState();
        copy.deserializeNBT(state.serializeNBT());
        assertEquals(state.seriousTrauma().conditions(), copy.seriousTrauma().conditions());
        assertEquals(11, copy.seriousTrauma().damage(HEAD));
        assertEquals(state.wounds().getFirst().gunshotRegions(), copy.wounds().getFirst().gunshotRegions());
        assertTrue(copy.forceRecoverForDebug());
        assertTrue(copy.seriousTrauma().conditions().isEmpty());
        assertTrue(copy.wounds().getFirst().gunshotRegions().isEmpty());
        var old = state.serializeNBT();
        old.remove("SeriousTrauma");
        copy.deserializeNBT(old);
        assertFalse(copy.seriousTrauma().enabled());
        assertTrue(copy.seriousTrauma().conditions().isEmpty());
        copy.deserializeNBT(new CompoundTag());
        assertFalse(copy.seriousTrauma().enabled());
    }

    @Test void concussionAddsPainWithoutChangingTheGunshotWound() {
        BodyState enabled = enabled(), disabled = new BodyState();
        hit(enabled, HEAD, 12, 0);
        hit(disabled, HEAD, 12, 0);
        assertEquals(disabled.pain() + 2, enabled.pain());
        assertEquals(disabled.respiratoryDistress(), enabled.respiratoryDistress());
        assertEquals(disabled.lifeState(), enabled.lifeState());
        assertEquals(disabled.wounds().getFirst().woundTags(), enabled.wounds().getFirst().woundTags());
        assertEquals(disabled.wounds().getFirst().healingProgress(), enabled.wounds().getFirst().healingProgress());
    }

    @Test void locationGeometryHandlesFeetAbdomenSideArmsYawAndCrouching() {
        AABB box = new AABB(-.3, 10, -.3, .3, 11.8, .3);
        assertEquals(HEAD, GunshotHitLocations.classify(box, 0, Pose.STANDING, new Vec3(0, 11.7, -.3)));
        assertEquals(CHEST, GunshotHitLocations.classify(box, 0, Pose.STANDING, new Vec3(0, 11.1, -.3)));
        assertNull(GunshotHitLocations.classify(box, 0, Pose.STANDING, new Vec3(0, 10.1, -.3)));
        assertNull(GunshotHitLocations.classify(box, 0, Pose.STANDING, new Vec3(0, 10.7, -.3)));
        assertNull(GunshotHitLocations.classify(box, 0, Pose.STANDING, new Vec3(.29, 11.1, 0)));
        assertEquals(CHEST, GunshotHitLocations.classify(box, 90, Pose.STANDING, new Vec3(-.3, 11.1, 0)));
        assertNull(GunshotHitLocations.classify(box, 90, Pose.STANDING, new Vec3(0, 11.1, .29)));
        assertNull(GunshotHitLocations.classify(box, 0, Pose.SWIMMING, new Vec3(0, 11.7, 0)));
        assertNull(GunshotHitLocations.classify(box, 0, Pose.STANDING, new Vec3(50, 11.7, 0)));
        AABB crouching = new AABB(-.3, 0, -.3, .3, 1.5, .3);
        assertEquals(HEAD, GunshotHitLocations.classify(crouching, 0, Pose.CROUCHING, new Vec3(0, 1.3, -.3)));
        assertEquals(CHEST, GunshotHitLocations.classify(crouching, 0, Pose.CROUCHING, new Vec3(0, .9, -.3)));
    }

    @Test void shotgunPelletsRetainSeparateRegionsAndOnlyPenetratingVolleysCount() {
        var accumulator = new ShotgunVolleyAccumulator();
        UUID victim = UUID.randomUUID();
        var key = new ShotgunVolleyAccumulator.VolleyKey(victim, UUID.randomUUID(), "shell", "shotgun", 0);
        for (int i = 0; i < 8; i++) accumulator.addHit(key, 2, 0, 2, 0, "bullet", null);
        accumulator.addHit(key, 2, 0, 2, 0, "bullet", HEAD);
        accumulator.addHit(key, 3, 0, 2, 0, "bullet", CHEST);
        var volley = accumulator.drainReady(victim, 2).getFirst();
        BodyState state = enabled();
        var result = state.applyGunshotDamage(WoundType.GUNSHOT_SHOTGUN, volley.totalFinalDamage(), 0, 2, false, 2);
        for (var hit : volley.regionalHits()) state.recordGunshotLocations(result, volley.totalFinalDamage(),
                hit.region() == HEAD ? hit.damage() : 0, hit.region() == CHEST ? hit.damage() : 0, hit.gameTime());
        assertEquals(21, result.wound().accumulatedDamage());
        assertEquals(2, state.seriousTrauma().damage(HEAD));
        assertEquals(3, state.seriousTrauma().damage(CHEST));
        assertTrue(state.seriousTrauma().conditions().isEmpty());
        var low = state.applyGunshotDamage(WoundType.GUNSHOT_SHOTGUN, 3, 12, 2, false, 5);
        state.recordGunshotLocations(low, 3, 3, 0, 5);
        assertEquals(2, state.seriousTrauma().damage(HEAD));
    }
}
