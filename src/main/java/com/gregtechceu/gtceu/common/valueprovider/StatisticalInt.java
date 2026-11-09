package com.gregtechceu.gtceu.common.valueprovider;

import net.minecraft.util.valueproviders.IntProvider;

/**
 * An {@link IntProvider} with a known mean and variance. This lets parallels approximate the sum of many rolls,
 * tooltips show the average roll and the odds shift with the recipe tier.
 */
public abstract class StatisticalInt extends IntProvider {

    public abstract double mean();

    public abstract double variance();

    /** The bonus weight high amounts get per tier above the recipe tier. Negative values favor low amounts. */
    public abstract double getTierWeightBoost();

    /**
     * Shifts the odds for a recipe that runs above its own tier.
     *
     * @param tierDiff the number of tiers above the recipe tier, see {@code ChanceBoostFunction#getTierDiff}
     * @return a provider with the shifted odds, or this one if nothing changes. The result has no tier weight boost
     *         of its own, so shifting it again does nothing
     */
    public abstract StatisticalInt withTierDiff(int tierDiff);

    public boolean hasTierWeightBoost() {
        return getTierWeightBoost() != 0;
    }
}
