package com.swampd.superficialtrauma.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.BodyState;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import com.swampd.superficialtrauma.common.init.ModSounds;
import com.swampd.superficialtrauma.common.audio.ConcussionHearingEnvelope;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Runs before the first HUD layer: world/hand blur, with medical screens, QTE and blood above it. */
public final class ClientConcussionEffects {
    private static final ConcussionVisualState VISUAL = new ConcussionVisualState();
    private static PostChain chain;
    private static int width, height;
    private static long lastFrame;
    private static Vec3 lastPosition;
    private static Object lastLevel;
    private static double actualSpeed;
    private static boolean shaderFailed;
    private static SoundInstance tinnitus;
    private ClientConcussionEffects() {}

    public static void synchronize(BodyState previous, BodyState updated, boolean received) {
        // A reconnect/initial state packet is not a new injury flash.
        if (ConcussionHearingEnvelope.isNewEpisode(received, previous.seriousTrauma().hasConcussion(), updated.seriousTrauma().hasConcussion())) {
            VISUAL.onset();
            stopTinnitus();
            tinnitus = SimpleSoundInstance.forUI(ModSounds.TINNITUS.get(), 1, 1);
            Minecraft.getInstance().getSoundManager().play(tinnitus);
        }
        if (!updated.seriousTrauma().hasConcussion()) stopTinnitus();
    }
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) { clear(); return; }
        if (mc.isPaused()) return;
        Vec3 position = mc.player.position();
        double displacement = lastPosition == null ? 0 : position.distanceTo(lastPosition);
        // Includes passive displacement, but not dimension changes or teleport jumps.
        actualSpeed = mc.level == lastLevel && displacement < 8 ? displacement * 20 : 0;
        lastLevel = mc.level;
        lastPosition = position;
    }
    public static void render() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        long now = System.nanoTime();
        double dt = lastFrame == 0 ? 0 : (now - lastFrame) / 1_000_000_000.0;
        lastFrame = now;
        BodyState state = ClientBodyState.snapshot();
        boolean active = state.seriousTrauma().hasConcussion()
                && state.lifeState() != com.swampd.superficialtrauma.common.body.BodyLifeState.BRAIN_DEAD;
        VISUAL.update(mc.isPaused() ? 0 : dt, active, state.pain() <= 0, actualSpeed);
        if (VISUAL.intensity() < .001 && VISUAL.pulse() == 0 && !active) {
            release();
            return;
        }
        if (shaderFailed) return;
        try {
            var target = mc.getMainRenderTarget();
            if (chain == null) {
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), target,
                        ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "shaders/post/concussion.json"));
                width = height = 0;
            }
            if (width != target.width || height != target.height) {
                width = target.width;
                height = target.height;
                chain.resize(width, height);
            }
            chain.setUniform("BlurRadius", VISUAL.blurRadiusPixels(height));
            chain.setUniform("ClearRadius", ConcussionVisualState.clearRadius(VISUAL.coverage(), (double) width / height));
            chain.setUniform("Intensity", VISUAL.intensity());
            chain.setUniform("Pulse", VISUAL.pulse());
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.resetTextureMatrix();
            chain.process((float) dt);
        } catch (Exception failure) {
            shaderFailed = true;
            release();
            LogUtils.getLogger().error("Could not render ST concussion blur; disabled until resource reload", failure);
        } finally {
            mc.getMainRenderTarget().bindWrite(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }
    private static void release() {
        if (chain != null) { chain.close(); chain = null; }
    }
    public static void reload() { release(); shaderFailed = false; }
    public static void clear() {
        stopTinnitus();
        release(); VISUAL.clear(); lastFrame = 0; lastPosition = null; lastLevel = null; actualSpeed = 0;
    }
    private static void stopTinnitus() {
        if (tinnitus != null) { Minecraft.getInstance().getSoundManager().stop(tinnitus); tinnitus = null; }
    }
}
