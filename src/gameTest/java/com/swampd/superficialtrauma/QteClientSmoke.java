package com.swampd.superficialtrauma;

import com.swampd.superficialtrauma.client.ClientTimingQteState;
import com.swampd.superficialtrauma.client.TimingQteRenderer;
import com.swampd.superficialtrauma.common.qte.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import org.lwjgl.glfw.GLFW;

/** Opt-in end-to-end local packet, render, keyboard and sound smoke test, excluded from the mod JAR. */
@EventBusSubscriber(modid = SuperficialTrauma.MOD_ID, value = Dist.CLIENT)
public final class QteClientSmoke {
    private static final TimingQteResult[] EXPECTED = { TimingQteResult.PERFECT, TimingQteResult.SUCCESS,
            TimingQteResult.EARLY_FAILURE, TimingQteResult.MISSED_FAILURE };
    private static volatile TimingQteResult serverResult;
    private static int ticks, caseIndex, soundCount;
    private static boolean running, pressed, captured;

    public static void tick() {
        var mc = Minecraft.getInstance();
        if (++ticks > 1200) throw new AssertionError("QTE client smoke timeout at " + caseIndex);
        if (running) {
            if (serverResult == null || ClientTimingQteState.active() != null) return;
            if (serverResult != EXPECTED[caseIndex % 4]) throw new AssertionError("QTE mismatch: " + serverResult);
            if (soundCount != 1) throw new AssertionError("QTE feedback must play once, actual " + soundCount);
            SuperficialTrauma.LOGGER.info("QTE CLIENT CASE PASSED: scale={}, result={}, sounds={}", 2 + caseIndex / 4, serverResult, soundCount);
            running = false;
            caseIndex++;
        }
        if (caseIndex == 12) {
            SuperficialTrauma.LOGGER.info("QTE CLIENT SMOKE PASSED: x2/x3/x4, perfect/success/early/missed, keyboard, packets, volume=0.5, exactly-once sounds");
            mc.stop();
            return;
        }
        running = true;
        pressed = captured = false;
        serverResult = null;
        soundCount = 0;
        mc.options.guiScale().set(2 + caseIndex / 4);
        mc.resizeDisplay();
        mc.setScreen(new ProbeScreen());
        var id = mc.player.getUUID();
        mc.getSingleplayerServer().execute(() -> {
            var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
            if (!TimingQteService.start(player, MedicalTimingQte.DEFINITION, (p, r) -> serverResult = r)) {
                throw new AssertionError("Probe QTE did not start");
            }
        });
    }

    @SubscribeEvent
    public static void sound(PlaySoundEvent event) {
        if (!Boolean.getBoolean("superficialtrauma.qteSmoke") || event.getSound() == null) return;
        var sound = event.getSound();
        if (!sound.getLocation().getNamespace().equals("superficialtrauma")
                || !sound.getLocation().getPath().startsWith("qte_")) return;
        soundCount++;
        // PlaySoundEvent fires before SoundEngine resolves the Sound, so getVolume() is not yet safe.
        try {
            var volume = net.minecraft.client.resources.sounds.AbstractSoundInstance.class.getDeclaredField("volume");
            volume.setAccessible(true);
            if (Math.abs(volume.getFloat(sound) - .5F) > .0001F) throw new AssertionError("QTE volume must be 0.5");
        } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }

    private static final class ProbeScreen extends Screen {
        ProbeScreen() { super(Component.literal("QTE smoke")); }
        @Override public boolean isPauseScreen() { return false; }
        @Override public void tick() { ClientTimingQteState.tick(); }
        @Override public boolean keyPressed(int key, int scan, int modifiers) {
            return key == GLFW.GLFW_KEY_SPACE && ClientTimingQteState.press() || super.keyPressed(key, scan, modifiers);
        }
        @Override public void render(GuiGraphics graphics, int x, int y, float partialTick) {
            graphics.fill(0, 0, width, height, 0xFF182020);
            TimingQteRenderer.render(graphics, font, width, height, partialTick);
            var snapshot = ClientTimingQteState.active();
            if (snapshot == null || pressed || ClientTimingQteState.submitted()) return;
            try {
                var field = ClientTimingQteState.class.getDeclaredField("timeline");
                field.setAccessible(true);
                var timeline = (TimingQteTimeline) field.get(null);
                float elapsed = timeline.inputElapsed(System.nanoTime());
                float target = switch (caseIndex % 4) {
                    case 0 -> snapshot.perfectStartTick() + .3F;
                    case 1 -> snapshot.normalStartTick() + .5F;
                    case 2 -> 1F;
                    default -> Float.MAX_VALUE;
                };
                if (!captured && elapsed > 1) {
                    graphics.flush();
                    var mc = Minecraft.getInstance();
                    Screenshot.grab(mc.gameDirectory, "qte-ui-" + caseIndex + ".png", mc.getMainRenderTarget(), msg -> {});
                    captured = true;
                }
                if (elapsed >= target) {
                    pressed = true;
                    if (!keyPressed(GLFW.GLFW_KEY_SPACE, 0, 0)) throw new AssertionError("Space input not handled");
                }
            } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
        }
    }
}
