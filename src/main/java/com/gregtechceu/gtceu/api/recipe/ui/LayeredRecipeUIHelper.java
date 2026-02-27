package com.gregtechceu.gtceu.api.recipe.ui;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.editor.IEditableUI;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IWorkableMultiController;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.LayeredRecipeHelper;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderFluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.common.machine.trait.LayeredRecipeLogic;
import com.gregtechceu.gtceu.integration.xei.widgets.GTRecipeWidget;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.google.common.collect.Tables;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;

public class LayeredRecipeUIHelper {

    public static void handleLayeredDisplayClick(IWorkableMultiController controller, String componentData,
                                                 ClickData clickData) {
        if (clickData.isRemote) return;

        if (componentData.equals("layered_cancel")) {
            controller.getRecipeLogic().interruptRecipe();
        }
    }

    public static void getLayeredAdditionalDisplay(IWorkableMultiController controller, List<Component> textList) {
        if (!controller.isFormed()) return;
        if (!(controller.getRecipeLogic() instanceof LayeredRecipeLogic layeredLogic)) return;

        var layers = layeredLogic.getLayeredRecipe();
        var step = layeredLogic.getLayeredRecipeLayerIndex();

        if (layers != null) {
            textList.add(Component.literal(""));
            textList.add(Component.translatable("gtceu.multiblock.layered.step",
                    layeredLogic.getLayeredRecipeLayerIndex() + 1, layers.size()));
        }

        if (layers != null) {
            var totalDuration = (int) layers.stream().map(l -> l.recipe().duration).reduce(0, Integer::sum);
            var totalProgress = layeredLogic.getProgress() + layers.stream()
                    .limit(layeredLogic.getLayeredRecipeLayerIndex())
                    .map(l -> l.recipe().duration).reduce(0, Integer::sum);

            var progressPercent = totalDuration == 0 ? 0.0 : totalProgress / (totalDuration * 1.0);

            var currentProgress = (int) (progressPercent * 100);
            var currentInSec = totalProgress / 20.0;
            var maxInSec = totalDuration / 20.0;

            textList.add(Component.literal(""));
            textList.add(Component.translatable("gtceu.multiblock.layered.progress",
                    String.format("%.2f", (float) currentInSec),
                    String.format("%.2f", (float) maxInSec), currentProgress));
        }

        if (layers != null && layeredLogic.getLastRecipe() == null) {
            textList.add(Component.literal(""));
            textList.add(Component.translatable("gtceu.multiblock.layered.next_step_inputs")
                    .append(ComponentPanelWidget.withButton(Component.literal(" [")
                            .append(Component.translatable("gtceu.multiblock.layered.cancel"))
                            .append(Component.literal("]")), "layered_cancel")));

            var recipe = layers.get(step).recipe();
            var itemInputs = recipe.getInputContents(ItemRecipeCapability.CAP);
            var fluidInputs = recipe.getInputContents(FluidRecipeCapability.CAP);

            for (var item : itemInputs) {
                ItemStack stack;
                if (item.content instanceof IntProviderIngredient provider) {
                    stack = provider.getMaxSizeStack();
                } else {
                    var stacks = ItemRecipeCapability.CAP.of(item.content).getItems();
                    if (stacks.length == 0) continue;
                    stack = stacks[0];
                }
                textList.add(Component.translatable("gtceu.multiblock.layered.recipe_contents_line",
                        stack.getHoverName(), FormattingUtil.formatNumberReadable(stack.getCount())));
            }

            for (var fluid : fluidInputs) {
                FluidStack stack;
                if (fluid.content instanceof IntProviderFluidIngredient provider) {
                    stack = provider.getMaxSizeStack();
                } else {
                    var stacks = FluidRecipeCapability.CAP.of(fluid.content).getStacks();
                    if (stacks.length == 0) continue;
                    stack = stacks[0];
                }
                textList.add(Component.translatable("gtceu.multiblock.layered.recipe_contents_line",
                        stack.getDisplayName(), FormattingUtil.formatBuckets(stack.getAmount())));
            }
        }

        if (layers != null && (step < layers.size() - 1 || layeredLogic.getLastRecipe() == null)) {
            textList.add(Component.literal(""));
            textList.add(Component.translatable("gtceu.multiblock.layered.final_step_outputs"));

            var recipe = layers.get(layers.size() - 1).recipe();
            var itemOutputs = recipe.getOutputContents(ItemRecipeCapability.CAP);
            var fluidOutputs = recipe.getOutputContents(FluidRecipeCapability.CAP);

            for (var item : itemOutputs) {
                ItemStack stack;
                Component displaycount;
                if (item.content instanceof IntProviderIngredient provider) {
                    stack = provider.getMaxSizeStack();
                    displaycount = Component.translatable("gtceu.gui.content.range",
                            provider.getCountProvider().getMinValue(),
                            provider.getCountProvider().getMaxValue());
                } else {
                    var stacks = ItemRecipeCapability.CAP.of(item.content).getItems();
                    if (stacks.length == 0) continue;
                    stack = stacks[0];
                    displaycount = Component.literal(String.valueOf(stack.getCount()));
                }

                textList.add(Component.translatable("gtceu.multiblock.layered.recipe_contents_line",
                        stack.getHoverName(), displaycount));
            }

            for (var fluid : fluidOutputs) {
                FluidStack stack;
                Component displaycount;
                if (fluid.content instanceof IntProviderFluidIngredient provider) {
                    stack = provider.getMaxSizeStack();
                    displaycount = Component.translatable("gtceu.gui.content.range",
                            provider.getCountProvider().getMinValue(),
                            provider.getCountProvider().getMaxValue());
                } else {
                    var stacks = FluidRecipeCapability.CAP.of(fluid.content).getStacks();
                    if (stacks.length == 0) continue;
                    stack = stacks[0];
                    displaycount = Component.literal(String.valueOf(stack.getAmount()));
                }
                textList.add(Component.translatable("gtceu.multiblock.layered.recipe_contents_line",
                        stack.getDisplayName(), displaycount));
            }
        }
    }

    public static GTRecipeTypeUI createRecipeUI(GTRecipeType recipeType) {
        return new GTRecipeTypeUI(recipeType) {

            @Override
            public IEditableUI<WidgetGroup, RecipeHolder> createEditableUITemplate(boolean isSteam,
                                                                                   boolean isHighPressure) {
                return new IEditableUI.Normal<>(() -> {
                    var group = new WidgetGroup(0, 0, 154, 150);

                    var outputsGroup = new WidgetGroup(3, 3, 148, 34);
                    outputsGroup.setId("outputs");
                    group.addWidget(outputsGroup);

                    var layersGroup = new DraggableScrollableWidgetGroup(3, 37, 148, 150 - 34);
                    layersGroup.setYScrollBarWidth(8);
                    layersGroup.setYBarStyle(GuiTextures.SLIDER_BACKGROUND_VERTICAL, GuiTextures.BUTTON);
                    layersGroup.setId("layers");
                    group.addWidget(layersGroup);

                    return group;
                }, (container, recipeHolder) -> {});
            }
        };
    }

    public static void buildLayeredUI(GTRecipe rootRecipe, WidgetGroup widgetGroup) {
        var recipeWidget = (GTRecipeWidget) widgetGroup;
        var recipeUI = rootRecipe.recipeType.getRecipeUI();

        var steps = LayeredRecipeHelper.getLayeredSteps(rootRecipe);
        if (steps == null) return;

        var layersGroup = (DraggableScrollableWidgetGroup) recipeWidget.getFirstWidgetById("^layers$");
        assert layersGroup != null;
        layersGroup.clearAllWidgets();

        var outputsGroup = (WidgetGroup) recipeWidget.getFirstWidgetById("^outputs$");
        assert outputsGroup != null;
        outputsGroup.clearAllWidgets();

        var outputX = 0;
        var scrollX = 0;
        var scrollY = 0;

        outputsGroup.addWidget(new LabelWidget(outputX, 0, "Output"));

        for (var recipeStep = 0; recipeStep < steps.size(); recipeStep++) {
            var stepLayer = steps.get(recipeStep);

            var recipe = stepLayer.recipe();
            var storages = Tables.newCustomTable(new EnumMap<>(IO.class),
                    LinkedHashMap<RecipeCapability<?>, Object>::new);
            recipeWidget.collectStorage(storages, Tables.newCustomTable(new EnumMap<>(IO.class),
                    LinkedHashMap<RecipeCapability<?>, List<Content>>::new), recipe);
            var recipeHolder = new GTRecipeTypeUI.RecipeHolder(ProgressWidget.JEIProgress, storages, recipe.data,
                    recipe.conditions, false, false);

            var labelStep = new LabelWidget(scrollX, scrollY, "Step " + (recipeStep + 1));
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
                    for (var i = 0; i < contents.size(); i++) {
                        var index = i;
                        var isLast = i == contents.size() - 1;
                        var slot = buildSlot(recipeUI, recipeWidget, io, recipe, recipeHolder, cap, object, index,
                                contents.get(i), isLast);
                        if (io == IO.IN) {
                            slot.setSelfPosition(scrollX, scrollY);
                            scrollX += 18;
                            layersGroup.addWidget(slot);
                        } else if (io == IO.OUT) {
                            slot.setSelfPosition(outputX, 10 + 2);
                            outputX += 18;
                            outputsGroup.addWidget(slot);
                        }
                    }
                    var tickContents = io == IO.IN ? recipe.getTickInputContents(cap) :
                            recipe.getTickOutputContents(cap);
                    for (var i = 0; i < tickContents.size(); i++) {
                        var index = i + contents.size();
                        var isLast = i == tickContents.size() - 1;
                        var slot = buildSlot(recipeUI, recipeWidget, io, recipe, recipeHolder, cap, object, index,
                                tickContents.get(i), isLast);
                        if (io == IO.IN) {
                            slot.setSelfPosition(scrollX, scrollY);
                            scrollX += 18;
                            layersGroup.addWidget(slot);
                        } else if (io == IO.OUT) {
                            slot.setSelfPosition(outputX, 10 + 2);
                            outputX += 18;
                            outputsGroup.addWidget(slot);
                        }
                    }
                    if (scrollX > 0) {
                        scrollY += 18;
                        scrollX = 0;
                    }
                }
            }

            scrollY += 2;
            var labelInfoStep = createLayerInfoLabel(recipe, recipeWidget, stepLayer.timeout());
            labelInfoStep.setSelfPosition(scrollX, scrollY);
            layersGroup.addWidget(labelInfoStep);

            scrollY += 10 + 4;
        }
    }

    private static IGuiTexture getOverlaysForSlot(GTRecipeTypeUI recipeUI, boolean isOutput,
                                                  RecipeCapability<?> capability, boolean isLast) {
        IGuiTexture base = capability == FluidRecipeCapability.CAP ? GuiTextures.FLUID_SLOT : GuiTextures.SLOT;
        byte overlayKey = (byte) ((isOutput ? 2 : 0) + (capability == FluidRecipeCapability.CAP ? 1 : 0) +
                (isLast ? 4 : 0));
        if (recipeUI.getSlotOverlays().containsKey(overlayKey)) {
            return new GuiTextureGroup(base, recipeUI.getSlotOverlays().get(overlayKey));
        }
        return base;
    }

    private static Widget buildSlot(GTRecipeTypeUI recipeUI, GTRecipeWidget recipeWidget, IO io, GTRecipe recipe,
                                    GTRecipeTypeUI.RecipeHolder recipeHolder, RecipeCapability<?> cap, Object storage,
                                    int storageIndex, Content recipeInput, boolean isLast) {
        var slot = cap.createWidget();
        assert slot != null;
        slot.setBackground(getOverlaysForSlot(recipeUI, false, cap, isLast));
        slot.setId(cap.slotName(io, storageIndex));
        cap.applyWidgetInfo(slot, storageIndex, true, io, recipeHolder, recipe.getType(), recipe, recipeInput, storage,
                recipeWidget.getMinTier(), recipeWidget.getTier());
        slot.setOverlay(recipeInput.createOverlay(false, recipeWidget.getMinTier(), recipeWidget.getTier(),
                recipe.getType().getChanceFunction()));
        return slot;
    }

    private static LabelWidget createLayerInfoLabel(GTRecipe recipe, GTRecipeWidget widget, int timeout) {
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
            text.append(Component.translatable("Timeout: %s secs", FormattingUtil.formatNumbers(timeout / 20f)));
        }

        var voltageText = new LabelWidget(0, 0, text).setTextColor(-1).setDropShadow(true);
        voltageText
                .setHoverTooltips(Component.translatable("gtceu.recipe.total", FormattingUtil.formatNumbers(euTotal)));
        return voltageText;
    }
}
