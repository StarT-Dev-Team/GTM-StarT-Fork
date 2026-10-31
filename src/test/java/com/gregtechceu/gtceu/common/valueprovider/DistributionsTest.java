package com.gregtechceu.gtceu.common.valueprovider;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.common.valueprovider.distribution.Distributions;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@PrefixGameTestTemplate(false)
@GameTestHolder(GTCEu.MOD_ID)
public class DistributionsTest {

    @GameTest(template = "empty")
    public static void distributionsGaussMatchesBellCurveTest(GameTestHelper helper) {
        WeightedInt w = Distributions.gauss(5, 2).build(1, 9);
        double[] expected = { 0.0401, 0.0655, 0.1210, 0.1747, 0.1974, 0.1747, 0.1210, 0.0655, 0.0401 };
        for (int i = 0; i < expected.length; i++) {
            helper.assertTrue(Math.abs(w.getChance(1 + i) - expected[i]) < 0.002,
                    "gauss chance for " + (1 + i) + " was " + w.getChance(1 + i) + ", expected " + expected[i]);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void distributionsGaussPresetIsCenteredAndHandlesSingleValueTest(GameTestHelper helper) {
        WeightedInt w = Distributions.GAUSS.build(1, 9);
        helper.assertTrue(Math.abs(w.mean() - 5) < 1e-6, "GAUSS mean should be the middle, got " + w.mean());
        helper.assertTrue(w.getChance(5) > w.getChance(4) && w.getChance(4) > w.getChance(1), "should be bell shaped");
        WeightedInt single = Distributions.GAUSS.build(3, 3);
        helper.assertTrue(Math.abs(single.getChance(3) - 1) < 1e-9, "single value must have chance 1");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void distributionsBinomialMatchesKnownOddsTest(GameTestHelper helper) {
        WeightedInt w = Distributions.binomial(0.25).build(0, 4);
        double[] expected = { 0.3164, 0.4219, 0.2109, 0.0469, 0.0039 };
        for (int i = 0; i < expected.length; i++) {
            helper.assertTrue(Math.abs(w.getChance(i) - expected[i]) < 0.001,
                    "binomial chance for " + i + " was " + w.getChance(i));
        }
        // a large n must not underflow to all zero weights
        WeightedInt big = Distributions.binomial(0.5).build(0, 2000);
        helper.assertTrue(Math.abs(big.mean() - 1000) < 1, "large binomial mean should be ~1000, got " + big.mean());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void distributionsGeometricIsCutAtMaxTest(GameTestHelper helper) {
        WeightedInt w = Distributions.geometric(0.5).build(1, 5);
        double[] expected = { 0.5161, 0.2581, 0.1290, 0.0645, 0.0323 };
        for (int i = 0; i < expected.length; i++) {
            helper.assertTrue(Math.abs(w.getChance(1 + i) - expected[i]) < 0.001,
                    "geometric chance for " + (1 + i) + " was " + w.getChance(1 + i));
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void distributionsTargetAverageHitsTheRequestedAverageTest(GameTestHelper helper) {
        helper.assertTrue(Math.abs(Distributions.targetAverage(3).build(1, 9).mean() - 3) < 1e-6,
                "tilt average should be 3");
        helper.assertTrue(Math.abs(Distributions.targetAverage(5).build(1, 9).getChance(1) -
                Distributions.targetAverage(5).build(1, 9).getChance(9)) < 1e-6,
                "an average at the middle should be flat");
        helper.assertTrue(
                Math.abs(Distributions.targetAverage(3, Distributions.BINOMIAL).build(1, 9).mean() - 3) < 1e-6,
                "binomial average should be 3");
        helper.assertTrue(
                Math.abs(Distributions.targetAverage(2, Distributions.GEOMETRIC).build(1, 9).mean() - 2) < 1e-6,
                "geometric average should be 2");
        helper.assertTrue(Math.abs(Distributions.targetAverage(3, Distributions.GAUSS).build(1, 9).mean() - 3) < 0.2,
                "gauss average should be close to 3");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void distributionsTargetAverageRejectsOutOfRangeTest(GameTestHelper helper) {
        helper.assertTrue(throwsIllegalArgument(() -> Distributions.targetAverage(10).build(1, 9)),
                "an average above max must throw");
        helper.assertTrue(throwsIllegalArgument(() -> Distributions.targetAverage(1).build(1, 9)),
                "an average at min must throw");
        helper.assertTrue(throwsIllegalArgument(() -> Distributions.targetAverage(3).build(4, 4)),
                "single value range must throw");
        helper.assertTrue(
                throwsIllegalArgument(() -> Distributions.targetAverage(6, Distributions.GEOMETRIC).build(1, 9)),
                "a geometric average above the middle must throw");
        helper.assertTrue(
                throwsIllegalArgument(() -> Distributions.targetAverage(9, Distributions.BINOMIAL).build(1, 9)),
                "a binomial average at max must throw");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void distributionsFactoriesRejectBadParametersTest(GameTestHelper helper) {
        helper.assertTrue(throwsIllegalArgument(() -> Distributions.gauss(5, 0)), "sd 0 must throw");
        helper.assertTrue(throwsIllegalArgument(() -> Distributions.binomial(0)), "p 0 must throw");
        helper.assertTrue(throwsIllegalArgument(() -> Distributions.binomial(1)), "p 1 must throw");
        helper.assertTrue(throwsIllegalArgument(() -> Distributions.geometric(1.5)), "p 1.5 must throw");
        helper.assertTrue(throwsIllegalArgument(() -> Distributions.GAUSS.build(5, 1)), "min > max must throw");
        helper.assertTrue(throwsIllegalArgument(() -> Distributions.GAUSS.build(-1, 3)), "negative min must throw");
        helper.assertTrue(throwsIllegalArgument(() -> Distributions.GAUSS.build(0, WeightedInt.MAX_ENTRIES)),
                "range larger than the table cap must throw");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void distributionsFunctionWeightsAreRelativeTest(GameTestHelper helper) {
        WeightedInt w = Distributions.fromFunction(n -> 1.0 / n).build(1, 5);
        helper.assertTrue(Math.abs(w.getChance(1) - 0.43796) < 0.0005, "1/n chance(1) was " + w.getChance(1));
        helper.assertTrue(throwsIllegalArgument(() -> Distributions.fromFunction(n -> -1).build(1, 3)),
                "negative function result must throw");
        helper.assertTrue(throwsIllegalArgument(() -> Distributions.fromFunction(n -> Double.NaN).build(1, 3)),
                "NaN function result must throw");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void distributionsFromMapAcceptsStringKeysTest(GameTestHelper helper) {
        Map<Object, Object> map = new LinkedHashMap<>();
        map.put(1, 50);
        map.put("2", 30.0);
        map.put(4, "20");
        WeightedInt w = Distributions.fromMap(map);
        helper.assertTrue(w.getMinValue() == 1 && w.getMaxValue() == 4, "bounds come from the keys");
        helper.assertTrue(Math.abs(w.getChance(1) - 0.5) < 1e-9, "chance(1) should be 0.5");
        helper.assertTrue(w.getChance(3) == 0, "missing key 3 should have weight 0");
        helper.assertTrue(Math.abs(w.getChance(4) - 0.2) < 1e-9, "chance(4) should be 0.2");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void distributionsFromMapRejectsBadInputTest(GameTestHelper helper) {
        helper.assertTrue(throwsIllegalArgument(() -> Distributions.fromMap(Map.of())), "empty map must throw");
        helper.assertTrue(throwsIllegalArgument(() -> Distributions.fromMap(Map.of(-1, 1))), "negative key must throw");
        helper.assertTrue(throwsIllegalArgument(() -> Distributions.fromMap(Map.of(1.5, 1))),
                "non-integer key must throw");
        helper.assertTrue(throwsIllegalArgument(() -> Distributions.fromMap(Map.of("abc", 1))), "text key must throw");
        helper.assertTrue(throwsIllegalArgument(() -> Distributions.fromMap(Map.of(1, "x"))), "text weight must throw");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void distributionsTierWeightBoostIsCarriedByTheBuiltTableTest(GameTestHelper helper) {
        WeightedInt plain = Distributions.GAUSS.build(1, 9);
        WeightedInt boosted = Distributions.GAUSS.tierWeightBoost(0.2).build(1, 9);
        helper.assertTrue(boosted.getTierWeightBoost() == 0.2, "boost should be attached");
        for (int amount = 1; amount <= 9; amount++) {
            helper.assertTrue(Math.abs(plain.getChance(amount) - boosted.getChance(amount)) < 1e-12,
                    "the base odds must not change for amount " + amount);
        }
        helper.assertTrue(
                Distributions.fromFunction(n -> 1.0).tierWeightBoost(0.1).build(1, 3).getTierWeightBoost() == 0.1,
                "function distributions can be boosted");
        helper.assertTrue(Distributions.fromMap(Map.of(1, 1, 2, 1)).tierWeightBoost(0.3).getTierWeightBoost() == 0.3,
                "map tables can be boosted");
        helper.assertTrue(throwsIllegalArgument(() -> Distributions.GAUSS.tierWeightBoost(Double.NaN).build(1, 9)),
                "a NaN boost must throw when built");
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
