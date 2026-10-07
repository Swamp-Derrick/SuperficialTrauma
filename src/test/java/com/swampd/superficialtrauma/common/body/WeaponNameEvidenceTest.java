package com.swampd.superficialtrauma.common.body;

import com.swampd.superficialtrauma.common.damage.*;
import com.swampd.superficialtrauma.common.entity.CorpseSnapshot;
import com.swampd.superficialtrauma.common.forensics.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class WeaponNameEvidenceTest {
    private static final String NAME = "{\"text\":\"改名后的冲锋枪\",\"color\":\"gold\"}";
    private static DamageClassification bullet() {
        return DamageClassification.cgmProjectile(DamageKind.CGM_LOW_VELOCITY, "test", "cgm:projectile",
                "cgm:basic_bullet", "cgm:pistol");
    }
    private static DowningHitRecord hit() {
        return new DowningHitRecord(9, "cgm.bullet", DamageKind.CGM_LOW_VELOCITY, "test", "cgm:projectile",
                "cgm:basic_bullet", "cgm:pistol", 3, 100).withWeaponDisplayName(NAME);
    }

    @Test void bodyPersistsNameAndDowningEvidenceCannotBeOverwritten() {
        var body = new BodyState();
        body.recordFinalDamage(9, "cgm.bullet", bullet(), 3, 100, NAME);
        body.deserializeNBT(body.serializeNBT());
        assertTrue(body.incapacitateFromLastDamage(CollapseReason.HEMORRHAGIC_SHOCK, 100));
        assertEquals(NAME, body.downingHitRecord().orElseThrow().weaponDisplayNameJson());
        body.recordFinalDamage(20, "cgm.bullet", bullet(), 3, 101, "\"另一把枪\"");
        body.deserializeNBT(body.serializeNBT());
        assertEquals(NAME, body.downingHitRecord().orElseThrow().weaponDisplayNameJson());
    }

    @Test void volleyEvidenceCorpseAndDetailedReportRetainNameOnlyAfterUnlock() {
        var hit = hit().withBulletEvidence(BulletHitLocation.HEAD, 9, true);
        assertEquals(NAME, hit.weaponDisplayNameJson());
        var corpse = new CorpseSnapshot(UUID.randomUUID(), "Victim", "", "", 100, null, List.of(), hit,
                CollapseReason.HEMORRHAGIC_SHOCK, false, false);
        assertEquals(hit, CorpseSnapshot.load(corpse.save()).downingHitRecord());
        for (boolean detailed : new boolean[]{false, true}) {
            var report = new AutopsyReport(1, "Victim", 0, List.of(), detailed, hit, false, false, false, false,
                    true, true, true, -1, AutopsyAction.NONE, -1);
            var saved = report.save();
            assertEquals(detailed, saved.contains("DowningHit"));
            assertEquals(detailed ? hit : null, AutopsyReport.load(saved).downingHit());
        }
    }

    @Test void oldSavesAndNewNonWeaponHitDoNotInventNames() {
        var old = hit().serializeNBT();
        old.remove("WeaponDisplayNameJson");
        assertEquals("", DowningHitRecord.deserializeNBT(old).weaponDisplayNameJson());
        var body = new BodyState();
        body.recordFinalDamage(2, "cgm.bullet", bullet(), 3, 99, NAME);
        body.recordFinalDamage(9, "cgm.bullet", bullet(), 100);
        body.incapacitateFromLastDamage(CollapseReason.HEMORRHAGIC_SHOCK, 100);
        assertEquals("", body.downingHitRecord().orElseThrow().weaponDisplayNameJson());
        assertEquals("", hit().withWeaponDisplayName("x".repeat(WeaponNameSnapshot.MAX_JSON_LENGTH + 1)).weaponDisplayNameJson());
    }
}
