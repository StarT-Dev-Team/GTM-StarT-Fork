package com.gregtechceu.gtceu.common.valueprovider;

import com.gregtechceu.gtceu.common.data.GTValueProviderTypes;

import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProviderType;

import com.google.common.primitives.Doubles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Rolls {@code min + index}, where each index is picked with a probability proportional to its weight. The weights
 * are relative, not percentages.
 * An optional tier weight boost shifts the odds for each tier above the recipe tier, see {@link #withTierDiff(int)}.
 */
public class WeightedInt extends StatisticalInt {

    /** The maximum number of entries, to keep recipe JSON and packets small */
    public static final int MAX_ENTRIES = 4096;

    // validated on the MapCodec, so the fields stay next to "type" like in the other providers
    public static final Codec<WeightedInt> CODEC = RecordCodecBuilder.<WeightedInt>mapCodec(instance -> instance.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("min").forGetter(provider -> provider.offset),
            Codec.DOUBLE.listOf().fieldOf("weights").forGetter(provider -> Doubles.asList(provider.weights)),
            Codec.DOUBLE.optionalFieldOf("tier_weight_boost", 0.0).forGetter(provider -> provider.tierWeightBoost))
            .apply(instance, WeightedInt::new))
            .flatXmap(WeightedInt::validate, DataResult::success)
            .codec();

    private final int offset;
    private final double[] weights;
    private final double tierWeightBoost;
    private final double[] cumulative;
    private final double total;
    private final int first;
    private final int last;
    private final double mean;
    private final double variance;

    // used by the codec, which validates the result afterwards
    public WeightedInt(int min, List<Double> weights, double tierWeightBoost) {
        this(min, Doubles.toArray(weights), tierWeightBoost);
    }

    private WeightedInt(int min, double[] weights, double tierWeightBoost) {
        this.offset = min;
        this.weights = weights;
        this.tierWeightBoost = tierWeightBoost;
        this.cumulative = new double[weights.length];

        double sum = 0;
        int firstNonZero = -1;
        int lastNonZero = -1;

        for (int i = 0; i < weights.length; i++) {
            if (weights[i] > 0) {
                sum += weights[i];

                if (firstNonZero < 0) firstNonZero = i;

                lastNonZero = i;
            }

            this.cumulative[i] = sum;
        }

        this.total = sum;
        this.first = Math.max(firstNonZero, 0);
        this.last = Math.max(lastNonZero, 0);

        double m = 0;
        double v = 0;

        if (sum > 0) {
            for (int i = 0; i < weights.length; i++) {
                if (weights[i] > 0) m += (min + i) * weights[i];
            }

            m /= sum;

            for (int i = 0; i < weights.length; i++) {
                if (weights[i] > 0) v += (min + i - m) * (min + i - m) * weights[i];
            }

            v /= sum;
        }

        this.mean = m;
        this.variance = v;
    }

    /**
     * Creates a provider from relative weights.
     *
     * @param min     the amount that the first weight belongs to
     * @param weights the relative weights for {@code min}, {@code min + 1} and so on
     * @throws IllegalArgumentException if the weights are empty, too many, negative, not finite or all zero
     */
    public static WeightedInt of(int min, double... weights) {
        return checked(new WeightedInt(min, weights.clone(), 0));
    }

    private static WeightedInt checked(WeightedInt candidate) {
        DataResult<WeightedInt> result = candidate.validate();

        return result.result().orElseThrow(() -> new IllegalArgumentException(
                result.error().map(DataResult.PartialResult::message).orElse("invalid weights")));
    }

    /**
     * Creates a copy with a tier weight boost. For each tier above the recipe tier, the highest amount gets
     * {@code boost} more weight relative to the lowest. Negative values favor low amounts.
     *
     * @param boost the weight boost per tier
     * @return the copy
     * @throws IllegalArgumentException if the boost is not finite
     */
    public WeightedInt tierWeightBoost(double boost) {
        return checked(new WeightedInt(offset, weights.clone(), boost));
    }

    private DataResult<WeightedInt> validate() {
        if (offset < 0) return DataResult.error(() -> "min must not be negative, got " + offset);
        if (weights.length == 0) return DataResult.error(() -> "weights must not be empty");
        if (weights.length > MAX_ENTRIES) return DataResult.error(() -> "too many weights (" + weights.length + "), the maximum is " + MAX_ENTRIES +
                    ". Use a smaller range.");

        for (double w : weights) {
            if (Double.isNaN(w) || Double.isInfinite(w) || w < 0) return DataResult.error(() -> "weights must be finite and not negative, got " + w);
        }

        if (!(total > 0)) return DataResult.error(() -> "at least one weight must be above 0");
        if (Double.isNaN(tierWeightBoost) || Double.isInfinite(tierWeightBoost)) return DataResult.error(() -> "tier boost must be a finite number, got " + tierWeightBoost);

        return DataResult.success(this);
    }

    /** Returns the probability, from 0 to 1, of rolling exactly {@code amount} */
    public double getChance(int amount) {
        int index = amount - offset;

        if (index < 0 || index >= weights.length || !(total > 0)) return 0;

        return Math.max(weights[index], 0) / total;
    }

    @Override
    public double getTierWeightBoost() {
        return tierWeightBoost;
    }

    /**
     * Multiplies the weight of each amount by {@code max(0, 1 + tierWeightBoost * tierDiff * position)}, where
     * {@code position} runs from 0 at the lowest to 1 at the highest amount that can be rolled.
     * The lowest amount keeps its weight, so the result is never empty and never wider than this provider.
     */
    @Override
    public WeightedInt withTierDiff(int tierDiff) {
        if (tierWeightBoost == 0 || tierDiff == 0) return this;

        int lo = getMinValue();
        int hi = getMaxValue();

        if (lo == hi) return this;

        double[] tilted = new double[weights.length];

        for (int i = 0; i < weights.length; i++) {
            double position = (double) (offset + i - lo) / (hi - lo);

            tilted[i] = weights[i] * Math.max(0, 1 + tierWeightBoost * tierDiff * position);
        }

        return new WeightedInt(offset, tilted, 0);
    }

    @Override
    public int sample(@NotNull RandomSource random) {
        double r = random.nextDouble() * total;
        int lo = 0;
        int hi = cumulative.length - 1;

        while (lo < hi) {
            int mid = (lo + hi) >>> 1;

            if (cumulative[mid] > r) hi = mid;

            else lo = mid + 1;
        }

        return offset + lo;
    }

    @Override
    public int getMinValue() {
        return offset + first;
    }

    @Override
    public int getMaxValue() {
        return offset + last;
    }

    @Override
    public double mean() {
        return mean;
    }

    @Override
    public double variance() {
        return variance;
    }

    @Override
    public @NotNull IntProviderType<?> getType() {
        return GTValueProviderTypes.WEIGHTED.get();
    }
}
