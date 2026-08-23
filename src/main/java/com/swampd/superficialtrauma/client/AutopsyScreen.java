package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.common.body.DowningHitRecord;
import com.swampd.superficialtrauma.common.body.WoundHistoryEntry;
import com.swampd.superficialtrauma.common.forensics.AutopsyAction;
import com.swampd.superficialtrauma.common.forensics.AutopsyReport;
import com.swampd.superficialtrauma.common.init.ModItems;
import com.swampd.superficialtrauma.common.qte.TimingQteSnapshot;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

public final class AutopsyScreen extends Screen {
    private static final int BACKGROUND_COLOR = 0xE8171C22;
    private static final int PANEL_COLOR = 0xF020262E;
    private static final int BORDER_COLOR = 0xFF76808E;
    private static final int TEXT_COLOR = 0xFFE7ECF2;
    private static final int MUTED_COLOR = 0xFFAEB8C5;
    private static final int ACCENT_COLOR = 0xFFE3B55D;
    private static final int SUCCESS_COLOR = 0xFF7DDC9A;
    private static final int QTE_TRACK_COLOR = 0xFF59626E;
    private static final int QTE_PERFECT_COLOR = 0xFFFFC94D;
    private static final int QTE_CURSOR_COLOR = 0xFFFFFFFF;
    private static final int QTE_SEGMENTS = 96;

    private AutopsyReport report;
    private Button penlightButton;
    private Button checklistButton;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int informationWidth;
    private int operationsLeft;
    private int operationsWidth;

    public AutopsyScreen(AutopsyReport report) {
        super(Component.translatable("screen.superficialtrauma.autopsy.title", report.ownerName()));
        this.report = report;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(980, Math.max(300, width - 24));
        panelHeight = Math.min(560, Math.max(220, height - 24));
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;
        operationsWidth = Math.max(140, panelWidth / 3);
        informationWidth = panelWidth - operationsWidth - 12;
        operationsLeft = panelLeft + informationWidth + 12;

        int buttonX = operationsLeft + 14;
        int buttonWidth = operationsWidth - 28;
        penlightButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.superficialtrauma.autopsy.penlight_button"),
                        button -> ModNetworking.requestAutopsyAction(report.corpseEntityId(), AutopsyAction.PENLIGHT)
                )
                .bounds(buttonX, panelTop + 92, buttonWidth, 28)
                .build());
        checklistButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.superficialtrauma.autopsy.checklist_button"),
                        button -> ModNetworking.requestAutopsyAction(report.corpseEntityId(), AutopsyAction.CHECKLIST)
                )
                .bounds(buttonX, panelTop + 134, buttonWidth, 28)
                .build());
        refreshButtons();
    }

    public boolean isInspecting(int corpseEntityId) {
        return report.corpseEntityId() == corpseEntityId;
    }

    public void applyReport(AutopsyReport updated) {
        if (updated.corpseEntityId() != report.corpseEntityId()) {
            return;
        }
        report = updated;
        refreshButtons();
    }

    @Override
    public void tick() {
        super.tick();
        ClientTimingQteState.tick();
        refreshButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, BACKGROUND_COLOR);
        drawPanel(graphics, panelLeft, panelTop, informationWidth, panelHeight);
        drawPanel(graphics, operationsLeft, panelTop, operationsWidth, panelHeight);

        graphics.drawCenteredString(
                font,
                Component.translatable("screen.superficialtrauma.autopsy.title", report.ownerName()),
                panelLeft + informationWidth / 2,
                panelTop + 14,
                TEXT_COLOR
        );
        renderInformation(graphics);
        renderOperations(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.renderItem(new ItemStack(ModItems.PUPIL_PENLIGHT.get()), operationsLeft + 20, panelTop + 98);
        graphics.renderItem(new ItemStack(ModItems.CHECKLIST.get()), operationsLeft + 20, panelTop + 140);
        renderTimingQte(graphics, partialTick);
        renderQteFailureFlash(graphics);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE && ClientTimingQteState.press()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        if (minecraft != null && minecraft.getConnection() != null) {
            ModNetworking.closeAutopsy(report.corpseEntityId());
        }
        ClientTimingQteState.clear();
        super.removed();
    }

    private void renderTimingQte(GuiGraphics graphics, float partialTick) {
        TimingQteSnapshot qte = ClientTimingQteState.active();
        if (qte == null) {
            return;
        }

        int radius = Math.min(58, Math.max(34, Math.min(width, height) / 7));
        int boxHalfWidth = radius + 34;
        int boxHalfHeight = radius + 30;
        int centerX = width / 2;
        int centerY = height / 2 + 4;
        graphics.fill(
                centerX - boxHalfWidth,
                centerY - boxHalfHeight,
                centerX + boxHalfWidth,
                centerY + boxHalfHeight,
                0xF0181D23
        );
        drawPanelBorder(
                graphics,
                centerX - boxHalfWidth,
                centerY - boxHalfHeight,
                boxHalfWidth * 2,
                boxHalfHeight * 2
        );

        drawQteRing(graphics, centerX, centerY, radius, qte);

        float progress = Mth.clamp(ClientTimingQteState.progress(partialTick), 0.0F, 1.0F);
        double cursorAngle = -Math.PI / 2.0D + progress * Math.PI * 2.0D;
        for (int step = -10; step <= 8; step += 2) {
            int cursorX = centerX + (int) Math.round(Math.cos(cursorAngle) * (radius + step));
            int cursorY = centerY + (int) Math.round(Math.sin(cursorAngle) * (radius + step));
            graphics.fill(cursorX - 1, cursorY - 1, cursorX + 2, cursorY + 2, QTE_CURSOR_COLOR);
        }

        graphics.drawCenteredString(
                font,
                Component.translatable("screen.superficialtrauma.qte.space"),
                centerX,
                centerY - font.lineHeight / 2,
                TEXT_COLOR
        );
    }

    private void renderQteFailureFlash(GuiGraphics graphics) {
        float strength = ClientTimingQteState.failureFlashStrength();
        if (strength <= 0.0F) {
            return;
        }
        int thickness = Math.max(10, Math.min(22, Math.min(width, height) / 24));
        for (int inset = 0; inset < thickness; inset++) {
            float inwardFade = 1.0F - inset / (float) thickness;
            int alpha = Mth.clamp((int) (72.0F * strength * inwardFade), 0, 72);
            int color = alpha << 24 | 0x00E02020;
            graphics.fill(0, inset, width, inset + 1, color);
            graphics.fill(0, height - inset - 1, width, height - inset, color);
            graphics.fill(inset, 0, inset + 1, height, color);
            graphics.fill(width - inset - 1, 0, width - inset, height, color);
        }
    }

    private void drawQteRing(
            GuiGraphics graphics,
            int centerX,
            int centerY,
            int radius,
            TimingQteSnapshot qte
    ) {
        for (int index = 0; index < QTE_SEGMENTS; index++) {
            float fraction = index / (float) QTE_SEGMENTS;
            int color = QTE_TRACK_COLOR;
            if (fraction >= qte.perfectStart() && fraction < qte.normalStart()) {
                color = QTE_PERFECT_COLOR;
            } else if (fraction >= qte.normalStart() && fraction < qte.successEnd()) {
                color = SUCCESS_COLOR;
            }
            double angle = -Math.PI / 2.0D + fraction * Math.PI * 2.0D;
            int x = centerX + (int) Math.round(Math.cos(angle) * radius);
            int y = centerY + (int) Math.round(Math.sin(angle) * radius);
            graphics.fill(x - 2, y - 2, x + 3, y + 3, color);
        }
    }

    private static void drawPanelBorder(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.hLine(x, x + width - 1, y, BORDER_COLOR);
        graphics.hLine(x, x + width - 1, y + height - 1, BORDER_COLOR);
        graphics.vLine(x, y, y + height - 1, BORDER_COLOR);
        graphics.vLine(x + width - 1, y, y + height - 1, BORDER_COLOR);
    }

    private void renderInformation(GuiGraphics graphics) {
        int x = panelLeft + 14;
        int y = panelTop + 44;
        int textWidth = informationWidth - 28;
        graphics.drawString(font, Component.translatable("screen.superficialtrauma.autopsy.information"), x, y, TEXT_COLOR, false);
        y += 20;

        Component deathTime = report.deathAgeTicks() < 0L
                ? Component.translatable("screen.superficialtrauma.autopsy.death_time_unknown")
                : deathTimeComponent(report.deathAgeTicks());
        y = drawWrapped(graphics, deathTime, x, y, textWidth, report.deathAgeTicks() < 0L ? MUTED_COLOR : SUCCESS_COLOR);
        y += 9;

        graphics.drawString(font, Component.translatable("screen.superficialtrauma.autopsy.visible_findings"), x, y, TEXT_COLOR, false);
        y += 15;
        List<WoundHistoryEntry> wounds = report.visibleWounds();
        if (wounds.isEmpty()) {
            y = drawWrapped(
                    graphics,
                    Component.translatable("screen.superficialtrauma.autopsy.no_wounds"),
                    x,
                    y,
                    textWidth,
                    MUTED_COLOR
            );
        } else {
            for (int index = 0; index < wounds.size(); index++) {
                WoundHistoryEntry entry = wounds.get(index);
                String prefixKey = index < 2
                        ? "screen.superficialtrauma.autopsy.recent_wound"
                        : "screen.superficialtrauma.autopsy.older_wound";
                Component woundName = Component.translatable(
                        "wound.superficialtrauma." + entry.type().serializedName() + ".severity_" + entry.severity()
                );
                Component line = Component.translatable(
                        prefixKey,
                        index < 2 ? index + 1 : index - 1,
                        woundName,
                        Component.translatable(entry.healed()
                                ? "screen.superficialtrauma.autopsy.healed_trace"
                                : "screen.superficialtrauma.autopsy.unhealed_trace")
                );
                y = drawWrapped(graphics, line, x, y, textWidth, index < 2 ? ACCENT_COLOR : MUTED_COLOR);
                y += 5;
            }
        }

        y += 5;
        if (report.detailedAutopsyRevealed()) {
            y = renderDowningEvidence(
                    graphics,
                    x,
                    y,
                    textWidth,
                    report.downingHit(),
                    report.administrativeDeath(),
                    report.organophosphatePoisoningDeath()
            );
            if (report.drowningDeath()) {
                y += 9;
                y = drawWrapped(
                        graphics,
                        Component.translatable("screen.superficialtrauma.autopsy.drowning_cause_of_death"),
                        x,
                        y,
                        textWidth,
                        ACCENT_COLOR
                );
            }
            if (report.suspectedMyocardialInfarction()) {
                y += 9;
                drawWrapped(
                        graphics,
                        Component.translatable(
                                "screen.superficialtrauma.autopsy.suspected_myocardial_infarction"
                        ),
                        x,
                        y,
                        textWidth,
                        ACCENT_COLOR
                );
            }
        } else {
            drawWrapped(
                    graphics,
                    Component.translatable("screen.superficialtrauma.autopsy.details_locked"),
                    x,
                    y,
                    textWidth,
                    MUTED_COLOR
            );
        }
    }

    private int renderDowningEvidence(
            GuiGraphics graphics,
            int x,
            int y,
            int textWidth,
            DowningHitRecord hit,
            boolean administrativeDeath,
            boolean organophosphatePoisoningDeath
    ) {
        graphics.drawString(font, Component.translatable("screen.superficialtrauma.autopsy.downing_evidence"), x, y, TEXT_COLOR, false);
        y += 15;
        if (administrativeDeath) {
            return drawWrapped(
                    graphics,
                    Component.translatable("screen.superficialtrauma.autopsy.no_explanatory_fatal_trauma"),
                    x,
                    y,
                    textWidth,
                    ACCENT_COLOR
            );
        }
        if (organophosphatePoisoningDeath) {
            return drawWrapped(
                    graphics,
                    Component.translatable(
                            "screen.superficialtrauma.autopsy.organophosphate_downing_cause"
                    ),
                    x,
                    y,
                    textWidth,
                    ACCENT_COLOR
            );
        }
        if (hit == null) {
            return drawWrapped(
                    graphics,
                    Component.translatable("screen.superficialtrauma.autopsy.downing_unknown"),
                    x,
                    y,
                    textWidth,
                    MUTED_COLOR
            );
        }
        Component weapon = weaponComponent(hit);
        y = drawWrapped(
                graphics,
                Component.translatable("screen.superficialtrauma.autopsy.downing_weapon", weapon),
                x,
                y,
                textWidth,
                ACCENT_COLOR
        );
        if (hit.isRanged() && hit.hasKnownDistance()) {
            y += 4;
            y = drawWrapped(
                    graphics,
                    Component.translatable(
                            "screen.superficialtrauma.autopsy.shot_distance",
                            String.format(Locale.ROOT, "%.1f", hit.attackerDistance())
                    ),
                    x,
                    y,
                    textWidth,
                    ACCENT_COLOR
            );
        }
        return y;
    }

    private void renderOperations(GuiGraphics graphics) {
        int x = operationsLeft + 14;
        int textWidth = operationsWidth - 28;
        graphics.drawCenteredString(
                font,
                Component.translatable("screen.superficialtrauma.autopsy.operations"),
                operationsLeft + operationsWidth / 2,
                panelTop + 44,
                TEXT_COLOR
        );
        int statusY = panelTop + 178;
        if (report.activeAction() != AutopsyAction.NONE) {
            long remainingTicks = minecraft == null || minecraft.level == null
                    ? 0L
                    : Math.max(0L, report.actionEndGameTime() - minecraft.level.getGameTime());
            drawWrapped(
                    graphics,
                    Component.translatable(
                            "screen.superficialtrauma.autopsy.action_progress",
                            Component.translatable("autopsy_action.superficialtrauma." + report.activeAction().serializedName()),
                            String.format(Locale.ROOT, "%.1f", remainingTicks / 20.0D)
                    ),
                    x,
                    statusY,
                    textWidth,
                    ACCENT_COLOR
            );
        } else {
            long cooldownTicks = penlightCooldownRemainingTicks();
            if (cooldownTicks > 0L) {
                drawWrapped(
                        graphics,
                        Component.translatable(
                                "screen.superficialtrauma.autopsy.penlight_cooldown",
                                (cooldownTicks + 19L) / 20L
                        ),
                        x,
                        statusY,
                        textWidth,
                        MUTED_COLOR
                );
            }
        }
    }

    private void refreshButtons() {
        if (penlightButton == null || checklistButton == null) {
            return;
        }
        boolean idle = report.activeAction() == AutopsyAction.NONE;
        long cooldownTicks = penlightCooldownRemainingTicks();
        penlightButton.setMessage(cooldownTicks > 0L
                ? Component.translatable(
                        "screen.superficialtrauma.autopsy.penlight_button_cooldown",
                        (cooldownTicks + 19L) / 20L
                )
                : Component.translatable("screen.superficialtrauma.autopsy.penlight_button"));
        penlightButton.active = idle
                && report.examinerKnowsForensics()
                && report.examinerHasPenlight()
                && cooldownTicks <= 0L;
        checklistButton.active = idle
                && report.examinerKnowsForensics()
                && report.examinerHasChecklist()
                && !report.detailedAutopsyRevealed();
    }

    private long penlightCooldownRemainingTicks() {
        if (report.penlightCooldownEndGameTime() < 0L) {
            return 0L;
        }
        long gameTime = minecraft == null || minecraft.level == null
                ? report.penlightCooldownEndGameTime()
                : minecraft.level.getGameTime();
        return Math.max(0L, report.penlightCooldownEndGameTime() - gameTime);
    }

    private Component weaponComponent(DowningHitRecord hit) {
        if (!"none".equals(hit.weaponId())) {
            ResourceLocation id = ResourceLocation.tryParse(hit.weaponId());
            Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
            if (item != null && item != Items.AIR) {
                return new ItemStack(item).getHoverName();
            }
            return Component.literal(hit.weaponId());
        }
        return Component.translatable("damage_kind.superficialtrauma." + hit.damageKind().serializedName());
    }

    private Component deathTimeComponent(long ageTicks) {
        long wholeMinutes = ageTicks / (20L * 60L);
        return wholeMinutes < 1L
                ? Component.translatable("screen.superficialtrauma.autopsy.death_time_under_minute")
                : Component.translatable("screen.superficialtrauma.autopsy.death_time_minutes", wholeMinutes);
    }

    private int drawWrapped(
            GuiGraphics graphics,
            Component text,
            int x,
            int y,
            int maximumWidth,
            int color
    ) {
        List<FormattedCharSequence> lines = font.split(text, maximumWidth);
        for (FormattedCharSequence line : lines) {
            graphics.drawString(font, line, x, y, color, false);
            y += font.lineHeight + 2;
        }
        return y;
    }

    private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, PANEL_COLOR);
        graphics.hLine(x, x + width - 1, y, BORDER_COLOR);
        graphics.hLine(x, x + width - 1, y + height - 1, BORDER_COLOR);
        graphics.vLine(x, y, y + height - 1, BORDER_COLOR);
        graphics.vLine(x + width - 1, y, y + height - 1, BORDER_COLOR);
    }
}
