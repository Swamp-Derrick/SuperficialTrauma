package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.common.body.BodyState;
import com.swampd.superficialtrauma.common.body.BodyLifeState;
import com.swampd.superficialtrauma.common.body.CollapseReason;
import com.swampd.superficialtrauma.common.body.InfusionType;
import com.swampd.superficialtrauma.common.body.DefibrillationEnergy;
import com.swampd.superficialtrauma.common.damage.DamageWindow;
import com.swampd.superficialtrauma.common.init.ModItems;
import com.swampd.superficialtrauma.common.item.DefibrillatorItem;
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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
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
    private static final int TREATMENT_BUTTON_SIZE = 22;
    private static final int TREATMENT_BUTTON_STEP = 25;
    private static final int TREATMENT_BUTTON_TOP = 9;

    private final boolean inspectingOtherPlayer;
    private final int inspectedEntityId;
    private final List<TreatmentItemButton> treatmentButtons = new ArrayList<>();
    private final List<MedicalActionButton> medicalActionButtons = new ArrayList<>();
    private long lastButtonRevision = Long.MIN_VALUE;
    private long lastInventorySignature = Long.MIN_VALUE;
    private boolean lastTreatmentActive;
    private TreatmentPreparation preparation;
    private PanelMode panelMode = PanelMode.TREATMENT;
    private boolean assistedBreathingHeld;
    private boolean cprHeld;
    private DefibrillationEnergy selectedDefibrillationEnergy = DefibrillationEnergy.J150;
    private boolean defibrillationHeld;
    private long defibrillationChargeStartGameTime = -1L;
    private long defibrillationReadyGameTime = -1L;
    private DefibrillationEnergySlider defibrillationEnergySlider;
    private DefibrillationChargeButton defibrillationChargeButton;

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
        long inventorySignature = inventorySignature();
        if (state.revision() != lastButtonRevision
                || inventorySignature != lastInventorySignature
                || treatmentActive != lastTreatmentActive) {
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
                displayedMaximumHealth()
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
        if (preparation != null) {
            drawPreparationShade(graphics, layout);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        for (TreatmentItemButton button : treatmentButtons) {
            if (button.isHovered()) {
                graphics.renderTooltip(font, button.tooltip(), mouseX, mouseY);
                break;
            }
        }
        for (MedicalActionButton button : medicalActionButtons) {
            if (button.isHovered()) {
                graphics.renderTooltip(font, button.tooltip(), mouseX, mouseY);
                break;
            }
        }
        if (defibrillationEnergySlider != null && defibrillationEnergySlider.isHovered()) {
            graphics.renderTooltip(font, defibrillationEnergySlider.tooltip(), mouseX, mouseY);
        } else if (defibrillationChargeButton != null && defibrillationChargeButton.isHovered()) {
            graphics.renderTooltip(font, defibrillationChargeButton.tooltip(), mouseX, mouseY);
        }
    }

    private void drawWholeBodyColumn(
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
            long dangerTicks = state.downedDangerRemainingTicks(gameTime);
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
        List<WoundInstance> sortedWounds = sortedWounds(state);
        List<WoundRow> visibleRows = visibleWoundRows(
                state,
                availableHeight,
                availableWidth,
                treatmentAvailableWidth
        );
        int cardY = y + 16;
        if (sortedWounds.isEmpty()) {
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

        for (WoundRow row : visibleRows) {
            WoundInstance wound = row.wound();
            int cardHeight = row.height() - 3;
            int cardColor = wound.severity() >= 3
                    ? 0xAA4C2529
                    : wound.severity() == 2 ? 0xAA4A3C24 : 0xAA263D31;
            graphics.fill(x, cardY, x + availableWidth, cardY + cardHeight, cardColor);
            drawBorder(
                    graphics,
                    x,
                    cardY,
                    availableWidth,
                    cardHeight,
                    wound.severity() >= 3 ? DANGER_COLOR : BORDER_COLOR
            );
            Component woundName = Component.translatable(wound.displayTranslationKey());
            Component triage = Component.translatable(wound.triageTranslationKey());
            int triageWidth = font.width(triage);
            int woundNameWidth = Math.max(20, availableWidth - triageWidth - 16);
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
                    x + availableWidth - 4 - triageWidth,
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
                    font.plainSubstrByWidth(woundValues.getString(), Math.max(20, availableWidth - 8)),
                    x + 4,
                    cardY + 17,
                    TEXT_COLOR,
                    false
            );
            String tagSummary = woundTagSummary(wound, state.movementBleedingActive());
            if (!tagSummary.isEmpty()) {
                int tagY = cardY + 28;
                for (FormattedCharSequence line : woundTagLines(tagSummary, availableWidth)) {
                    graphics.drawString(font, line, x + 4, tagY, MUTED_COLOR, false);
                    tagY += 11;
                }
            }
            cardY += row.height();
        }

        int columnBottom = y + availableHeight;
        if (sortedWounds.size() > visibleRows.size() && cardY + font.lineHeight <= columnBottom) {
            Component moreWounds = Component.translatable(
                    "screen.superficialtrauma.health.more_wounds",
                    sortedWounds.size() - visibleRows.size()
            );
            graphics.drawString(
                    font,
                    font.plainSubstrByWidth(moreWounds.getString(), availableWidth),
                    x,
                    cardY,
                    MUTED_COLOR,
                    false
            );
            cardY += 13;
        }

        int pendingY = cardY + 2;
        List<DamageWindow> pendingWindows = new ArrayList<>(state.damageWindows().values());
        pendingWindows.sort(Comparator.comparingLong(DamageWindow::startedGameTime));
        for (DamageWindow window : pendingWindows) {
            pendingY = drawPendingWindow(graphics, window, x, pendingY, availableWidth, columnBottom);
        }
    }

    private int drawPendingWindow(
            GuiGraphics graphics,
            DamageWindow window,
            int x,
            int y,
            int availableWidth,
            int columnBottom
    ) {
        if (y + font.lineHeight > columnBottom) {
            return y;
        }
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

    private String woundTagSummary(WoundInstance wound, boolean movementBleedingActive) {
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

        int effectiveBleedingLevel = wound.bleedingLevel(movementBleedingActive);
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
            case EMERGENCY -> drawEmergencyColumn(graphics, state, x, y + 22, availableWidth, availableHeight);
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
        int textY = y + 30;
        Component text = state.hasActiveInfusion()
                ? Component.translatable(
                        "screen.superficialtrauma.health.infusion_active",
                        Component.translatable(state.infusionType().translationKey()),
                        oneDecimal(state.infusionRemainingTicks(currentGameTime()) / 20.0F)
                )
                : Component.translatable("screen.superficialtrauma.health.infusion_available");
        drawWrappedWithin(graphics, text, x + 4, textY, availableWidth - 8,
                state.hasActiveInfusion() ? GOOD_COLOR : MUTED_COLOR, y + availableHeight - 18);
    }

    private void drawEmergencyColumn(
            GuiGraphics graphics,
            BodyState state,
            int x,
            int y,
            int availableWidth,
            int availableHeight
    ) {
        long gameTime = currentGameTime();
        Component text;
        int color;
        if (state.lifeState() == BodyLifeState.CARDIAC_ARREST) {
            text = Component.translatable(
                    "screen.superficialtrauma.health.cpr_status",
                    state.accumulatedCprSeconds(),
                    oneDecimal((float) (state.currentCprSuccessChance() * 100.0D)),
                    oneDecimal(state.downedDangerRemainingTicks(gameTime) / 20.0F)
            );
            color = cprHeld ? GOOD_COLOR : WARN_COLOR;
        } else if (state.lifeState() == BodyLifeState.VENTRICULAR_FIBRILLATION) {
            DefibrillationChargeButton.VisualState visualState = defibrillationVisualState();
            text = Component.translatable(
                    visualState == DefibrillationChargeButton.VisualState.READY
                            ? "screen.superficialtrauma.health.defibrillation_ready"
                            : visualState == DefibrillationChargeButton.VisualState.CHARGING
                            ? "screen.superficialtrauma.health.defibrillation_charging"
                            : "screen.superficialtrauma.health.defibrillation_status",
                    oneDecimal(state.ventricularFibrillationRemainingTicks(gameTime) / 20.0F),
                    oneDecimal(state.downedDangerRemainingTicks(gameTime) / 20.0F),
                    defibrillatorEnergy(),
                    attemptedDefibrillationText(state)
            );
            color = DANGER_COLOR;
        } else {
            text = Component.translatable(
                    assistedBreathingHeld
                            ? "screen.superficialtrauma.health.assisted_breathing_active"
                            : "screen.superficialtrauma.health.assisted_breathing_available"
            );
            color = assistedBreathingHeld ? GOOD_COLOR : MUTED_COLOR;
        }
        drawWrappedWithin(graphics, text, x + 4, y + 84, availableWidth - 8,
                color, y + availableHeight - 18);
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
        List<WoundRow> visibleRows = visibleWoundRows(
                state,
                availableHeight,
                middleAvailableWidth,
                availableWidth
        );
        int rowY = y + 16;
        for (WoundRow row : visibleRows) {
            WoundInstance wound = row.wound();
            if (wound.covering().isApplied()
                    || wound.woundPackingApplied()
                    || wound.tourniquetApplied()) {
                int buttonRows = treatmentButtonRows(
                        availableWidth,
                        visibleTreatmentTypes(wound).size()
                );
                int statusY = rowY + TREATMENT_BUTTON_TOP + buttonRows * TREATMENT_BUTTON_STEP;
                List<String> statusParts = new ArrayList<>();
                if (wound.covering().isApplied()) {
                    statusParts.add(Component.translatable(
                            "screen.superficialtrauma.health.covering_status",
                            Component.translatable(wound.covering().translationKey()),
                            oneDecimal(wound.baseHealingPerSecond())
                    ).getString());
                }
                if (wound.woundPackingApplied()) {
                    statusParts.add(Component.translatable(
                            "screen.superficialtrauma.health.wound_packing_status"
                    ).getString());
                }
                if (wound.tourniquetApplied()) {
                    statusParts.add(Component.translatable(
                            "screen.superficialtrauma.health.tourniquet_status",
                            formatDuration(wound.tourniquetAccumulatedTicks())
                    ).getString());
                }
                Component status = Component.literal(String.join(" · ", statusParts));
                if (statusY + font.lineHeight <= rowY + row.height()) {
                    graphics.drawString(
                            font,
                            font.plainSubstrByWidth(status.getString(), Math.max(20, availableWidth - 8)),
                            x + 4,
                            statusY,
                            GOOD_COLOR,
                            false
                    );
                }
            }
            rowY += row.height();
        }
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
        if (!hasSnapshot() || minecraft == null || minecraft.player == null) {
            return;
        }

        BodyState state = displayedState();
        boolean anyTreatmentActive = ClientTreatmentState.isActive();
        if (panelMode == PanelMode.TREATMENT) {
            int availableHeight = layout.innerHeight - 12;
            int treatmentAvailableWidth = layout.rightWidth - 12;
            List<WoundRow> visibleRows = visibleWoundRows(
                    state,
                    availableHeight,
                    layout.middleWidth - 12,
                    treatmentAvailableWidth
            );
            int patientEntityId = displayedEntityId();
            int rowY = layout.innerY + 6 + 16;
            int buttonsPerRow = treatmentButtonsPerRow(treatmentAvailableWidth);

            for (WoundRow row : visibleRows) {
                WoundInstance wound = row.wound();
                List<TreatmentType> visibleTypes = visibleTreatmentTypes(wound);
                if (!visibleTypes.isEmpty()) {
                    int treatmentIndex = 0;
                    for (TreatmentType type : visibleTypes) {
                        addTreatmentButton(
                                layout.rightX + 10 + treatmentIndex % buttonsPerRow * TREATMENT_BUTTON_STEP,
                                rowY + TREATMENT_BUTTON_TOP
                                        + treatmentIndex / buttonsPerRow * TREATMENT_BUTTON_STEP,
                                patientEntityId,
                                wound,
                                type,
                                anyTreatmentActive
                        );
                        treatmentIndex++;
                    }
                }
                rowY += row.height();
            }
        } else if (panelMode == PanelMode.MEDICATION) {
            addInfusionButtons(layout, state);
        } else if (panelMode == PanelMode.EMERGENCY) {
            addEmergencyButtons(layout, state);
        }

        lastButtonRevision = state.revision();
        lastInventorySignature = inventorySignature();
        lastTreatmentActive = anyTreatmentActive;
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

    private void switchPanelMode(PanelMode newMode) {
        if (panelMode == newMode) {
            return;
        }
        stopAssistedBreathing();
        stopCpr();
        cancelDefibrillation();
        clearPreparation();
        panelMode = newMode;
        rebuildTreatmentButtons();
    }

    private void addInfusionButtons(Layout layout, BodyState state) {
        int x = layout.rightX + 10;
        int y = layout.innerY + 6 + 22;
        addInfusionButton(x, y, state, InfusionType.BLOOD_BAG, ModItems.BLOOD_BAG.get());
        addInfusionButton(
                x + TREATMENT_BUTTON_STEP,
                y,
                state,
                InfusionType.SALINE,
                ModItems.SALINE_SOLUTION.get()
        );
    }

    private void addInfusionButton(int x, int y, BodyState state, InfusionType type, Item item) {
        boolean active = inspectingOtherPlayer
                && actorCanAct()
                && !state.canAct()
                && state.lifeState() != BodyLifeState.BRAIN_DEAD
                && !state.hasActiveInfusion()
                && countItem(item) > 0;
        Component tooltip = Component.translatable(
                "screen.superficialtrauma.health.infusion_tooltip",
                Component.translatable(type.translationKey()),
                oneDecimal(type.healingPerPulse()),
                BodyState.INFUSION_DURATION_TICKS / 20L
        );
        MedicalActionButton button = new MedicalActionButton(
                x,
                y,
                item,
                Component.translatable(type.translationKey()),
                tooltip,
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
                Component.translatable("screen.superficialtrauma.health.assisted_breathing"),
                Component.translatable("screen.superficialtrauma.health.assisted_breathing_tooltip"),
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
                () -> defibrillationChargeTooltip(state)
        ));
        defibrillationChargeButton.active = defibrillationHeld
                || canDefibrillate(state, selectedDefibrillationEnergy);
    }

    private void addCprButton(int x, int y, BodyState state) {
        MedicalActionButton button = new MedicalActionButton(
                x,
                y,
                net.minecraft.world.item.Items.RED_DYE,
                Component.translatable("screen.superficialtrauma.health.cpr"),
                Component.translatable("screen.superficialtrauma.health.cpr_tooltip"),
                this::beginCpr
        );
        button.active = canPerformCpr(state);
        medicalActionButtons.add(addRenderableWidget(button));
    }

    private boolean canAssistBreathing(BodyState state) {
        return inspectingOtherPlayer
                && actorCanAct()
                && patientInAssistedBreathingRange()
                && (state.lifeState() == BodyLifeState.INCAPACITATED
                || state.lifeState() == BodyLifeState.AWAKENING
                || state.lifeState() == BodyLifeState.CARDIAC_ARREST
                || state.lifeState() == BodyLifeState.VENTRICULAR_FIBRILLATION)
                && countItem(ModItems.MANUAL_RESUSCITATOR.get()) > 0;
    }

    private boolean canPerformCpr(BodyState state) {
        return inspectingOtherPlayer
                && actorCanAct()
                && patientInAssistedBreathingRange()
                && state.lifeState() == BodyLifeState.CARDIAC_ARREST;
    }

    private boolean canDefibrillate(BodyState state, DefibrillationEnergy energy) {
        return !defibrillationHeld && canContinueDefibrillation(state, energy);
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

    private Component defibrillationChargeTooltip(BodyState state) {
        if (!inspectingOtherPlayer || !actorCanAct()) {
            return Component.translatable("screen.superficialtrauma.health.defibrillation_unavailable");
        }
        if (!actorHasFirstAidSkill()) {
            return Component.translatable("screen.superficialtrauma.health.defibrillation_requires_skill");
        }
        if (state.lifeState() != BodyLifeState.VENTRICULAR_FIBRILLATION) {
            return Component.translatable("screen.superficialtrauma.health.defibrillation_requires_vf");
        }
        if (!patientInAssistedBreathingRange()) {
            return Component.translatable("screen.superficialtrauma.health.defibrillation_requires_range");
        }
        int energy = defibrillatorEnergy();
        if (energy < selectedDefibrillationEnergy.joules()) {
            return Component.translatable(
                    "screen.superficialtrauma.health.defibrillation_requires_energy",
                    selectedDefibrillationEnergy.joules(),
                    energy
            );
        }
        boolean escalationComplete = state.hasAttemptedDefibrillation(DefibrillationEnergy.J150)
                && state.hasAttemptedDefibrillation(DefibrillationEnergy.J200);
        boolean unsafe = selectedDefibrillationEnergy.isUnsafeWithoutEscalation() && !escalationComplete;
        return Component.translatable(
                unsafe
                        ? "screen.superficialtrauma.health.defibrillation_unsafe_tooltip"
                        : "screen.superficialtrauma.health.defibrillation_tooltip",
                selectedDefibrillationEnergy.joules(),
                (int) Math.round(selectedDefibrillationEnergy.successChance() * 100.0D)
        );
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

    private static String attemptedDefibrillationText(BodyState state) {
        List<String> attempted = new ArrayList<>();
        for (DefibrillationEnergy energy : DefibrillationEnergy.values()) {
            if (state.hasAttemptedDefibrillation(energy)) {
                attempted.add(energy.joules() + " J");
            }
        }
        return attempted.isEmpty() ? "-" : String.join(", ", attempted);
    }

    private void addTreatmentButton(
            int x,
            int y,
            int patientEntityId,
            WoundInstance wound,
            TreatmentType type,
            boolean anyTreatmentActive
    ) {
        boolean active = false;
        boolean removal = false;
        Component message = Component.translatable(type.translationKey());
        Component tooltip;
        Runnable onPress = () -> {
        };

        TreatmentProcedure appliedProcedure = TreatmentProcedure.forCovering(wound.covering());
        if (anyTreatmentActive) {
            tooltip = Component.translatable("screen.superficialtrauma.health.treatment_busy_tooltip");
        } else if (preparation != null) {
            if (preparation.matches(patientEntityId, wound.id())
                    && preparation.kind() == PreparationKind.BANDAGE
                    && (type == TreatmentType.MEDICAL_TAPE
                    || type == TreatmentType.SELF_ADHESIVE_BANDAGE)) {
                TreatmentProcedure procedure = TreatmentProcedure.bandageCombination(type);
                active = procedure != null && hasRequiredItems(procedure);
                tooltip = Component.translatable(
                        "screen.superficialtrauma.health.treatment_combo_finish_tooltip",
                        Component.translatable(type.translationKey())
                );
                if (procedure != null) {
                    onPress = () -> submitPreparedTreatment(patientEntityId, wound.id(), procedure);
                }
            } else if (preparation.matches(patientEntityId, wound.id())
                    && preparation.kind() == PreparationKind.DEBRIDEMENT
                    && type == TreatmentType.SALINE_SOLUTION) {
                TreatmentProcedure procedure = TreatmentProcedure.DEBRIDEMENT;
                active = actorHasSurgerySkill()
                        && procedure.isApplicable(wound, TreatmentAction.APPLY)
                        && hasRequiredItems(procedure);
                tooltip = Component.translatable(
                        "screen.superficialtrauma.health.debridement_finish_tooltip",
                        procedure.durationTicks() / 20L
                );
                onPress = () -> submitPreparedTreatment(patientEntityId, wound.id(), procedure);
            } else {
                tooltip = Component.translatable("screen.superficialtrauma.health.treatment_preparation_locked");
            }
        } else if (type == TreatmentType.MEDICAL_GAUZE) {
            TreatmentProcedure procedure = TreatmentProcedure.WOUND_PACKING;
            if (wound.woundPackingApplied()) {
                active = true;
                removal = true;
                message = Component.translatable(TreatmentAction.REMOVE.translationKey(procedure));
                tooltip = Component.translatable(
                        "screen.superficialtrauma.health.treatment_remove_tooltip",
                        message,
                        procedure.durationTicks() / 20L
                );
                onPress = () -> ModNetworking.requestTreatment(
                        patientEntityId,
                        wound.id(),
                        procedure,
                        TreatmentAction.REMOVE
                );
            } else {
                active = procedure.isApplicable(wound, TreatmentAction.APPLY) && hasRequiredItems(procedure);
                tooltip = Component.translatable("screen.superficialtrauma.health.medical_gauze_tooltip");
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
                tooltip = Component.translatable(
                        "screen.superficialtrauma.health.tourniquet_remove_tooltip",
                        procedure.durationTicks() / 20L,
                        formatDuration(wound.tourniquetAccumulatedTicks())
                );
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
                tooltip = skillAvailable
                        ? Component.translatable(
                                "screen.superficialtrauma.health.tourniquet_tooltip",
                                procedure.durationTicks() / 20L
                        )
                        : Component.translatable(
                                "screen.superficialtrauma.health.first_aid_skill_required"
                        );
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
                tooltip = Component.translatable(
                        "screen.superficialtrauma.health.treatment_remove_tooltip",
                        message,
                        appliedProcedure.durationTicks() / 20L
                );
                TreatmentProcedure procedure = appliedProcedure;
                onPress = () -> ModNetworking.requestTreatment(
                        patientEntityId,
                        wound.id(),
                        procedure,
                        TreatmentAction.REMOVE
                );
            } else {
                tooltip = Component.translatable(
                        "screen.superficialtrauma.health.treatment_blocked_by_covering",
                        Component.translatable(wound.covering().translationKey())
                );
            }
        } else {
            switch (type) {
                case TEMPORARY_DRESSING -> {
                    TreatmentProcedure procedure = TreatmentProcedure.TEMPORARY_DRESSING;
                    active = hasRequiredItems(procedure);
                    tooltip = singleTreatmentTooltip(type);
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
                    tooltip = Component.translatable("screen.superficialtrauma.health.bandage_combo_tooltip");
                    onPress = () -> beginPreparation(
                            patientEntityId,
                            wound.id(),
                            PreparationKind.BANDAGE
                    );
                }
                case MEDICAL_TAPE -> tooltip = Component.translatable(
                        "screen.superficialtrauma.health.medical_tape_requires_bandage"
                );
                case SELF_ADHESIVE_BANDAGE -> {
                    TreatmentProcedure procedure = TreatmentProcedure.SELF_ADHESIVE_BANDAGE;
                    active = hasRequiredItems(procedure);
                    tooltip = Component.translatable(
                            "screen.superficialtrauma.health.self_adhesive_bandage_tooltip"
                    );
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
                    tooltip = Component.translatable(
                            "screen.superficialtrauma.health.ice_pack_tooltip",
                            procedure.durationTicks() / 20L
                    );
                    onPress = () -> ModNetworking.requestTreatment(
                            patientEntityId,
                            wound.id(),
                            procedure,
                            TreatmentAction.APPLY
                    );
                }
                case TOURNIQUET -> tooltip = Component.empty();
                case SALINE_SOLUTION -> tooltip = Component.translatable(
                        "screen.superficialtrauma.health.saline_requires_surgical_kit"
                );
                case SURGICAL_KIT -> {
                    TreatmentProcedure procedure = TreatmentProcedure.DEBRIDEMENT;
                    boolean skillAvailable = actorHasSurgerySkill();
                    active = skillAvailable
                            && procedure.isApplicable(wound, TreatmentAction.APPLY)
                            && hasRequiredItems(procedure);
                    if (!skillAvailable) {
                        tooltip = Component.translatable(
                                "screen.superficialtrauma.health.surgery_skill_required"
                        );
                    } else if (!procedure.isApplicable(wound, TreatmentAction.APPLY)) {
                        tooltip = Component.translatable(
                                "screen.superficialtrauma.health.debridement_not_available"
                        );
                    } else {
                        tooltip = Component.translatable(
                                "screen.superficialtrauma.health.debridement_begin_tooltip",
                                procedure.durationTicks() / 20L
                        );
                    }
                    onPress = () -> beginPreparation(
                            patientEntityId,
                            wound.id(),
                            PreparationKind.DEBRIDEMENT
                    );
                }
                default -> tooltip = Component.empty();
            }
        }

        TreatmentItemButton button = new TreatmentItemButton(
                x,
                y,
                type,
                removal,
                message,
                tooltip,
                onPress
        );
        button.active = active;
        treatmentButtons.add(addRenderableWidget(button));
    }

    private Component singleTreatmentTooltip(TreatmentType type) {
        return Component.translatable(
                "screen.superficialtrauma.health.treatment_tooltip",
                Component.translatable(type.translationKey()),
                1,
                type.requiredItem().getDescription()
        );
    }

    private boolean actorHasSurgerySkill() {
        return ClientBodyState.hasReceivedSnapshot() && ClientBodyState.snapshot().hasSurgerySkill();
    }

    private boolean actorHasFirstAidSkill() {
        return ClientBodyState.hasReceivedSnapshot() && ClientBodyState.snapshot().hasFirstAidSkill();
    }

    private static String formatDuration(long ticks) {
        long totalSeconds = Math.max(0L, ticks / 20L);
        return String.format(Locale.ROOT, "%d:%02d", totalSeconds / 60L, totalSeconds % 60L);
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
            case DEBRIDEMENT -> actorHasSurgerySkill()
                    && TreatmentProcedure.DEBRIDEMENT.isApplicable(wound, TreatmentAction.APPLY)
                    && hasRequiredItems(TreatmentProcedure.DEBRIDEMENT);
        };
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

    private List<WoundRow> visibleWoundRows(
            BodyState state,
            int availableHeight,
            int woundAvailableWidth,
            int treatmentAvailableWidth
    ) {
        List<WoundInstance> sorted = sortedWounds(state);
        List<WoundRow> visible = new ArrayList<>();
        int maximumRowsHeight = Math.max(0, availableHeight - 18);
        int usedHeight = 0;
        for (WoundInstance wound : sorted) {
            if (visible.size() >= 5) {
                break;
            }
            int rowHeight = woundRowHeight(wound, woundAvailableWidth, treatmentAvailableWidth);
            if (!visible.isEmpty() && usedHeight + rowHeight > maximumRowsHeight) {
                break;
            }
            visible.add(new WoundRow(wound, rowHeight));
            usedHeight += rowHeight;
        }
        return visible;
    }

    private int woundRowHeight(
            WoundInstance wound,
            int woundAvailableWidth,
            int treatmentAvailableWidth
    ) {
        String tagSummary = woundTagSummary(wound, displayedState().movementBleedingActive());
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
        String promptKey = preparation.kind() == PreparationKind.DEBRIDEMENT
                ? "screen.superficialtrauma.health.debridement_preparation_prompt"
                : "screen.superficialtrauma.health.treatment_preparation_prompt";
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

    public boolean isInspecting(int entityId) {
        return inspectingOtherPlayer && inspectedEntityId == entityId;
    }

    @Override
    public void removed() {
        stopAssistedBreathing();
        stopCpr();
        cancelDefibrillation();
        clearPreparation();
        super.removed();
        if (inspectingOtherPlayer) {
            if (minecraft != null && minecraft.getConnection() != null) {
                ModNetworking.closeInspection(inspectedEntityId);
            }
            ClientInspectionState.clear();
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            releaseDefibrillation();
            stopAssistedBreathing();
            stopCpr();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
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

    private enum PreparationKind {
        BANDAGE,
        DEBRIDEMENT
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
