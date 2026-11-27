package com.gregtechceu.gtceu.integration.rei.orevein;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockore.BedrockOreDefinition;
import com.gregtechceu.gtceu.integration.xei.GTXEIUI;
import com.gregtechceu.gtceu.integration.xei.widgets.GTOreVeinWidget;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.integration.xei.rei.ModularUIDisplay;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;

import java.util.ArrayList;
import java.util.List;

public class GTBedrockOreDisplay implements ModularUIDisplay {

    private final BedrockOreDefinition bedrockOre;

    public GTBedrockOreDisplay(BedrockOreDefinition bedrockOre) {
        this.bedrockOre = bedrockOre;
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        List<EntryIngredient> outputs = new ArrayList<>();
        for (Material material : bedrockOre.getAllMaterials()) {
            outputs.add(EntryIngredients.of(ChemicalHelper.get(TagPrefix.rawOre, material)));
        }
        return outputs;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return GTBedrockOreDisplayCategory.CATEGORY;
    }

    public ModularUI createModularUI() {
        return GTXEIUI.of(new GTOreVeinWidget(bedrockOre));
    }
}
