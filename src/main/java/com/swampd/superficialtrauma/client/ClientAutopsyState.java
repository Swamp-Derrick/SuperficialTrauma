package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.common.forensics.AutopsyReport;
import net.minecraft.client.Minecraft;

public final class ClientAutopsyState {
    private static AutopsyReport report;

    private ClientAutopsyState() {
    }

    public static void update(AutopsyReport updated, boolean openScreen) {
        report = updated;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof AutopsyScreen screen
                && screen.isInspecting(updated.corpseEntityId())) {
            screen.applyReport(updated);
        } else if (openScreen) {
            minecraft.setScreen(new AutopsyScreen(updated));
        }
    }

    public static void close(int corpseEntityId) {
        if (report == null || report.corpseEntityId() != corpseEntityId) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof AutopsyScreen screen && screen.isInspecting(corpseEntityId)) {
            minecraft.setScreen(null);
        }
        clear();
    }

    public static void clear() {
        report = null;
        ClientTimingQteState.clear();
    }
}
