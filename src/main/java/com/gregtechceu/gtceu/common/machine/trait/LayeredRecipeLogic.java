package com.gregtechceu.gtceu.common.machine.trait;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.ActionResult;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.LayeredRecipeHelper;
import com.gregtechceu.gtceu.utils.FluidStackHashStrategy;
import com.gregtechceu.gtceu.utils.ItemStackHashStrategy;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import it.unimi.dsi.fastutil.objects.Object2LongOpenCustomHashMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class LayeredRecipeLogic extends RecipeLogic {

    @Persisted
    @DescSynced
    @Getter
    private List<LayeredRecipeHelper.Layer> layeredRecipe;

    @Persisted
    @DescSynced
    @Getter
    private int layeredRecipeLayerIndex = -1;

    public LayeredRecipeLogic(IRecipeLogicMachine machine) {
        super(machine);
    }

    public @Nullable LayeredRecipeHelper.Layer getCurrentLayer() {
        if (layeredRecipe == null || layeredRecipeLayerIndex < 0 || layeredRecipeLayerIndex >= layeredRecipe.size()) {
            return null;
        }
        return layeredRecipe.get(layeredRecipeLayerIndex);
    }

    public int getCoverRedstoneOutput() {
        return Mth.clamp(layeredRecipeLayerIndex + (lastRecipe == null ? 0 : 1), 0, 15);
    }

    public GTRecipe getNextLayeredRecipe() {
        if (layeredRecipe == null || layeredRecipeLayerIndex < 0) {
            return null;
        }
        if (lastRecipe == null) {
            // not running and waiting for inputs
            return layeredRecipe.get(layeredRecipeLayerIndex).recipe();
        }
        if (layeredRecipeLayerIndex < layeredRecipe.size() - 1) {
            return layeredRecipe.get(layeredRecipeLayerIndex + 1).recipe();
        }
        return null;
    }

    @Override
    public boolean hasCustomProgressLine() {
        return true;
    }

    @Override
    public @Nullable Component getCustomProgressLine() {
        var currentProgress = (int) (getProgressPercent() * 100);
        var currentInSec = progress / 20.0;
        var maxInSec = duration / 20.0;

        return Component.translatable("gtceu.multiblock.layered.step_progress",
                String.format("%.2f", (float) currentInSec),
                String.format("%.2f", (float) maxInSec), currentProgress);
    }

    @Override
    public void interruptRecipe() {
        machine.afterWorking();
        setStatus(Status.IDLE);
        progress = 0;
        duration = 0;
        layeredRecipeLayerIndex = -1;
        layeredRecipe = null;
        lastRecipe = null;
    }

    @Override
    public void resetRecipeLogic() {
        super.resetRecipeLogic();
        layeredRecipeLayerIndex = -1;
        layeredRecipe = null;
    }

    @Override
    public void onRecipeFinish() {
        var finishedLastStep = false;
        if (layeredRecipe != null && lastRecipe != null && lastRecipe.data.getBoolean("is_layer")) {
            // we were doing a recipe layer
            layeredRecipeLayerIndex++;
            finishedLastStep = layeredRecipe.size() == layeredRecipeLayerIndex;
        }

        suspendAfterFinish = true;
        super.onRecipeFinish();
        setStatus(RecipeLogic.Status.IDLE);

        if (finishedLastStep) {
            // try the first step again
            var firstStepRecipe = layeredRecipe.get(0).recipe();
            var recipeMatch = checkRecipe(firstStepRecipe);
            if (recipeMatch.isSuccess()) {
                layeredRecipeLayerIndex = 0;
                setupRecipe(firstStepRecipe);
            } else {
                layeredRecipe = null;
                layeredRecipeLayerIndex = -1;
            }
        } else {
            // already transformed
            var nextStepRecipe = layeredRecipe.get(layeredRecipeLayerIndex).recipe();
            var recipeMatch = checkRecipe(nextStepRecipe);
            if (recipeMatch.isSuccess()) {
                setupRecipe(nextStepRecipe);
            }
        }
    }

    @Override
    public @NotNull Iterator<GTRecipe> searchRecipe() {
        if (layeredRecipe != null) {
            return Collections.singleton(layeredRecipe.get(layeredRecipeLayerIndex).recipe()).iterator();
        }
        return machine.getRecipeType().searchRecipe(machine, r -> {
            // TODO: maybe add support for running non layered recipes as well
            // ignore non layered recipes
            if (!LayeredRecipeHelper.hasLayeredSteps(r)) return false;
            return matchRecipe(r).isSuccess();
        });
    }

    @Override
    public void setupRecipe(GTRecipe recipe) {
        if (LayeredRecipeHelper.hasLayeredSteps(recipe)) {
            // we are starting a layered craft
            layeredRecipe = LayeredRecipeHelper.getLayeredSteps(recipe);
            assert layeredRecipe != null;
            layeredRecipeLayerIndex = 0;
            recipe = layeredRecipe.get(0).recipe();
        } else if (!recipe.data.getBoolean("is_layer")) {
            // non layered recipe, should never happen
            layeredRecipe = null;
            layeredRecipeLayerIndex = -1;
        }
        // otherwise we are just doing a subsequent layer
        super.setupRecipe(recipe);
        recipeDirty = true; // always mark dirty, we have custom retry logic
    }

    @Override
    public boolean checkMatchedRecipeAvailable(GTRecipe match) {
        var isAlreadyModified = match.data.getBoolean("is_layer");
        var modified = isAlreadyModified ? match : machine.fullModifyRecipe(match);
        if (modified != null) {
            var recipeMatch = checkRecipe(modified);
            if (recipeMatch.isSuccess()) {
                // TODO: fail the match if there are other inputs in the machine
                setupRecipe(modified);
            }

            if (lastRecipe != null && getStatus() == RecipeLogic.Status.WORKING) {
                lastOriginRecipe = null; // custom handling
                lastFailedMatches = null;
                return true;
            }
        }
        return false;
    }

    @Override
    protected ActionResult checkRecipe(GTRecipe recipe) {
        // normal match first
        var normalMatch = super.checkRecipe(recipe);
        if (!normalMatch.isSuccess()) return normalMatch;

        var inputItems = new Object2LongOpenCustomHashMap<>(ItemStackHashStrategy.comparingAllButCount());
        machine.getCapabilitiesFlat(IO.IN, ItemRecipeCapability.CAP).stream()
                .flatMap(s -> s.getContents().stream())
                .filter(ItemStack.class::isInstance).map(ItemStack.class::cast).filter(s -> !s.isEmpty())
                .forEach(s -> inputItems.addTo(s, s.getCount()));

        for (var rawContent : recipe.getInputContents(ItemRecipeCapability.CAP)) {
            var content = ItemRecipeCapability.CAP.of(rawContent.getContent());
            inputItems.keySet().stream().filter(content).forEach(inputItems::removeLong);
        }
        if (!inputItems.isEmpty()) {
            // TODO: missing language key
            return ActionResult.fail(Component.translatable("Layer inputs aren't the only inputs in the machine."),
                    null, IO.IN);
        }

        var inputFluids = new Object2LongOpenCustomHashMap<>(FluidStackHashStrategy.comparingAllButAmount());
        machine.getCapabilitiesFlat(IO.IN, FluidRecipeCapability.CAP).stream()
                .flatMap(s -> s.getContents().stream())
                .filter(FluidStack.class::isInstance).map(FluidStack.class::cast).filter(s -> !s.isEmpty())
                .forEach(s -> inputFluids.addTo(s, s.getAmount()));

        for (var rawContent : recipe.getInputContents(FluidRecipeCapability.CAP)) {
            var content = FluidRecipeCapability.CAP.of(rawContent.getContent());
            inputFluids.keySet().stream().filter(content).forEach(inputFluids::removeLong);
        }
        if (!inputFluids.isEmpty()) {
            // TODO: missing language key
            return ActionResult.fail(Component.translatable("Layer inputs aren't the only inputs in the machine."),
                    null, IO.IN);
        }

        return ActionResult.SUCCESS;
    }
}
