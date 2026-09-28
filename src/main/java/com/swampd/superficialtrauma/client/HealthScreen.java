package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.BodyState;
import com.swampd.superficialtrauma.common.body.BodyLifeState;
import com.swampd.superficialtrauma.common.body.CollapseReason;
import com.swampd.superficialtrauma.common.body.InfusionType;
import com.swampd.superficialtrauma.common.body.DefibrillationEnergy;
import com.swampd.superficialtrauma.common.body.HealthStatus;
import com.swampd.superficialtrauma.common.body.PainSensation;
import com.swampd.superficialtrauma.common.damage.DamageWindow;
import com.swampd.superficialtrauma.common.init.ModItems;
import com.swampd.superficialtrauma.common.item.DefibrillatorItem;
import com.swampd.superficialtrauma.common.medication.MedicationType;
import com.swampd.superficialtrauma.common.treatment.TreatmentAction;
import com.swampd.superficialtrauma.common.treatment.TreatmentIngredient;
import com.swampd.superficialtrauma.common.treatment.TreatmentMovementRules;
import com.swampd.superficialtrauma.common.treatment.TreatmentProcedure;
import com.swampd.superficialtrauma.common.treatment.TreatmentPreparationType;
import com.swampd.superficialtrauma.common.treatment.TreatmentType;
import com.swampd.superficialtrauma.common.wound.WoundCovering;
import com.swampd.superficialtrauma.common.wound.WoundInstance;
import com.swampd.superficialtrauma.common.wound.WoundTag;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class HealthScreen extends Screen {
    private static final int BACKGROUND_COLOR = 0xF0181C22;
    private static final int COLUMN_COLOR = 0xD9242932;
    private static final int BORDER_COLOR = 0xFF68717F;
    private static final int TITLE_COLOR = 0xFFE8EDF2;
    private static final int TEXT_COLOR = 0xFFD4DAE1;
    private static final int MUTED_COLOR = 0xFF98A2AD;
    private static final int GOOD_COLOR = 0xFF83C991;
    private static final int WARN_COLOR = 0xFFE3B866;
    private static final int DANGER_COLOR = 0xFFE06C75;
    private static final int MINIMUM_WOUND_ROW_HEIGHT = 43;
    private static final int RESPIRATORY_DISTRESS_ROW_HEIGHT = 43;
    private static final int ORGANOPHOSPHATE_POISONING_ROW_HEIGHT = 43;
    private static final int TREATMENT_BUTTON_SIZE = 22;
    private static final int TREATMENT_BUTTON_STEP = 25;
    private static final int TREATMENT_BUTTON_TOP = 9;
    private static final int WOUND_SCROLLBAR_RESERVED_WIDTH = 7;
    private static final int WOUND_SCROLLBAR_TRACK_WIDTH = 3;
    private static final int WOUND_SCROLLBAR_HIT_WIDTH = 9;
    private static final int WOUND_SCROLL_WHEEL_STEP = 24;
    private static final int MINIMUM_WOUND_SCROLLBAR_THUMB_HEIGHT = 18;
    private static final ResourceLocation CPR_ICON = ResourceLocation.fromNamespaceAndPath(
            SuperficialTrauma.MOD_ID,
            "textures/gui/cpr.png"
    );

    private final boolean inspectingOtherPlayer;
    private final int inspectedEntityId;
    private final List<TreatmentItemButton> treatmentButtons = new ArrayList<>();
    private final List<MedicalActionButton> medicalActionButtons = new ArrayList<>();
    private long lastButtonRevision = Long.MIN_VALUE;
    private long lastInventorySignature = Long.MIN_VALUE;
    private boolean lastTreatmentActive;
    private boolean lastMedicationActive;
    private TreatmentPreparation preparation;
    private MedicationPreparation medicationPreparation;
    private PanelMode panelMode = PanelMode.TREATMENT;
    private boolean assistedBreathingHeld;
    private boolean cprHeld;
    private DefibrillationEnergy selectedDefibrillationEnergy = DefibrillationEnergy.J150;
    private boolean defibrillationHeld;
    private long defibrillationChargeStartGameTime = -1L;
    private long defibrillationReadyGameTime = -1L;
    private DefibrillationEnergySlider defibrillationEnergySlider;
    private DefibrillationChargeButton defibrillationChargeButton;
    private boolean adminDebugView;
    private int woundScrollOffset;
    private boolean draggingWoundScrollbar;
    private int woundScrollbarGrabOffset;

    public HealthScreen() {
        super(Component.translatable("screen.superficialtrauma.health.title"));
        inspectingOtherPlayer = false;
        inspectedEntityId = -1;
    }

    public HealthScreen(int inspectedEntityId, Component inspectedName) {
        super(Component.translatable("screen.superficialtrauma.health.target_title", inspectedName));
        inspectingOtherPlayer = true;
        this.inspectedEntityId = inspectedEntityId;
    }

    @Override
    protected void init() {
        super.init();
        rebuildTreatmentButtons();
    }

    @Override
    public void tick() {
        super.tick();
        ClientTimingQteState.tick();
        if (!hasSnapshot()) {
            return;
        }
        BodyState state = displayedState();
        if (assistedBreathingHeld && !canAssistBreathing(state)) {
            stopAssistedBreathing();
        }
        if (cprHeld && !canPerformCpr(state)) {
            stopCpr();
        }
        if (defibrillationHeld && !canContinueDefibrillation(state)) {
            cancelDefibrillation();
        }
        if (defibrillationEnergySlider != null) {
            defibrillationEnergySlider.active = !defibrillationHeld;
        }
        if (defibrillationChargeButton != null) {
            defibrillationChargeButton.active = defibrillationHeld
                    || canDefibrillate(state, selectedDefibrillationEnergy);
        }
        boolean treatmentActive = ClientTreatmentState.isActive();
        boolean medicationActive = ClientMedicationState.isActive();
        if (preparation != null) {
            if (treatmentActive) {
                clearPreparation();
            } else if (patientMovedSincePreparation()) {
                clearPreparation();
                if (minecraft != null) {
                    minecraft.setScreen(null);
                }
                return;
            } else if (!preparationStillValid(state)) {
                clearPreparation();
                rebuildTreatmentButtons();
            }
        }
        if (medicationPreparation != null) {
            if (medicationActive || treatmentActive) {
                clearMedicationPreparation();
            } else if (patientMovedSinceMedicationPreparation()) {
                clearMedicationPreparation();
                if (minecraft != null) {
                    minecraft.setScreen(null);
                }
                return;
            } else if (!medicationPreparationStillValid(state)) {
                clearMedicationPreparation();
                rebuildTreatmentButtons();
            }
        }
        long inventorySignature = inventorySignature();
        if (state.revision() != lastButtonRevision
                || inventorySignature != lastInventorySignature
                || treatmentActive != lastTreatmentActive
                || medicationActive != lastMedicationActive) {
            rebuildTreatmentButtons();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        Layout layout = layout();

        graphics.fill(
                layout.panelX,
                layout.panelY,
                layout.panelX + layout.panelWidth,
                layout.panelY + layout.panelHeight,
                BACKGROUND_COLOR
        );
        drawBorder(graphics, layout.panelX, layout.panelY, layout.panelWidth, layout.panelHeight, BORDER_COLOR);
        graphics.drawCenteredString(font, title, layout.panelX + layout.panelWidth / 2, layout.panelY + 10, TITLE_COLOR);

        drawColumn(graphics, layout.innerX, layout.innerY, layout.leftWidth, layout.innerHeight);
        drawColumn(graphics, layout.middleX, layout.innerY, layout.middleWidth, layout.innerHeight);
        drawColumn(graphics, layout.rightX, layout.innerY, layout.rightWidth, layout.innerHeight);

        if (!hasSnapshot()) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("screen.superficialtrauma.health.loading"),
                    layout.panelX + layout.panelWidth / 2,
                    layout.panelY + layout.panelHeight / 2,
                    TEXT_COLOR
            );
            super.render(graphics, mouseX, mouseY, partialTick);
            ClientMedicalInspectionNotice.render(graphics, width, height);
            return;
        }

        BodyState state = displayedState();
        int contentHeight = layout.innerHeight - 12;
        drawWholeBodyColumn(
                graphics,
                state,
                layout.innerX + 6,
                layout.innerY + 6,
                layout.leftWidth - 12,
                displayedHealth(),
                displayedMaximumHealth(),
                partialTick
        );
        drawWoundColumn(
                graphics,
                state,
                layout.middleX + 6,
                layout.innerY + 6,
                layout.middleWidth - 12,
                contentHeight,
                layout.rightWidth - 12
        );
        drawRightPanel(
                graphics,
                state,
                layout.rightX + 6,
                layout.innerY + 6,
                layout.rightWidth - 12,
                contentHeight,
                layout.middleWidth - 12
        );
        if (preparation != null || medicationPreparation != null) {
            drawPreparationShade(graphics, layout);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        for (TreatmentItemButton button : treatmentButtons) {
            if (button.isHovered()) {
                Component tooltip = button.tooltip();
                if (!tooltip.getString().isEmpty()) {
                    graphics.renderTooltip(font, tooltip, mouseX, mouseY);
                }
                break;
            }
        }
        for (MedicalActionButton button : medicalActionButtons) {
            if (button.isHovered()) {
                Component tooltip = button.tooltip();
                if (!tooltip.getString().isEmpty()) {
                    graphics.renderTooltip(font, tooltip, mouseX, mouseY);
                }
                break;
            }
        }
        if (defibrillationChargeButton != null && defibrillationChargeButton.isHovered()) {
            Component tooltip = defibrillationChargeButton.tooltip();
            if (!tooltip.getString().isEmpty()) {
                graphics.renderTooltip(font, tooltip, mouseX, mouseY);
            }
        }
        ClientMedicalInspectionNotice.render(graphics, width, height);
        TimingQteRenderer.render(graphics, font, width, height, partialTick);
    }

    private void drawWholeBodyColumn(
            GuiGraphics graphics,
            BodyState state,
            int x,
            int y,
            int availableWidth,
            float currentHealth,
            float maximumHealth,
            float partialTick
    ) {
        if (adminDebugView && hasAdminPermissions()) {
            drawDebugWholeBodyColumn(
                    graphics,
                    state,
                    x,
                    y,
                    availableWidth,
                    currentHealth,
                    maximumHealth
            );
            return;
        }
        drawStandardWholeBodyColumn(
                graphics,
                state,
                x,
                y,
                availableWidth,
                currentHealth,
                partialTick
        );
    }

    private void drawStandardWholeBodyColumn(
            GuiGraphics graphics,
            BodyState state,
            int x,
            int y,
            int availableWidth,
            float currentHealth,
            float partialTick
    ) {
        graphics.drawString(
                font,
                Component.translatable("screen.superficialtrauma.health.vital_signs"),
                x,
                y,
                TITLE_COLOR,
                false
        );

        int lineY = y + 16;
        graphics.drawString(
                font,
                Component.translatable("screen.superficialtrauma.health.electrocardiogram"),
                x,
                lineY,
                MUTED_COLOR,
                false
        );
        int electrocardiogramY = lineY + 11;
        drawElectrocardiogram(
                graphics,
                state,
                x,
                electrocardiogramY,
                availableWidth,
                36,
                partialTick
        );
        lineY = electrocardiogramY + 43;

        lineY = drawValue(
                graphics,
                x,
                lineY,
                availableWidth,
                "screen.superficialtrauma.health.life",
                oneDecimal(currentHealth),
                healthStatusColor(HealthStatus.from(currentHealth, !state.canAct()))
        );
        HealthStatus healthStatus = HealthStatus.from(currentHealth, !state.canAct());
        lineY = drawValue(
                graphics,
                x,
                lineY,
                availableWidth,
                "screen.superficialtrauma.health.status",
                Component.translatable(healthStatus.translationKey()).getString(),
                healthStatusColor(healthStatus)
        );

        PainSensation painSensation = PainSensation.from(state.pain());
        if (painSensation != PainSensation.NONE) {
            lineY = drawValue(
                    graphics,
                    x,
                    lineY,
                    availableWidth,
                    "screen.superficialtrauma.health.pain_sensation",
                    Component.translatable(painSensation.translationKey()).getString(),
                    painSensationColor(painSensation)
            );
        }

        lineY = drawValue(graphics, x, lineY, availableWidth,
                "screen.superficialtrauma.health.fatigue", Integer.toString(state.fatigueLevel()),
                state.fatigueLevel() > 0 ? WARN_COLOR : TEXT_COLOR);
        lineY += 7;
        if (!hasStethoscope()) {
            drawWrappedWithin(
                    graphics,
                    Component.translatable("screen.superficialtrauma.health.stethoscope_required"),
                    x,
                    lineY,
                    availableWidth,
                    MUTED_COLOR,
                    y + 224
            );
            return;
        }

        lineY = drawValue(
                graphics,
                x,
                lineY,
                availableWidth,
                "screen.superficialtrauma.health.heart_rate",
                Component.translatable(heartRateDisplayTranslationKey(state)).getString(),
                heartRateDisplayColor(state)
        );
        lineY = drawValue(
                graphics,
                x,
                lineY,
                availableWidth,
                "screen.superficialtrauma.health.infection_index",
                Integer.toString(Math.max(0, (int) Math.floor(state.infection()))),
                state.infection() >= 20.0F ? DANGER_COLOR : TEXT_COLOR
        );
        lineY = drawValue(
                graphics,
                x,
                lineY,
                availableWidth,
                "screen.superficialtrauma.health.drug_concentration",
                Integer.toString(Math.max(0, Math.round(state.bloodDrugConcentration()))),
                state.bloodDrugConcentration() >= BodyState.OVERDOSE_THRESHOLD
                        ? DANGER_COLOR
                        : TEXT_COLOR
        );

        String countdown = state.canAct()
                ? Component.translatable("screen.superficialtrauma.health.not_applicable").getString()
                : Component.translatable(
                        "screen.superficialtrauma.health.danger_countdown_value",
                        oneDecimal(state.totalDownedDangerRemainingTicks(currentGameTime()) / 20.0F)
                ).getString();
        drawValue(
                graphics,
                x,
                lineY,
                availableWidth,
                "screen.superficialtrauma.health.death_countdown",
                countdown,
                state.canAct() ? MUTED_COLOR : DANGER_COLOR
        );
    }

    private void drawDebugWholeBodyColumn(
            GuiGraphics graphics,
            BodyState state,
            int x,
            int y,
            int availableWidth,
            float currentHealth,
            float maximumHealth
    ) {
        graphics.drawString(
                font,
                Component.translatable("screen.superficialtrauma.health.whole_body"),
                x,
                y,
                TITLE_COLOR,
                false
        );
        int lineY = y + 16;
        lineY = drawValue(
                graphics,
                x,
                lineY,
                availableWidth,
                "screen.superficialtrauma.health.vanilla_health",
                oneDecimal(currentHealth) + "/" + oneDecimal(maximumHealth),
                GOOD_COLOR
        );
        lineY = drawValue(
                graphics,
                x,
                lineY,
                availableWidth,
                "screen.superficialtrauma.health.life_state",
                Component.translatable("life_state.superficialtrauma." + state.lifeState().serializedName()).getString(),
                TEXT_COLOR
        );
        float effectivePain = state.pain();
        lineY = drawValue(graphics, x, lineY, availableWidth,
                "screen.superficialtrauma.health.fatigue", Integer.toString(state.fatigueLevel()),
                state.fatigueLevel() > 0 ? WARN_COLOR : TEXT_COLOR);
        lineY = drawValue(
                graphics,
                x,
                lineY,
                availableWidth,
                "screen.superficialtrauma.health.pain",
                effectivePain > 20.0F ? "20+/20" : oneDecimal(effectivePain) + "/20",
                effectivePain >= 20.0F ? DANGER_COLOR : TEXT_COLOR
        );

        long gameTime = minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : 0L;
        long stressTicks = state.stressRemainingTicks(gameTime);
        String stressValue = stressTicks > 0L
                ? Component.translatable(
                        "screen.superficialtrauma.health.stress_active",
                        oneDecimal(stressTicks / 20.0F)
                ).getString()
                : Component.translatable("screen.superficialtrauma.health.stress_inactive").getString();
        lineY = drawValue(
                graphics,
                x,
                lineY,
                availableWidth,
                "screen.superficialtrauma.health.stress",
                stressValue,
                stressTicks > 0L ? WARN_COLOR : MUTED_COLOR
        );
        if (state.isShockWarningActive(gameTime)) {
            lineY = drawValue(
                    graphics,
                    x,
                    lineY,
                    availableWidth,
                    "screen.superficialtrauma.health.shock_warning",
                    Component.translatable("screen.superficialtrauma.health.shock_warning_active").getString(),
                    DANGER_COLOR
            );
        }
        if (state.collapseReason() != CollapseReason.NONE) {
            lineY = drawValue(
                    graphics,
                    x,
                    lineY,
                    availableWidth,
                    "screen.superficialtrauma.health.collapse_reason",
                    Component.translatable(state.collapseReason().translationKey()).getString(),
                    DANGER_COLOR
            );
        }
        lineY = drawValue(
                graphics,
                x,
                lineY,
                availableWidth,
                "screen.superficialtrauma.health.infection",
                oneDecimal(state.infection()),
                TEXT_COLOR
        );
        lineY = drawValue(
                graphics,
                x,
                lineY,
                availableWidth,
                "screen.superficialtrauma.health.drug",
                oneDecimal(state.bloodDrugConcentration()),
                TEXT_COLOR
        );
        lineY = drawValue(
                graphics,
                x,
                lineY,
                availableWidth,
                "screen.superficialtrauma.health.respiratory_distress_debug",
                respiratoryDistressDebugValue(state),
                state.respiratoryDistress() >= BodyState.RESPIRATORY_DISTRESS_COLLAPSE_THRESHOLD
                        ? DANGER_COLOR
                        : state.hasVisibleRespiratoryDistress() ? WARN_COLOR : TEXT_COLOR
        );
        lineY = drawValue(
                graphics,
                x,
                lineY,
                availableWidth,
                "screen.superficialtrauma.health.heart_rate_debug",
                heartRateDebugValue(state),
                heartRateDisplayColor(state)
        );
        if (state.canAct()) {
            lineY = drawValue(
                    graphics,
                    x,
                    lineY,
                    availableWidth,
                    "screen.superficialtrauma.health.oxygen",
                    oneDecimal(state.bloodOxygen()) + "/30",
                    TEXT_COLOR
            );
        } else {
            long dangerTicks = state.totalDownedDangerRemainingTicks(gameTime);
            lineY = drawValue(
                    graphics,
                    x,
                    lineY,
                    availableWidth,
                    "screen.superficialtrauma.health.danger_countdown",
                    Component.translatable(
                            "screen.superficialtrauma.health.danger_countdown_value",
                            oneDecimal(dangerTicks / 20.0F)
                    ).getString(),
                    DANGER_COLOR
            );
        }

        lineY += 5;
        graphics.drawString(
                font,
                Component.translatable("screen.superficialtrauma.health.stage0_debug"),
                x,
                lineY,
                MUTED_COLOR,
                false
        );
        lineY += 13;
        lineY = drawValue(
                graphics,
                x,
                lineY,
                availableWidth,
                "screen.superficialtrauma.health.last_damage",
                oneDecimal(state.lastFinalDamage()),
                WARN_COLOR
        );
        lineY = drawValue(
                graphics,
                x,
                lineY,
                availableWidth,
                "screen.superficialtrauma.health.damage_type",
                state.lastDamageType(),
                MUTED_COLOR
        );
        lineY = drawValue(
                graphics,
                x,
                lineY,
                availableWidth,
                "screen.superficialtrauma.health.damage_classification",
                Component.translatable(state.lastDamageKind().translationKey()).getString(),
                WARN_COLOR
        );
        if (!"none".equals(state.lastAmmoId())) {
            lineY = drawValue(
                    graphics,
                    x,
                    lineY,
                    availableWidth,
                    "screen.superficialtrauma.health.ammo",
                    compactIdentifier(state.lastAmmoId()),
                    MUTED_COLOR
            );
        }
        drawValue(
                graphics,
                x,
                lineY,
                availableWidth,
                "screen.superficialtrauma.health.data_version",
                Integer.toString(state.dataVersion()),
                MUTED_COLOR
        );
    }

    private void drawElectrocardiogram(
            GuiGraphics graphics,
            BodyState state,
            int x,
            int y,
            int width,
            int height,
            float partialTick
    ) {
        graphics.fill(x, y, x + width, y + height, 0xE00C1714);
        drawBorder(graphics, x, y, width, height, 0xFF385149);

        int plotLeft = x + 2;
        int plotRight = x + width - 2;
        int plotTop = y + 2;
        int plotBottom = y + height - 2;
        int baseline = (plotTop + plotBottom) / 2;
        for (int gridX = plotLeft + 7; gridX < plotRight; gridX += 8) {
            graphics.fill(gridX, plotTop, gridX + 1, plotBottom, 0x3035633F);
        }
        for (int gridY = plotTop + 7; gridY < plotBottom; gridY += 8) {
            graphics.fill(plotLeft, gridY, plotRight, gridY + 1, 0x3035633F);
        }

        ElectrocardiogramRhythm rhythm = electrocardiogramRhythm(state);
        float time = currentGameTime() + partialTick;
        int amplitude = Math.max(4, (plotBottom - plotTop) / 2 - 2);
        int previousY = baseline;
        for (int pixelX = plotLeft; pixelX < plotRight; pixelX++) {
            float sample = electrocardiogramSample(
                    rhythm,
                    pixelX - plotLeft,
                    time,
                    state.effectiveHeartRateLevel()
            );
            int sampleY = baseline - Math.round(sample * amplitude);
            sampleY = Math.max(plotTop, Math.min(plotBottom - 1, sampleY));
            int minimumY = Math.min(previousY, sampleY);
            int maximumY = Math.max(previousY, sampleY);
            graphics.fill(pixelX, minimumY, pixelX + 1, maximumY + 1, 0xFF72E292);
            previousY = sampleY;
        }
    }

    private ElectrocardiogramRhythm electrocardiogramRhythm(BodyState state) {
        return switch (state.lifeState()) {
            case CARDIAC_ARREST, BRAIN_DEAD -> ElectrocardiogramRhythm.FLATLINE;
            case VENTRICULAR_FIBRILLATION -> ElectrocardiogramRhythm.FIBRILLATION;
            default -> state.effectiveHeartRateLevel() > 0
                    ? ElectrocardiogramRhythm.TACHYCARDIA
                    : state.effectiveHeartRateLevel() < 0
                            ? ElectrocardiogramRhythm.BRADYCARDIA
                            : ElectrocardiogramRhythm.NORMAL;
        };
    }

    private static float electrocardiogramSample(
            ElectrocardiogramRhythm rhythm,
            int horizontalPosition,
            float time,
            int heartRateLevel
    ) {
        if (rhythm == ElectrocardiogramRhythm.FLATLINE) {
            return 0.0F;
        }
        if (rhythm == ElectrocardiogramRhythm.FIBRILLATION) {
            double shifted = horizontalPosition + Math.floor(time * 1.35D);
            return (float) (
                    Math.sin(shifted * 0.51D) * 0.42D
                            + Math.sin(shifted * 1.19D + 0.8D) * 0.27D
                            + Math.sin(shifted * 2.03D + 1.7D) * 0.18D
            );
        }

        float period = switch (rhythm) {
            case TACHYCARDIA -> Math.max(12.0F, 19.5F - heartRateLevel * 2.0F);
            case BRADYCARDIA -> 28.0F + Math.abs(heartRateLevel) * 8.0F;
            default -> 28.0F;
        };
        float scrollingOffset = (float) Math.floor(time * 0.55F);
        float phase = positiveFraction((horizontalPosition + scrollingOffset) / period);
        float waveform = heartbeatSample(phase);
        return rhythm == ElectrocardiogramRhythm.TACHYCARDIA ? waveform * 0.9F : waveform;
    }

    private static float heartbeatSample(float phase) {
        if (phase < 0.12F) {
            return 0.0F;
        }
        if (phase < 0.20F) {
            return (float) Math.sin((phase - 0.12F) / 0.08F * Math.PI) * 0.16F;
        }
        if (phase < 0.30F) {
            return 0.0F;
        }
        if (phase < 0.34F) {
            return -((phase - 0.30F) / 0.04F) * 0.22F;
        }
        if (phase < 0.37F) {
            return -0.22F + ((phase - 0.34F) / 0.03F) * 1.22F;
        }
        if (phase < 0.41F) {
            return 1.0F - ((phase - 0.37F) / 0.04F) * 1.46F;
        }
        if (phase < 0.47F) {
            return -0.46F + ((phase - 0.41F) / 0.06F) * 0.46F;
        }
        if (phase < 0.62F) {
            return 0.0F;
        }
        if (phase < 0.78F) {
            return (float) Math.sin((phase - 0.62F) / 0.16F * Math.PI) * 0.28F;
        }
        return 0.0F;
    }

    private static float positiveFraction(float value) {
        return value - (float) Math.floor(value);
    }

    private static int healthStatusColor(HealthStatus status) {
        return switch (status) {
            case OK, VERY_MINOR_DAMAGE -> GOOD_COLOR;
            case MINOR_DAMAGE, MODERATE_DAMAGE -> WARN_COLOR;
            case SEVERE_DAMAGE, TERMINAL_DAMAGE, DOWNED -> DANGER_COLOR;
        };
    }

    private static int painSensationColor(PainSensation sensation) {
        return switch (sensation) {
            case NONE, MINOR_PAIN -> TEXT_COLOR;
            case PAIN -> WARN_COLOR;
            case SEVERE_PAIN, EXTREME_PAIN -> DANGER_COLOR;
        };
    }

    private static String heartRateTranslationKey(int level) {
        int clampedLevel = Math.max(
                BodyState.MIN_HEART_RATE_LEVEL,
                Math.min(BodyState.MAX_HEART_RATE_LEVEL, level)
        );
        return "heart_rate.superficialtrauma." + switch (clampedLevel) {
            case -3 -> "bradycardia_3";
            case -2 -> "bradycardia_2";
            case -1 -> "bradycardia_1";
            case 1 -> "tachycardia_1";
            case 2 -> "tachycardia_2";
            case 3 -> "tachycardia_3";
            default -> "normal";
        };
    }

    private static String heartRateDisplayTranslationKey(BodyState state) {
        return switch (state.lifeState()) {
            case CARDIAC_ARREST -> "life_state.superficialtrauma.cardiac_arrest";
            case VENTRICULAR_FIBRILLATION -> "life_state.superficialtrauma.ventricular_fibrillation";
            case BRAIN_DEAD -> "life_state.superficialtrauma.brain_dead";
            default -> heartRateTranslationKey(state.effectiveHeartRateLevel());
        };
    }

    private static int heartRateDisplayColor(BodyState state) {
        return switch (state.lifeState()) {
            case CARDIAC_ARREST, VENTRICULAR_FIBRILLATION, BRAIN_DEAD -> DANGER_COLOR;
            default -> heartRateColor(state.effectiveHeartRateLevel());
        };
    }

    private static String heartRateDebugValue(BodyState state) {
        String internalValues = state.heartRateLevel()
                + " / " + signedInteger(state.medicationHeartRateShift())
                + " / " + signedInteger(state.respiratoryHeartRateShift())
                + " / " + state.effectiveHeartRateLevel();
        return switch (state.lifeState()) {
            case CARDIAC_ARREST, VENTRICULAR_FIBRILLATION, BRAIN_DEAD ->
                    Component.translatable(heartRateDisplayTranslationKey(state)).getString()
                            + " | " + internalValues;
            default -> internalValues;
        };
    }

    private static String respiratoryDistressDebugValue(BodyState state) {
        return oneDecimal(state.baseRespiratoryDistress())
                + "/" + oneDecimal(state.respiratoryDistressModifier())
                + "/" + oneDecimal(state.respiratoryDistress());
    }

    private static int heartRateColor(int level) {
        return switch (Math.abs(level)) {
            case 3 -> DANGER_COLOR;
            case 2 -> WARN_COLOR;
            case 1 -> TEXT_COLOR;
            default -> GOOD_COLOR;
        };
    }

    private static String signedInteger(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    private void drawWoundColumn(
            GuiGraphics graphics,
            BodyState state,
            int x,
            int y,
            int availableWidth,
            int availableHeight,
            int treatmentAvailableWidth
    ) {
        graphics.drawString(
                font,
                Component.translatable("screen.superficialtrauma.health.wounds"),
                x,
                y,
                TITLE_COLOR,
                false
        );
        WoundListLayout woundLayout = woundListLayout(
                state,
                availableHeight,
                availableWidth,
                treatmentAvailableWidth
        );
        clampWoundScroll(woundLayout);
        int viewportTop = y + 16;
        int viewportBottom = viewportTop + woundLayout.viewportHeight();
        int contentWidth = woundLayout.contentWidth();
        int cardY = viewportTop - woundScrollOffset;

        graphics.enableScissor(x, viewportTop, x + contentWidth, viewportBottom);
        if (woundLayout.rows().isEmpty()
                && !showsOrganophosphatePoisoning(state)
                && !state.hasVisibleRespiratoryDistress()) {
            graphics.drawString(
                    font,
                    Component.translatable("screen.superficialtrauma.health.no_wounds"),
                    x,
                    cardY + 2,
                    GOOD_COLOR,
                    false
            );
            cardY += 18;
        }

        for (WoundRow row : woundLayout.rows()) {
            if (cardY < viewportBottom && cardY + row.height() > viewportTop) {
                WoundInstance wound = row.wound();
                int cardHeight = row.height() - 3;
                int cardColor = wound.severity() >= 3
                        ? 0xAA4C2529
                        : wound.severity() == 2 ? 0xAA4A3C24 : 0xAA263D31;
                graphics.fill(x, cardY, x + contentWidth, cardY + cardHeight, cardColor);
                drawBorder(
                        graphics,
                        x,
                        cardY,
                        contentWidth,
                        cardHeight,
                        wound.severity() >= 3 ? DANGER_COLOR : BORDER_COLOR
                );
                Component woundName = Component.translatable(wound.displayTranslationKey());
                Component triage = Component.translatable(wound.triageTranslationKey());
                int triageWidth = font.width(triage);
                int woundNameWidth = Math.max(20, contentWidth - triageWidth - 16);
                graphics.drawString(
                        font,
                        font.plainSubstrByWidth(woundName.getString(), woundNameWidth),
                        x + 4,
                        cardY + 4,
                        TITLE_COLOR,
                        false
                );
                graphics.drawString(
                        font,
                        triage,
                        x + contentWidth - 4 - triageWidth,
                        cardY + 4,
                        wound.severity() >= 3 ? DANGER_COLOR : wound.severity() == 2 ? WARN_COLOR : GOOD_COLOR,
                        false
                );
                Component woundValues = Component.translatable(
                        "screen.superficialtrauma.health.wound_values",
                        wound.severity(),
                        oneDecimal(wound.accumulatedDamage()),
                        oneDecimal(wound.healingProgress())
                );
                graphics.drawString(
                        font,
                        font.plainSubstrByWidth(woundValues.getString(), Math.max(20, contentWidth - 8)),
                        x + 4,
                        cardY + 17,
                        TEXT_COLOR,
                        false
                );
                String tagSummary = woundTagSummary(wound, state);
                if (!tagSummary.isEmpty()) {
                    int tagY = cardY + 28;
                    for (FormattedCharSequence line : woundTagLines(tagSummary, contentWidth)) {
                        graphics.drawString(font, line, x + 4, tagY, MUTED_COLOR, false);
                        tagY += 11;
                    }
                }
            }
            cardY += row.height();
        }

        if (showsOrganophosphatePoisoning(state)) {
            if (cardY < viewportBottom && cardY + ORGANOPHOSPHATE_POISONING_ROW_HEIGHT > viewportTop) {
                int cardHeight = ORGANOPHOSPHATE_POISONING_ROW_HEIGHT - 3;
                graphics.fill(x, cardY, x + contentWidth, cardY + cardHeight, 0xAA5A1E24);
                drawBorder(graphics, x, cardY, contentWidth, cardHeight, DANGER_COLOR);
                graphics.drawString(
                        font,
                        Component.translatable("screen.superficialtrauma.health.organophosphate_poisoning"),
                        x + 4,
                        cardY + 4,
                        DANGER_COLOR,
                        false
                );
                graphics.drawString(
                        font,
                        Component.translatable(
                                "screen.superficialtrauma.health.organophosphate_poisoning_stage",
                                state.organophosphatePoisoningStage()
                        ),
                        x + 4,
                        cardY + 17,
                        TEXT_COLOR,
                        false
                );
            }
            cardY += ORGANOPHOSPHATE_POISONING_ROW_HEIGHT;
        }

        if (state.hasVisibleRespiratoryDistress()) {
            if (cardY < viewportBottom && cardY + RESPIRATORY_DISTRESS_ROW_HEIGHT > viewportTop) {
                int cardHeight = RESPIRATORY_DISTRESS_ROW_HEIGHT - 3;
                graphics.fill(x, cardY, x + contentWidth, cardY + cardHeight, 0xAA5A491E);
                drawBorder(graphics, x, cardY, contentWidth, cardHeight, WARN_COLOR);
                graphics.drawString(
                        font,
                        Component.translatable("screen.superficialtrauma.health.respiratory_distress"),
                        x + 4,
                        cardY + 4,
                        WARN_COLOR,
                        false
                );
            }
            cardY += RESPIRATORY_DISTRESS_ROW_HEIGHT;
        }

        int pendingY = cardY + 2;
        List<DamageWindow> pendingWindows = new ArrayList<>(state.damageWindows().values());
        pendingWindows.sort(Comparator.comparingLong(DamageWindow::startedGameTime));
        for (DamageWindow window : pendingWindows) {
            pendingY = drawPendingWindow(graphics, window, x, pendingY, contentWidth);
        }
        graphics.disableScissor();

        drawWoundScrollbar(
                graphics,
                x + availableWidth - WOUND_SCROLLBAR_TRACK_WIDTH,
                viewportTop,
                woundLayout
        );
    }

    private int drawPendingWindow(
            GuiGraphics graphics,
            DamageWindow window,
            int x,
            int y,
            int availableWidth
    ) {
        Component pending = Component.translatable(
                "screen.superficialtrauma.health.pending_wound",
                Component.translatable(window.type().translationKey()),
                oneDecimal(window.accumulatedDamage())
        );
        graphics.drawString(
                font,
                font.plainSubstrByWidth(pending.getString(), availableWidth),
                x,
                y,
                MUTED_COLOR,
                false
        );
        return y + 13;
    }

    private void drawWoundScrollbar(
            GuiGraphics graphics,
            int trackX,
            int trackY,
            WoundListLayout woundLayout
    ) {
        if (!woundLayout.scrollable() || woundLayout.viewportHeight() <= 0) {
            return;
        }
        int trackHeight = woundLayout.viewportHeight();
        int thumbHeight = woundScrollbarThumbHeight(woundLayout);
        int thumbY = trackY + woundScrollbarThumbOffset(woundLayout, thumbHeight);
        graphics.fill(
                trackX,
                trackY,
                trackX + WOUND_SCROLLBAR_TRACK_WIDTH,
                trackY + trackHeight,
                0xFF161C22
        );
        graphics.fill(
                trackX,
                thumbY,
                trackX + WOUND_SCROLLBAR_TRACK_WIDTH,
                thumbY + thumbHeight,
                draggingWoundScrollbar ? TITLE_COLOR : BORDER_COLOR
        );
    }

    private static int woundScrollbarThumbHeight(WoundListLayout woundLayout) {
        if (!woundLayout.scrollable() || woundLayout.viewportHeight() <= 0) {
            return 0;
        }
        int proportionalHeight = Math.round(
                woundLayout.viewportHeight()
                        * (woundLayout.viewportHeight() / (float) woundLayout.totalContentHeight())
        );
        return Math.min(
                woundLayout.viewportHeight(),
                Math.max(MINIMUM_WOUND_SCROLLBAR_THUMB_HEIGHT, proportionalHeight)
        );
    }

    private int woundScrollbarThumbOffset(WoundListLayout woundLayout, int thumbHeight) {
        if (!woundLayout.scrollable()) {
            return 0;
        }
        int availableTravel = Math.max(0, woundLayout.viewportHeight() - thumbHeight);
        return Math.round(availableTravel * (woundScrollOffset / (float) woundLayout.maximumScroll()));
    }

    private String woundTagSummary(WoundInstance wound, BodyState state) {
        List<String> labels = new ArrayList<>();
        boolean hasBleedingTag = wound.woundTags().stream().anyMatch(tag -> tag.bleedingLevel() > 0);
        if (wound.covering().isApplied()) {
            labels.add(Component.translatable(wound.covering().translationKey()).getString());
        }
        if (wound.woundPackingApplied()) {
            labels.add(Component.translatable("wound_packing.superficialtrauma.applied").getString());
        }
        if (wound.tourniquetApplied()) {
            labels.add(Component.translatable("tourniquet.superficialtrauma.applied").getString());
        }
        if (wound.icePackApplied()) {
            labels.add(Component.translatable("wound_treatment.superficialtrauma.ice_pack_applied").getString());
        }

        int effectiveBleedingLevel = wound.bleedingLevel(state.movementBleedingActive());
        if (effectiveBleedingLevel > 0) {
            labels.add(Component.translatable(
                    "wound_tag.superficialtrauma.bleeding_" + effectiveBleedingLevel
            ).getString());
        } else if (hasBleedingTag && (wound.covering().isApplied()
                || wound.woundPackingApplied()
                || wound.tourniquetApplied())) {
            labels.add(Component.translatable("screen.superficialtrauma.health.bleeding_controlled").getString());
        }

        for (WoundTag tag : wound.woundTags()) {
            if (tag.bleedingLevel() > 0) {
                continue;
            }
            if (tag == WoundTag.INFECTED_1 && wound.covering().isApplied()) {
                if (hasStethoscope()) {
                    labels.add(Component.translatable(
                            "screen.superficialtrauma.health.suspected_infection"
                    ).getString());
                }
                continue;
            }
            if (tag.disorientationLevel() > 0) {
                continue;
            }
            labels.add(Component.translatable("wound_tag.superficialtrauma." + tag.serializedName()).getString());
        }
        return String.join(" · ", labels);
    }

    private void drawRightPanel(
            GuiGraphics graphics,
            BodyState state,
            int x,
            int y,
            int availableWidth,
            int availableHeight,
            int middleAvailableWidth
    ) {
        switch (panelMode) {
            case TREATMENT -> drawTreatmentColumn(
                    graphics,
                    state,
                    x,
                    y,
                    availableWidth,
                    availableHeight,
                    middleAvailableWidth
            );
            case MEDICATION -> drawMedicationColumn(graphics, state, x, y + 22, availableWidth, availableHeight);
            case EMERGENCY -> {
                // Emergency buttons carry their own progress state; no persistent prose is drawn below them.
            }
        }
    }

    private void drawMedicationColumn(
            GuiGraphics graphics,
            BodyState state,
            int x,
            int y,
            int availableWidth,
            int availableHeight
    ) {
        Component text = null;
        int color = MUTED_COLOR;
        if (ClientMedicationState.isActive()) {
            text = Component.translatable(
                    "screen.superficialtrauma.health.medication_action_active",
                    Component.translatable(ClientMedicationState.activeMedication().type().translationKey()),
                    oneDecimal(ClientMedicationState.remainingSeconds())
            );
            color = WARN_COLOR;
        } else if (state.hasActiveInfusion()) {
            text = Component.translatable(
                        "screen.superficialtrauma.health.infusion_active",
                        Component.translatable(state.infusionType().translationKey()),
                        oneDecimal(state.infusionRemainingTicks(currentGameTime()) / 20.0F)
                );
            color = GOOD_COLOR;
        }
        if (text != null) {
            int buttonsPerRow = treatmentButtonsPerRow(availableWidth);
            int buttonRows = (10 + buttonsPerRow - 1) / buttonsPerRow;
            if (!state.canAct()) {
                buttonRows += (2 + buttonsPerRow - 1) / buttonsPerRow;
            }
            int textY = y + buttonRows * TREATMENT_BUTTON_STEP + 6;
            drawWrappedWithin(graphics, text, x + 4, textY, availableWidth - 8,
                    color, y + availableHeight - 18);
        }
    }

    private void drawTreatmentColumn(
            GuiGraphics graphics,
            BodyState state,
            int x,
            int y,
            int availableWidth,
            int availableHeight,
            int middleAvailableWidth
    ) {
        if (ClientTreatmentState.isActive()) {
            ClientTreatmentState.ActiveTreatment active = ClientTreatmentState.activeTreatment();
            drawWrappedWithin(
                    graphics,
                    Component.translatable(
                            "screen.superficialtrauma.health.treatment_progress",
                            Component.translatable(active.action().translationKey(active.procedure())),
                            oneDecimal(ClientTreatmentState.remainingSeconds())
                    ),
                    x,
                    y + availableHeight - 26,
                    availableWidth,
                    WARN_COLOR,
                    y + availableHeight
            );
        }
    }

    private void drawPanelPlaceholder(
            GuiGraphics graphics,
            String descriptionKey,
            int x,
            int y,
            int availableWidth,
            int availableHeight
    ) {
        drawWrappedWithin(
                graphics,
                Component.translatable(descriptionKey),
                x + 4,
                y,
                availableWidth - 8,
                MUTED_COLOR,
                y + availableHeight - 18
        );
        graphics.drawCenteredString(
                font,
                Component.translatable("screen.superficialtrauma.health.panel.no_actions"),
                x + availableWidth / 2,
                y + Math.min(58, availableHeight / 2),
                MUTED_COLOR
        );
    }

    private void rebuildTreatmentButtons() {
        clearWidgets();
        treatmentButtons.clear();
        medicalActionButtons.clear();
        defibrillationEnergySlider = null;
        defibrillationChargeButton = null;
        Layout layout = layout();
        addPanelModeButtons(layout);
        addAdminDebugButton();
        if (!hasSnapshot() || minecraft == null || minecraft.player == null) {
            return;
        }

        BodyState state = displayedState();
        boolean anyTreatmentActive = ClientTreatmentState.isActive() || ClientMedicationState.isActive();
        if (panelMode == PanelMode.TREATMENT) {
            int availableHeight = layout.innerHeight - 12;
            int treatmentAvailableWidth = layout.rightWidth - 12;
            WoundListLayout woundLayout = woundListLayout(
                    state,
                    availableHeight,
                    layout.middleWidth - 12,
                    treatmentAvailableWidth
            );
            clampWoundScroll(woundLayout);
            int patientEntityId = displayedEntityId();
            int viewportTop = layout.innerY + 6 + 16;
            int viewportBottom = viewportTop + woundLayout.viewportHeight();
            int clipLeft = layout.rightX + 6;
            int clipRight = layout.rightX + layout.rightWidth - 6;
            int rowY = viewportTop - woundScrollOffset;
            int buttonsPerRow = treatmentButtonsPerRow(treatmentAvailableWidth);

            for (WoundRow row : woundLayout.rows()) {
                WoundInstance wound = row.wound();
                List<TreatmentType> visibleTypes = visibleTreatmentTypes(wound);
                if (!visibleTypes.isEmpty()) {
                    int treatmentIndex = 0;
                    for (TreatmentType type : visibleTypes) {
                        int buttonX = layout.rightX + 10
                                + treatmentIndex % buttonsPerRow * TREATMENT_BUTTON_STEP;
                        int buttonY = rowY + TREATMENT_BUTTON_TOP
                                + treatmentIndex / buttonsPerRow * TREATMENT_BUTTON_STEP;
                        if (buttonY < viewportBottom && buttonY + TREATMENT_BUTTON_SIZE > viewportTop) {
                            addTreatmentButton(
                                    buttonX,
                                    buttonY,
                                    patientEntityId,
                                    wound,
                                    type,
                                    anyTreatmentActive,
                                    clipLeft,
                                    viewportTop,
                                    clipRight,
                                    viewportBottom
                            );
                        }
                        treatmentIndex++;
                    }
                }
                rowY += row.height();
            }
        } else if (panelMode == PanelMode.MEDICATION) {
            addMedicationButtons(layout, state, anyTreatmentActive);
        } else if (panelMode == PanelMode.EMERGENCY) {
            addEmergencyButtons(layout, state);
        }

        lastButtonRevision = state.revision();
        lastInventorySignature = inventorySignature();
        lastTreatmentActive = ClientTreatmentState.isActive();
        lastMedicationActive = ClientMedicationState.isActive();
    }

    private void addPanelModeButtons(Layout layout) {
        int gap = 2;
        int totalWidth = Math.max(3, layout.rightWidth - 6);
        int tabWidth = Math.max(1, (totalWidth - gap * 2) / 3);
        int startX = layout.rightX + 3;
        int tabY = layout.innerY + 1;
        int index = 0;
        for (PanelMode mode : PanelMode.values()) {
            int tabX = startX + index * (tabWidth + gap);
            addRenderableWidget(new HealthPanelTabButton(
                    tabX,
                    tabY,
                    tabWidth,
                    Component.translatable(mode.translationKey()),
                    panelMode == mode,
                    () -> switchPanelMode(mode)
            ));
            index++;
        }
    }

    private void addAdminDebugButton() {
        if (!hasAdminPermissions()) {
            adminDebugView = false;
            return;
        }
        Component label = Component.literal("DEV");
        int buttonWidth = font.width(label) + 8;
        addRenderableWidget(Button.builder(label, ignored -> {
                    adminDebugView = !adminDebugView;
                    rebuildTreatmentButtons();
                })
                .bounds(4, Math.max(4, height - 18), buttonWidth, 14)
                .build());
    }

    private void switchPanelMode(PanelMode newMode) {
        if (panelMode == newMode) {
            return;
        }
        stopAssistedBreathing();
        stopCpr();
        cancelDefibrillation();
        clearPreparation();
        clearMedicationPreparation();
        panelMode = newMode;
        rebuildTreatmentButtons();
    }

    private void addMedicationButtons(Layout layout, BodyState state, boolean anyMedicalActionActive) {
        int startX = layout.rightX + 10;
        int startY = layout.innerY + 6 + 22;
        int availableWidth = layout.rightWidth - 12;
        int buttonsPerRow = treatmentButtonsPerRow(availableWidth);
        int rowOffset = 0;

        if (!state.canAct()) {
            addInfusionButton(
                    startX,
                    startY,
                    state,
                    InfusionType.BLOOD_BAG,
                    ModItems.BLOOD_BAG.get()
            );
            addInfusionButton(
                    startX + (1 % buttonsPerRow) * TREATMENT_BUTTON_STEP,
                    startY + (1 / buttonsPerRow) * TREATMENT_BUTTON_STEP,
                    state,
                    InfusionType.SALINE,
                    ModItems.SALINE_SOLUTION.get()
            );
            rowOffset = (2 + buttonsPerRow - 1) / buttonsPerRow;
        }

        int drugY = startY + rowOffset * TREATMENT_BUTTON_STEP;
        addMedicationItemButton(
                startX,
                drugY,
                state,
                ModItems.SYRINGE.get(),
                MedicationButtonType.SYRINGE,
                anyMedicalActionActive
        );
        addMedicationItemButton(
                startX + (1 % buttonsPerRow) * TREATMENT_BUTTON_STEP,
                drugY + (1 / buttonsPerRow) * TREATMENT_BUTTON_STEP,
                state,
                ModItems.PARACETAMOL.get(),
                MedicationButtonType.PARACETAMOL,
                anyMedicalActionActive
        );
        addMedicationItemButton(
                startX + (2 % buttonsPerRow) * TREATMENT_BUTTON_STEP,
                drugY + (2 / buttonsPerRow) * TREATMENT_BUTTON_STEP,
                state,
                ModItems.MORPHINE_VIAL.get(),
                MedicationButtonType.MORPHINE,
                anyMedicalActionActive
        );
        addMedicationItemButton(
                startX + (3 % buttonsPerRow) * TREATMENT_BUTTON_STEP,
                drugY + (3 / buttonsPerRow) * TREATMENT_BUTTON_STEP,
                state,
                ModItems.REMIFENTANIL_INJECTION.get(),
                MedicationButtonType.REMIFENTANIL,
                anyMedicalActionActive
        );
        addMedicationItemButton(
                startX + (4 % buttonsPerRow) * TREATMENT_BUTTON_STEP,
                drugY + (4 / buttonsPerRow) * TREATMENT_BUTTON_STEP,
                state,
                ModItems.NALOXONE.get(),
                MedicationButtonType.NALOXONE,
                anyMedicalActionActive
        );
        addMedicationItemButton(
                startX + (5 % buttonsPerRow) * TREATMENT_BUTTON_STEP,
                drugY + (5 / buttonsPerRow) * TREATMENT_BUTTON_STEP,
                state,
                ModItems.EPINEPHRINE_INJECTION.get(),
                MedicationButtonType.EPINEPHRINE,
                anyMedicalActionActive
        );
        addMedicationItemButton(
                startX + (6 % buttonsPerRow) * TREATMENT_BUTTON_STEP,
                drugY + (6 / buttonsPerRow) * TREATMENT_BUTTON_STEP,
                state,
                ModItems.METOPROLOL.get(),
                MedicationButtonType.METOPROLOL,
                anyMedicalActionActive
        );
        addMedicationItemButton(
                startX + (7 % buttonsPerRow) * TREATMENT_BUTTON_STEP,
                drugY + (7 / buttonsPerRow) * TREATMENT_BUTTON_STEP,
                state,
                ModItems.ATROPINE_SULFATE_INJECTION.get(),
                MedicationButtonType.ATROPINE_SULFATE,
                anyMedicalActionActive
        );
        addMedicationItemButton(
                startX + (8 % buttonsPerRow) * TREATMENT_BUTTON_STEP,
                drugY + (8 / buttonsPerRow) * TREATMENT_BUTTON_STEP,
                state,
                ModItems.PRALIDOXIME_CHLORIDE_INJECTION.get(),
                MedicationButtonType.PRALIDOXIME_CHLORIDE,
                anyMedicalActionActive
        );
        addMedicationItemButton(
                startX + (9 % buttonsPerRow) * TREATMENT_BUTTON_STEP,
                drugY + (9 / buttonsPerRow) * TREATMENT_BUTTON_STEP,
                state,
                ModItems.CEFTRIAXONE.get(),
                MedicationButtonType.CEFTRIAXONE,
                anyMedicalActionActive
        );
        addMedicationItemButton(
                startX + (10 % buttonsPerRow) * TREATMENT_BUTTON_STEP,
                drugY + (10 / buttonsPerRow) * TREATMENT_BUTTON_STEP,
                state,
                ModItems.AMOXICILLIN.get(),
                MedicationButtonType.AMOXICILLIN,
                anyMedicalActionActive
        );
    }

    private void addMedicationItemButton(
            int x,
            int y,
            BodyState state,
            Item item,
            MedicationButtonType buttonType,
            boolean anyMedicalActionActive
    ) {
        boolean active = false;
        boolean missingRequiredItem = false;
        Runnable onPress = () -> {
        };

        if (anyMedicalActionActive) {
            // Busy buttons remain disabled without implying that their item is missing.
        } else if (medicationPreparation != null) {
            if (buttonType == MedicationButtonType.MORPHINE
                    && medicationPreparation.patientEntityId() == displayedEntityId()) {
                active = medicationPreparationStillValid(state)
                        && countItem(ModItems.MORPHINE_VIAL.get()) > 0;
                missingRequiredItem = countItem(item) <= 0;
                onPress = () -> submitPreparedMedication(MedicationType.MORPHINE);
            } else if (buttonType == MedicationButtonType.REMIFENTANIL
                    && medicationPreparation.patientEntityId() == displayedEntityId()) {
                active = medicationPreparationStillValid(state)
                        && countItem(ModItems.REMIFENTANIL_INJECTION.get()) > 0;
                missingRequiredItem = countItem(item) <= 0;
                onPress = () -> submitPreparedMedication(MedicationType.REMIFENTANIL);
            } else if (buttonType == MedicationButtonType.NALOXONE
                    && medicationPreparation.patientEntityId() == displayedEntityId()) {
                active = medicationPreparationStillValid(state)
                        && state.hasActiveOpioidDose()
                        && countItem(ModItems.NALOXONE.get()) > 0;
                missingRequiredItem = state.hasActiveOpioidDose() && countItem(item) <= 0;
                onPress = () -> submitPreparedMedication(MedicationType.NALOXONE);
            } else if (buttonType == MedicationButtonType.EPINEPHRINE
                    && medicationPreparation.patientEntityId() == displayedEntityId()) {
                active = medicationPreparationStillValid(state)
                        && countItem(ModItems.EPINEPHRINE_INJECTION.get()) > 0;
                missingRequiredItem = countItem(item) <= 0;
                onPress = () -> submitPreparedMedication(MedicationType.EPINEPHRINE);
            } else if (buttonType == MedicationButtonType.ATROPINE_SULFATE
                    && medicationPreparation.patientEntityId() == displayedEntityId()) {
                active = medicationPreparationStillValid(state)
                        && countItem(ModItems.ATROPINE_SULFATE_INJECTION.get()) > 0;
                missingRequiredItem = countItem(item) <= 0;
                onPress = () -> submitPreparedMedication(MedicationType.ATROPINE_SULFATE);
            } else if (buttonType == MedicationButtonType.PRALIDOXIME_CHLORIDE
                    && medicationPreparation.patientEntityId() == displayedEntityId()) {
                active = medicationPreparationStillValid(state)
                        && countItem(ModItems.PRALIDOXIME_CHLORIDE_INJECTION.get()) > 0;
                missingRequiredItem = countItem(item) <= 0;
                onPress = () -> submitPreparedMedication(MedicationType.PRALIDOXIME_CHLORIDE);
            } else if (buttonType == MedicationButtonType.CEFTRIAXONE
                    && medicationPreparation.patientEntityId() == displayedEntityId()) {
                active = medicationPreparationStillValid(state)
                        && countItem(ModItems.CEFTRIAXONE.get()) > 0;
                missingRequiredItem = countItem(item) <= 0;
                onPress = () -> submitPreparedMedication(MedicationType.CEFTRIAXONE);
            }
        } else {
            switch (buttonType) {
                case SYRINGE -> {
                    active = canPrepareInjection(state);
                    missingRequiredItem = actorCanAct()
                            && state.lifeState() != BodyLifeState.BRAIN_DEAD
                            && countItem(item) <= 0
                            && (countItem(ModItems.MORPHINE_VIAL.get()) > 0
                            || countItem(ModItems.REMIFENTANIL_INJECTION.get()) > 0
                            || (state.hasActiveOpioidDose() && countItem(ModItems.NALOXONE.get()) > 0)
                            || countItem(ModItems.EPINEPHRINE_INJECTION.get()) > 0
                            || countItem(ModItems.ATROPINE_SULFATE_INJECTION.get()) > 0
                            || countItem(ModItems.PRALIDOXIME_CHLORIDE_INJECTION.get()) > 0
                            || countItem(ModItems.CEFTRIAXONE.get()) > 0);
                    onPress = this::beginMedicationPreparation;
                }
                case PARACETAMOL -> {
                    active = actorCanAct()
                            && !inspectingOtherPlayer
                            && state.canAct()
                            && countItem(ModItems.PARACETAMOL.get()) > 0;
                    missingRequiredItem = actorCanAct() && state.canAct() && countItem(item) <= 0;
                    onPress = () -> ModNetworking.requestMedication(
                            displayedEntityId(),
                            MedicationType.PARACETAMOL
                    );
                }
                case MORPHINE, REMIFENTANIL, NALOXONE, EPINEPHRINE, ATROPINE_SULFATE,
                        PRALIDOXIME_CHLORIDE, CEFTRIAXONE -> {
                }
                case METOPROLOL -> {
                    active = actorCanAct()
                            && !inspectingOtherPlayer
                            && state.canAct()
                            && countItem(ModItems.METOPROLOL.get()) > 0;
                    missingRequiredItem = actorCanAct()
                            && state.canAct()
                            && countItem(item) <= 0;
                    onPress = () -> ModNetworking.requestMedication(
                            displayedEntityId(),
                            MedicationType.METOPROLOL
                    );
                }
                case AMOXICILLIN -> {
                    active = actorCanAct()
                            && !inspectingOtherPlayer
                            && state.canAct()
                            && countItem(ModItems.AMOXICILLIN.get()) > 0;
                    missingRequiredItem = actorCanAct() && state.canAct() && countItem(item) <= 0;
                    onPress = () -> ModNetworking.requestMedication(
                            displayedEntityId(),
                            MedicationType.AMOXICILLIN
                    );
                }
            }
        }

        MedicalActionButton button = new MedicalActionButton(
                x,
                y,
                item,
                item.getDescription(),
                missingRequiredItem,
                onPress
        );
        button.active = active;
        medicalActionButtons.add(addRenderableWidget(button));
    }

    private void addInfusionButton(int x, int y, BodyState state, InfusionType type, Item item) {
        boolean otherwiseAvailable = inspectingOtherPlayer
                && actorCanAct()
                && !ClientTreatmentState.isActive()
                && !ClientMedicationState.isActive()
                && !state.canAct()
                && state.lifeState() != BodyLifeState.BRAIN_DEAD
                && !state.hasActiveInfusion();
        boolean active = otherwiseAvailable && countItem(item) > 0;
        MedicalActionButton button = new MedicalActionButton(
                x,
                y,
                item,
                item.getDescription(),
                otherwiseAvailable && countItem(item) <= 0,
                () -> ModNetworking.requestInfusion(displayedEntityId(), type)
        );
        button.active = active;
        medicalActionButtons.add(addRenderableWidget(button));
    }

    private void addAssistedBreathingButton(Layout layout, BodyState state) {
        MedicalActionButton button = new MedicalActionButton(
                layout.rightX + 10,
                layout.innerY + 6 + 22,
                ModItems.MANUAL_RESUSCITATOR.get(),
                ModItems.MANUAL_RESUSCITATOR.get().getDescription(),
                canAssistBreathingWithoutItem(state)
                        && countItem(ModItems.MANUAL_RESUSCITATOR.get()) <= 0,
                this::beginAssistedBreathing
        );
        button.active = canAssistBreathing(state);
        medicalActionButtons.add(addRenderableWidget(button));
    }

    private void addEmergencyButtons(Layout layout, BodyState state) {
        addAssistedBreathingButton(layout, state);
        int startX = layout.rightX + 10;
        int startY = layout.innerY + 6 + 22;
        addCprButton(
                startX + TREATMENT_BUTTON_STEP,
                startY,
                state
        );

        int defibrillationWidth = Math.max(64, layout.rightWidth - 20);
        defibrillationEnergySlider = addRenderableWidget(new DefibrillationEnergySlider(
                startX,
                startY + 28,
                defibrillationWidth,
                selectedDefibrillationEnergy,
                energy -> selectedDefibrillationEnergy = energy
        ));
        defibrillationEnergySlider.active = !defibrillationHeld;

        defibrillationChargeButton = addRenderableWidget(new DefibrillationChargeButton(
                startX,
                startY + 48,
                defibrillationWidth,
                this::beginDefibrillation,
                this::defibrillationVisualState,
                this::defibrillationChargeProgress,
                this::defibrillationChargeLabel,
                this::defibrillationDisabledTooltip
        ));
        defibrillationChargeButton.active = defibrillationHeld
                || canDefibrillate(state, selectedDefibrillationEnergy);
    }

    private void addCprButton(int x, int y, BodyState state) {
        MedicalActionButton button = new MedicalActionButton(
                x,
                y,
                CPR_ICON,
                Component.translatable("screen.superficialtrauma.health.cpr"),
                false,
                this::beginCpr
        );
        button.active = canPerformCpr(state);
        medicalActionButtons.add(addRenderableWidget(button));
    }

    private boolean canAssistBreathing(BodyState state) {
        return canAssistBreathingWithoutItem(state)
                && countItem(ModItems.MANUAL_RESUSCITATOR.get()) > 0;
    }

    private boolean canAssistBreathingWithoutItem(BodyState state) {
        return inspectingOtherPlayer
                && actorCanAct()
                && patientInAssistedBreathingRange()
                && (state.lifeState() == BodyLifeState.INCAPACITATED
                || state.lifeState() == BodyLifeState.AWAKENING
                || state.lifeState() == BodyLifeState.CARDIAC_ARREST
                || state.lifeState() == BodyLifeState.VENTRICULAR_FIBRILLATION);
    }

    private boolean canPerformCpr(BodyState state) {
        return inspectingOtherPlayer
                && actorCanAct()
                && patientInAssistedBreathingRange()
                && state.lifeState() == BodyLifeState.CARDIAC_ARREST;
    }

    private boolean canDefibrillate(BodyState state, DefibrillationEnergy energy) {
        return !defibrillationHeld
                && hasRequiredDefibrillatorCount()
                && canContinueDefibrillation(state, energy);
    }

    private boolean canContinueDefibrillation(BodyState state) {
        return canContinueDefibrillation(state, selectedDefibrillationEnergy);
    }

    private boolean canContinueDefibrillation(BodyState state, DefibrillationEnergy energy) {
        return inspectingOtherPlayer
                && actorCanAct()
                && actorHasFirstAidSkill()
                && patientInAssistedBreathingRange()
                && state.lifeState() == BodyLifeState.VENTRICULAR_FIBRILLATION
                && defibrillatorEnergy() >= energy.joules();
    }

    private boolean patientInAssistedBreathingRange() {
        if (minecraft == null || minecraft.player == null || minecraft.level == null) {
            return false;
        }
        Entity patient = minecraft.level.getEntity(displayedEntityId());
        return patient != null
                && minecraft.player.distanceToSqr(patient) <= 2.5D * 2.5D
                && minecraft.player.hasLineOfSight(patient);
    }

    private boolean actorCanAct() {
        return ClientBodyState.hasReceivedSnapshot() && ClientBodyState.snapshot().canAct();
    }

    private boolean hasAdminPermissions() {
        return minecraft != null
                && minecraft.player != null
                && minecraft.player.hasPermissions(2);
    }

    private boolean hasStethoscope() {
        return countItem(ModItems.STETHOSCOPE.get()) > 0;
    }

    private boolean showsOrganophosphatePoisoning(BodyState state) {
        return hasStethoscope() && state.hasOrganophosphatePoisoning();
    }

    private void beginAssistedBreathing() {
        if (assistedBreathingHeld) {
            return;
        }
        assistedBreathingHeld = true;
        ModNetworking.setAssistedBreathing(displayedEntityId(), true);
    }

    private void stopAssistedBreathing() {
        if (!assistedBreathingHeld) {
            return;
        }
        assistedBreathingHeld = false;
        if (minecraft != null && minecraft.getConnection() != null) {
            ModNetworking.setAssistedBreathing(displayedEntityId(), false);
        }
    }

    private void beginCpr() {
        if (cprHeld) {
            return;
        }
        stopAssistedBreathing();
        cprHeld = true;
        ModNetworking.setCpr(displayedEntityId(), true);
    }

    private void stopCpr() {
        if (!cprHeld) {
            return;
        }
        cprHeld = false;
        if (minecraft != null && minecraft.getConnection() != null) {
            ModNetworking.setCpr(displayedEntityId(), false);
        }
    }

    private void beginDefibrillation() {
        if (defibrillationHeld || !hasSnapshot()
                || !canDefibrillate(displayedState(), selectedDefibrillationEnergy)) {
            return;
        }
        stopAssistedBreathing();
        stopCpr();
        defibrillationHeld = true;
        defibrillationChargeStartGameTime = currentGameTime();
        defibrillationReadyGameTime = defibrillationChargeStartGameTime
                + com.swampd.superficialtrauma.common.treatment.DefibrillationService.CHARGE_DURATION_TICKS;
        ModNetworking.startDefibrillation(displayedEntityId(), selectedDefibrillationEnergy);
    }

    private void releaseDefibrillation() {
        if (!defibrillationHeld) {
            return;
        }
        DefibrillationEnergy energy = selectedDefibrillationEnergy;
        boolean ready = currentGameTime() >= defibrillationReadyGameTime;
        clearDefibrillationClientState();
        if (minecraft != null && minecraft.getConnection() != null) {
            if (ready) {
                ModNetworking.releaseDefibrillation(displayedEntityId(), energy);
            } else {
                ModNetworking.cancelDefibrillation(displayedEntityId(), energy);
            }
        }
    }

    private void cancelDefibrillation() {
        if (!defibrillationHeld) {
            return;
        }
        DefibrillationEnergy energy = selectedDefibrillationEnergy;
        clearDefibrillationClientState();
        if (minecraft != null && minecraft.getConnection() != null) {
            ModNetworking.cancelDefibrillation(displayedEntityId(), energy);
        }
    }

    private void clearDefibrillationClientState() {
        defibrillationHeld = false;
        defibrillationChargeStartGameTime = -1L;
        defibrillationReadyGameTime = -1L;
    }

    private DefibrillationChargeButton.VisualState defibrillationVisualState() {
        if (!defibrillationHeld) {
            return DefibrillationChargeButton.VisualState.IDLE;
        }
        return currentGameTime() >= defibrillationReadyGameTime
                ? DefibrillationChargeButton.VisualState.READY
                : DefibrillationChargeButton.VisualState.CHARGING;
    }

    private double defibrillationChargeProgress() {
        if (!defibrillationHeld) {
            return 0.0D;
        }
        long duration = com.swampd.superficialtrauma.common.treatment.DefibrillationService.CHARGE_DURATION_TICKS;
        return Math.max(0.0D, Math.min(1.0D,
                (currentGameTime() - defibrillationChargeStartGameTime) / (double) duration));
    }

    private Component defibrillationChargeLabel() {
        return switch (defibrillationVisualState()) {
            case IDLE -> Component.translatable(
                    "screen.superficialtrauma.health.defibrillation_hold",
                    selectedDefibrillationEnergy.joules()
            );
            case CHARGING -> Component.translatable(
                    "screen.superficialtrauma.health.defibrillation_charge_progress",
                    (int) Math.round(defibrillationChargeProgress() * 100.0D)
            );
            case READY -> Component.translatable("screen.superficialtrauma.health.defibrillation_release");
        };
    }

    private Component defibrillationDisabledTooltip() {
        BodyState state = displayedState();
        if (!actorHasFirstAidSkill()) {
            return Component.translatable(
                    "screen.superficialtrauma.health.defibrillation_requires_skill"
            );
        }
        if (state.lifeState() != BodyLifeState.VENTRICULAR_FIBRILLATION) {
            return Component.translatable(
                    "screen.superficialtrauma.health.defibrillation_requires_vf"
            );
        }
        if (!hasRequiredDefibrillatorCount()) {
            return Component.translatable(
                    "screen.superficialtrauma.health.defibrillation_requires_two"
            );
        }
        if (defibrillatorEnergy() < selectedDefibrillationEnergy.joules()) {
            return Component.translatable(
                    "screen.superficialtrauma.health.defibrillation_requires_energy"
            );
        }
        return Component.empty();
    }

    private boolean hasRequiredDefibrillatorCount() {
        return countItem(ModItems.DEFIBRILLATOR.get()) >= 2;
    }

    private int defibrillatorEnergy() {
        if (minecraft == null || minecraft.player == null) {
            return 0;
        }
        int maximum = 0;
        for (int slot = 0; slot < minecraft.player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = minecraft.player.getInventory().getItem(slot);
            if (stack.is(ModItems.DEFIBRILLATOR.get())) {
                maximum = Math.max(maximum, DefibrillatorItem.getEnergy(stack));
            }
        }
        return maximum;
    }

    private void addTreatmentButton(
            int x,
            int y,
            int patientEntityId,
            WoundInstance wound,
            TreatmentType type,
            boolean anyTreatmentActive,
            int clipLeft,
            int clipTop,
            int clipRight,
            int clipBottom
    ) {
        boolean active = false;
        boolean removal = false;
        boolean missingRequiredItem = false;
        Component message = Component.translatable(type.translationKey());
        Runnable onPress = () -> {
        };

        TreatmentProcedure appliedProcedure = TreatmentProcedure.forCovering(wound.covering());
        if (anyTreatmentActive) {
            // Busy buttons remain disabled without implying that their item is missing.
        } else if (preparation != null) {
            if (preparation.matches(patientEntityId, wound.id())
                    && preparation.kind() == PreparationKind.BANDAGE
                    && (type == TreatmentType.MEDICAL_TAPE
                    || type == TreatmentType.SELF_ADHESIVE_BANDAGE)) {
                TreatmentProcedure procedure = TreatmentProcedure.bandageCombination(type);
                active = procedure != null && hasRequiredItems(procedure);
                missingRequiredItem = countItem(type) <= 0;
                if (procedure != null) {
                    onPress = () -> submitPreparedTreatment(patientEntityId, wound.id(), procedure);
                }
            } else if (preparation.matches(patientEntityId, wound.id())
                    && preparation.kind() == PreparationKind.DEBRIDEMENT
                    && (type == TreatmentType.SALINE_SOLUTION || type == TreatmentType.ARTIFICIAL_DERMIS)) {
                TreatmentProcedure procedure = type == TreatmentType.ARTIFICIAL_DERMIS
                        ? TreatmentProcedure.SKIN_GRAFT : TreatmentProcedure.DEBRIDEMENT;
                active = actorHasSurgerySkill()
                        && procedure.isApplicable(wound, TreatmentAction.APPLY)
                        && hasRequiredItems(procedure);
                missingRequiredItem = actorHasSurgerySkill()
                        && procedure.isApplicable(wound, TreatmentAction.APPLY)
                        && countItem(type) <= 0;
                onPress = () -> submitPreparedTreatment(patientEntityId, wound.id(), procedure);
            }
        } else if (type == TreatmentType.MEDICAL_GAUZE) {
            TreatmentProcedure procedure = TreatmentProcedure.WOUND_PACKING;
            if (wound.woundPackingApplied()) {
                active = true;
                removal = true;
                message = Component.translatable(TreatmentAction.REMOVE.translationKey(procedure));
                onPress = () -> ModNetworking.requestTreatment(
                        patientEntityId,
                        wound.id(),
                        procedure,
                        TreatmentAction.REMOVE
                );
            } else {
                active = procedure.isApplicable(wound, TreatmentAction.APPLY) && hasRequiredItems(procedure);
                missingRequiredItem = procedure.isApplicable(wound, TreatmentAction.APPLY)
                        && countItem(type) <= 0;
                onPress = () -> ModNetworking.requestTreatment(
                        patientEntityId,
                        wound.id(),
                        procedure,
                        TreatmentAction.APPLY
                );
            }
        } else if (type == TreatmentType.TOURNIQUET) {
            TreatmentProcedure procedure = TreatmentProcedure.TOURNIQUET;
            if (wound.tourniquetApplied()) {
                active = true;
                removal = true;
                message = Component.translatable(TreatmentAction.REMOVE.translationKey(procedure));
                onPress = () -> ModNetworking.requestTreatment(
                        patientEntityId,
                        wound.id(),
                        procedure,
                        TreatmentAction.REMOVE
                );
            } else {
                boolean skillAvailable = actorHasFirstAidSkill();
                active = skillAvailable
                        && procedure.isApplicable(wound, TreatmentAction.APPLY)
                        && hasRequiredItems(procedure);
                missingRequiredItem = skillAvailable
                        && procedure.isApplicable(wound, TreatmentAction.APPLY)
                        && countItem(type) <= 0;
                onPress = () -> ModNetworking.requestTreatment(
                        patientEntityId,
                        wound.id(),
                        procedure,
                        TreatmentAction.APPLY
                );
            }
        } else if (appliedProcedure != null) {
            if (type == appliedProcedure.removalAnchor()) {
                active = true;
                removal = true;
                message = Component.translatable(
                        TreatmentAction.REMOVE.translationKey(appliedProcedure)
                );
                TreatmentProcedure procedure = appliedProcedure;
                onPress = () -> ModNetworking.requestTreatment(
                        patientEntityId,
                        wound.id(),
                        procedure,
                        TreatmentAction.REMOVE
                );
            }
        } else {
            switch (type) {
                case TEMPORARY_DRESSING -> {
                    TreatmentProcedure procedure = TreatmentProcedure.TEMPORARY_DRESSING;
                    active = hasRequiredItems(procedure);
                    missingRequiredItem = countItem(type) <= 0;
                    onPress = () -> ModNetworking.requestTreatment(
                            patientEntityId,
                            wound.id(),
                            procedure,
                            TreatmentAction.APPLY
                    );
                }
                case BANDAGE -> {
                    active = countItem(TreatmentType.BANDAGE) > 0
                            && (countItem(TreatmentType.MEDICAL_TAPE) > 0
                            || countItem(TreatmentType.SELF_ADHESIVE_BANDAGE) > 0);
                    missingRequiredItem = countItem(type) <= 0;
                    onPress = () -> beginPreparation(
                            patientEntityId,
                            wound.id(),
                            PreparationKind.BANDAGE
                    );
                }
                case MEDICAL_TAPE -> {
                }
                case SELF_ADHESIVE_BANDAGE -> {
                    TreatmentProcedure procedure = TreatmentProcedure.SELF_ADHESIVE_BANDAGE;
                    active = hasRequiredItems(procedure);
                    missingRequiredItem = countItem(type) <= 0;
                    onPress = () -> ModNetworking.requestTreatment(
                            patientEntityId,
                            wound.id(),
                            procedure,
                            TreatmentAction.APPLY
                    );
                }
                case ICE_PACK -> {
                    TreatmentProcedure procedure = TreatmentProcedure.ICE_PACK;
                    active = procedure.isApplicable(wound, TreatmentAction.APPLY)
                            && hasRequiredItems(procedure);
                    missingRequiredItem = procedure.isApplicable(wound, TreatmentAction.APPLY)
                            && countItem(type) <= 0;
                    onPress = () -> ModNetworking.requestTreatment(
                            patientEntityId,
                            wound.id(),
                            procedure,
                            TreatmentAction.APPLY
                    );
                }
                case POVIDONE_IODINE, MEDICAL_ALCOHOL -> {
                    TreatmentProcedure procedure = TreatmentProcedure.singleStepFor(type);
                    active = procedure != null
                            && procedure.isApplicable(wound, TreatmentAction.APPLY)
                            && hasRequiredItems(procedure);
                    missingRequiredItem = procedure != null
                            && procedure.isApplicable(wound, TreatmentAction.APPLY)
                            && countItem(type) <= 0;
                    if (procedure != null) {
                        onPress = () -> ModNetworking.requestTreatment(
                                patientEntityId,
                                wound.id(),
                                procedure,
                                TreatmentAction.APPLY
                        );
                    }
                }
                case TOURNIQUET, SALINE_SOLUTION, ARTIFICIAL_DERMIS -> {
                }
                case SURGICAL_KIT -> {
                    boolean skillAvailable = actorHasSurgerySkill();
                    active = canPrepareSurgery(wound);
                    missingRequiredItem = skillAvailable
                            && countItem(type) <= 0;
                    onPress = () -> beginPreparation(
                            patientEntityId,
                            wound.id(),
                            PreparationKind.DEBRIDEMENT
                    );
                }
                default -> {
                }
            }
        }

        TreatmentItemButton button = new TreatmentItemButton(
                x,
                y,
                type,
                removal,
                message,
                missingRequiredItem,
                onPress,
                clipLeft,
                clipTop,
                clipRight,
                clipBottom
        );
        button.active = active;
        treatmentButtons.add(addRenderableWidget(button));
    }

    private boolean actorHasSurgerySkill() {
        return ClientBodyState.hasReceivedSnapshot() && ClientBodyState.snapshot().hasSurgerySkill();
    }

    private boolean actorHasFirstAidSkill() {
        return ClientBodyState.hasReceivedSnapshot() && ClientBodyState.snapshot().hasFirstAidSkill();
    }

    private static List<TreatmentType> visibleTreatmentTypes(WoundInstance wound) {
        List<TreatmentType> visible = new ArrayList<>();
        for (TreatmentType type : TreatmentType.values()) {
            if (TreatmentProcedure.supportsType(wound, type)) {
                visible.add(type);
            }
        }
        return visible;
    }

    private void beginPreparation(
            int patientEntityId,
            UUID woundId,
            PreparationKind kind
    ) {
        Vec3 patientPosition = displayedPatientPosition();
        if (patientPosition == null) {
            return;
        }
        clearPreparation();
        preparation = new TreatmentPreparation(patientEntityId, woundId, patientPosition, kind);
        ModNetworking.setTreatmentPreparationSound(
                patientEntityId,
                woundId,
                TreatmentPreparationType.valueOf(kind.name()),
                true
        );
        rebuildTreatmentButtons();
    }

    private void submitPreparedTreatment(
            int patientEntityId,
            UUID woundId,
            TreatmentProcedure procedure
    ) {
        clearPreparation();
        ModNetworking.requestTreatment(patientEntityId, woundId, procedure, TreatmentAction.APPLY);
        rebuildTreatmentButtons();
    }

    private void clearPreparation() {
        if (preparation == null) {
            return;
        }
        if (minecraft != null && minecraft.getConnection() != null) {
            ModNetworking.setTreatmentPreparationSound(
                    preparation.patientEntityId(),
                    preparation.woundId(),
                    TreatmentPreparationType.valueOf(preparation.kind().name()),
                    false
            );
        }
        preparation = null;
    }

    private void beginMedicationPreparation() {
        BodyState state = displayedState();
        Vec3 patientPosition = displayedPatientPosition();
        if (patientPosition == null || !canPrepareInjection(state)) {
            return;
        }
        clearPreparation();
        clearMedicationPreparation();
        medicationPreparation = new MedicationPreparation(displayedEntityId(), patientPosition);
        ModNetworking.setMedicationPreparation(displayedEntityId(), true);
        rebuildTreatmentButtons();
    }

    private void submitPreparedMedication(MedicationType type) {
        int patientEntityId = displayedEntityId();
        // Keep the server-side first-step token until the ordered start packet consumes it.
        // Sending a separate cancellation here would let the second packet arrive without
        // proof that the syringe step was completed.
        medicationPreparation = null;
        ModNetworking.requestMedication(patientEntityId, type);
        rebuildTreatmentButtons();
    }

    private boolean canPrepareInjection(BodyState state) {
        return actorCanAct()
                && state.lifeState() != BodyLifeState.BRAIN_DEAD
                && countItem(ModItems.SYRINGE.get()) > 0
                && (countItem(ModItems.MORPHINE_VIAL.get()) > 0
                || countItem(ModItems.REMIFENTANIL_INJECTION.get()) > 0
                || (state.hasActiveOpioidDose() && countItem(ModItems.NALOXONE.get()) > 0)
                || countItem(ModItems.EPINEPHRINE_INJECTION.get()) > 0
                || countItem(ModItems.ATROPINE_SULFATE_INJECTION.get()) > 0
                || countItem(ModItems.PRALIDOXIME_CHLORIDE_INJECTION.get()) > 0
                || countItem(ModItems.CEFTRIAXONE.get()) > 0);
    }

    private boolean medicationPreparationStillValid(BodyState state) {
        return medicationPreparation != null
                && medicationPreparation.patientEntityId() == displayedEntityId()
                && canPrepareInjection(state);
    }

    private boolean patientMovedSinceMedicationPreparation() {
        if (medicationPreparation == null) {
            return false;
        }
        Vec3 current = displayedPatientPosition();
        return current == null
                || current.distanceToSqr(medicationPreparation.patientStartPosition())
                > TreatmentMovementRules.MOVEMENT_TOLERANCE_SQUARED;
    }

    private void clearMedicationPreparation() {
        if (medicationPreparation == null) {
            return;
        }
        if (minecraft != null && minecraft.getConnection() != null) {
            ModNetworking.setMedicationPreparation(medicationPreparation.patientEntityId(), false);
        }
        medicationPreparation = null;
    }

    private boolean preparationStillValid(BodyState state) {
        if (preparation == null || preparation.patientEntityId != displayedEntityId()) {
            return false;
        }
        WoundInstance wound = state.wound(preparation.woundId).orElse(null);
        if (wound == null) {
            return false;
        }
        return switch (preparation.kind()) {
            case BANDAGE -> !wound.covering().isApplied()
                    && countItem(TreatmentType.BANDAGE) > 0
                    && (countItem(TreatmentType.MEDICAL_TAPE) > 0
                    || countItem(TreatmentType.SELF_ADHESIVE_BANDAGE) > 0);
            case DEBRIDEMENT -> canPrepareSurgery(wound);
        };
    }

    private boolean canPrepareSurgery(WoundInstance wound) {
        return actorHasSurgerySkill()
                && ((TreatmentProcedure.DEBRIDEMENT.isApplicable(wound, TreatmentAction.APPLY)
                && hasRequiredItems(TreatmentProcedure.DEBRIDEMENT))
                || (TreatmentProcedure.SKIN_GRAFT.isApplicable(wound, TreatmentAction.APPLY)
                && hasRequiredItems(TreatmentProcedure.SKIN_GRAFT)));
    }

    private boolean patientMovedSincePreparation() {
        if (preparation == null) {
            return false;
        }
        Vec3 current = displayedPatientPosition();
        return current == null
                || current.distanceToSqr(preparation.patientStartPosition)
                > TreatmentMovementRules.MOVEMENT_TOLERANCE_SQUARED;
    }

    private Vec3 displayedPatientPosition() {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        Entity entity = minecraft.level.getEntity(displayedEntityId());
        return entity == null ? null : entity.position();
    }

    private boolean hasRequiredItems(TreatmentProcedure procedure) {
        for (TreatmentIngredient ingredient : procedure.ingredients()) {
            if (countItem(ingredient.type()) < ingredient.count()) {
                return false;
            }
        }
        return true;
    }

    private long inventorySignature() {
        long signature = 1L;
        for (TreatmentType type : TreatmentType.values()) {
            signature = signature * 31L + countItem(type);
        }
        signature = signature * 31L + countItem(ModItems.BLOOD_BAG.get());
        signature = signature * 31L + countItem(ModItems.MANUAL_RESUSCITATOR.get());
        signature = signature * 31L + defibrillatorEnergy();
        return signature;
    }

    private List<WoundInstance> sortedWounds(BodyState state) {
        List<WoundInstance> sorted = new ArrayList<>(state.wounds());
        sorted.sort(Comparator
                .comparingInt(WoundInstance::severity).reversed()
                .thenComparing(Comparator.comparingLong(WoundInstance::createdGameTime).reversed()));
        return sorted;
    }

    private WoundListLayout woundListLayout(
            BodyState state,
            int availableHeight,
            int woundAvailableWidth,
            int treatmentAvailableWidth
    ) {
        int viewportHeight = Math.max(0, availableHeight - 16);
        int contentWidth = Math.max(20, woundAvailableWidth);
        List<WoundRow> rows = woundRows(state, contentWidth, treatmentAvailableWidth);
        int totalContentHeight = woundListContentHeight(state, rows);
        if (totalContentHeight > viewportHeight) {
            contentWidth = Math.max(20, woundAvailableWidth - WOUND_SCROLLBAR_RESERVED_WIDTH);
            rows = woundRows(state, contentWidth, treatmentAvailableWidth);
            totalContentHeight = woundListContentHeight(state, rows);
        }
        return new WoundListLayout(
                List.copyOf(rows),
                contentWidth,
                viewportHeight,
                totalContentHeight,
                Math.max(0, totalContentHeight - viewportHeight)
        );
    }

    private List<WoundRow> woundRows(
            BodyState state,
            int woundAvailableWidth,
            int treatmentAvailableWidth
    ) {
        List<WoundInstance> sorted = sortedWounds(state);
        List<WoundRow> rows = new ArrayList<>();
        for (WoundInstance wound : sorted) {
            int rowHeight = woundRowHeight(wound, woundAvailableWidth, treatmentAvailableWidth);
            rows.add(new WoundRow(wound, rowHeight));
        }
        return rows;
    }

    private int woundListContentHeight(BodyState state, List<WoundRow> rows) {
        int contentHeight = rows.isEmpty()
                && !showsOrganophosphatePoisoning(state)
                && !state.hasVisibleRespiratoryDistress()
                ? 18
                : rows.stream().mapToInt(WoundRow::height).sum();
        if (showsOrganophosphatePoisoning(state)) {
            contentHeight += ORGANOPHOSPHATE_POISONING_ROW_HEIGHT;
        }
        if (state.hasVisibleRespiratoryDistress()) {
            contentHeight += RESPIRATORY_DISTRESS_ROW_HEIGHT;
        }
        if (!state.damageWindows().isEmpty()) {
            contentHeight += 2 + state.damageWindows().size() * 13;
        }
        return contentHeight;
    }

    private void clampWoundScroll(WoundListLayout woundLayout) {
        woundScrollOffset = Mth.clamp(woundScrollOffset, 0, woundLayout.maximumScroll());
        if (!woundLayout.scrollable()) {
            draggingWoundScrollbar = false;
            woundScrollbarGrabOffset = 0;
        }
    }

    private int woundRowHeight(
            WoundInstance wound,
            int woundAvailableWidth,
            int treatmentAvailableWidth
    ) {
        String tagSummary = woundTagSummary(wound, displayedState());
        int tagLines = tagSummary.isEmpty() ? 0 : woundTagLines(tagSummary, woundAvailableWidth).size();
        int woundContentHeight = MINIMUM_WOUND_ROW_HEIGHT + Math.max(0, tagLines - 1) * 11;
        int treatmentContentHeight = TREATMENT_BUTTON_TOP
                + treatmentButtonRows(
                        treatmentAvailableWidth,
                        visibleTreatmentTypes(wound).size()
                ) * TREATMENT_BUTTON_STEP
                + font.lineHeight;
        return Math.max(woundContentHeight, treatmentContentHeight);
    }

    private List<FormattedCharSequence> woundTagLines(String tagSummary, int availableWidth) {
        return font.split(Component.literal(tagSummary), Math.max(20, availableWidth - 8));
    }

    private int treatmentButtonsPerRow(int availableWidth) {
        int buttonAreaWidth = Math.max(TREATMENT_BUTTON_SIZE, availableWidth - 8);
        return Math.max(
                1,
                1 + Math.max(0, buttonAreaWidth - TREATMENT_BUTTON_SIZE) / TREATMENT_BUTTON_STEP
        );
    }

    private int treatmentButtonRows(int availableWidth, int buttonCount) {
        if (buttonCount <= 0) {
            return 0;
        }
        int buttonsPerRow = treatmentButtonsPerRow(availableWidth);
        return (buttonCount + buttonsPerRow - 1) / buttonsPerRow;
    }

    private int countItem(TreatmentType type) {
        return countItem(type.requiredItem());
    }

    private int countItem(Item item) {
        if (minecraft == null || minecraft.player == null) {
            return 0;
        }
        int count = 0;
        for (int slot = 0; slot < minecraft.player.getInventory().getContainerSize(); slot++) {
            if (minecraft.player.getInventory().getItem(slot).is(item)) {
                count += minecraft.player.getInventory().getItem(slot).getCount();
            }
        }
        return count;
    }

    private void drawPreparationShade(GuiGraphics graphics, Layout layout) {
        graphics.fill(
                layout.innerX,
                layout.innerY,
                layout.innerX + layout.leftWidth,
                layout.innerY + layout.innerHeight,
                0xB8101217
        );
        graphics.fill(
                layout.middleX,
                layout.innerY,
                layout.middleX + layout.middleWidth,
                layout.innerY + layout.innerHeight,
                0xB8101217
        );
        String promptKey;
        if (medicationPreparation != null) {
            promptKey = "screen.superficialtrauma.health.injection_preparation_prompt";
        } else if (preparation != null && preparation.kind() == PreparationKind.DEBRIDEMENT) {
            promptKey = "screen.superficialtrauma.health.debridement_preparation_prompt";
        } else {
            promptKey = "screen.superficialtrauma.health.treatment_preparation_prompt";
        }
        Component prompt = Component.translatable(promptKey);
        int combinedStart = layout.innerX;
        int combinedEnd = layout.middleX + layout.middleWidth;
        graphics.drawCenteredString(
                font,
                prompt,
                combinedStart + (combinedEnd - combinedStart) / 2,
                layout.innerY + layout.innerHeight / 2,
                WARN_COLOR
        );
    }

    private boolean hasSnapshot() {
        return inspectingOtherPlayer
                ? ClientInspectionState.hasReceivedSnapshot(inspectedEntityId)
                : ClientBodyState.hasReceivedSnapshot();
    }

    private BodyState displayedState() {
        return inspectingOtherPlayer ? ClientInspectionState.snapshot() : ClientBodyState.snapshot();
    }

    private int displayedEntityId() {
        if (inspectingOtherPlayer) {
            return inspectedEntityId;
        }
        return minecraft != null && minecraft.player != null ? minecraft.player.getId() : -1;
    }

    private float displayedHealth() {
        if (inspectingOtherPlayer) {
            return ClientInspectionState.health();
        }
        return minecraft != null && minecraft.player != null ? minecraft.player.getHealth() : 0.0F;
    }

    private float displayedMaximumHealth() {
        if (inspectingOtherPlayer) {
            return ClientInspectionState.maximumHealth();
        }
        return minecraft != null && minecraft.player != null ? minecraft.player.getMaxHealth() : 0.0F;
    }

    private int drawValue(
            GuiGraphics graphics,
            int x,
            int y,
            int availableWidth,
            String labelKey,
            String value,
            int valueColor
    ) {
        Component label = Component.translatable(labelKey);
        graphics.drawString(font, label, x, y, MUTED_COLOR, false);
        int maximumValueWidth = Math.max(10, availableWidth - font.width(label) - 5);
        String visibleValue = font.plainSubstrByWidth(value, maximumValueWidth);
        int valueX = x + availableWidth - font.width(visibleValue);
        graphics.drawString(font, visibleValue, Math.max(x, valueX), y, valueColor, false);
        return y + 13;
    }

    private int drawWrappedWithin(
            GuiGraphics graphics,
            Component text,
            int x,
            int y,
            int availableWidth,
            int color,
            int maximumY
    ) {
        List<FormattedCharSequence> lines = font.split(text, Math.max(20, availableWidth));
        int lineY = y;
        for (FormattedCharSequence line : lines) {
            if (lineY + font.lineHeight > maximumY) {
                break;
            }
            graphics.drawString(font, line, x, lineY, color, false);
            lineY += 11;
        }
        return lineY;
    }

    private Layout layout() {
        int panelWidth = Math.min(510, width - 20);
        int panelHeight = Math.min(272, height - 20);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;
        int innerX = panelX + 8;
        int innerY = panelY + 30;
        int innerHeight = panelHeight - 38;
        int gap = 5;
        int leftWidth = Math.max(105, panelWidth * 25 / 100);
        int middleWidth = Math.max(145, panelWidth * 39 / 100);
        int middleX = innerX + leftWidth + gap;
        int rightX = middleX + middleWidth + gap;
        int rightWidth = panelWidth - 16 - leftWidth - middleWidth - gap * 2;
        return new Layout(
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                innerX,
                innerY,
                innerHeight,
                leftWidth,
                middleX,
                middleWidth,
                rightX,
                rightWidth
        );
    }

    private WoundScrollInteraction woundScrollInteraction() {
        if (!hasSnapshot()) {
            return null;
        }
        Layout layout = layout();
        WoundListLayout woundLayout = woundListLayout(
                displayedState(),
                layout.innerHeight - 12,
                layout.middleWidth - 12,
                layout.rightWidth - 12
        );
        clampWoundScroll(woundLayout);
        if (!woundLayout.scrollable()) {
            return null;
        }
        int viewportTop = layout.innerY + 6 + 16;
        int trackX = layout.middleX + layout.middleWidth - 9;
        int thumbHeight = woundScrollbarThumbHeight(woundLayout);
        int thumbY = viewportTop + woundScrollbarThumbOffset(woundLayout, thumbHeight);
        return new WoundScrollInteraction(
                layout,
                woundLayout,
                viewportTop,
                trackX,
                thumbY,
                thumbHeight
        );
    }

    private boolean setWoundScrollOffset(int requestedOffset, WoundListLayout woundLayout) {
        int clampedOffset = Mth.clamp(requestedOffset, 0, woundLayout.maximumScroll());
        if (clampedOffset == woundScrollOffset) {
            return false;
        }
        woundScrollOffset = clampedOffset;
        if (panelMode == PanelMode.TREATMENT) {
            rebuildTreatmentButtons();
        }
        return true;
    }

    private void updateWoundScrollbarDrag(double mouseY, WoundScrollInteraction interaction) {
        int availableTravel = interaction.woundLayout().viewportHeight() - interaction.thumbHeight();
        if (availableTravel <= 0) {
            return;
        }
        int requestedThumbY = Mth.clamp(
                (int) Math.round(mouseY) - woundScrollbarGrabOffset,
                interaction.viewportTop(),
                interaction.viewportTop() + availableTravel
        );
        int requestedOffset = Math.round(
                (requestedThumbY - interaction.viewportTop())
                        * (interaction.woundLayout().maximumScroll() / (float) availableTravel)
        );
        setWoundScrollOffset(requestedOffset, interaction.woundLayout());
    }

    private static boolean inside(
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height
    ) {
        return mouseX >= x
                && mouseX < x + width
                && mouseY >= y
                && mouseY < y + height;
    }

    public boolean isInspecting(int entityId) {
        return inspectingOtherPlayer && inspectedEntityId == entityId;
    }

    @Override
    public void removed() {
        if (minecraft != null && minecraft.getConnection() != null) {
            ModNetworking.cancelSkinGraft();
        }
        ClientTimingQteState.clear();
        draggingWoundScrollbar = false;
        stopAssistedBreathing();
        stopCpr();
        cancelDefibrillation();
        clearPreparation();
        clearMedicationPreparation();
        super.removed();
        if (inspectingOtherPlayer) {
            if (minecraft != null && minecraft.getConnection() != null) {
                ModNetworking.closeInspection(inspectedEntityId);
            }
            ClientInspectionState.clear();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            WoundScrollInteraction interaction = woundScrollInteraction();
            if (interaction != null) {
                int hitX = interaction.trackX()
                        - (WOUND_SCROLLBAR_HIT_WIDTH - WOUND_SCROLLBAR_TRACK_WIDTH) / 2;
                if (inside(
                        mouseX,
                        mouseY,
                        hitX,
                        interaction.viewportTop(),
                        WOUND_SCROLLBAR_HIT_WIDTH,
                        interaction.woundLayout().viewportHeight()
                )) {
                    draggingWoundScrollbar = true;
                    if (mouseY >= interaction.thumbY()
                            && mouseY < interaction.thumbY() + interaction.thumbHeight()) {
                        woundScrollbarGrabOffset = (int) Math.round(mouseY) - interaction.thumbY();
                    } else {
                        woundScrollbarGrabOffset = interaction.thumbHeight() / 2;
                        updateWoundScrollbarDrag(mouseY, interaction);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY
    ) {
        if (button == 0 && draggingWoundScrollbar) {
            WoundScrollInteraction interaction = woundScrollInteraction();
            if (interaction != null) {
                updateWoundScrollbarDrag(mouseY, interaction);
                return true;
            }
            draggingWoundScrollbar = false;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        WoundScrollInteraction interaction = woundScrollInteraction();
        if (interaction != null && delta != 0.0D) {
            Layout layout = interaction.layout();
            int viewportTop = interaction.viewportTop();
            int viewportHeight = interaction.woundLayout().viewportHeight();
            boolean overWounds = inside(
                    mouseX,
                    mouseY,
                    layout.middleX + 6,
                    viewportTop,
                    layout.middleWidth - 12,
                    viewportHeight
            );
            boolean overTreatmentButtons = panelMode == PanelMode.TREATMENT && inside(
                    mouseX,
                    mouseY,
                    layout.rightX + 6,
                    viewportTop,
                    layout.rightWidth - 12,
                    viewportHeight
            );
            if (overWounds || overTreatmentButtons) {
                int direction = (int) Math.signum(delta);
                setWoundScrollOffset(
                        woundScrollOffset - direction * WOUND_SCROLL_WHEEL_STEP,
                        interaction.woundLayout()
                );
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean releasedWoundScrollbar = button == 0 && draggingWoundScrollbar;
        if (button == 0) {
            draggingWoundScrollbar = false;
            woundScrollbarGrabOffset = 0;
        }
        if (button == 0) {
            releaseDefibrillation();
            stopAssistedBreathing();
            stopCpr();
        }
        return releasedWoundScrollbar || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE && ClientTimingQteState.press()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static String oneDecimal(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private long currentGameTime() {
        return minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : 0L;
    }

    private static String compactIdentifier(String identifier) {
        int separator = identifier.indexOf(':');
        return separator >= 0 && separator + 1 < identifier.length()
                ? identifier.substring(separator + 1)
                : identifier;
    }

    private static void drawColumn(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, COLUMN_COLOR);
        drawBorder(graphics, x, y, width, height, BORDER_COLOR);
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private record Layout(
            int panelX,
            int panelY,
            int panelWidth,
            int panelHeight,
            int innerX,
            int innerY,
            int innerHeight,
            int leftWidth,
            int middleX,
            int middleWidth,
            int rightX,
            int rightWidth
    ) {
    }

    private record TreatmentPreparation(
            int patientEntityId,
            UUID woundId,
            Vec3 patientStartPosition,
            PreparationKind kind
    ) {
        private boolean matches(int entityId, UUID candidateWoundId) {
            return patientEntityId == entityId && woundId.equals(candidateWoundId);
        }
    }

    private record WoundRow(WoundInstance wound, int height) {
    }

    private record WoundListLayout(
            List<WoundRow> rows,
            int contentWidth,
            int viewportHeight,
            int totalContentHeight,
            int maximumScroll
    ) {
        private boolean scrollable() {
            return maximumScroll > 0;
        }
    }

    private record WoundScrollInteraction(
            Layout layout,
            WoundListLayout woundLayout,
            int viewportTop,
            int trackX,
            int thumbY,
            int thumbHeight
    ) {
    }

    private record MedicationPreparation(int patientEntityId, Vec3 patientStartPosition) {
    }

    private enum PreparationKind {
        BANDAGE,
        DEBRIDEMENT
    }

    private enum MedicationButtonType {
        SYRINGE,
        PARACETAMOL,
        MORPHINE,
        REMIFENTANIL,
        NALOXONE,
        EPINEPHRINE,
        METOPROLOL,
        ATROPINE_SULFATE,
        PRALIDOXIME_CHLORIDE,
        CEFTRIAXONE,
        AMOXICILLIN
    }

    private enum ElectrocardiogramRhythm {
        NORMAL,
        TACHYCARDIA,
        BRADYCARDIA,
        FIBRILLATION,
        FLATLINE
    }

    private enum PanelMode {
        TREATMENT("screen.superficialtrauma.health.panel.treatment"),
        MEDICATION("screen.superficialtrauma.health.panel.medication"),
        EMERGENCY("screen.superficialtrauma.health.panel.emergency");

        private final String translationKey;

        PanelMode(String translationKey) {
            this.translationKey = translationKey;
        }

        private String translationKey() {
            return translationKey;
        }
    }
}
