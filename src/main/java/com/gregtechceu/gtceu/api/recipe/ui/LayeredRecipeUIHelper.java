package com.gregtechceu.gtceu.api.recipe.ui;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.editor.IEditableUI;
import com.gregtechceu.gtceu.api.gui.widget.ProgressTextureBar;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.LayeredRecipeHelper;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.integration.xei.widgets.GTRecipeWidget;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;

import net.minecraft.network.chat.Component;

import com.google.common.collect.Tables;
import dev.vfyjxf.taffy.style.TaffyPosition;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;

public class LayeredRecipeUIHelper {

    private static final int LAYERED_GUI_WIDTH = 164;
    private static final int LAYERED_GUI_HEIGHT = 128;

    public static GTRecipeTypeUI createRecipeUI(GTRecipeType recipeType) {
        var ui = new GTRecipeTypeUI(recipeType) {

            @Override
            protected RecipeUILayout buildLayout(boolean isSteam, boolean isHighPressure) {
                var root = new UIElement();
                root.layout(layout -> layout.width(LAYERED_GUI_WIDTH).height(LAYERED_GUI_HEIGHT));
                return new RecipeUILayout(root, LAYERED_GUI_WIDTH, LAYERED_GUI_HEIGHT);
            }

            @Override
            public IEditableUI<UIElement, RecipeHolder> createEditableUITemplate(boolean isSteam,
                                                                                 boolean isHighPressure) {
                return new IEditableUI.Normal<>(() -> buildLayout(isSteam, isHighPressure).root(),
                        (container, recipeHolder) -> {});
            }
        };
        ui.setUiBuilder(LayeredRecipeUIHelper::buildLayeredUI);
        return ui;
    }

    private static UIElement place(UIElement element, float x, float y, float width, float height) {
        return element.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(x).top(y).width(width)
                .height(height));
    }

    public static void buildLayeredUI(GTRecipe rootRecipe, UIElement root) {
        var recipeWidget = (GTRecipeWidget) root;

        var group = recipeWidget.selectId(GTRecipeWidget.RECIPE_CONTENT_GROUP_ID).findFirst().orElse(null);
        assert group != null;
        group.clearAllChildren();

        new UIBuilder(recipeWidget, group, rootRecipe).build();
    }

    private static class UIBuilder {

        private final GTRecipeWidget recipeWidget;
        private final UIElement group;
        private final GTRecipe rootRecipe;
        private final GTRecipeTypeUI recipeUI;

        UIBuilder(GTRecipeWidget recipeWidget, UIElement group, GTRecipe rootRecipe) {
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
                var recipe = steps.get(recipeStep);
                var inputHeight = buildStepInput((int) posX, recipeStep, getRecipeHolder(recipe), recipe,
                        LayeredRecipeHelper.getLayerTimeout(recipe));
                if (inputHeight > inputsHeight) inputsHeight = inputHeight;
            }

            var bottomY = inputsHeight + 2;

            var outputsEndX = centerX + (steps.size() / 2f) * sliceWidth + 1f;
            var progressWidget = ProgressTextureBar.of(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT, 20,
                    20);
            progressWidget.transform(transform -> transform.rotation(90f));
            place(progressWidget, (int) (outputsEndX - 21f), bottomY, 20, 20);
            progressWidget.bindDataSource(SupplierDataSource.of(
                    () -> (float) GTRecipeTypeUI.XEI_PROGRESS.getAsDouble()));
            group.addChild(progressWidget);

            if (GTCEu.Mods.isREILoaded() || GTCEu.Mods.isJEILoaded() || GTCEu.Mods.isEMILoaded()) {
                progressWidget.addChild(GTRecipeTypeUI.createShowRecipesButton(rootRecipe.getType()));
            }

            var recipe = steps.get(steps.size() - 1);
            buildStepOutput(outputsEndX, bottomY + 22, getRecipeHolder(recipe), recipe);
        }

        private GTRecipeTypeUI.RecipeHolder getRecipeHolder(GTRecipe recipe) {
            @SuppressWarnings("UnstableApiUsage")
            var storages = Tables.newCustomTable(new EnumMap<>(IO.class),
                    LinkedHashMap<RecipeCapability<?>, Object>::new);
            @SuppressWarnings("UnstableApiUsage")
            var extraStorages = Tables.newCustomTable(new EnumMap<>(IO.class),
                    LinkedHashMap<RecipeCapability<?>, List<Content>>::new);
            recipeWidget.collectStorage(storages, extraStorages, recipe);

            return new GTRecipeTypeUI.RecipeHolder(GTRecipeTypeUI.XEI_PROGRESS, storages, recipe.data,
                    recipe.conditions, false, false);
        }

        private void buildStepOutput(float outputsEndX, int posY, GTRecipeTypeUI.RecipeHolder recipeHolder,
                                     GTRecipe recipe) {
            var slots = new ArrayList<UIElement>();

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
            var startX = outputsEndX - slots.size() * slotWidth - 2;
            for (var slotIndex = 0; slotIndex < slots.size(); slotIndex++) {
                var posX = startX + slotIndex * slotWidth;
                var slot = slots.get(slotIndex);
                place(slot, (int) posX, posY, 18, 18);
                group.addChild(slot);
            }
        }

        private int buildStepInput(int posX, int recipeStep, GTRecipeTypeUI.RecipeHolder recipeHolder, GTRecipe recipe,
                                   int timeout) {
            var stepInfo = getLayerInfo(recipe, timeout);

            var slotSize = 18;
            var posY = 2;

            group.addChild(place(new UIElement(), posX, posY, slotSize, 11)
                    .style(style -> style.backgroundTexture(new ColorRectTexture(0xffa8a8a8))));
            var stepLabel = new Label().setText(Component.literal(FormattingUtil.toRomanNumeral(recipeStep + 1)));
            stepLabel.textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER).textShadow(true));
            stepLabel.style(style -> style.tooltips(
                    getLayerInfoTooltips(recipeStep, stepInfo).toArray(Component[]::new)));
            place(stepLabel, posX, posY + 2, slotSize, 9);
            group.addChild(stepLabel);

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
                    place(slot, posX, posY, slotSize, slotSize);
                    group.addChild(slot);
                    posY += slotSize;
                }
                var tickContents = recipe.getTickInputContents(cap);
                for (var i = 0; i < tickContents.size(); i++) {
                    var index = i + contents.size();
                    var isLast = i == tickContents.size() - 1;
                    var slot = buildSlot(IO.IN, recipe, recipeHolder, cap, object, index, tickContents.get(i), isLast);
                    place(slot, posX, posY, slotSize, slotSize);
                    group.addChild(slot);
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
                return IGuiTexture.group(base, recipeUI.getSlotOverlays().get(overlayKey));
            }
            return base;
        }

        private UIElement buildSlot(IO io, GTRecipe recipe, GTRecipeTypeUI.RecipeHolder recipeHolder,
                                    RecipeCapability<?> cap, Object storage,
                                    int storageIndex, Content recipeInput, boolean isLast) {
            var slot = cap.createWidget();
            assert slot != null;
            var background = getOverlaysForSlot(recipeUI, io == IO.OUT, cap, isLast);
            slot.style(style -> style.backgroundTexture(background));
            cap.applyWidgetInfo(slot, storageIndex, true, io, recipeHolder, recipe.getType(), recipe, recipeInput,
                    storage,
                    recipeWidget.getMinTier(), recipeWidget.getTier());
            var overlay = recipeInput.createOverlay(false, recipeWidget.getMinTier(), recipeWidget.getTier(), true,
                    recipe.getType().getChanceFunction());
            slot.style(style -> style.overlay(overlay));
            return slot;
        }

        private record LayerInfo(int duration, int timeout, long euTotal) {}

        private List<Component> getLayerInfoTooltips(int recipeStep, LayerInfo stepInfo) {
            var result = new ArrayList<Component>();

            result.add(Component.translatable("gtceu.recipe.layered.step", Integer.toString(recipeStep + 1)));
            result.add(Component.translatable("gtceu.recipe.duration",
                    FormattingUtil.formatDurationShort(stepInfo.duration, Long.MAX_VALUE)));
            result.add(Component.translatable("gtceu.recipe.total", FormattingUtil.formatNumbers(stepInfo.euTotal)));
            if (stepInfo.timeout > 0) {
                result.add(Component.translatable("gtceu.recipe.layered.timeout",
                        FormattingUtil.formatDurationShort(stepInfo.timeout, Long.MAX_VALUE)));
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
