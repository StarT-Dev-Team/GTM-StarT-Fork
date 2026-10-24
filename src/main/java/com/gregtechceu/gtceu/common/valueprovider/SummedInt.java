package com.gregtechceu.gtceu.common.valueprovider;

import com.gregtechceu.gtceu.common.data.GTValueProviderTypes;

import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviderType;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * The sum of {@code rolls} independent rolls of {@code source}, plus {@code addition}. Used for parallels, where each
 * parallel rolls on its own.
 * Up to {@link #EXACT_ROLL_LIMIT} rolls are summed one by one. Above that the sum is drawn from a normal distribution
 * with the exact mean and variance (central limit theorem), so 2000 parallels cost a single sample.
 */
public class SummedInt extends StatisticalInt {

    public static final int EXACT_ROLL_LIMIT = 32;

    private static final Codec<StatisticalInt> SOURCE_CODEC = IntProvider.CODEC.comapFlatMap(
            provider -> provider instanceof StatisticalInt statistical ? DataResult.success(statistical) :
                    DataResult.<StatisticalInt>error(() -> "summed source must be a weighted or summed provider"),
            statistical -> statistical);

    public static final Codec<SummedInt> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SOURCE_CODEC.fieldOf("source").forGetter(provider -> provider.source),
            ExtraCodecs.POSITIVE_INT.fieldOf("rolls").forGetter(provider -> provider.rolls),
            Codec.INT.optionalFieldOf("addition", 0).forGetter(provider -> provider.addition))
            .apply(instance, SummedInt::new));

    private final StatisticalInt source;
    private final int rolls;
    private final int addition;

    public SummedInt(StatisticalInt source, int rolls, int addition) {
        this.source = source;
        this.rolls = rolls;
        this.addition = addition;
    }

    @Override
    public int sample(@NotNull RandomSource random) {
        if (rolls <= EXACT_ROLL_LIMIT) {
            int total = addition;

            for (int i = 0; i < rolls; i++) {
                total += source.sample(random);
            }

            return total;
        }

        long value = Math.round(mean() + Math.sqrt(variance()) * random.nextGaussian());

        return (int) Math.max(getMinValue(), Math.min(getMaxValue(), value));
    }

    @Override
    public int getMinValue() {
        return rolls * source.getMinValue() + addition;
    }

    @Override
    public int getMaxValue() {
        return rolls * source.getMaxValue() + addition;
    }

    @Override
    public double mean() {
        return rolls * source.mean() + addition;
    }

    @Override
    public double variance() {
        return rolls * source.variance();
    }

    @Override
    public double getTierWeightBoost() {
        return source.getTierWeightBoost();
    }

    @Override
    public SummedInt withTierDiff(int tierDiff) {
        StatisticalInt tilted = source.withTierDiff(tierDiff);

        return tilted == source ? this : new SummedInt(tilted, rolls, addition);
    }

    @Override
    public @NotNull IntProviderType<?> getType() {
        return GTValueProviderTypes.SUMMED.get();
    }
}
