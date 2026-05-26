package com.gregtechceu.gtceu.common.data;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.data.medicalcondition.MedicalCondition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IOverclockMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.EnergyStack;
import com.gregtechceu.gtceu.api.recipe.modifier.IdentifiedRecipeModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.common.capability.EnvironmentalHazardSavedData;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.FusionReactorMachine;
import com.gregtechceu.gtceu.common.recipe.condition.EUToStartCondition;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.gregtechceu.gtceu.api.recipe.OverclockingLogic.*;

public class GTRecipeModifiers {

    public static final List<String> ignoreModifiers = new ArrayList<>(
            List.of("default_environment_requirement", "oc_non_perfect", "consume_eu_to_start"));

    /**
     * Given an {@link OverclockingLogic}, creates a {@link RecipeModifier} designed for an {@link IOverclockMachine}
     */
    public static final Function<OverclockingLogic, RecipeModifier> ELECTRIC_OVERCLOCK = Util
            .memoize(logic -> (machine, recipe) -> {
                if (!(machine instanceof IOverclockMachine overclockMachine)) return ModifierFunction.IDENTITY;
                if (RecipeHelper.getRecipeEUtTier(recipe) > overclockMachine.getMaxOverclockTier()) {
                    return ModifierFunction
                            .cancel(Component.translatable("gtceu.recipe_modifier.insufficient_voltage"));
                }
                return logic.getModifier(machine, recipe, overclockMachine.getOverclockVoltage());
            });

    // Shortcuts for common OC logics
    public static final RecipeModifier OC_PERFECT = new IdentifiedRecipeModifier("oc_perfect",
            ELECTRIC_OVERCLOCK.apply(PERFECT_OVERCLOCK));
    public static final RecipeModifier OC_NON_PERFECT = new IdentifiedRecipeModifier("oc_non_perfect",
            ELECTRIC_OVERCLOCK.apply(NON_PERFECT_OVERCLOCK));
    public static final RecipeModifier OC_PERFECT_SUBTICK = new IdentifiedRecipeModifier("oc_perfect_subtick",
            ELECTRIC_OVERCLOCK.apply(PERFECT_OVERCLOCK_SUBTICK));
    public static final RecipeModifier OC_NON_PERFECT_SUBTICK = new IdentifiedRecipeModifier("oc_non_perfect_subtick",
            ELECTRIC_OVERCLOCK.apply(NON_PERFECT_OVERCLOCK_SUBTICK));

    public static final BiFunction<MedicalCondition, Integer, RecipeModifier> ENVIRONMENT_REQUIREMENT = Util
            .memoize((condition, maxAllowedStrength) -> (machine, recipe) -> {
                if (!ConfigHolder.INSTANCE.gameplay.environmentalHazards) return ModifierFunction.IDENTITY;
                if (!(machine.getLevel() instanceof ServerLevel serverLevel)) return ModifierFunction.NULL;

                EnvironmentalHazardSavedData data = EnvironmentalHazardSavedData.getOrCreate(serverLevel);
                BlockPos machinePos = machine.getPos();
                var zone = data.getZoneByContainedPosAndCondition(machinePos, condition);
                if (zone == null) return ModifierFunction.IDENTITY;

                float strength = zone.strength();
                if (strength > maxAllowedStrength) return ModifierFunction.NULL;

                int multiplier = (1 + (int) (strength * 5 / maxAllowedStrength));
                if (multiplier > 5) return ModifierFunction.NULL;

                return ModifierFunction.builder()
                        .durationMultiplier(multiplier)
                        .build();
            });

    public static final RecipeModifier DEFAULT_ENVIRONMENT_REQUIREMENT = new IdentifiedRecipeModifier(
            "default_environment_requirement", ENVIRONMENT_REQUIREMENT
                    .apply(GTMedicalConditions.CARBON_MONOXIDE_POISONING, 1000));

    public static final RecipeModifier PARALLEL_HATCH = new IdentifiedRecipeModifier("parallel_hatch",
            GTRecipeModifiers::hatchParallel);
    public static final RecipeModifier BATCH_MODE = new IdentifiedRecipeModifier("batch_mode",
            GTRecipeModifiers::batchMode);
    public static final RecipeModifier CRACKER_OVERCLOCK = new IdentifiedRecipeModifier("cracker_oc",
            GTRecipeModifiers::crackerOverclock);
    public static final RecipeModifier EBF_OVERCLOCK = new IdentifiedRecipeModifier("ebf_oc",
            GTRecipeModifiers::ebfOverclock);
    public static final RecipeModifier PYROLYSE_OVEN_OVERCLOCK = new IdentifiedRecipeModifier("pyrolyse_oven_oc",
            GTRecipeModifiers::pyrolyseOvenOverclock);
    public static final RecipeModifier MULTI_SMELTER_PARALLEL = new IdentifiedRecipeModifier("multi_smellter_parallel",
            GTRecipeModifiers::multiSmelterParallel);
    public static final RecipeModifier CHEMICAL_REACTOR_OVERCLOCK = new IdentifiedRecipeModifier("chemical_reactor_oc",
            GTRecipeModifiers::chemicalReactorOverclock);
    public static final RecipeModifier CONSUME_EU_TO_START = new IdentifiedRecipeModifier("consume_eu_to_start",
            GTRecipeModifiers::consumeEuToStart);
    public static final RecipeModifier FUSION_OVERCLOCK = new IdentifiedRecipeModifier("fusion_overclock",
            FusionReactorMachine::recipeModifier);

    /**
     * Recipe Modifier for <b>Parallel Multiblock Machines</b> - can be used as a valid {@link RecipeModifier}
     * <p>
     * Looks for the Parallel Hatch on a Multiblock and attempts to parallelize the recipe up to the set amount
     * </p>
     *
     * @param machine an {@link IMultiController} machine
     * @param recipe  recipe
     * @return A {@link ModifierFunction} for the given Parallel Multiblock
     */
    public static @NotNull ModifierFunction hatchParallel(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (machine instanceof IMultiController controller && controller.isFormed()) {
            int parallels = controller.getParallelHatch()
                    .map(hatch -> ParallelLogic.getParallelAmount(machine, recipe, hatch.getCurrentParallel()))
                    .orElse(1);

            if (parallels == 1) return ModifierFunction.IDENTITY;
            return ModifierFunction.builder()
                    .modifyAllContents(ContentModifier.multiplier(parallels))
                    .eutMultiplier(parallels)
                    .parallels(parallels)
                    .build();
        }
        return ModifierFunction.IDENTITY;
    }

    public static @NotNull ModifierFunction batchMode(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (machine instanceof IMultiController controller && controller.isFormed() && controller.isBatchEnabled()) {
            if (recipe.duration < ConfigHolder.INSTANCE.machines.batchDuration) {
                int parallel = ConfigHolder.INSTANCE.machines.batchDuration / recipe.duration;
                parallel = ParallelLogic.getParallelAmountWithoutEU(machine, recipe, parallel);

                if (parallel == 0) return ModifierFunction.NULL;
                if (parallel == 1) return ModifierFunction.IDENTITY;

                return ModifierFunction.builder()
                        .inputModifier(ContentModifier.multiplier(parallel))
                        .outputModifier(ContentModifier.multiplier(parallel))
                        .durationMultiplier(parallel)
                        .batchParallels(parallel)
                        .build();
            }
        }
        return ModifierFunction.IDENTITY;
    }

    /**
     * Recipe Modifier for <b>Cracker Multiblocks</b> - can be used as a valid {@link RecipeModifier}
     * <p>
     * Recipe is OC'd via {@link OverclockingLogic#NON_PERFECT_OVERCLOCK_SUBTICK}.
     * Then, EUt is multiplied by {@code 1 - (0.1 × coilTier)}
     * </p>
     *
     * @param machine a {@link CoilWorkableElectricMultiblockMachine} used for Cracking
     * @param recipe  recipe
     * @return A {@link ModifierFunction} for the given Cracker
     */
    public static @NotNull ModifierFunction crackerOverclock(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof CoilWorkableElectricMultiblockMachine coilMachine)) {
            return RecipeModifier.nullWrongType(CoilWorkableElectricMultiblockMachine.class, machine);
        }
        if (RecipeHelper.getRecipeEUtTier(recipe) > coilMachine.getTier()) return ModifierFunction.NULL;

        var oc = OverclockingLogic.NON_PERFECT_OVERCLOCK_SUBTICK.getModifier(machine, recipe,
                coilMachine.getOverclockVoltage());
        if (coilMachine.getCoilTier() > 0) {
            var tier = coilMachine.getCoilTier();
            var discount = tier > 9 ? (0.9 + (tier - 9) * 0.025) : tier * 0.1;
            var coilModifier = ModifierFunction.builder()
                    .eutMultiplier(Math.max(0.0001, 1.0 - discount))
                    .build();
            oc = oc.andThen(coilModifier);
        }
        return oc;
    }

    /**
     * Recipe Modifier for <b>Blast Furnace Multiblocks</b> - can be used as a valid {@link RecipeModifier}
     * <p>
     * Recipe is rejected if the required temperature is higher than the blast furnace's working temperature.
     * This working temperature is equal to {@code coilTemp + (100K × (voltageTier - MV))} for energy tiers over MV.
     * </p>
     * <p>
     * Recipe is OC'd via {@link OverclockingLogic#heatingCoilOC}.<br>
     * Then, EUt is multiplied by {@code 0.95×} for every {@code 900K} over the required temperature.
     * </p>
     *
     * @param machine a {@link CoilWorkableElectricMultiblockMachine} used for Blasting
     * @param recipe  recipe
     * @return A {@link ModifierFunction} for the given Blast Furnace
     */
    public static @NotNull ModifierFunction ebfOverclock(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof CoilWorkableElectricMultiblockMachine coilMachine)) {
            return RecipeModifier.nullWrongType(CoilWorkableElectricMultiblockMachine.class, machine);
        }

        int blastFurnaceTemperature = coilMachine.getCoilType().getCoilTemperature() +
                (100 * Math.max(0, coilMachine.getTier() - GTValues.MV));
        int recipeTemp = recipe.data.getInt("ebf_temp");
        if (!recipe.data.contains("ebf_temp") || recipeTemp > blastFurnaceTemperature) {
            return ModifierFunction.cancel(Component.translatable("gtceu.recipe_modifier.coil_temperature_too_low"));
        }

        if (RecipeHelper.getRecipeEUtTier(recipe) > coilMachine.getTier()) {
            return ModifierFunction.cancel(Component.translatable("gtceu.recipe_modifier.insufficient_voltage"));
        }

        var discount = ModifierFunction.builder()
                .eutMultiplier(getCoilEUtDiscount(recipeTemp, blastFurnaceTemperature))
                .build();

        OverclockingLogic logic = (p, v) -> OverclockingLogic.heatingCoilOC(p, v, recipeTemp, blastFurnaceTemperature);
        var oc = logic.getModifier(machine, recipe, coilMachine.getOverclockVoltage());

        return oc.compose(discount);
    }

    /**
     * Recipe Modifier for <b>Pyrolyse Oven Multiblocks</b> - can be used as a valid {@link RecipeModifier}
     * <p>
     * Recipe is OC'd via {@link OverclockingLogic#NON_PERFECT_OVERCLOCK_SUBTICK}.<br>
     * Then, duration is multiplied by {@code 1.333×} for Cupronickel Coils
     * or {@code 2 / (tier + 1)} for higher tiercoils.
     * </p>
     *
     * @param machine a {@link CoilWorkableElectricMultiblockMachine} used for Pyrolysis
     * @param recipe  recipe
     * @return A {@link ModifierFunction} for the given Pyrolyse Oven
     */
    public static @NotNull ModifierFunction pyrolyseOvenOverclock(@NotNull MetaMachine machine,
                                                                  @NotNull GTRecipe recipe) {
        if (!(machine instanceof CoilWorkableElectricMultiblockMachine coilMachine)) {
            return RecipeModifier.nullWrongType(CoilWorkableElectricMultiblockMachine.class, machine);
        }
        if (RecipeHelper.getRecipeEUtTier(recipe) > coilMachine.getTier()) return ModifierFunction.NULL;

        int tier = coilMachine.getCoilTier();
        double durationMultiplier = (tier == 0) ? (4.0 / 3.0) : (2.0 / (tier + 1)); // 75% speed with cupro coils
        var durationModifier = ModifierFunction.builder()
                .durationMultiplier(durationMultiplier)
                .build();

        var oc = NON_PERFECT_OVERCLOCK_SUBTICK.getModifier(machine, recipe, coilMachine.getOverclockVoltage());
        return oc.andThen(durationModifier);
    }

    /**
     * Recipe Modifier for <b>Multi Smelters</b> - can be used as a valid {@link RecipeModifier}
     * <p>
     * Modifies the recipe in the following order:
     * <ol>
     * <li>Calculates the maximum parallels as {@code 32 × coilLevel}</li>
     * <li>Finds the actual parallel amount that the smelter can do</li>
     * <li>Sets the recipe duration to {@code 128 × 2 × parallels / maxParallels}</li>
     * <li>Sets the recipe EUt to {@code (4 × maxParallels / (8 × coilDiscount))}</li>
     * <li>Applies {@link OverclockingLogic#NON_PERFECT_OVERCLOCK} to this modified recipe</li>
     * <li>Multiplies the recipe contents by the parallel amount</li>
     * </ol>
     * </p>
     *
     * @param machine a {@link CoilWorkableElectricMultiblockMachine} used for parallel smelting
     * @param recipe  recipe
     * @return A {@link ModifierFunction} for the given Multi Smelter
     */
    public static @NotNull ModifierFunction multiSmelterParallel(@NotNull MetaMachine machine,
                                                                 @NotNull GTRecipe recipe) {
        if (!(machine instanceof CoilWorkableElectricMultiblockMachine coilMachine)) {
            return RecipeModifier.nullWrongType(CoilWorkableElectricMultiblockMachine.class, machine);
        }

        int maxParallel = 32 * coilMachine.getCoilType().getLevel();
        int parallels = ParallelLogic.getParallelAmount(machine, recipe, maxParallel);
        if (parallels == 0) return ModifierFunction.NULL;

        int duration = (int) (128 * 2.0 * parallels / maxParallel);
        long eut = (long) (4L * maxParallel / (8.0 * coilMachine.getCoilType().getEnergyDiscount()));
        ModifierFunction baseModifier = r -> {
            var copy = r.copy();
            EURecipeCapability.putEUContent(copy.tickInputs, new EnergyStack(Math.max(1, eut)));
            copy.duration = Math.max(1, duration);
            return copy;
        };

        var recipeCopy = baseModifier.apply(recipe);
        assert recipeCopy != null;
        var ocModifier = NON_PERFECT_OVERCLOCK.getModifier(machine, recipeCopy, coilMachine.getOverclockVoltage());

        var parallelModifier = ModifierFunction.builder()
                .modifyAllContents(ContentModifier.multiplier(parallels))
                .parallels(parallels)
                .build();

        // apply subtick the overclocks after
        var recipeCopy2 = baseModifier.andThen(ocModifier).andThen(parallelModifier).apply(recipe);
        assert recipeCopy2 != null;
        var ocSubtickModifier = NON_PERFECT_OVERCLOCK_SUBTICK.getModifier(machine, recipeCopy2,
                coilMachine.getOverclockVoltage());
        return baseModifier.andThen(ocModifier).andThen(parallelModifier).andThen(ocSubtickModifier);
    }

    public static @NotNull ModifierFunction chemicalReactorOverclock(@NotNull MetaMachine machine,
                                                                     @NotNull GTRecipe recipe) {
        if (!(machine instanceof CoilWorkableElectricMultiblockMachine coilMachine)) {
            return RecipeModifier.nullWrongType(CoilWorkableElectricMultiblockMachine.class, machine);
        }

        if (RecipeHelper.getRecipeEUtTier(recipe) > coilMachine.getTier()) return ModifierFunction.NULL;

        int coilTier = coilMachine.getCoilTier();

        var durationModifier = ModifierFunction.builder().durationMultiplier(1.0 / (0.75 + coilTier * 0.25)).build();

        return durationModifier
                .andThen(NON_PERFECT_OVERCLOCK_SUBTICK.getModifier(machine, durationModifier.apply(recipe),
                        coilMachine.getOverclockVoltage()))
                .andThen(ModifierFunction.builder().eutMultiplier(1.0 - coilTier * 0.05).build());
    }

    public static @NotNull ModifierFunction consumeEuToStart(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        var euToStartConditions = recipe.conditions.stream()
                .filter(EUToStartCondition.class::isInstance)
                .map(EUToStartCondition.class::cast)
                .toList();

        if (euToStartConditions.isEmpty()) return ModifierFunction.IDENTITY;

        var recipeLogic = machine.getTraits().stream().filter(RecipeLogic.class::isInstance)
                .map(RecipeLogic.class::cast).findFirst();
        if (recipeLogic.isEmpty()) return ModifierFunction.IDENTITY; // should never happen

        if (!RecipeHelper.checkConditions(recipe, recipeLogic.get()).isSuccess()) {
            return ModifierFunction
                    .cancel(Component.translatable("gtceu.recipe_modifier.insufficient_eu_to_start_recipe"));
        }

        var energyToConsume = euToStartConditions.stream()
                .reduce(0L, (acc, condition) -> acc + condition.getEuToStart(), Long::sum);

        if (machine instanceof WorkableElectricMultiblockMachine multiblockMachine) {
            multiblockMachine.getEnergyContainer().removeEnergy(energyToConsume);
        } else {
            var toConsume = machine.getTraits().stream().filter(IEnergyContainer.class::isInstance)
                    .map(IEnergyContainer.class::cast)
                    .filter(energyContainer -> energyContainer.getEnergyCapacity() > energyToConsume)
                    .findFirst();
            if (toConsume.isEmpty()) {
                return ModifierFunction
                        .cancel(Component.translatable("gtceu.recipe_modifier.insufficient_eu_to_start_recipe"));
            }

            toConsume.get().removeEnergy(energyToConsume);
        }

        return ModifierFunction.IDENTITY;
    }

    public static void addToIgnoreList(String ignoreId) {
        ignoreModifiers.add(ignoreId);
    }
}
