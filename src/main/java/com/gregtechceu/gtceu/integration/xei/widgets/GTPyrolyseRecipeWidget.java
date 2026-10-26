package com.gregtechceu.gtceu.integration.xei.widgets;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

import net.minecraft.network.chat.Component;

import java.util.List;

public class GTPyrolyseRecipeWidget extends GTCoilRecipeWidget {

    public GTPyrolyseRecipeWidget(GTRecipe recipe) {
        super(recipe);
    }

    @Override
    protected double getDurationMultiplier() {
        if (getSelectedCoil() == null) return 1;

        return GTRecipeModifiers.pyrolyseDurationMultiplier(getSelectedCoil().getTier());
    }

    @Override
    protected List<Component> getDurationMultiplierTexts() {
        if (getSelectedCoil() == null) return List.of();

        return getCoilSpeedTexts(getDurationMultiplier());
    }
}
