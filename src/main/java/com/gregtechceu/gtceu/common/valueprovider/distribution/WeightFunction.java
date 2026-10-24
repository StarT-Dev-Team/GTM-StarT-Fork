package com.gregtechceu.gtceu.common.valueprovider.distribution;

/** Gets the weight of an amount relative to the other amounts. This is not a percentage */
@FunctionalInterface
public interface WeightFunction {

    double weight(int amount);
}
