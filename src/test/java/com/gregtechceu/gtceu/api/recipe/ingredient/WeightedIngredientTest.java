package com.gregtechceu.gtceu.api.recipe.ingredient;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.valueprovider.WeightedInt;
import com.gregtechceu.gtceu.common.valueprovider.distribution.Distributions;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.google.gson.JsonObject;

@PrefixGameTestTemplate(false)
@GameTestHolder(GTCEu.MOD_ID)
public class WeightedIngredientTest {

    @GameTest(template = "empty")
    public static void weightedIngredientWeightedZeroRollGivesEmptyStacksTest(GameTestHelper helper) {
        IntProviderIngredient ingredient = IntProviderIngredient.of(new ItemStack(Items.STONE),
                WeightedInt.of(0, 70, 25, 5));
        helper.assertTrue(ingredient.getCountProvider().getMinValue() == 0, "min should be 0");
        ingredient.setSampledCount(0);
        helper.assertTrue(ingredient.getItems().length == 0, "a rolled 0 must give no stacks");
        ingredient.reset();
        ingredient.setSampledCount(2);
        helper.assertTrue(ingredient.getItems()[0].getCount() == 2, "a rolled 2 must give a stack of 2");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void weightedIngredientMaxSizeStackUsesTheHighestPossibleRollTest(GameTestHelper helper) {
        IntProviderIngredient ingredient = IntProviderIngredient.of(new ItemStack(Items.STONE),
                Distributions.binomial(0.25).build(0, 4));
        helper.assertTrue(ingredient.getMaxSizeStack().getCount() == 4, "max stack should be 4");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void weightedIngredientWeightedIngredientSurvivesJsonTest(GameTestHelper helper) {
        IntProviderIngredient ingredient = IntProviderIngredient.of(new ItemStack(Items.STONE),
                WeightedInt.of(0, 70, 25, 5).tierWeightBoost(0.2));
        JsonObject json = ingredient.toJson().getAsJsonObject();
        IntProviderIngredient back = IntProviderIngredient.fromJson(json);
        helper.assertTrue(back.getCountProvider() instanceof WeightedInt, "provider type must survive JSON");
        WeightedInt weighted = (WeightedInt) back.getCountProvider();
        helper.assertTrue(Math.abs(weighted.getChance(0) - 0.70) < 1e-9, "chance(0) changed");
        helper.assertTrue(weighted.getMaxValue() == 2, "max changed");
        helper.assertTrue(weighted.getTierWeightBoost() == 0.2, "the tier boost must survive JSON");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void weightedIngredientJavaBuilderAddsWeightedContentTest(GameTestHelper helper) {
        GTRecipe recipe = GTRecipeTypes.CHEMICAL_RECIPES.recipeBuilder(GTCEu.id("test_weighted_java_builder"))
                .inputItemsWeighted(new ItemStack(Items.COBBLESTONE), 1, 4, Distributions.fromFunction(n -> 1.0))
                .outputItemsWeighted(new ItemStack(Items.STONE), 1, 9, Distributions.GAUSS.tierWeightBoost(0.2))
                .inputFluidsWeighted(GTMaterials.Water.getFluid(1), 100, 500, Distributions.gauss(300, 80))
                .outputFluidsWeighted(GTMaterials.Water.getFluid(1), 0, 4, Distributions.binomial(0.25))
                .duration(20)
                .EUt(GTValues.V[GTValues.LV])
                .buildRawRecipe();

        var itemIn = (IntProviderIngredient) recipe.getInputContents(ItemRecipeCapability.CAP).get(0).content;
        helper.assertTrue(itemIn.getCountProvider() instanceof WeightedInt &&
                itemIn.getCountProvider().getMinValue() == 1 && itemIn.getCountProvider().getMaxValue() == 4,
                "the item input should be a weighted 1..4");
        var itemOut = (IntProviderIngredient) recipe.getOutputContents(ItemRecipeCapability.CAP).get(0).content;
        helper.assertTrue(itemOut.getCountProvider() instanceof WeightedInt weighted &&
                weighted.getTierWeightBoost() == 0.2 && weighted.getMaxValue() == 9,
                "the item output should be a weighted 1..9 that keeps its tier weight boost");
        var fluidIn = (IntProviderFluidIngredient) recipe.getInputContents(FluidRecipeCapability.CAP).get(0).content;
        helper.assertTrue(fluidIn.getCountProvider() instanceof WeightedInt &&
                fluidIn.getCountProvider().getMinValue() == 100 && fluidIn.getCountProvider().getMaxValue() == 500,
                "the fluid input should be a weighted 100..500");
        var fluidOut = (IntProviderFluidIngredient) recipe.getOutputContents(FluidRecipeCapability.CAP).get(0).content;
        helper.assertTrue(fluidOut.getCountProvider() instanceof WeightedInt &&
                fluidOut.getCountProvider().getMinValue() == 0 && fluidOut.getCountProvider().getMaxValue() == 4,
                "the fluid output should be a weighted 0..4");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void weightedIngredientJavaBuilderRejectsBadDistributionsTest(GameTestHelper helper) {
        boolean threw = false;
        try {
            GTRecipeTypes.CHEMICAL_RECIPES.recipeBuilder(GTCEu.id("test_weighted_java_builder_bad"))
                    .outputItemsWeighted(new ItemStack(Items.STONE), 9, 1, Distributions.GAUSS);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        helper.assertTrue(threw, "min above max must throw from the builder");
        helper.succeed();
    }
}
