package com.gregtechceu.gtceu.common.machine.trait.customlogic;

import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.LayeredRecipeHelper;

import org.jetbrains.annotations.Nullable;

public class LayeredLogic implements GTRecipeType.ICustomRecipeLogic {

    private final GTRecipeType recipeType;

    public LayeredLogic(GTRecipeType recipeType) {
        this.recipeType = recipeType;
        recipeType.enableSyntheticCategory();
    }

    @Override
    public @Nullable GTRecipe createCustomRecipe(IRecipeCapabilityHolder holder) {
        return null;
    }

    @Override
    public void buildRepresentativeRecipes() {
        var category = recipeType.getSyntheticCategory();
        for (var recipe : recipeType.getRecipesInCategory(recipeType.getCategory())) {
            var original = LayeredRecipeHelper.getFullLayeredRecipe(recipe);
            if (original == null) continue;
            category.addRecipe(original);
        }
    }
}
