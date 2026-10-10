package com.gregtechceu.gtceu.common.valueprovider;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

@PrefixGameTestTemplate(false)
@GameTestHolder(GTCEu.MOD_ID)
public class SummedIntTest {

    @GameTest(template = "empty")
    public static void summedIntSmallParallelSumsExactRollsTest(GameTestHelper helper) {
        WeightedInt base = WeightedInt.of(1, 1, 1, 1);
        IntProvider modified = ModifiedIntProvider.of(base, ContentModifier.multiplier(8));
        helper.assertTrue(modified instanceof SummedInt, "weighted + whole multiplier should be a SummedInt");
        helper.assertTrue(modified.getMinValue() == 8 && modified.getMaxValue() == 24, "bounds should be 8..24");
        RandomSource random = RandomSource.create(5L);
        double total = 0;
        for (int i = 0; i < 20_000; i++) {
            int v = modified.sample(random);
            helper.assertTrue(v >= 8 && v <= 24, "roll out of bounds: " + v);
            total += v;
        }
        helper.assertTrue(Math.abs(total / 20_000 - 16) < 0.1, "average should be ~16, got " + total / 20_000);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void summedIntLargeParallelStaysInBoundsAndCenteredTest(GameTestHelper helper) {
        WeightedInt base = WeightedInt.of(1, 50, 30, 20);
        IntProvider modified = ModifiedIntProvider.of(base, ContentModifier.multiplier(2000));
        helper.assertTrue(modified.getMinValue() == 2000 && modified.getMaxValue() == 6000,
                "bounds should be 2000..6000");
        RandomSource random = RandomSource.create(7L);
        int rolls = 2000;
        double sum = 0;
        double sumSq = 0;
        for (int i = 0; i < rolls; i++) {
            int v = modified.sample(random);
            helper.assertTrue(v >= 2000 && v <= 6000, "roll out of bounds: " + v);
            sum += v;
            sumSq += (double) v * v;
        }
        double mean = sum / rolls;
        double sd = Math.sqrt(sumSq / rolls - mean * mean);
        // the exact mean is 3400 and the exact spread is sqrt(0.61 * 2000), about 34.9
        helper.assertTrue(Math.abs(mean - 3400) < 5, "mean should be ~3400, got " + mean);
        helper.assertTrue(sd > 25 && sd < 45, "spread should be ~35, got " + sd);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void summedIntAdditionIsAppliedOnceTest(GameTestHelper helper) {
        WeightedInt base = WeightedInt.of(1, 1, 1);
        IntProvider modified = ModifiedIntProvider.of(base, new ContentModifier(2, 5));
        helper.assertTrue(modified.getMinValue() == 7 && modified.getMaxValue() == 9, "bounds should be 7..9");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void summedIntIdentityKeepsTheSourceAndFractionsFallBackTest(GameTestHelper helper) {
        WeightedInt base = WeightedInt.of(1, 1, 1);
        helper.assertTrue(ModifiedIntProvider.of(base, ContentModifier.IDENTITY) == base, "identity should not wrap");
        helper.assertTrue(!(ModifiedIntProvider.of(base, ContentModifier.multiplier(1.5)) instanceof SummedInt),
                "fractional multipliers use the generic scaling path");
        IntProvider uniform = ModifiedIntProvider.of(UniformInt.of(1, 3), ContentModifier.multiplier(4));
        helper.assertTrue(uniform instanceof UniformInt && uniform.getMinValue() == 4 && uniform.getMaxValue() == 12,
                "uniform keeps its existing behaviour");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void summedIntStackedModifiersComposeTest(GameTestHelper helper) {
        WeightedInt base = WeightedInt.of(1, 1, 1);
        IntProvider twice = ModifiedIntProvider.of(base, ContentModifier.multiplier(2));
        IntProvider six = ModifiedIntProvider.of(twice, ContentModifier.multiplier(3));
        helper.assertTrue(six.getMinValue() == 6 && six.getMaxValue() == 12, "2x then 3x should be 6..12");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void summedIntSummedCodecRoundTripsTest(GameTestHelper helper) {
        WeightedInt base = WeightedInt.of(1, 50, 30, 20).tierWeightBoost(0.25);
        IntProvider modified = ModifiedIntProvider.of(base, ContentModifier.multiplier(40));
        JsonElement json = IntProvider.CODEC.encodeStart(JsonOps.INSTANCE, modified)
                .getOrThrow(false, GTCEu.LOGGER::error);
        IntProvider back = IntProvider.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, GTCEu.LOGGER::error);
        helper.assertTrue(back instanceof SummedInt, "decoded provider should be a SummedInt");
        helper.assertTrue(back.getMinValue() == 40 && back.getMaxValue() == 120, "bounds changed");
        helper.assertTrue(((SummedInt) back).getTierWeightBoost() == 0.25, "the tier boost was lost");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void summedIntTierWeightBoostPassesThroughParallelsTest(GameTestHelper helper) {
        WeightedInt base = WeightedInt.of(1, 1, 1, 1).tierWeightBoost(0.5);
        StatisticalInt summed = (StatisticalInt) ModifiedIntProvider.of(base, ContentModifier.multiplier(4));
        helper.assertTrue(summed.hasTierWeightBoost(), "the parallel provider must keep the tier boost");
        helper.assertTrue(Math.abs(summed.mean() - 8) < 1e-9, "untilted mean should be 4 x 2 = 8");
        StatisticalInt tilted = summed.withTierDiff(2);
        helper.assertTrue(tilted != summed, "a tier difference must produce a new provider");
        helper.assertTrue(tilted.getMinValue() == 4 && tilted.getMaxValue() == 12, "bounds must not change");
        // the tilted weights are 1, 1.5 and 2, so the mean is 10 / 4.5 per roll and there are 4 rolls
        helper.assertTrue(Math.abs(tilted.mean() - 80.0 / 9) < 1e-9, "tilted mean was " + tilted.mean());
        helper.assertTrue(!tilted.hasTierWeightBoost() && tilted.withTierDiff(2) == tilted,
                "tilting must not compound");
        helper.assertTrue(summed.withTierDiff(0) == summed, "no tier difference must return the same instance");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void summedIntMidRollUsesTheProviderMeanTest(GameTestHelper helper) {
        IntProviderIngredient ingredient = IntProviderIngredient.of(new ItemStack(Items.STONE),
                WeightedInt.of(1, 50, 30, 20));
        helper.assertTrue(Math.abs(ingredient.getMidRoll() - 1.7) < 1e-9,
                "mid roll should be the weighted mean 1.7, got " + ingredient.getMidRoll());
        IntProviderIngredient uniform = IntProviderIngredient.of(new ItemStack(Items.STONE), UniformInt.of(1, 3));
        helper.assertTrue(Math.abs(uniform.getMidRoll() - 2.0) < 1e-9, "uniform mid roll should stay (min+max)/2");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void summedIntMidRollFollowsTheTierDiffTest(GameTestHelper helper) {
        IntProviderIngredient boosted = IntProviderIngredient.of(new ItemStack(Items.STONE),
                WeightedInt.of(1, 1, 1, 1).tierWeightBoost(0.5));
        helper.assertTrue(Math.abs(boosted.getMidRoll(0) - 2.0) < 1e-9, "diff 0 should give the base mean");
        helper.assertTrue(Math.abs(boosted.getMidRoll(2) - 10 / 4.5) < 1e-9,
                "diff 2 should give the tilted mean, got " + boosted.getMidRoll(2));
        IntProviderIngredient uniform = IntProviderIngredient.of(new ItemStack(Items.STONE), UniformInt.of(1, 3));
        helper.assertTrue(Math.abs(uniform.getMidRoll(5) - 2.0) < 1e-9, "uniform ignores the tier difference");
        helper.succeed();
    }
}
