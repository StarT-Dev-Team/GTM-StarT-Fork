package com.gregtechceu.gtceu.integration.xei.widgets;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

import net.minecraft.network.chat.Component;

import java.util.List;

public class GTChemicalRecipeWidget extends GTCoilRecipeWidget {

    public GTChemicalRecipeWidget(GTRecipe recipe) {
        super(recipe);
    }

    @Override
    protected int getBaseDuration() {
        if (getSelectedCoil() == null) return super.getBaseDuration();

        return Math.max(1, (int) (recipe.duration *
                GTRecipeModifiers.chemicalReactorDurationMultiplier(getSelectedCoil().getTier())));
    }

    @Override
    protected List<Component> getDurationMultiplierTexts() {
        if (getSelectedCoil() == null) return List.of();

        return getCoilSpeedTexts(GTRecipeModifiers.chemicalReactorDurationMultiplier(getSelectedCoil().getTier()));
    }

    @Override
    protected double getEUtMultiplier() {
        if (getSelectedCoil() == null) return 1;

        return GTRecipeModifiers.chemicalReactorEUtMultiplier(getSelectedCoil().getTier());
    }
}
