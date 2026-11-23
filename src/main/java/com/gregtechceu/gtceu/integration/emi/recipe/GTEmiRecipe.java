package com.gregtechceu.gtceu.integration.emi.recipe;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.integration.xei.widgets.GTRecipeWidget;

import com.lowdragmc.lowdraglib2.integration.xei.emi.ModularUIEMIRecipe;

import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import org.jetbrains.annotations.Nullable;

public class GTEmiRecipe extends ModularUIEMIRecipe {

    final EmiRecipeCategory category;
    final GTRecipe recipe;

    public GTEmiRecipe(GTRecipe recipe, EmiRecipeCategory category) {
        super(emiRecipe -> GTRecipeWidget.createModularUI(recipe));
        this.category = category;
        this.recipe = recipe;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return category;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return recipe.getId();
    }

    @Override
    public int getDisplayWidth() {
        return recipe.recipeType.getRecipeUI().getJEISize().width;
    }

    @Override
    public int getDisplayHeight() {
        return recipe.recipeType.getRecipeUI().getJEISize().height;
    }
}
