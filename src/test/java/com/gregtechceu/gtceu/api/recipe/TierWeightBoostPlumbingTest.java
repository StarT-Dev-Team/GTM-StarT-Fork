package com.gregtechceu.gtceu.api.recipe;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderFluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.valueprovider.StatisticalInt;
import com.gregtechceu.gtceu.common.valueprovider.WeightedInt;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
@GameTestHolder(GTCEu.MOD_ID)
public class TierWeightBoostPlumbingTest {

    @GameTest(template = "empty")
    public static void tierWeightBoostPlumbingItemCapabilityTiltsOnlyBoostedTest(GameTestHelper helper) {
        ItemStack stone = new ItemStack(Items.STONE);
        IntProviderIngredient boosted = IntProviderIngredient.of(stone,
                WeightedInt.of(1, 1, 1, 1).tierWeightBoost(0.5));
        ItemRecipeCapability cap = ItemRecipeCapability.CAP;

        helper.assertTrue(cap.copyWithTierDiff(boosted, 0) == boosted, "diff 0 must return the same instance");

        Ingredient tilted = cap.copyWithTierDiff(boosted, 2);
        helper.assertTrue(tilted != boosted && tilted instanceof IntProviderIngredient, "diff 2 must tilt");
        IntProviderIngredient tiltedIngredient = (IntProviderIngredient) tilted;
        helper.assertTrue(Math.abs(((StatisticalInt) tiltedIngredient.getCountProvider()).mean() - 10 / 4.5) < 1e-9,
                "the tilted copy must roll from the tilted odds");
        helper.assertTrue(tiltedIngredient.getSampledCount() == -1, "the tilted copy must be unrolled");
        helper.assertTrue(((StatisticalInt) boosted.getCountProvider()).mean() == 2.0, "the original must not change");
        helper.assertTrue(cap.copyContentWithTierDiff(boosted, 2) instanceof IntProviderIngredient,
                "the untyped entry point must work too");

        IntProviderIngredient plain = IntProviderIngredient.of(stone, WeightedInt.of(1, 1, 1, 1));
        helper.assertTrue(cap.copyWithTierDiff(plain, 2) == plain, "no boost means no copy");
        IntProviderIngredient uniform = IntProviderIngredient.of(stone, UniformInt.of(1, 3));
        helper.assertTrue(cap.copyWithTierDiff(uniform, 2) == uniform, "uniform ranges are never copied");
        Ingredient ordinary = Ingredient.of(new ItemStack(Items.STONE));
        helper.assertTrue(cap.copyWithTierDiff(ordinary, 2) == ordinary, "ordinary ingredients are never copied");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tierWeightBoostPlumbingFluidCapabilityTiltsOnlyBoostedTest(GameTestHelper helper) {
        FluidIngredient water = FluidIngredient.of(GTMaterials.Water.getFluid(1));
        IntProviderFluidIngredient boosted = IntProviderFluidIngredient.of(water,
                WeightedInt.of(1, 1, 1, 1).tierWeightBoost(0.5));
        FluidRecipeCapability cap = FluidRecipeCapability.CAP;

        helper.assertTrue(cap.copyWithTierDiff(boosted, 0) == boosted, "diff 0 must return the same instance");

        FluidIngredient tilted = cap.copyWithTierDiff(boosted, 2);
        helper.assertTrue(tilted != boosted && tilted instanceof IntProviderFluidIngredient, "diff 2 must tilt");
        IntProviderFluidIngredient tiltedIngredient = (IntProviderFluidIngredient) tilted;
        helper.assertTrue(Math.abs(((StatisticalInt) tiltedIngredient.getCountProvider()).mean() - 10 / 4.5) < 1e-9,
                "the tilted copy must roll from the tilted odds");

        IntProviderFluidIngredient plain = IntProviderFluidIngredient.of(water, WeightedInt.of(1, 1, 1, 1));
        helper.assertTrue(cap.copyWithTierDiff(plain, 2) == plain, "no boost means no copy");
        FluidIngredient ordinary = FluidIngredient.of(GTMaterials.Water.getFluid(1000));
        helper.assertTrue(cap.copyWithTierDiff(ordinary, 2) == ordinary, "ordinary ingredients are never copied");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tierWeightBoostPlumbingParallelProvidersKeepTheirBoostTest(GameTestHelper helper) {
        WeightedInt base = WeightedInt.of(1, 1, 1, 1).tierWeightBoost(0.5);
        var summed = com.gregtechceu.gtceu.common.valueprovider.ModifiedIntProvider.of(base,
                com.gregtechceu.gtceu.api.recipe.content.ContentModifier.multiplier(4));
        IntProviderIngredient parallel = IntProviderIngredient.of(new ItemStack(Items.STONE), summed);
        Ingredient tilted = ItemRecipeCapability.CAP.copyWithTierDiff(parallel, 2);
        helper.assertTrue(tilted != parallel, "a boosted parallel ingredient must tilt");
        helper.assertTrue(Math.abs(((IntProviderIngredient) tilted).getMidRoll() - 80.0 / 9) < 1e-9,
                "the parallel mean must follow the tilt");
        helper.succeed();
    }
}
