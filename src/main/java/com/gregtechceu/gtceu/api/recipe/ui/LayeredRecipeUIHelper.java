package com.gregtechceu.gtceu.api.recipe.ui;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.editor.IEditableUI;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.LayeredRecipeHelper;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.integration.xei.widgets.GTRecipeWidget;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.network.chat.Component;

import com.google.common.collect.Tables;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;

public class LayeredRecipeUIHelper {

    public static GTRecipeTypeUI createRecipeUI(GTRecipeType recipeType) {
        var ui = new GTRecipeTypeUI(recipeType) {

            @Override
            public IEditableUI<WidgetGroup, RecipeHolder> createEditableUITemplate(boolean isSteam,
                                                                                   boolean isHighPressure) {
                return new IEditableUI.Normal<>(() -> {
                    var group = new WidgetGroup(0, 0, 154, 138);

                    var outputsGroup = new WidgetGroup(3, 3, 148, 34);
                    outputsGroup.setId("outputs");
                    group.addWidget(outputsGroup);

                    var layersGroup = new DraggableScrollableWidgetGroup(3, 37, 148, 138 - 34);
                    layersGroup.setYScrollBarWidth(8);
                    layersGroup.setYBarStyle(GuiTextures.SLIDER_BACKGROUND_VERTICAL, GuiTextures.BUTTON);
                    layersGroup.setId("layers");
                    group.addWidget(layersGroup);

                    return group;
                }, (container, recipeHolder) -> {});
            }
        };
        ui.setUiBuilder(LayeredRecipeUIHelper::buildLayeredUI);
        return ui;
    }

    public static void buildLayeredUI(GTRecipe rootRecipe, WidgetGroup widgetGroup) {
        var recipeWidget = (GTRecipeWidget) widgetGroup;

        var layersGroup = (DraggableScrollableWidgetGroup) recipeWidget.getFirstWidgetById("^layers$");
        assert layersGroup != null;
        layersGroup.clearAllWidgets();

        var outputsGroup = (WidgetGroup) recipeWidget.getFirstWidgetById("^outputs$");
        assert outputsGroup != null;
        outputsGroup.clearAllWidgets();

        new UIBuilder(recipeWidget, layersGroup, outputsGroup, rootRecipe).build();
    }

    private static class UIBuilder {

        private int outputX = 0;
        private int scrollX = 0;
        private int scrollY = 0;

        private final GTRecipeWidget recipeWidget;
        private final DraggableScrollableWidgetGroup layersGroup;
        private final WidgetGroup outputsGroup;
        private final GTRecipe rootRecipe;
        private final GTRecipeTypeUI recipeUI;

        UIBuilder(GTRecipeWidget recipeWidget, DraggableScrollableWidgetGroup layersGroup, WidgetGroup outputsGroup,
                  GTRecipe rootRecipe) {
            this.recipeWidget = recipeWidget;
            this.layersGroup = layersGroup;
            this.outputsGroup = outputsGroup;
            this.rootRecipe = rootRecipe;
            recipeUI = rootRecipe.recipeType.getRecipeUI();
        }

        private void build() {
            var steps = LayeredRecipeHelper.getLayeredSteps(rootRecipe);
            if (steps == null) return;

            outputsGroup.addWidget(new LabelWidget(0, 0,
                    Component.translatable("gtceu.recipe.layered.output", Integer.toString(steps.size()))));

            for (var recipeStep = 0; recipeStep < steps.size(); recipeStep++) {
                var stepLayer = steps.get(recipeStep);

                var recipe = stepLayer.recipe();
                var storages = Tables.newCustomTable(new EnumMap<>(IO.class),
                        LinkedHashMap<RecipeCapability<?>, Object>::new);
                recipeWidget.collectStorage(storages, Tables.newCustomTable(new EnumMap<>(IO.class),
                        LinkedHashMap<RecipeCapability<?>, List<Content>>::new), recipe);

                var recipeHolder = new GTRecipeTypeUI.RecipeHolder(ProgressWidget.JEIProgress, storages, recipe.data,
                        recipe.conditions, false, false);

                buildStep(recipeStep, recipeHolder, recipe, stepLayer.timeout());
            }
        }

        private void buildStep(int recipeStep, GTRecipeTypeUI.RecipeHolder recipeHolder, GTRecipe recipe, int timeout) {
            var labelStep = new LabelWidget(scrollX, scrollY,
                    Component.translatable("gtceu.recipe.layered.step", Integer.toString(recipeStep + 1)));
            layersGroup.addWidget(labelStep);
            scrollY += 10 + 2;

            for (var entryCol : recipeHolder.storages().rowMap().entrySet()) {
                var io = entryCol.getKey();
                for (var entry : entryCol.getValue().entrySet()) {
                    var cap = entry.getKey();
                    if (cap.getWidgetClass() == null) {
                        continue;
                    }
                    var object = entry.getValue();
                    var contents = io == IO.IN ? recipe.getInputContents(cap) : recipe.getOutputContents(cap);
                    for (var index = 0; index < contents.size(); index++) {
                        var isLast = index == contents.size() - 1;
                        var slot = buildSlot(io, recipe, recipeHolder, cap, object, index, contents.get(index), isLast);
                        setStepPosition(slot, io);
                    }
                    var tickContents = io == IO.IN ? recipe.getTickInputContents(cap) :
                            recipe.getTickOutputContents(cap);
                    for (var i = 0; i < tickContents.size(); i++) {
                        var index = i + contents.size();
                        var isLast = i == tickContents.size() - 1;
                        var slot = buildSlot(io, recipe, recipeHolder, cap, object, index, tickContents.get(i), isLast);
                        setStepPosition(slot, io);
                    }
                }
            }

            if (scrollX > 0) {
                scrollY += 18;
                scrollX = 0;
            }

            scrollY += 2;
            var labelInfoStep = createLayerInfoLabel(recipe, recipeWidget, timeout);
            labelInfoStep.setSelfPosition(scrollX, scrollY);
            layersGroup.addWidget(labelInfoStep);

            scrollY += 10 + 4;
        }

        private void setStepPosition(Widget slot, IO io) {
            if (io == IO.IN) {
                slot.setSelfPosition(scrollX, scrollY);
                scrollX += 18;
                if (scrollX > 148 - 10) {
                    scrollY += 18;
                    scrollX = 0;
                }

                layersGroup.addWidget(slot);
            } else if (io == IO.OUT) {
                slot.setSelfPosition(outputX, 10 + 2);
                outputX += 18;
                outputsGroup.addWidget(slot);
            }
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
            slot.setId("layered_" + cap.slotName(io, storageIndex));
            cap.applyWidgetInfo(slot, storageIndex, true, io, recipeHolder, recipe.getType(), recipe, recipeInput,
                    storage,
                    recipeWidget.getMinTier(), recipeWidget.getTier());
            slot.setOverlay(recipeInput.createOverlay(false, recipeWidget.getMinTier(), recipeWidget.getTier(),
                    recipe.getType().getChanceFunction()));
            return slot;
        }

        private LabelWidget createLayerInfoLabel(GTRecipe recipe, GTRecipeWidget widget, int timeout) {
            var logic = widget.getOcLogic();
            var tier = widget.getTier();
            var minTier = widget.getMinTier();

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

            var text = Component.translatable("gtceu.recipe.duration", FormattingUtil.formatNumbers(duration / 20f));
            if (timeout > 0) {
                text.append(Component.translatable("gtceu.recipe.layered.timeout",
                        FormattingUtil.formatNumbers(timeout / 20f)));
            }

            var voltageText = new LabelWidget(0, 0, text).setTextColor(-1).setDropShadow(true);
            voltageText
                    .setHoverTooltips(
                            Component.translatable("gtceu.recipe.total", FormattingUtil.formatNumbers(euTotal)));
            return voltageText;
        }
    }
}
