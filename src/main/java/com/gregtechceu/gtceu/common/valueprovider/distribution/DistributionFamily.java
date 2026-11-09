package com.gregtechceu.gtceu.common.valueprovider.distribution;

/**
 * A distribution shape that can be built with its default parameters, or with parameters that give a requested mean.
 */
public abstract class DistributionFamily extends IntDistribution {

    public abstract IntDistribution withAverage(double average);
}
