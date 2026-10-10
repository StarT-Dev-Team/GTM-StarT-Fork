package com.gregtechceu.gtceu.common.valueprovider;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

@PrefixGameTestTemplate(false)
@GameTestHolder(GTCEu.MOD_ID)
public class WeightedIntTest {

    @GameTest(template = "empty")
    public static void weightedIntChancesAreNormalizedAndStatsMatchTest(GameTestHelper helper) {
        WeightedInt w = WeightedInt.of(1, 50, 30, 20);
        helper.assertTrue(Math.abs(w.getChance(1) - 0.5) < 1e-9, "chance(1) should be 0.5");
        helper.assertTrue(Math.abs(w.getChance(2) - 0.3) < 1e-9, "chance(2) should be 0.3");
        helper.assertTrue(Math.abs(w.getChance(3) - 0.2) < 1e-9, "chance(3) should be 0.2");
        helper.assertTrue(w.getChance(4) == 0 && w.getChance(0) == 0, "outside the table must be 0");
        helper.assertTrue(w.getMinValue() == 1 && w.getMaxValue() == 3, "min/max should be 1 and 3");
        helper.assertTrue(Math.abs(w.mean() - 1.7) < 1e-9, "mean should be 1.7");
        helper.assertTrue(Math.abs(w.variance() - 0.61) < 1e-9, "variance should be 0.61");
        helper.assertTrue(!w.hasTierWeightBoost(), "no tier boost by default");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void weightedIntMinMaxIgnoreZeroWeightEndsTest(GameTestHelper helper) {
        WeightedInt w = WeightedInt.of(1, 0, 10, 5, 0);
        helper.assertTrue(w.getMinValue() == 2, "min should skip the leading zero weight, got " + w.getMinValue());
        helper.assertTrue(w.getMaxValue() == 3, "max should skip the trailing zero weight, got " + w.getMaxValue());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void weightedIntSamplingFollowsWeightsTest(GameTestHelper helper) {
        WeightedInt w = WeightedInt.of(0, 70, 25, 5);
        RandomSource random = RandomSource.create(1234L);
        int[] counts = new int[3];
        int rolls = 100_000;
        for (int i = 0; i < rolls; i++) {
            counts[w.sample(random)]++;
        }
        helper.assertTrue(Math.abs(counts[0] / (double) rolls - 0.70) < 0.01, "0 should be ~70%");
        helper.assertTrue(Math.abs(counts[1] / (double) rolls - 0.25) < 0.01, "1 should be ~25%");
        helper.assertTrue(Math.abs(counts[2] / (double) rolls - 0.05) < 0.01, "2 should be ~5%");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void weightedIntZeroWeightEntriesAreNeverSampledTest(GameTestHelper helper) {
        WeightedInt w = WeightedInt.of(1, 0, 1, 0, 1);
        RandomSource random = RandomSource.create(99L);
        for (int i = 0; i < 20_000; i++) {
            int v = w.sample(random);
            helper.assertTrue(v == 2 || v == 4, "rolled a zero-weight amount: " + v);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void weightedIntOfRejectsInvalidInputTest(GameTestHelper helper) {
        helper.assertTrue(throwsIllegalArgument(() -> WeightedInt.of(0)), "empty weights must throw");
        helper.assertTrue(throwsIllegalArgument(() -> WeightedInt.of(0, 0, 0)), "all-zero weights must throw");
        helper.assertTrue(throwsIllegalArgument(() -> WeightedInt.of(0, 1, -1)), "negative weight must throw");
        helper.assertTrue(throwsIllegalArgument(() -> WeightedInt.of(0, 1, Double.NaN)), "NaN weight must throw");
        helper.assertTrue(throwsIllegalArgument(() -> WeightedInt.of(-1, 1)), "negative min must throw");
        helper.assertTrue(throwsIllegalArgument(() -> WeightedInt.of(0, new double[WeightedInt.MAX_ENTRIES + 1])),
                "oversized table must throw");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void weightedIntTierDiffTiltsWeightsTowardTheTopTest(GameTestHelper helper) {
        WeightedInt base = WeightedInt.of(1, 1, 1, 1).tierWeightBoost(0.5);
        helper.assertTrue(base.hasTierWeightBoost() && base.getTierWeightBoost() == 0.5, "boost should be stored");
        helper.assertTrue(base.withTierDiff(0) == base, "no tier difference must return the same instance");
        WeightedInt tilted = base.withTierDiff(2);
        // the factors are 1.0, 1.5 and 2.0 at positions 0, 0.5 and 1, so the total weight is 4.5
        helper.assertTrue(Math.abs(tilted.getChance(1) - 1 / 4.5) < 1e-9, "chance(1) was " + tilted.getChance(1));
        helper.assertTrue(Math.abs(tilted.getChance(2) - 1.5 / 4.5) < 1e-9, "chance(2) was " + tilted.getChance(2));
        helper.assertTrue(Math.abs(tilted.getChance(3) - 2 / 4.5) < 1e-9, "chance(3) was " + tilted.getChance(3));
        helper.assertTrue(!tilted.hasTierWeightBoost(), "a tilted copy must not tilt again");
        helper.assertTrue(tilted.withTierDiff(2) == tilted, "tilting a tilted copy must be a no-op");
        helper.assertTrue(Math.abs(base.getChance(1) - 1 / 3.0) < 1e-9, "the original must not change");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void weightedIntNegativeBoostOnlyShrinksTheRangeTest(GameTestHelper helper) {
        // the factors are 1, 0 and -1 at diff 4, and the negative one is clamped to 0
        WeightedInt tilted = WeightedInt.of(1, 1, 1, 1).tierWeightBoost(-0.5).withTierDiff(4);
        helper.assertTrue(tilted.getMinValue() == 1 && tilted.getMaxValue() == 1, "only the lowest amount is left");
        helper.assertTrue(Math.abs(tilted.getChance(1) - 1) < 1e-9, "chance(1) should be 1");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void weightedIntTiltedSupportIsNeverLargerThanTheBaseTest(GameTestHelper helper) {
        WeightedInt base = WeightedInt.of(1, 0, 1, 1, 0);
        for (double boost : new double[] { -2, -0.5, 0.5, 2 }) {
            WeightedInt boosted = base.tierWeightBoost(boost);
            for (int diff = 1; diff <= 6; diff++) {
                WeightedInt tilted = boosted.withTierDiff(diff);
                helper.assertTrue(tilted.getMinValue() >= base.getMinValue() &&
                        tilted.getMaxValue() <= base.getMaxValue(),
                        "tilt widened the range for boost " + boost + " diff " + diff);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void weightedIntTierWeightBoostRejectsNonFiniteValuesTest(GameTestHelper helper) {
        WeightedInt base = WeightedInt.of(1, 1, 1);
        helper.assertTrue(throwsIllegalArgument(() -> base.tierWeightBoost(Double.NaN)), "NaN boost must throw");
        helper.assertTrue(throwsIllegalArgument(() -> base.tierWeightBoost(Double.POSITIVE_INFINITY)),
                "infinite boost must throw");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void weightedIntCodecRoundTripsTest(GameTestHelper helper) {
        WeightedInt w = WeightedInt.of(2, 5, 0, 3);
        JsonElement json = IntProvider.CODEC.encodeStart(JsonOps.INSTANCE, w)
                .getOrThrow(false, GTCEu.LOGGER::error);
        IntProvider back = IntProvider.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(false, GTCEu.LOGGER::error);
        helper.assertTrue(back instanceof WeightedInt, "decoded provider should be a WeightedInt");
        WeightedInt decoded = (WeightedInt) back;
        helper.assertTrue(decoded.getMinValue() == 2 && decoded.getMaxValue() == 4, "bounds changed");
        helper.assertTrue(Math.abs(decoded.getChance(2) - 0.625) < 1e-9, "chance(2) changed");
        helper.assertTrue(decoded.getChance(3) == 0, "chance(3) changed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void weightedIntTierWeightBoostSurvivesCodecTest(GameTestHelper helper) {
        WeightedInt boosted = WeightedInt.of(1, 1, 2).tierWeightBoost(0.25);
        JsonElement json = IntProvider.CODEC.encodeStart(JsonOps.INSTANCE, boosted)
                .getOrThrow(false, GTCEu.LOGGER::error);
        IntProvider back = IntProvider.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(false, GTCEu.LOGGER::error);
        helper.assertTrue(back instanceof WeightedInt w && w.getTierWeightBoost() == 0.25,
                "tier_weight_boost was lost");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void weightedIntOldJsonWithoutTierWeightBoostStillLoadsTest(GameTestHelper helper) {
        JsonObject old = new JsonObject();
        old.addProperty("type", "gtceu:weighted");
        old.addProperty("min", 1);
        JsonArray weights = new JsonArray();
        weights.add(1.0);
        weights.add(2.0);
        old.add("weights", weights);
        IntProvider loaded = IntProvider.CODEC.parse(JsonOps.INSTANCE, old).getOrThrow(false, GTCEu.LOGGER::error);
        helper.assertTrue(loaded instanceof WeightedInt w && w.getTierWeightBoost() == 0,
                "JSON without tier_weight_boost must load with boost 0");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void weightedIntCodecRejectsNegativeWeightTest(GameTestHelper helper) {
        JsonObject bad = new JsonObject();
        bad.addProperty("type", "gtceu:weighted");
        bad.addProperty("min", 0);
        JsonArray weights = new JsonArray();
        weights.add(-1.0);
        weights.add(2.0);
        bad.add("weights", weights);
        var result = IntProvider.CODEC.parse(JsonOps.INSTANCE, bad);
        helper.assertTrue(result.result().isEmpty(), "negative weight must be a codec error, not a value");
        helper.succeed();
    }

    private static boolean throwsIllegalArgument(Runnable action) {
        try {
            action.run();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }
}
