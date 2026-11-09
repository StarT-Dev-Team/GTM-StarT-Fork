package com.gregtechceu.gtceu.api.recipe;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.recipe.chance.boost.ChanceBoostFunction;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
@GameTestHolder(GTCEu.MOD_ID)
public class ChanceBoostFunctionTest {

    @GameTest(template = "empty")
    public static void chanceBoostFunctionTierDiffFollowsTheOverclockRulesTest(GameTestHelper helper) {
        ChanceBoostFunction f = ChanceBoostFunction.OVERCLOCK;
        helper.assertTrue(f.getTierDiff(GTValues.LV, GTValues.HV) == 2, "LV -> HV is 2 tiers");
        helper.assertTrue(f.getTierDiff(GTValues.HV, GTValues.HV) == 0, "same tier is 0");
        helper.assertTrue(f.getTierDiff(GTValues.HV, GTValues.LV) == 0, "a lower tier is 0, never negative");
        helper.assertTrue(f.getTierDiff(GTValues.ULV, GTValues.LV) == 0, "LV does not boost over ULV");
        helper.assertTrue(f.getTierDiff(GTValues.ULV, GTValues.HV) == 2, "ULV -> HV skips the free LV tier");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chanceBoostFunctionNoneNeverBoostsTest(GameTestHelper helper) {
        Content content = new Content(null, 2000, ChanceLogic.getMaxChancedValue(), 500);
        helper.assertTrue(ChanceBoostFunction.NONE.getTierDiff(GTValues.LV, GTValues.HV) == 0,
                "NONE must report a tier difference of 0");
        helper.assertTrue(ChanceBoostFunction.NONE.getBoostedChance(content, GTValues.LV, GTValues.HV) == 2000,
                "NONE must not change the chance");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chanceBoostFunctionOverclockChanceBehaviourIsUnchangedTest(GameTestHelper helper) {
        int max = ChanceLogic.getMaxChancedValue();
        Content content = new Content(null, 2000, max, 500);
        ChanceBoostFunction f = ChanceBoostFunction.OVERCLOCK;
        helper.assertTrue(f.getBoostedChance(content, GTValues.LV, GTValues.HV) == 3000, "LV -> HV adds 2 x 500");
        helper.assertTrue(f.getBoostedChance(content, GTValues.ULV, GTValues.LV) == 2000, "LV over ULV adds nothing");
        helper.assertTrue(f.getBoostedChance(content, GTValues.ULV, GTValues.HV) == 3000, "ULV -> HV adds 2 x 500");
        helper.assertTrue(f.getBoostedChance(content, GTValues.HV, GTValues.LV) == 2000, "a lower tier adds nothing");
        helper.assertTrue(f.getBoostedChance(content, GTValues.HV, GTValues.HV) == 2000, "the same tier adds nothing");
        Content nearMax = new Content(null, max - 100, max, 500);
        helper.assertTrue(f.getBoostedChance(nearMax, GTValues.LV, GTValues.HV) == max,
                "the boost clamps to maxChance");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chanceBoostFunctionFixedPinsTheChanceAndNeverTiersTest(GameTestHelper helper) {
        Content content = new Content(null, 2000, ChanceLogic.getMaxChancedValue(), 500);
        ChanceBoostFunction pinned = ChanceBoostFunction.fixed(1234);
        helper.assertTrue(pinned.getBoostedChance(content, GTValues.LV, GTValues.HV) == 1234, "chance must be pinned");
        helper.assertTrue(pinned.getTierDiff(GTValues.LV, GTValues.HV) == 0, "a pinned chance never tiers");
        helper.succeed();
    }
}
