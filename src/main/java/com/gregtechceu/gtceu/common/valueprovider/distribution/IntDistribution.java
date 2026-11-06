package com.gregtechceu.gtceu.common.valueprovider.distribution;

import com.gregtechceu.gtceu.common.valueprovider.WeightedInt;

/**
 * Turns an amount range into a {@link WeightedInt}.
 * This is an abstract class instead of an interface so that KubeJS never converts a JS lambda to it, which keeps the
 * overloads that take a {@link WeightFunction} unambiguous.
 */
public abstract class IntDistribution {

    /**
     * @throws IllegalArgumentException if the range or the distribution's parameters are invalid
     */
    public abstract WeightedInt build(int min, int max);

    /**
     * Attaches a tier weight boost to the tables this distribution builds, see
     * {@link WeightedInt#tierWeightBoost(double)}.
     *
     * @param boost the weight boost per tier above the recipe tier
     * @return the boosted distribution
     */
    public IntDistribution tierWeightBoost(double boost) {
        IntDistribution base = this;

        return new IntDistribution() {

            @Override
            public WeightedInt build(int min, int max) {
                return base.build(min, max).tierWeightBoost(boost);
            }
        };
    }

    protected static void checkRange(int min, int max) {
        if (min < 0) throw new IllegalArgumentException("min must not be negative, got " + min);
        if (min > max) throw new IllegalArgumentException("min (" + min + ") must not be above max (" + max + ")");
        if ((long) max - min + 1 > WeightedInt.MAX_ENTRIES) throw new IllegalArgumentException("range " + min + ".." + max + " is too large, at most " +
                    WeightedInt.MAX_ENTRIES + " different amounts are supported");
    }

    protected static void checkAverage(double average, int min, int max) {
        if (min >= max) throw new IllegalArgumentException("a target average needs a range with more than one value, got " + min +
                    ".." + max);
        if (!(average > min && average < max)) throw new IllegalArgumentException("target average " + average + " must be above min (" + min +
                    ") and below max (" + max + ")");
    }
}
