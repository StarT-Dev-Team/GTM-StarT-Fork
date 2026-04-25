package com.gregtechceu.gtceu.api.recipe.modifier;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import org.jetbrains.annotations.NotNull;

public final class IdentifiedRecipeModifier implements RecipeModifier {

    private final String id;
    private final RecipeModifier delegate;

    public IdentifiedRecipeModifier(String id, RecipeModifier delegate) {
        this.id = id;
        this.delegate = delegate;
    }

    @Override
    public @NotNull ModifierFunction getModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        return delegate.getModifier(machine, recipe);
    }

    @Override
    public @NotNull String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }
}
