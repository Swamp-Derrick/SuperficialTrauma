package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.common.body.BodyState;
import com.swampd.superficialtrauma.common.damage.DamageWindow;
import com.swampd.superficialtrauma.common.wound.WoundInstance;
import com.swampd.superficialtrauma.common.wound.WoundTag;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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

    public HealthScreen() {
        super(Component.translatable("screen.superficialtrauma.health.title"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

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
        int rightWidth = panelWidth - 16 - leftWidth - middleWidth - gap * 2;

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, BACKGROUND_COLOR);
        drawBorder(graphics, panelX, panelY, panelWidth, panelHeight, BORDER_COLOR);
        graphics.drawCenteredString(font, title, panelX + panelWidth / 2, panelY + 10, TITLE_COLOR);

        int middleX = innerX + leftWidth + gap;
        int rightX = middleX + middleWidth + gap;
        drawColumn(graphics, innerX, innerY, leftWidth, innerHeight);
        drawColumn(graphics, middleX, innerY, middleWidth, innerHeight);
        drawColumn(graphics, rightX, innerY, rightWidth, innerHeight);

        if (!ClientBodyState.hasReceivedSnapshot()) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("screen.superficialtrauma.health.loading"),
                    panelX + panelWidth / 2,
                    panelY + panelHeight / 2,
                    TEXT_COLOR
            );
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        BodyState state = ClientBodyState.snapshot();
        int contentHeight = innerHeight - 12;
        drawWholeBodyColumn(graphics, state, innerX + 6, innerY + 6, leftWidth - 12);
        WoundInstance selected = drawWoundColumn(
                graphics,
                state,
                middleX + 6,
                innerY + 6,
                middleWidth - 12,
                contentHeight
        );
        drawActionColumn(
                graphics,
                selected,
                rightX + 6,
                innerY + 6,
                rightWidth - 12,
                contentHeight
        );

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawWholeBodyColumn(GuiGraphics graphics, BodyState state, int x, int y, int availableWidth) {
        graphics.drawString(font, Component.translatable("screen.superficialtrauma.health.whole_body"), x, y, TITLE_COLOR, false);
        int lineY = y + 16;
        float currentHealth = minecraft != null && minecraft.player != null ? minecraft.player.getHealth() : 0.0F;
        float maximumHealth = minecraft != null && minecraft.player != null ? minecraft.player.getMaxHealth() : 0.0F;

        lineY = drawValue(graphics, x, lineY, availableWidth, "screen.superficialtrauma.health.vanilla_health",
                oneDecimal(currentHealth) + "/" + oneDecimal(maximumHealth), GOOD_COLOR);
        lineY = drawValue(graphics, x, lineY, availableWidth, "screen.superficialtrauma.health.life_state",
                Component.translatable("life_state.superficialtrauma." + state.lifeState().serializedName()).getString(), TEXT_COLOR);
        lineY = drawValue(graphics, x, lineY, availableWidth, "screen.superficialtrauma.health.pain",
                oneDecimal(Math.min(state.pain(), 20.0F)) + "/20", state.pain() >= 20.0F ? DANGER_COLOR : TEXT_COLOR);
        lineY = drawValue(graphics, x, lineY, availableWidth, "screen.superficialtrauma.health.infection",
                oneDecimal(state.infection()), TEXT_COLOR);
        lineY = drawValue(graphics, x, lineY, availableWidth, "screen.superficialtrauma.health.drug",
                oneDecimal(state.bloodDrugConcentration()), TEXT_COLOR);
        lineY = drawValue(graphics, x, lineY, availableWidth, "screen.superficialtrauma.health.oxygen",
                oneDecimal(state.bloodOxygen()) + "/30", TEXT_COLOR);

        lineY += 5;
        graphics.drawString(font, Component.translatable("screen.superficialtrauma.health.stage0_debug"), x, lineY, MUTED_COLOR, false);
        lineY += 13;
        lineY = drawValue(graphics, x, lineY, availableWidth, "screen.superficialtrauma.health.last_damage",
                oneDecimal(state.lastFinalDamage()), WARN_COLOR);
        lineY = drawValue(graphics, x, lineY, availableWidth, "screen.superficialtrauma.health.damage_type",
                state.lastDamageType(), MUTED_COLOR);
        lineY = drawValue(graphics, x, lineY, availableWidth, "screen.superficialtrauma.health.damage_classification",
                Component.translatable(state.lastDamageKind().translationKey()).getString(), WARN_COLOR);
        if (!"none".equals(state.lastAmmoId())) {
            lineY = drawValue(graphics, x, lineY, availableWidth, "screen.superficialtrauma.health.ammo",
                    compactIdentifier(state.lastAmmoId()), MUTED_COLOR);
        }
        drawValue(graphics, x, lineY, availableWidth, "screen.superficialtrauma.health.data_version",
                Integer.toString(state.dataVersion()), MUTED_COLOR);
    }

    private WoundInstance drawWoundColumn(
            GuiGraphics graphics,
            BodyState state,
            int x,
            int y,
            int availableWidth,
            int availableHeight
    ) {
        graphics.drawString(font, Component.translatable("screen.superficialtrauma.health.wounds"), x, y, TITLE_COLOR, false);

        List<WoundInstance> sortedWounds = new ArrayList<>(state.wounds());
        sortedWounds.sort(Comparator
                .comparingInt(WoundInstance::severity).reversed()
                .thenComparing(Comparator.comparingLong(WoundInstance::createdGameTime).reversed()));

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

        int maximumVisibleCards = Math.max(1, (availableHeight - 18) / 43);
        int count = Math.min(Math.min(5, maximumVisibleCards), sortedWounds.size());
        for (int i = 0; i < count; i++) {
            WoundInstance wound = sortedWounds.get(i);
            int cardColor = wound.severity() >= 3 ? 0xAA4C2529 : wound.severity() == 2 ? 0xAA4A3C24 : 0xAA263D31;
            graphics.fill(x, cardY, x + availableWidth, cardY + 40, cardColor);
            drawBorder(graphics, x, cardY, availableWidth, 40, wound.severity() >= 3 ? DANGER_COLOR : BORDER_COLOR);
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
            String tagSummary = woundTagSummary(wound);
            if (!tagSummary.isEmpty()) {
                graphics.drawString(
                        font,
                        font.plainSubstrByWidth(tagSummary, Math.max(20, availableWidth - 8)),
                        x + 4,
                        cardY + 28,
                        MUTED_COLOR,
                        false
                );
            }
            cardY += 43;
        }

        int columnBottom = y + availableHeight;
        if (sortedWounds.size() > count && cardY + font.lineHeight <= columnBottom) {
            Component moreWounds = Component.translatable(
                    "screen.superficialtrauma.health.more_wounds",
                    sortedWounds.size() - count
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
        return sortedWounds.isEmpty() ? null : sortedWounds.get(0);
    }

    private int drawPendingWindow(
            GuiGraphics graphics,
            DamageWindow window,
            int x,
            int pendingY,
            int availableWidth,
            int columnBottom
    ) {
        if (pendingY + font.lineHeight > columnBottom) {
            return pendingY;
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
                pendingY,
            MUTED_COLOR,
            false
        );
        return pendingY + 13;
    }

    private String woundTagSummary(WoundInstance wound) {
        List<String> labels = new ArrayList<>();
        for (WoundTag tag : wound.woundTags()) {
            labels.add(Component.translatable("wound_tag.superficialtrauma." + tag.serializedName()).getString());
        }
        return String.join(" · ", labels);
    }

    private void drawActionColumn(
            GuiGraphics graphics,
            WoundInstance selected,
            int x,
            int y,
            int availableWidth,
            int availableHeight
    ) {
        graphics.drawString(font, Component.translatable("screen.superficialtrauma.health.actions"), x, y, TITLE_COLOR, false);
        int lineY = y + 17;
        Component closeHint = Component.translatable("screen.superficialtrauma.health.close_hint");
        List<FormattedCharSequence> footerLines = font.split(closeHint, Math.max(20, availableWidth));
        int footerHeight = footerLines.size() * 11;
        int footerY = Math.max(lineY, y + availableHeight - footerHeight);
        int contentBottom = footerY - 5;

        if (selected == null) {
            drawWrappedWithin(
                    graphics,
                    Component.translatable("screen.superficialtrauma.health.no_action_needed"),
                    x,
                    lineY,
                    availableWidth,
                    GOOD_COLOR,
                    contentBottom
            );
        } else {
            graphics.drawString(font, Component.translatable(selected.displayTranslationKey()), x, lineY, TEXT_COLOR, false);
            lineY += 15;
            float healingRate = selected.baseHealingPerSecond();
            Component healingStatus = healingRate > 0.0F
                    ? Component.translatable(
                            "screen.superficialtrauma.health.natural_healing_active",
                            oneDecimal(healingRate)
                    )
                    : Component.translatable("screen.superficialtrauma.health.natural_healing_stopped");
            lineY = drawWrappedWithin(
                    graphics,
                    healingStatus,
                    x,
                    lineY,
                    availableWidth,
                    healingRate > 0.0F ? GOOD_COLOR : DANGER_COLOR,
                    contentBottom
            );
            lineY += 5;
            Component recommendation = Component.translatable(
                    "screen.superficialtrauma.health.recommend_"
                            + selected.type().serializedName()
                            + "_"
                            + selected.severity()
            );
            lineY = drawWrappedWithin(
                    graphics,
                    recommendation,
                    x,
                    lineY,
                    availableWidth,
                    WARN_COLOR,
                    contentBottom
            );
            lineY += 7;
            drawWrappedWithin(
                    graphics,
                    Component.translatable("screen.superficialtrauma.health.actions_placeholder"),
                    x,
                    lineY,
                    availableWidth,
                    MUTED_COLOR,
                    contentBottom
            );
        }

        int footerLineY = footerY;
        for (FormattedCharSequence footerLine : footerLines) {
            graphics.drawString(font, footerLine, x, footerLineY, MUTED_COLOR, false);
            footerLineY += 11;
        }
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

    private static String oneDecimal(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
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

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
