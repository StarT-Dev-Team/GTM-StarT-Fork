package com.gregtechceu.gtceu.integration.rei.recipe;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.integration.xei.widgets.GTRecipeWidget;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.integration.xei.rei.ModularUIDisplay;

import net.minecraft.resources.ResourceLocation;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;

import java.util.Optional;

public class GTRecipeDisplay implements ModularUIDisplay {

    private final GTRecipe recipe;
    private final CategoryIdentifier<?> category;

    public GTRecipeDisplay(GTRecipe recipe, CategoryIdentifier<?> category) {
        this.recipe = recipe;
        this.category = category;
    }

    @Override
    public Optional<ResourceLocation> getDisplayLocation() {
        return Optional.of(recipe.id);
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return category;
    }

    public ModularUI createModularUI() {
        return GTRecipeWidget.createModularUI(recipe);
    }
}
