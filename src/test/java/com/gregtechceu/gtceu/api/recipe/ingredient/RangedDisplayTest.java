package com.gregtechceu.gtceu.api.recipe.ingredient;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.common.valueprovider.WeightedInt;
import com.gregtechceu.gtceu.common.valueprovider.distribution.Distributions;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@PrefixGameTestTemplate(false)
@GameTestHolder(GTCEu.MOD_ID)
public class RangedDisplayTest {

    private static TranslatableContents contents(Component component) {
        return (TranslatableContents) component.getContents();
    }

    @GameTest(template = "empty")
    public static void rangedDisplayFormatMeanStaysShortEnoughForTheSlotTest(GameTestHelper helper) {
        helper.assertTrue(RangedDisplay.formatMean(5.0).equals("5"), "5.0 -> 5, got " + RangedDisplay.formatMean(5.0));
        helper.assertTrue(RangedDisplay.formatMean(4.62).equals("4.6"), "4.62 -> 4.6");
        helper.assertTrue(RangedDisplay.formatMean(99.94).equals("99.9"), "99.94 -> 99.9");
        helper.assertTrue(RangedDisplay.formatMean(123.4).equals("123"), "123.4 -> 123");
        helper.assertTrue(RangedDisplay.formatMean(0.04).equals("0"), "0.04 -> 0");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rangedDisplaySlotAmountIsOnlyForStatisticalProvidersTest(GameTestHelper helper) {
        helper.assertTrue(RangedDisplay.slotAmount(UniformInt.of(1, 9), 0).isEmpty(),
                "uniform ranges keep the min-max label");
        var amount = RangedDisplay.slotAmount(WeightedInt.of(1, 50, 30, 20), 0);
        helper.assertTrue(amount.isPresent(), "weighted amounts get an average label");
        helper.assertTrue(Math.abs(amount.get().mean() - 1.7) < 1e-9, "wrong mean " + amount.get().mean());
        helper.assertTrue(amount.get().shift() == RangedDisplay.Shift.NONE, "no boost means no shift");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rangedDisplaySlotAmountFollowsTheOcTierTest(GameTestHelper helper) {
        WeightedInt up = WeightedInt.of(1, 1, 1, 1).tierWeightBoost(0.5);
        var atBase = RangedDisplay.slotAmount(up, 0).get();
        helper.assertTrue(atBase.shift() == RangedDisplay.Shift.NONE && Math.abs(atBase.mean() - 2.0) < 1e-9,
                "no tier difference must show the base average");
        var higher = RangedDisplay.slotAmount(up, 2).get();
        helper.assertTrue(higher.shift() == RangedDisplay.Shift.UP && Math.abs(higher.mean() - 10 / 4.5) < 1e-9,
                "a positive boost raises the average, got " + higher.mean());
        var lower = RangedDisplay.slotAmount(WeightedInt.of(1, 1, 1, 1).tierWeightBoost(-0.25), 1).get();
        helper.assertTrue(lower.shift() == RangedDisplay.Shift.DOWN, "a negative boost lowers the average");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rangedDisplayAverageLineIsPlainTest(GameTestHelper helper) {
        var line = RangedDisplay.averageLine(WeightedInt.of(1, 50, 30, 20));
        helper.assertTrue(line.isPresent() && contents(line.get()).getKey().equals("gtceu.gui.content.average"),
                "weighted providers have an average line");
        helper.assertTrue(contents(line.get()).getArgs()[0].equals(FormattingUtil.formatNumber2Places(1.7)),
                "wrong average");
        helper.assertTrue(RangedDisplay.averageLine(UniformInt.of(1, 3)).isEmpty(), "uniform has no average line");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rangedDisplaySummaryWithoutBoostIsOneLineTest(GameTestHelper helper) {
        List<Component> lines = RangedDisplay.summaryLines(WeightedInt.of(1, 50, 30, 20), 3);
        helper.assertTrue(lines.size() == 1, "expected only the average, got " + lines.size());
        helper.assertTrue(contents(lines.get(0)).getKey().equals("gtceu.gui.content.average"), "wrong key");
        helper.assertTrue(RangedDisplay.summaryLines(UniformInt.of(1, 3), 3).isEmpty(), "uniform has no summary");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rangedDisplaySummaryWithBoostMirrorsTheChanceTooltipTest(GameTestHelper helper) {
        List<Component> lines = RangedDisplay.summaryLines(WeightedInt.of(1, 1, 1, 1).tierWeightBoost(0.5), 2);
        helper.assertTrue(lines.size() == 3, "expected base, boost and at-tier lines, got " + lines.size());
        helper.assertTrue(contents(lines.get(0)).getKey().equals("gtceu.gui.content.average_base") &&
                contents(lines.get(0)).getArgs()[0].equals(FormattingUtil.formatNumber2Places(2.0)),
                "wrong base average line");
        helper.assertTrue(contents(lines.get(1)).getKey().equals("gtceu.gui.content.average_tier_boost_plus") &&
                contents(lines.get(1)).getArgs()[0].equals(FormattingUtil.formatNumber2Places(50f)),
                "wrong tier boost line");
        helper.assertTrue(contents(lines.get(2)).getKey().equals("gtceu.gui.content.average_boosted") &&
                contents(lines.get(2)).getArgs()[0].equals(FormattingUtil.formatNumber2Places(10 / 4.5)),
                "wrong at-tier average line");
        // at the recipe's own tier, the at-tier line equals the base line
        List<Component> atBase = RangedDisplay.summaryLines(WeightedInt.of(1, 1, 1, 1).tierWeightBoost(0.5), 0);
        helper.assertTrue(contents(atBase.get(2)).getArgs()[0].equals(FormattingUtil.formatNumber2Places(2.0)),
                "tier 0 must equal the base average");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rangedDisplayOddsListEveryPossibleAmountTest(GameTestHelper helper) {
        List<Component> lines = RangedDisplay.oddsLines(WeightedInt.of(1, 50, 0, 20), 0);
        helper.assertTrue(lines.size() == 2, "zero-weight amounts must not be listed, got " + lines.size());
        helper.assertTrue(lines.get(0).getSiblings().isEmpty(), "no boost means no change marker");
        helper.assertTrue(RangedDisplay.oddsLines(UniformInt.of(1, 3), 0).isEmpty(), "uniform has no odds rows");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rangedDisplayOddsShowTheChangeAtTheDisplayedTierTest(GameTestHelper helper) {
        // the base odds are 33.3 each, and 22.2, 33.3 and 44.4 at diff 2
        List<Component> lines = RangedDisplay.oddsLines(WeightedInt.of(1, 1, 1, 1).tierWeightBoost(0.5), 2);
        helper.assertTrue(lines.size() == 3, "all base amounts are listed, got " + lines.size());
        helper.assertTrue(contents(lines.get(0)).getArgs()[1].equals(FormattingUtil.formatNumber2Places(100 / 4.5)),
                "wrong chance for 1x");
        helper.assertTrue(lines.get(0).getSiblings().size() == 1, "1x changed, so it gets a change marker");
        helper.assertTrue(lines.get(1).getSiblings().isEmpty(), "2x did not change, so no marker");
        helper.assertTrue(contents(lines.get(2)).getArgs()[1].equals(FormattingUtil.formatNumber2Places(200 / 4.5)),
                "wrong chance for 3x");
        helper.assertTrue(lines.get(2).getSiblings().size() == 1, "3x changed, so it gets a change marker");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rangedDisplayOddsKeepBaseAmountsWhenTheTierRemovesOneTest(GameTestHelper helper) {
        // at diff 4 only 1x is still possible, and 2x and 3x stay listed at 0
        List<Component> lines = RangedDisplay.oddsLines(WeightedInt.of(1, 1, 1, 1).tierWeightBoost(-0.5), 4);
        helper.assertTrue(lines.size() == 3, "removed amounts must stay visible, got " + lines.size());
        helper.assertTrue(contents(lines.get(1)).getArgs()[1].equals(FormattingUtil.formatNumber2Places(0.0)),
                "2x should show 0");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rangedDisplayOddsCollapseForWideRangesTest(GameTestHelper helper) {
        List<Component> lines = RangedDisplay.oddsLines(Distributions.GAUSS.build(0, 100), 0);
        helper.assertTrue(lines.size() == 1, "wide ranges collapse to one line, got " + lines.size());
        helper.assertTrue(contents(lines.get(0)).getKey().equals("gtceu.gui.content.odds_most_likely"), "wrong key");
        helper.assertTrue(contents(lines.get(0)).getArgs()[0].equals(50), "most likely amount should be 50");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rangedDisplayTinyChanceIsNotShownAsZeroTest(GameTestHelper helper) {
        // 9 needs all 8 tries to succeed: 0.25^8 is about 0.0015%, which would round to 0
        List<Component> lines = RangedDisplay.oddsLines(
                Distributions.targetAverage(3, Distributions.BINOMIAL).build(1, 9), 0);
        helper.assertTrue(lines.size() == 9, "all nine amounts are possible, got " + lines.size());
        helper.assertTrue(contents(lines.get(8)).getArgs()[1].equals("<0.01"), "9x must show <0.01");
        helper.assertTrue(!contents(lines.get(7)).getArgs()[1].equals("<0.01"), "8x is large enough to show");
        helper.succeed();
    }
}
