package com.gregtechceu.gtceu.integration.xei.widgets;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

public class GTCrackingRecipeWidget extends GTCoilRecipeWidget {

    public GTCrackingRecipeWidget(GTRecipe recipe) {
        super(recipe);
    }

    @Override
    protected double getEUtMultiplier() {
        if (getSelectedCoil() == null) return 1;

        return GTRecipeModifiers.crackerEUtMultiplier(getSelectedCoil().getTier());
    }
}
