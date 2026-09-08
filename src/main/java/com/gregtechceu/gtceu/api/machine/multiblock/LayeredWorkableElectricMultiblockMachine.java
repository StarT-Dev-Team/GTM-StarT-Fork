package com.gregtechceu.gtceu.api.machine.multiblock;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDisplayUIMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.ParallelType;
import com.gregtechceu.gtceu.common.machine.trait.LayeredRecipeLogic;

import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;

import it.unimi.dsi.fastutil.objects.Reference2IntMaps;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LayeredWorkableElectricMultiblockMachine extends WorkableElectricMultiblockMachine
                                                      implements IDisplayUIMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            LayeredWorkableElectricMultiblockMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    public LayeredWorkableElectricMultiblockMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public LayeredRecipeLogic getRecipeLogic() {
        return (LayeredRecipeLogic) super.getRecipeLogic();
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new LayeredRecipeLogic(this);
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        var logic = getRecipeLogic();
        var parallels = 0;
        var parallelsByType = Reference2IntMaps.<ParallelType>emptyMap();
        var exact = false;

        var initialStep = logic.getFirstLayer();
        if (recipeLogic.isActive() && recipeLogic.getLastRecipe() != null) {
            parallels = recipeLogic.getLastRecipe().parallels;
            parallelsByType = recipeLogic.getLastRecipe().parallelsByType;
            exact = true;
        } else if (initialStep != null) {
            parallels = initialStep.parallels;
            parallelsByType = initialStep.parallelsByType;
            exact = true;
        }

        MultiblockDisplayText.builder(textList, isFormed())
                .setWorkingStatus(logic.isWorkingEnabled(), logic.isActive())
                .addPatternErrorLine(getMultiblockState().error)
                .addEnergyUsageLine(energyContainer)
                .addEnergyTierLine(tier)
                .addMachineModeLine(getRecipeType(), getRecipeTypes().length > 1)
                .addTotalRunsLine(parallels)
                .addParallelHatchLine(getParallelHatch().orElse(null), exact)
                .addParallelsLine(parallelsByType)
                .addWorkingStatusLine()
                .addProgressLine(logic)
                .addRecipeFailReasonLine(logic)
                .addLayeredSteps(logic)
                .addLayeredTotalProgress(logic)
                .addLayeredFinalStepOutputs(logic)
                .addLayeredNextStepInputs(logic);

        getDefinition().getAdditionalDisplay().accept(this, textList);

        for (var part : this.getParts()) {
            part.addMultiText(textList);
        }
    }

    @Override
    public void handleDisplayClick(String componentData, ClickData clickData) {
        super.handleDisplayClick(componentData, clickData);
        if (clickData.isRemote) return;

        if (componentData.equals("layered_cancel")) {
            getRecipeLogic().resetRecipeLogic();
        }
    }
}
