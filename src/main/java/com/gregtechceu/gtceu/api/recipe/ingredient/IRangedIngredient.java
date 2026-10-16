package com.gregtechceu.gtceu.api.recipe.ingredient;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.common.valueprovider.StatisticalInt;

import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;

import org.jetbrains.annotations.NotNull;

public interface IRangedIngredient {

    IntProvider getCountProvider();

    int getSampledCount();

    void setSampledCount(int count);

    /**
     * If this ingredient has not yet had its count rolled, rolls it and returns the roll.
     * If it has, returns the existing roll.
     * Passthrough method, invokes {@code rollSampledCount()} using the threadsafe {@link GTValues#RNG}.
     *
     * @return the amount rolled
     */
    default int rollSampledCount() {
        return rollSampledCount(GTValues.RNG);
    }

    int rollSampledCount(@NotNull RandomSource random);

    /**
     * @return the average roll of this ranged amount
     */
    default double getMidRoll() {
        if (getCountProvider() instanceof StatisticalInt statistical) {
            return statistical.mean();
        }

        return ((getCountProvider().getMaxValue() + getCountProvider().getMinValue()) / 2.0);
    }

    /**
     * @param tierDiff the number of tiers above the recipe tier, see {@code ChanceBoostFunction#getTierDiff}
     * @return the average roll of this ranged amount when run at that tier
     */
    default double getMidRoll(int tierDiff) {
        if (getCountProvider() instanceof StatisticalInt statistical) {
            return statistical.withTierDiff(tierDiff).mean();
        }

        return getMidRoll();
    }

    default boolean isRolled() {
        return getSampledCount() != -1;
    }

    void reset();
}
