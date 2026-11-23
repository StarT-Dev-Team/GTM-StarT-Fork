package com.gregtechceu.gtceu.integration.rei.orevein;

import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.integration.xei.GTXEIUI;
import com.gregtechceu.gtceu.integration.xei.widgets.GTOreVeinWidget;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.integration.xei.rei.ModularUIDisplay;

import net.minecraft.world.item.ItemStack;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;

import java.util.ArrayList;
import java.util.List;

public class GTOreVeinDisplay implements ModularUIDisplay {

    private final GTOreDefinition oreDefinition;

    public GTOreVeinDisplay(GTOreDefinition oreDefinition) {
        this.oreDefinition = oreDefinition;
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        List<EntryIngredient> ingredients = new ArrayList<>();
        for (ItemStack output : GTOreVeinWidget.getContainedOresAndBlocks(oreDefinition)) {
            ingredients.add(EntryIngredients.of(output));
        }
        return ingredients;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return GTOreVeinDisplayCategory.CATEGORY;
    }

    public ModularUI createModularUI() {
        return GTXEIUI.of(new GTOreVeinWidget(oreDefinition));
    }
}
