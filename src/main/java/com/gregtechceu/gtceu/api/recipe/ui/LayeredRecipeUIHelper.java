package com.gregtechceu.gtceu.api.recipe.ui;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.editor.IEditableUI;
import com.gregtechceu.gtceu.api.gui.widget.LabelWidget;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.LayeredRecipeHelper;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.recipe.category.GTRecipeCategory;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.integration.emi.recipe.GTRecipeEMICategory;
import com.gregtechceu.gtceu.integration.jei.recipe.GTRecipeJEICategory;
import com.gregtechceu.gtceu.integration.rei.recipe.GTRecipeREICategory;
import com.gregtechceu.gtceu.integration.xei.widgets.GTRecipeWidget;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.layout.Align;
import com.lowdragmc.lowdraglib.jei.JEIPlugin;

import net.minecraft.network.chat.Component;

import com.google.common.collect.Tables;
import dev.emi.emi.api.EmiApi;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.lowdragmc.lowdraglib.gui.texture.ProgressTexture.FillDirection.LEFT_TO_RIGHT;

public class LayeredRecipeUIHelper {

    private static final int LAYERED_GUI_WIDTH = 164;
    private static final int LAYERED_GUI_HEIGHT = 128;

    public static GTRecipeTypeUI createRecipeUI(GTRecipeType recipeType) {
        var ui = new GTRecipeTypeUI(recipeType) {

            @Override
            public IEditableUI<WidgetGroup, RecipeHolder> createEditableUITemplate(boolean isSteam,
                                                                                   boolean isHighPressure) {
                return new IEditableUI.Normal<>(
                        () -> new WidgetGroup(0, 0, LAYERED_GUI_WIDTH, LAYERED_GUI_HEIGHT),
                        (container, recipeHolder) -> {});
            }
        };
        ui.setUiBuilder(LayeredRecipeUIHelper::buildLayeredUI);
        return ui;
    }

    public static void buildLayeredUI(GTRecipe rootRecipe, WidgetGroup widgetGroup) {
        var recipeWidget = (GTRecipeWidget) widgetGroup;

        var group = (WidgetGroup) recipeWidget.getFirstWidgetById(GTRecipeWidget.RECIPE_CONTENT_GROUP_ID_REGEX);
        assert group != null;
        group.clearAllWidgets();

        new UIBuilder(recipeWidget, group, rootRecipe).build();
    }

    private static class UIBuilder {

        private final GTRecipeWidget recipeWidget;
        private final WidgetGroup group;
        private final GTRecipe rootRecipe;
        private final GTRecipeTypeUI recipeUI;

        UIBuilder(GTRecipeWidget recipeWidget, WidgetGroup group, GTRecipe rootRecipe) {
            this.recipeWidget = recipeWidget;
            this.group = group;
            this.rootRecipe = rootRecipe;
            recipeUI = rootRecipe.recipeType.getRecipeUI();
        }

        private void build() {
            var steps = LayeredRecipeHelper.getLayeredSteps(rootRecipe);
            if (steps == null) return;

            var centerX = LAYERED_GUI_WIDTH / 2f;
            var sliceWidth = 20f;
            var startX = centerX - (steps.size() / 2f) * sliceWidth + 1f;
            var inputsHeight = 0;

            for (var recipeStep = 0; recipeStep < steps.size(); recipeStep++) {
                var posX = startX + recipeStep * sliceWidth;
                var stepLayer = steps.get(recipeStep);
                var recipe = stepLayer.recipe();
                var inputHeight = buildStepInput((int) posX, recipeStep, getRecipeHolder(recipe), recipe,
                        stepLayer.timeout());
                if (inputHeight > inputsHeight) inputsHeight = inputHeight;
            }

            var bottomY = inputsHeight + 2;

            var progressBarTexture = new ProgressTexture(GuiTextures.PROGRESS_BAR_ARROW.getSubTexture(0, 0, 1, 0.5),
                    GuiTextures.PROGRESS_BAR_ARROW.getSubTexture(0, 0.5, 1, 0.5))
                    .setFillDirection(LEFT_TO_RIGHT);
            progressBarTexture.rotate(90f);
            var progressWidget = new ProgressWidget(ProgressWidget.JEIProgress, (int) (centerX - 10f), bottomY, 20, 20,
                    progressBarTexture);
            group.addWidget(progressWidget);

            progressWidget.setProgressSupplier(ProgressWidget.JEIProgress);
            if (GTCEu.Mods.isREILoaded() || GTCEu.Mods.isJEILoaded() || GTCEu.Mods.isEMILoaded()) {
                group.addWidget(new ButtonWidget(progressWidget.getPosition().x, progressWidget.getPosition().y,
                        progressWidget.getSize().width, progressWidget.getSize().height, IGuiTexture.EMPTY, cd -> {
                            if (cd.isRemote) {
                                if (GTCEu.Mods.isREILoaded()) {
                                    ViewSearchBuilder.builder().addCategories(
                                            rootRecipe.getType().getCategories().stream()
                                                    .filter(GTRecipeCategory::isXEIVisible)
                                                    .map(GTRecipeREICategory::machineCategory)
                                                    .collect(Collectors.toList()))
                                            .open();
                                } else if (GTCEu.Mods.isJEILoaded()) {
                                    JEIPlugin.jeiRuntime.getRecipesGui().showTypes(
                                            rootRecipe.getType().getCategories().stream()
                                                    .filter(GTRecipeCategory::isXEIVisible)
                                                    .map(GTRecipeJEICategory::machineType)
                                                    .collect(Collectors.toList()));
                                } else if (GTCEu.Mods.isEMILoaded()) {
                                    EmiApi.displayRecipeCategory(
                                            GTRecipeEMICategory.machineCategory(rootRecipe.getType().getCategory()));
                                }
                            }
                        }).setHoverTooltips("gtceu.recipe_type.show_recipes"));
            }

            var lastStepLayer = steps.get(steps.size() - 1);
            var recipe = lastStepLayer.recipe();
            buildStepOutput(centerX, bottomY + 22, getRecipeHolder(recipe), recipe);
        }

        private GTRecipeTypeUI.RecipeHolder getRecipeHolder(GTRecipe recipe) {
            @SuppressWarnings("UnstableApiUsage")
            var storages = Tables.newCustomTable(new EnumMap<>(IO.class),
                    LinkedHashMap<RecipeCapability<?>, Object>::new);
            @SuppressWarnings("UnstableApiUsage")
            var extraStorages = Tables.newCustomTable(new EnumMap<>(IO.class),
                    LinkedHashMap<RecipeCapability<?>, List<Content>>::new);
            recipeWidget.collectStorage(storages, extraStorages, recipe);

            return new GTRecipeTypeUI.RecipeHolder(ProgressWidget.JEIProgress, storages, recipe.data,
                    recipe.conditions, false, false);
        }

        private void buildStepOutput(float centerX, int posY, GTRecipeTypeUI.RecipeHolder recipeHolder,
                                     GTRecipe recipe) {
            var slots = new ArrayList<Widget>();

            for (var entry : recipeHolder.storages().row(IO.OUT).entrySet()) {
                var cap = entry.getKey();
                if (cap.getWidgetClass() == null) {
                    continue;
                }
                var object = entry.getValue();
                var contents = recipe.getOutputContents(cap);
                for (var index = 0; index < contents.size(); index++) {
                    var isLast = index == contents.size() - 1;
                    var slot = buildSlot(IO.OUT, recipe, recipeHolder, cap, object, index, contents.get(index), isLast);
                    slots.add(slot);
                }
                var tickContents = recipe.getTickOutputContents(cap);
                for (var i = 0; i < tickContents.size(); i++) {
                    var index = i + contents.size();
                    var isLast = i == tickContents.size() - 1;
                    var slot = buildSlot(IO.OUT, recipe, recipeHolder, cap, object, index, tickContents.get(i), isLast);
                    slots.add(slot);
                }
            }

            var slotWidth = 18f;
            var startX = centerX - (slots.size() / 2f) * slotWidth;
            for (var slotIndex = 0; slotIndex < slots.size(); slotIndex++) {
                var posX = startX + slotIndex * slotWidth;
                var slot = slots.get(slotIndex);
                slot.setSelfPosition((int) posX, posY);
                group.addWidget(slot);
            }
        }

        private int buildStepInput(int posX, int recipeStep, GTRecipeTypeUI.RecipeHolder recipeHolder, GTRecipe recipe,
                                   int timeout) {
            var stepInfo = getLayerInfo(recipe, timeout);

            var slotSize = 18;
            var posY = 2;

            group.addWidget(new Widget(posX, posY, slotSize, 11).setBackground(new ColorRectTexture(0xffa8a8a8)));
            group.addWidget(new LabelWidget(posX, posY + 2, slotSize, 9,
                    Component.literal(FormattingUtil.toRomanNumeral(recipeStep + 1)))
                    .setTextAlign(Align.CENTER)
                    .setDropShadow(true)
                    .setHoverTooltips(getLayerInfoTooltips(recipeStep, stepInfo)));

            posY += 11 + 2;

            for (var entry : recipeHolder.storages().row(IO.IN).entrySet()) {
                var cap = entry.getKey();
                if (cap.getWidgetClass() == null) {
                    continue;
                }
                var object = entry.getValue();
                var contents = recipe.getInputContents(cap);
                for (var index = 0; index < contents.size(); index++) {
                    var isLast = index == contents.size() - 1;
                    var slot = buildSlot(IO.IN, recipe, recipeHolder, cap, object, index, contents.get(index), isLast);
                    slot.setSelfPosition(posX, posY);
                    group.addWidget(slot);
                    posY += slotSize;
                }
                var tickContents = recipe.getTickInputContents(cap);
                for (var i = 0; i < tickContents.size(); i++) {
                    var index = i + contents.size();
                    var isLast = i == tickContents.size() - 1;
                    var slot = buildSlot(IO.IN, recipe, recipeHolder, cap, object, index, tickContents.get(i), isLast);
                    slot.setSelfPosition(posX, posY);
                    group.addWidget(slot);
                    posY += slotSize;
                }
            }
            return posY;
        }

        private IGuiTexture getOverlaysForSlot(GTRecipeTypeUI recipeUI, boolean isOutput,
                                               RecipeCapability<?> capability, boolean isLast) {
            IGuiTexture base = capability == FluidRecipeCapability.CAP ? GuiTextures.FLUID_SLOT : GuiTextures.SLOT;
            byte overlayKey = (byte) ((isOutput ? 2 : 0) + (capability == FluidRecipeCapability.CAP ? 1 : 0) +
                    (isLast ? 4 : 0));
            if (recipeUI.getSlotOverlays().containsKey(overlayKey)) {
                return new GuiTextureGroup(base, recipeUI.getSlotOverlays().get(overlayKey));
            }
            return base;
        }

        private Widget buildSlot(IO io, GTRecipe recipe, GTRecipeTypeUI.RecipeHolder recipeHolder,
                                 RecipeCapability<?> cap, Object storage,
                                 int storageIndex, Content recipeInput, boolean isLast) {
            var slot = cap.createWidget();
            assert slot != null;
            slot.setBackground(getOverlaysForSlot(recipeUI, io == IO.OUT, cap, isLast));
            cap.applyWidgetInfo(slot, storageIndex, true, io, recipeHolder, recipe.getType(), recipe, recipeInput,
                    storage,
                    recipeWidget.getMinTier(), recipeWidget.getTier());
            slot.setOverlay(recipeInput.createOverlay(false, recipeWidget.getMinTier(), recipeWidget.getTier(),
                    recipe.getType().getChanceFunction()));
            return slot;
        }

        private record LayerInfo(int duration, int timeout, long euTotal) {}

        private List<Component> getLayerInfoTooltips(int recipeStep, LayerInfo stepInfo) {
            var result = new ArrayList<Component>();

            result.add(Component.translatable("gtceu.recipe.layered.step", Integer.toString(recipeStep + 1)));
            result.add(Component.translatable("gtceu.recipe.duration",
                    FormattingUtil.formatNumbers(stepInfo.duration / 20f)));
            result.add(Component.translatable("gtceu.recipe.total", FormattingUtil.formatNumbers(stepInfo.euTotal)));
            if (stepInfo.timeout > 0) {
                result.add(Component.translatable("gtceu.recipe.layered.timeout",
                        FormattingUtil.formatNumbers(stepInfo.timeout / 20f)));
            }
            return result;
        }

        private LayerInfo getLayerInfo(GTRecipe recipe, int timeout) {
            var logic = recipeWidget.getOcLogic();
            var tier = recipeWidget.getTier();
            var minTier = recipeWidget.getMinTier();

            var EUt = recipe.getInputEUt();
            var duration = recipe.duration;

            if (tier > minTier && !EUt.isEmpty()) {
                var ocs = tier - minTier;
                if (minTier == GTValues.ULV) ocs--;
                var params = new OverclockingLogic.OCParams(EUt.voltage(), recipe.duration, ocs, ocs, 1);
                var result = logic.runOverclockingLogic(params, GTValues.V[tier]);
                duration = (int) (duration * result.durationMultiplier());
                EUt = EUt.multiplyVoltage(result.eutMultiplier());
            }

            var euTotal = EUt.getTotalEU() * duration;
            return new LayerInfo(duration, timeout, euTotal);
        }
    }
}
