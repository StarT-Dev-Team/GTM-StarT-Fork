package com.gregtechceu.gtceu.integration.rei.orevein;

import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidDefinition;
import com.gregtechceu.gtceu.integration.xei.GTXEIUI;
import com.gregtechceu.gtceu.integration.xei.widgets.GTOreVeinWidget;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.integration.xei.rei.ModularUIDisplay;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;

import java.util.ArrayList;
import java.util.List;

public class GTBedrockFluidDisplay implements ModularUIDisplay {

    private final BedrockFluidDefinition fluid;

    public GTBedrockFluidDisplay(BedrockFluidDefinition fluid) {
        this.fluid = fluid;
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        List<EntryIngredient> outputs = new ArrayList<>();
        outputs.add(EntryIngredients.of(fluid.getStoredFluid().get()));
        return outputs;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return GTBedrockFluidDisplayCategory.CATEGORY;
    }

    public ModularUI createModularUI() {
        return GTXEIUI.of(new GTOreVeinWidget(fluid));
    }
}
