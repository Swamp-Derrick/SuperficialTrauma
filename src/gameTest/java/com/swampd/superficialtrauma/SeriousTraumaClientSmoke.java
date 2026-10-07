package com.swampd.superficialtrauma;

import com.swampd.superficialtrauma.client.*;
import com.swampd.superficialtrauma.common.body.*;
import com.swampd.superficialtrauma.common.init.ModItems;
import com.swampd.superficialtrauma.common.medication.MedicationType;
import com.swampd.superficialtrauma.common.qte.TimingQteTimeline;
import com.swampd.superficialtrauma.common.treatment.*;
import com.swampd.superficialtrauma.common.wound.WoundType;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import java.util.UUID;
import java.util.function.Consumer;

/** Development-only GL/UI/real-server treatment probe. Never included in the shipped JAR. */
@EventBusSubscriber(modid = SuperficialTrauma.MOD_ID, value = Dist.CLIENT)
public final class SeriousTraumaClientSmoke {
    private static int ticks, stage, lastScreenshot = -1;
    private static volatile Throwable failure;
    private static UUID woundId;
    private static double visualSpeed;
    private static int qtePresses, lastValidatedStage = -1, pixelChecks;

    public static void tick() throws Exception {
        if (failure != null) throw new AssertionError("Serious trauma server probe", failure);
        var mc = Minecraft.getInstance();
        if (++ticks > 3600) throw new AssertionError("Serious trauma client smoke timeout stage " + stage);
        if (ticks % 80 != 1) return;
        var failedShader = ClientConcussionEffects.class.getDeclaredField("shaderFailed");
        failedShader.setAccessible(true);
        require(!failedShader.getBoolean(null), "Concussion shader must load and render successfully");
        switch (stage) {
            case 0 -> server(player -> {
                player.teleportTo(.5, -60, .5);
                var state = BodyStateCapability.get(player).orElseThrow();
                long now = player.level().getGameTime();
                state.configureSeriousTrauma(true, now);
                var hit = state.applyGunshotDamage(WoundType.GUNSHOT_HIGH_VELOCITY, 28, 0, 10, false, now);
                state.forceRecoverForDebug();
                state.recordGunshotLocations(hit, 28, 12, 16, now);
                state.unlockSurgerySkill(); state.unlockFirstAidSkill();
                woundId = hit.wound().id();
                player.getInventory().clearContent();
                for (var item : new net.minecraft.world.item.Item[]{ModItems.CHEST_SEAL.get(), ModItems.SURGICAL_KIT.get(),
                        ModItems.MEDICAL_GAUZE.get(), ModItems.STETHOSCOPE.get()}) {
                    player.getInventory().add(new ItemStack(item, 2));
                }
                ModNetworking.syncBodyState(player);
            });
            case 1, 2, 3 -> {
                mc.options.guiScale().set(stage + 1); mc.resizeDisplay();
                mc.setScreen(new HealthScreen());
            }
            case 4 -> server(player -> require(TreatmentService.start(player, player.getId(), woundId,
                    TreatmentProcedure.CHEST_SEAL, TreatmentAction.APPLY), "Start three-second chest seal"));
            case 5 -> server(player -> {
                var state = BodyStateCapability.get(player).orElseThrow();
                require(state.seriousTrauma().sealed(), "Chest seal completes through real service");
                require(player.getInventory().countItem(ModItems.CHEST_SEAL.get()) == 1, "Consumes exactly one seal");
                require(TreatmentService.start(player, player.getId(), woundId, TreatmentProcedure.PNEUMOTHORAX_REPAIR,
                        TreatmentAction.APPLY), "Start repair over chest seal");
            });
            case 6 -> server(player -> {
                require(TreatmentService.isActorTreating(player.getUUID()), "45-second surgery remains active");
                TreatmentService.cancelSkinGraft(player);
                require(!TreatmentService.isActorTreating(player.getUUID()), "Closing QTE surgery cancels repair too");
                require(player.getInventory().countItem(ModItems.SURGICAL_KIT.get()) == 2
                        && player.getInventory().countItem(ModItems.MEDICAL_GAUZE.get()) == 2, "Cancellation consumes nothing");
                require(TreatmentService.start(player, player.getId(), woundId, TreatmentProcedure.PNEUMOTHORAX_REPAIR,
                        TreatmentAction.APPLY), "Restart actual repair");
            });
            case 7 -> {
                if (ClientBodyState.snapshot().seriousTrauma().hasPneumothorax()) return;
                server(player -> {
                    require(player.getInventory().countItem(ModItems.SURGICAL_KIT.get()) == 1
                            && player.getInventory().countItem(ModItems.MEDICAL_GAUZE.get()) == 1,
                            "Successful repair consumes one kit and one gauze");
                    require(!BodyStateCapability.get(player).orElseThrow().seriousTrauma().hasPneumothorax(),
                            "Server clears repaired pneumothorax");
                });
                require(qtePresses > 0, "Surgery must actually exercise reusable QTE");
                mc.options.guiScale().set(2); mc.resizeDisplay();
                mc.setScreen(new ProbeScreen("Stationary: ~25% peripheral blur"));
            }
            case 8 -> { visualSpeed = 4.3; mc.setScreen(new ProbeScreen("Walking: ~40% peripheral blur")); }
            case 9 -> { visualSpeed = 6; mc.setScreen(new ProbeScreen("Fast/passive movement: ~80% blur")); }
            case 10 -> {
                server(player -> { var state = BodyStateCapability.get(player).orElseThrow();
                    state.setRespiratoryDistressForDebug(0, player.level().getGameTime());
                    state.applyMedication(MedicationType.REMIFENTANIL, player.level().getGameTime(), false);
                    require(state.pain() == 0, "Visual fixture must actually reach zero pain");
                    ModNetworking.syncBodyState(player); });
                mc.setScreen(new ProbeScreen("Pain controlled: at most ~20% blur"));
            }
            case 11 -> {
                server(player -> { var state = BodyStateCapability.get(player).orElseThrow();
                    state.seriousTrauma().clear(); ModNetworking.syncBodyState(player); });
                mc.setScreen(new ProbeScreen("Recovered: full clear scene, one-second fade"));
            }
            default -> {
                require(pixelChecks == 5, "Every visual probe must actually run its pixel assertions");
                SuperficialTrauma.LOGGER.info("SERIOUS TRAUMA CLIENT SMOKE PASSED: x2/x3/x4, chest seal consumption, repair cancellation/completion, {} QTE inputs, actual GL blur/peripheral pixel comparison and cure fade", qtePresses);
                mc.stop(); return;
            }
        }
        SuperficialTrauma.LOGGER.info("SERIOUS TRAUMA CLIENT STAGE {}", stage);
        stage++;
    }
    @SubscribeEvent
    public static void beforeFrame(RenderFrameEvent.Pre event) throws Exception {
        if (!Boolean.getBoolean("superficialtrauma.seriousTraumaSmoke")) return;
        var speed = ClientConcussionEffects.class.getDeclaredField("actualSpeed");
        speed.setAccessible(true); speed.setDouble(null, visualSpeed);
    }
    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("superficialtrauma.seriousTraumaSmoke")) return;
        var active = ClientTimingQteState.active();
        if (active != null && !ClientTimingQteState.submitted()) {
            var field = ClientTimingQteState.class.getDeclaredField("timeline"); field.setAccessible(true);
            var timeline = (TimingQteTimeline) field.get(null);
            if (timeline.inputElapsed(System.nanoTime()) >= active.normalStartTick() + .5F) {
                ClientTimingQteState.press(); qtePresses++;
            }
        }
        if (ticks % 80 < 65 || lastScreenshot == stage) return;
        lastScreenshot = stage;
        var mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "serious-trauma-" + stage + ".png", mc.getMainRenderTarget(), text -> {});
    }
    private static void server(Consumer<ServerPlayer> task) {
        var mc = Minecraft.getInstance(); var id = mc.player.getUUID();
        mc.getSingleplayerServer().execute(() -> {
            try { task.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(id)); }
            catch (Throwable thrown) { failure = thrown; }
        });
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    private static final class ProbeScreen extends Screen {
        ProbeScreen(String title) { super(Component.literal(title)); }
        @Override public boolean isPauseScreen() { return false; }
        @Override public void render(GuiGraphics graphics, int x, int y, float partial) {
            for (int row = 0; row < height; row += 4) for (int col = 0; col < width; col += 4) {
                graphics.fill(col, row, col + 4, row + 4, ((row/4 + col/4) % 2 == 0) ? 0xFFEEEEEE : 0xFF222222);
            }
            graphics.flush();
            ClientConcussionEffects.render();
            graphics.fill(10, 10, 340, 42, 0xFF142626);
            graphics.drawString(font, title, 16, 18, 0xFFFFFFFF);
            graphics.drawString(font, "HUD / medical menus / QTE stay sharp", 16, 30, 0xFFFFFFFF);
            graphics.flush();
            if (ticks % 80 >= 55 && lastValidatedStage != stage) {
                lastValidatedStage = stage; pixelChecks++;
                try {
                    var field = ClientConcussionEffects.class.getDeclaredField("VISUAL"); field.setAccessible(true);
                    var visual = (ConcussionVisualState) field.get(null);
                    if (stage < 12) {
                        double expected = ConcussionVisualState.targetCoverage(visualSpeed, stage == 11);
                        require(Math.abs(visual.coverage() - expected) < .03, "Rendered coverage must match stage: " + stage + ": " + visual.coverage());
                    } else require(visual.intensity() == 0, "Cure fade must finish");
                } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
                try (var pixels = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
                    int cx = pixels.getWidth()/2 + 2, cy = pixels.getHeight()/2 + 2;
                    int center = pixels.getPixelRGBA(cx, cy) & 255;
                    int corner = pixels.getPixelRGBA(4, pixels.getHeight()-4) & 255;
                    require(center < 45 || center > 215, "Center must remain crisp after onset pulse");
                    if (stage < 12) require(corner > 45 && corner < 215, "Peripheral checkerboard must be truly blurred");
                    else require(corner < 45 || corner > 215, "Cured scene must fade completely clear");
                    SuperficialTrauma.LOGGER.info("BLUR PIXELS stage={} center={} corner={}", stage, center, corner);
                }
            }
        }
    }
}
