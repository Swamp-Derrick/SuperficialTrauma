package com.swampd.superficialtrauma.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.swampd.superficialtrauma.common.body.BodyLifeState;
import com.swampd.superficialtrauma.common.body.BodyState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ClientVitalSignsOverlay {
    private static final VitalSignsVisualState VISUALS = new VitalSignsVisualState();
    private static final List<VeinSegment> VEINS = createVeins();
    private static LocalPlayer subject;

    private ClientVitalSignsOverlay() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null
                || !minecraft.player.isAlive() || minecraft.player.isSpectator()
                || !ClientBodyState.hasReceivedSnapshot()) {
            clear();
            return;
        }
        if (subject != minecraft.player) {
            // Death/respawn must not transfer the old body's visual effects to the new player.
            clear();
            subject = minecraft.player;
        }
        if (minecraft.isPaused()) {
            return;
        }
        BodyState state = ClientBodyState.snapshot();
        int heartRate = switch (state.lifeState()) {
            case CARDIAC_ARREST, VENTRICULAR_FIBRILLATION, BRAIN_DEAD -> 0;
            default -> state.effectiveHeartRateLevel();
        };
        float distress = state.lifeState() == BodyLifeState.BRAIN_DEAD
                ? 0.0F : state.respiratoryDistress();
        VISUALS.tick(heartRate, distress);
    }

    public static void render(GuiGraphics graphics, int width, int height, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player != subject
                || !minecraft.player.isAlive() || minecraft.player.isSpectator()
                || !ClientBodyState.hasReceivedSnapshot() || width <= 0 || height <= 0) {
            return;
        }
        VitalSignsVisualState.Frame frame = VISUALS.frame(minecraft.isPaused() ? 1.0F : partialTick);
        if (!frame.visible()) {
            return;
        }

        // Normal alpha blending, independent of vanilla vignette/graphics settings.
        // The overlay is registered before HUD text and before downed/recovery overlays.
        graphics.flush();
        VertexConsumer vertices = graphics.bufferSource().getBuffer(RenderType.guiOverlay());
        Matrix4f pose = graphics.pose().last().pose();
        float shortSide = Math.min(width, height);
        if (frame.redness() > 0.0001F) {
            vignette(vertices, pose, width, height,
                    shortSide * (0.10F + 0.08F * frame.redness()),
                    0xA91420, frame.redness() * 0.42F);
        }
        if (frame.veins() > 0.0001F) {
            renderVeins(vertices, pose, width, height, shortSide, frame.veins());
        }
        if (frame.respiratoryEdge() > 0.0001F) {
            vignette(vertices, pose, width, height,
                    shortSide * 0.30F * frame.respiratoryEdge(),
                    0x000000, frame.respiratoryEdge() * 0.92F);
        }
        if (frame.respiratoryDim() > 0.0001F) {
            float alpha = frame.respiratoryDim() * 0.70F;
            vertex(vertices, pose, 0, 0, 0, alpha);
            vertex(vertices, pose, 0, height, 0, alpha);
            vertex(vertices, pose, width, height, 0, alpha);
            vertex(vertices, pose, width, 0, 0, alpha);
        }
        graphics.flush();
    }

    public static void clear() {
        VISUALS.clear();
        subject = null;
    }

    private static void vignette(
            VertexConsumer vertices, Matrix4f pose, float width, float height,
            float depth, int color, float opacity
    ) {
        // Four non-overlapping trapezoids fade continuously to a clear center.
        // Fractional coordinates avoid whole-GUI-pixel jumps while thickness animates.
        float right = width - depth;
        float bottom = height - depth;
        vertex(vertices, pose, 0, 0, color, opacity);
        vertex(vertices, pose, depth, depth, color, 0);
        vertex(vertices, pose, right, depth, color, 0);
        vertex(vertices, pose, width, 0, color, opacity);

        vertex(vertices, pose, width, 0, color, opacity);
        vertex(vertices, pose, right, depth, color, 0);
        vertex(vertices, pose, right, bottom, color, 0);
        vertex(vertices, pose, width, height, color, opacity);

        vertex(vertices, pose, width, height, color, opacity);
        vertex(vertices, pose, right, bottom, color, 0);
        vertex(vertices, pose, depth, bottom, color, 0);
        vertex(vertices, pose, 0, height, color, opacity);

        vertex(vertices, pose, 0, height, color, opacity);
        vertex(vertices, pose, depth, bottom, color, 0);
        vertex(vertices, pose, depth, depth, color, 0);
        vertex(vertices, pose, 0, 0, color, opacity);
    }

    private static void renderVeins(
            VertexConsumer vertices, Matrix4f pose, int width, int height,
            float shortSide, float strength
    ) {
        float lineWidth = Math.max(0.30F, shortSide * 0.0011F);
        for (VeinSegment vein : VEINS) {
            float edgeLength = vein.side % 2 == 0 ? width : height;
            float alongStart = vein.startAlong * edgeLength;
            float alongEnd = vein.endAlong * edgeLength;
            float depthStart = vein.startDepth * shortSide;
            float depthEnd = vein.endDepth * shortSide;
            float x1, y1, x2, y2;
            switch (vein.side) {
                case 0 -> { x1 = alongStart; y1 = depthStart; x2 = alongEnd; y2 = depthEnd; }
                case 1 -> { x1 = width - depthStart; y1 = alongStart; x2 = width - depthEnd; y2 = alongEnd; }
                case 2 -> { x1 = alongStart; y1 = height - depthStart; x2 = alongEnd; y2 = height - depthEnd; }
                default -> { x1 = depthStart; y1 = alongStart; x2 = depthEnd; y2 = alongEnd; }
            }
            float thickness = lineWidth * vein.taper;
            stroke(vertices, pose, x1, y1, x2, y2, thickness * 2.8F,
                    strength * vein.taper * 0.10F);
            stroke(vertices, pose, x1, y1, x2, y2, thickness,
                    strength * vein.taper * 0.62F);
        }
    }

    private static void stroke(
            VertexConsumer vertices, Matrix4f pose, float x1, float y1, float x2, float y2,
            float width, float alpha
    ) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float scale = width * 0.5F / (float) Math.sqrt(dx * dx + dy * dy);
        float nx = -dy * scale;
        float ny = dx * scale;
        vertex(vertices, pose, x1 - nx, y1 - ny, 0x880B16, alpha);
        vertex(vertices, pose, x1 + nx, y1 + ny, 0x880B16, alpha);
        vertex(vertices, pose, x2 + nx, y2 + ny, 0x880B16, alpha * 0.85F);
        vertex(vertices, pose, x2 - nx, y2 - ny, 0x880B16, alpha * 0.85F);
    }

    private static void vertex(
            VertexConsumer vertices, Matrix4f pose, float x, float y, int rgb, float alpha
    ) {
        vertices.vertex(pose, x, y, 0.0F).color(
                (rgb >> 16 & 255) / 255.0F,
                (rgb >> 8 & 255) / 255.0F,
                (rgb & 255) / 255.0F, alpha
        ).endVertex();
    }

    private static List<VeinSegment> createVeins() {
        // Generated once: stable thin branches, no per-frame random flicker or resize regeneration.
        Random random = new Random(0x53545645494EL);
        List<VeinSegment> result = new ArrayList<>();
        for (int side = 0; side < 4; side++) {
            for (int root = 0; root < 6; root++) {
                float along = 0.06F + root * 0.17F + random.nextFloat() * 0.025F;
                float depth = -0.002F;
                float step = 0.011F + random.nextFloat() * 0.008F;
                for (int segment = 0; segment < 5; segment++) {
                    float nextAlong = along + (random.nextFloat() - 0.5F) * 0.018F;
                    float nextDepth = depth + step;
                    float taper = 1.0F - segment * 0.15F;
                    result.add(new VeinSegment(side, along, depth, nextAlong, nextDepth, taper));
                    if (segment == 1 || segment == 3) {
                        float branchAlong = nextAlong + (random.nextBoolean() ? 1 : -1) * 0.013F;
                        float branchDepth = nextDepth + step * 0.6F;
                        result.add(new VeinSegment(side, nextAlong, nextDepth,
                                branchAlong, branchDepth, taper * 0.65F));
                        result.add(new VeinSegment(side, branchAlong, branchDepth,
                                branchAlong + (branchAlong - nextAlong) * 0.45F,
                                branchDepth + step * 0.35F, taper * 0.35F));
                    }
                    along = nextAlong;
                    depth = nextDepth;
                }
            }
        }
        return List.copyOf(result);
    }

    private record VeinSegment(
            int side, float startAlong, float startDepth, float endAlong, float endDepth, float taper
    ) {
    }
}
