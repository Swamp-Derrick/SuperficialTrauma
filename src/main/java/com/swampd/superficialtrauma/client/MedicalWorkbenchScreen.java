package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.common.block.MedicalWorkbenchBlockEntity;
import com.swampd.superficialtrauma.common.block.MedicalWorkbenchMenu;
import com.swampd.superficialtrauma.common.crafting.MedicalWorkbenchIngredient;
import com.swampd.superficialtrauma.common.crafting.MedicalWorkbenchRecipe;
import com.swampd.superficialtrauma.common.crafting.MedicalWorkbenchRecipes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class MedicalWorkbenchScreen extends AbstractContainerScreen<MedicalWorkbenchMenu> {
    private static final int IMAGE_WIDTH = 300;
    private static final int IMAGE_HEIGHT = 214;
    private static final int CATALOG_WIDTH = 98;
    private static final int VISIBLE_RECIPE_ROWS = 8;
    private static final int RECIPE_ROW_HEIGHT = 22;
    private static final int RECIPE_LIST_Y = 22;
    private static final int QUEUE_ROW_Y = 59;
    private static final int QUEUE_ROW_HEIGHT = 20;

    private static final int PANEL_COLOR = 0xF0181D24;
    private static final int SUBPANEL_COLOR = 0xFF20272F;
    private static final int PANEL_BORDER_COLOR = 0xFF76808E;
    private static final int ROW_COLOR = 0xFF273039;
    private static final int SELECTED_ROW_COLOR = 0xFF3B493F;
    private static final int FIRST_QUEUE_COLOR = 0xFF493B29;
    private static final int PROGRESS_COLOR = 0xFF5EBC78;
    private static final int SLOT_COLOR = 0xFF27333B;
    private static final int SLOT_BORDER_COLOR = 0xFF66717E;
    private static final int TEXT_COLOR = 0xFFE7ECF2;
    private static final int MUTED_TEXT_COLOR = 0xFFAAB4C0;

    private final Inventory playerInventory;
    private final List<Button> cancelButtons = new ArrayList<>();
    private int selectedRecipeId;
    private int recipeScroll;
    private Button craftButton;

    public MedicalWorkbenchScreen(
            MedicalWorkbenchMenu menu,
            Inventory playerInventory,
            Component title
    ) {
        super(menu, playerInventory, title);
        this.playerInventory = playerInventory;
        imageWidth = IMAGE_WIDTH;
        imageHeight = IMAGE_HEIGHT;
        titleLabelX = 106;
        titleLabelY = 7;
        inventoryLabelY = -1000;
    }

    @Override
    protected void init() {
        super.init();
        craftButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.superficialtrauma.workbench.craft"),
                        button -> sendMenuButton(MedicalWorkbenchMenu.CRAFT_BUTTON_BASE + selectedRecipeId)
                )
                .bounds(leftPos + 124, topPos + 185, 150, 18)
                .build());

        cancelButtons.clear();
        for (int queueIndex = 0; queueIndex < MedicalWorkbenchBlockEntity.QUEUE_LIMIT; queueIndex++) {
            final int capturedIndex = queueIndex;
            Button cancel = addRenderableWidget(Button.builder(
                            Component.literal("×"),
                            button -> sendMenuButton(MedicalWorkbenchMenu.CANCEL_BUTTON_BASE + capturedIndex)
                    )
                    .bounds(
                            leftPos + 278,
                            topPos + QUEUE_ROW_Y + capturedIndex * QUEUE_ROW_HEIGHT + 2,
                            14,
                            14
                    )
                    .build());
            cancelButtons.add(cancel);
        }
        updateButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    private void updateButtons() {
        MedicalWorkbenchRecipe selected = MedicalWorkbenchRecipes.byId(selectedRecipeId);
        if (craftButton != null) {
            boolean queueFull = menu.isQueueFull();
            boolean hasMaterials = MedicalWorkbenchBlockEntity.hasIngredients(playerInventory, selected);
            craftButton.active = selected != null && !queueFull && hasMaterials;
            craftButton.setMessage(Component.translatable(
                    queueFull
                            ? "screen.superficialtrauma.workbench.queue_full"
                            : hasMaterials
                            ? "screen.superficialtrauma.workbench.craft"
                            : "screen.superficialtrauma.workbench.materials_missing"
            ));
        }
        for (int index = 0; index < cancelButtons.size(); index++) {
            cancelButtons.get(index).visible = menu.queuedRecipeId(index) >= 0;
        }
    }

    private void sendMenuButton(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderCustomItemTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL_COLOR);
        drawBorder(graphics, leftPos, topPos, imageWidth, imageHeight, PANEL_BORDER_COLOR);
        graphics.fill(
                leftPos + 5,
                topPos + 5,
                leftPos + CATALOG_WIDTH,
                topPos + imageHeight - 5,
                SUBPANEL_COLOR
        );
        drawBorder(
                graphics,
                leftPos + 5,
                topPos + 5,
                CATALOG_WIDTH - 5,
                imageHeight - 10,
                PANEL_BORDER_COLOR
        );
        graphics.vLine(
                leftPos + CATALOG_WIDTH + 3,
                topPos + 5,
                topPos + imageHeight - 6,
                PANEL_BORDER_COLOR
        );

        renderRecipeCatalog(graphics);
        renderSelectedRecipe(graphics);
        renderQueue(graphics);
        renderIngredients(graphics);

        for (int slotIndex = 0; slotIndex < MedicalWorkbenchBlockEntity.OUTPUT_SLOT_COUNT; slotIndex++) {
            Slot slot = menu.getSlot(slotIndex);
            int slotLeft = leftPos + slot.x - 1;
            int slotTop = topPos + slot.y - 1;
            graphics.fill(slotLeft, slotTop, slotLeft + 18, slotTop + 18, SLOT_COLOR);
            drawBorder(graphics, slotLeft, slotTop, 18, 18, SLOT_BORDER_COLOR);
        }
    }

    private void renderRecipeCatalog(GuiGraphics graphics) {
        List<MedicalWorkbenchRecipe> recipes = MedicalWorkbenchRecipes.all();
        for (int visibleRow = 0; visibleRow < VISIBLE_RECIPE_ROWS; visibleRow++) {
            int recipeIndex = recipeScroll + visibleRow;
            if (recipeIndex >= recipes.size()) {
                break;
            }
            MedicalWorkbenchRecipe recipe = recipes.get(recipeIndex);
            int rowY = topPos + RECIPE_LIST_Y + visibleRow * RECIPE_ROW_HEIGHT;
            graphics.fill(
                    leftPos + 8,
                    rowY,
                    leftPos + CATALOG_WIDTH - 7,
                    rowY + RECIPE_ROW_HEIGHT - 2,
                    recipe.id() == selectedRecipeId ? SELECTED_ROW_COLOR : ROW_COLOR
            );
            ItemStack result = recipe.resultStack();
            graphics.renderItem(result, leftPos + 10, rowY + 2);
            graphics.renderItemDecorations(font, result, leftPos + 10, rowY + 2);
        }
        renderCatalogScrollbar(graphics, recipes.size());
    }

    private void renderCatalogScrollbar(GuiGraphics graphics, int recipeCount) {
        if (recipeCount <= VISIBLE_RECIPE_ROWS) {
            return;
        }
        int trackX = leftPos + CATALOG_WIDTH - 5;
        int trackY = topPos + RECIPE_LIST_Y;
        int trackHeight = VISIBLE_RECIPE_ROWS * RECIPE_ROW_HEIGHT - 2;
        int thumbHeight = Math.max(18, trackHeight * VISIBLE_RECIPE_ROWS / recipeCount);
        int maximumScroll = recipeCount - VISIBLE_RECIPE_ROWS;
        int thumbOffset = (trackHeight - thumbHeight) * recipeScroll / maximumScroll;
        graphics.fill(trackX, trackY, trackX + 2, trackY + trackHeight, 0xFF161C22);
        graphics.fill(
                trackX,
                trackY + thumbOffset,
                trackX + 2,
                trackY + thumbOffset + thumbHeight,
                PANEL_BORDER_COLOR
        );
    }

    private void renderSelectedRecipe(GuiGraphics graphics) {
        MedicalWorkbenchRecipe selected = MedicalWorkbenchRecipes.byId(selectedRecipeId);
        if (selected != null) {
            ItemStack result = selected.resultStack();
            graphics.renderItem(result, leftPos + 110, topPos + 21);
            graphics.renderItemDecorations(font, result, leftPos + 110, topPos + 21);
        }
        graphics.hLine(leftPos + 106, leftPos + imageWidth - 7, topPos + 45, PANEL_BORDER_COLOR);
    }

    private void renderQueue(GuiGraphics graphics) {
        for (int queueIndex = 0; queueIndex < MedicalWorkbenchBlockEntity.QUEUE_LIMIT; queueIndex++) {
            int rowY = topPos + QUEUE_ROW_Y + queueIndex * QUEUE_ROW_HEIGHT;
            int recipeId = menu.queuedRecipeId(queueIndex);
            MedicalWorkbenchRecipe recipe = MedicalWorkbenchRecipes.byId(recipeId);
            graphics.fill(
                    leftPos + 108,
                    rowY,
                    leftPos + 294,
                    rowY + 18,
                    queueIndex == 0 && recipe != null ? FIRST_QUEUE_COLOR : ROW_COLOR
            );
            if (recipe == null) {
                continue;
            }
            int total = Math.max(1, menu.queuedTotalTicks(queueIndex));
            int remaining = Math.max(0, menu.queuedRemainingTicks(queueIndex));
            int progressWidth = Math.round(168.0F * (1.0F - remaining / (float) total));
            if (queueIndex == 0 && progressWidth > 0) {
                graphics.fill(
                        leftPos + 108,
                        rowY + 16,
                        leftPos + 108 + progressWidth,
                        rowY + 18,
                        PROGRESS_COLOR
                );
            }
            ItemStack result = recipe.resultStack();
            graphics.renderItem(result, leftPos + 110, rowY + 1);
            graphics.renderItemDecorations(font, result, leftPos + 110, rowY + 1);
        }
    }

    private void renderIngredients(GuiGraphics graphics) {
        MedicalWorkbenchRecipe selected = MedicalWorkbenchRecipes.byId(selectedRecipeId);
        if (selected == null) {
            return;
        }
        for (int ingredientIndex = 0; ingredientIndex < selected.ingredients().size(); ingredientIndex++) {
            MedicalWorkbenchIngredient ingredient = selected.ingredients().get(ingredientIndex);
            int x = leftPos + 110 + ingredientIndex * 42;
            int y = topPos + 158;
            ItemStack stack = ingredient.displayStack();
            graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_COLOR);
            drawBorder(graphics, x - 1, y - 1, 18, 18, SLOT_BORDER_COLOR);
            graphics.renderItem(stack, x, y);
            graphics.renderItemDecorations(font, stack, x, y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(
                font,
                Component.translatable("screen.superficialtrauma.workbench.catalog"),
                9,
                9,
                TEXT_COLOR,
                false
        );
        graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT_COLOR, false);
        graphics.drawString(
                font,
                Component.translatable("screen.superficialtrauma.workbench.outputs"),
                207,
                8,
                MUTED_TEXT_COLOR,
                false
        );

        List<MedicalWorkbenchRecipe> recipes = MedicalWorkbenchRecipes.all();
        for (int visibleRow = 0; visibleRow < VISIBLE_RECIPE_ROWS; visibleRow++) {
            int recipeIndex = recipeScroll + visibleRow;
            if (recipeIndex >= recipes.size()) {
                break;
            }
            MedicalWorkbenchRecipe recipe = recipes.get(recipeIndex);
            String name = font.plainSubstrByWidth(recipe.resultStack().getHoverName().getString(), 58);
            graphics.drawString(
                    font,
                    name,
                    31,
                    RECIPE_LIST_Y + visibleRow * RECIPE_ROW_HEIGHT + 6,
                    TEXT_COLOR,
                    false
            );
        }

        MedicalWorkbenchRecipe selected = MedicalWorkbenchRecipes.byId(selectedRecipeId);
        if (selected != null) {
            graphics.drawString(
                    font,
                    font.plainSubstrByWidth(selected.resultStack().getHoverName().getString(), 78),
                    132,
                    22,
                    TEXT_COLOR,
                    false
            );
            graphics.drawString(
                    font,
                    Component.translatable(
                            "screen.superficialtrauma.workbench.duration",
                            formatTime(selected.craftTimeTicks())
                    ),
                    132,
                    34,
                    MUTED_TEXT_COLOR,
                    false
            );
        }

        graphics.drawString(
                font,
                Component.translatable("screen.superficialtrauma.workbench.queue"),
                108,
                49,
                TEXT_COLOR,
                false
        );
        for (int queueIndex = 0; queueIndex < MedicalWorkbenchBlockEntity.QUEUE_LIMIT; queueIndex++) {
            MedicalWorkbenchRecipe queued = MedicalWorkbenchRecipes.byId(menu.queuedRecipeId(queueIndex));
            if (queued == null) {
                continue;
            }
            int rowY = QUEUE_ROW_Y + queueIndex * QUEUE_ROW_HEIGHT;
            graphics.drawString(
                    font,
                    font.plainSubstrByWidth(queued.resultStack().getHoverName().getString(), 82),
                    130,
                    rowY + 5,
                    TEXT_COLOR,
                    false
            );
            Component time = queueIndex == 0 && menu.queuedRemainingTicks(queueIndex) <= 0
                    ? Component.translatable("screen.superficialtrauma.workbench.output_blocked")
                    : Component.literal(formatTime(menu.queuedRemainingTicks(queueIndex)));
            graphics.drawString(font, time, 218, rowY + 5, MUTED_TEXT_COLOR, false);
        }

        graphics.drawString(
                font,
                Component.translatable("screen.superficialtrauma.workbench.ingredients"),
                108,
                145,
                TEXT_COLOR,
                false
        );
    }

    private void renderCustomItemTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int visibleRecipe = recipeAt(mouseX, mouseY);
        if (visibleRecipe >= 0) {
            graphics.renderTooltip(font, MedicalWorkbenchRecipes.all().get(visibleRecipe).resultStack(), mouseX, mouseY);
            return;
        }

        MedicalWorkbenchRecipe selected = MedicalWorkbenchRecipes.byId(selectedRecipeId);
        if (selected == null) {
            return;
        }
        if (inside(mouseX, mouseY, leftPos + 110, topPos + 21, 16, 16)) {
            graphics.renderTooltip(font, selected.resultStack(), mouseX, mouseY);
            return;
        }
        for (int index = 0; index < selected.ingredients().size(); index++) {
            int x = leftPos + 110 + index * 42;
            int y = topPos + 158;
            if (inside(mouseX, mouseY, x, y, 16, 16)) {
                graphics.renderTooltip(font, selected.ingredients().get(index).displayStack(), mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int recipeIndex = recipeAt(mouseX, mouseY);
            if (recipeIndex >= 0) {
                selectedRecipeId = MedicalWorkbenchRecipes.all().get(recipeIndex).id();
                updateButtons();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (inside(
                mouseX,
                mouseY,
                leftPos + 6,
                topPos + RECIPE_LIST_Y,
                CATALOG_WIDTH - 12,
                VISIBLE_RECIPE_ROWS * RECIPE_ROW_HEIGHT
        )) {
            int maximumScroll = Math.max(0, MedicalWorkbenchRecipes.all().size() - VISIBLE_RECIPE_ROWS);
            recipeScroll = Mth.clamp(recipeScroll - (int) Math.signum(delta), 0, maximumScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private int recipeAt(double mouseX, double mouseY) {
        if (!inside(
                mouseX,
                mouseY,
                leftPos + 8,
                topPos + RECIPE_LIST_Y,
                CATALOG_WIDTH - 15,
                VISIBLE_RECIPE_ROWS * RECIPE_ROW_HEIGHT
        )) {
            return -1;
        }
        int visibleRow = (int) (mouseY - topPos - RECIPE_LIST_Y) / RECIPE_ROW_HEIGHT;
        int recipeIndex = recipeScroll + visibleRow;
        return recipeIndex >= 0 && recipeIndex < MedicalWorkbenchRecipes.all().size()
                ? recipeIndex
                : -1;
    }

    private static String formatTime(int ticks) {
        int seconds = Math.max(0, (ticks + 19) / 20);
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private static boolean inside(
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height
    ) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static void drawBorder(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        graphics.hLine(x, x + width - 1, y, color);
        graphics.hLine(x, x + width - 1, y + height - 1, color);
        graphics.vLine(x, y, y + height - 1, color);
        graphics.vLine(x + width - 1, y, y + height - 1, color);
    }
}
