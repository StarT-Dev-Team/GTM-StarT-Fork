package com.gregtechceu.gtceu.integration.emi.multipage;

import com.gregtechceu.gtceu.api.gui.widget.PatternPreviewWidget;
import com.gregtechceu.gtceu.api.gui.widget.preview.MBPattern;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.integration.xei.GTXEIUI;

import com.lowdragmc.lowdraglib2.integration.xei.emi.ModularUIEMIRecipe;

import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MultiblockInfoEmiRecipe extends ModularUIEMIRecipe {

    private final MultiblockMachineDefinition definition;
    private SlotWidget slotWidget;
    @Nullable
    private PatternPreviewWidget activeWidget;

    public MultiblockInfoEmiRecipe(MultiblockMachineDefinition definition) {
        super(recipe -> {
            var widget = PatternPreviewWidget.getPatternWidget(definition);
            ((MultiblockInfoEmiRecipe) recipe).activeWidget = widget;
            return GTXEIUI.of(widget);
        });
        this.definition = definition;
    }

    @Override
    public int getDisplayWidth() {
        return PatternPreviewWidget.WIDGET_WIDTH;
    }

    @Override
    public int getDisplayHeight() {
        return PatternPreviewWidget.WIDGET_HEIGHT;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        super.addWidgets(widgets);
        // numbers gotten from the size of the widget
        slotWidget = new SlotWidget(EmiStack.of(definition.getItem().asItem()),
                PatternPreviewWidget.WIDGET_WIDTH - 22, 10)
                .recipeContext(this)
                .drawBack(false);

        widgets.add(slotWidget);
    }

    @Override
    public List<EmiIngredient> getInputs() {
        int page = (activeWidget != null) ? activeWidget.getIndex() : PatternPreviewWidget.getPage(definition);
        MBPattern pattern = PatternPreviewWidget.getPattern(definition, page);
        if (pattern != null && pattern.parts != null) {
            return pattern.parts.stream()
                    .filter(list -> !list.isEmpty())
                    .map(list -> {
                        long amount = list.get(0).getCount();
                        List<EmiStack> stacks = list.stream().map(EmiStack::of).toList();
                        return EmiIngredient.of(stacks, amount);
                    })
                    .toList();
        }
        return super.getInputs();
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return MultiblockInfoEmiCategory.CATEGORY;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return definition.getId().withPrefix("/");
    }

    @Override
    public List<EmiStack> getOutputs() {
        return List.of(EmiStack.of(definition.getItem()));
    }
}
