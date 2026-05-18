package com.gregtechceu.gtceu.api.machine.multiblock;

import com.gregtechceu.gtceu.api.capability.IParallelHatch;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDisplayUIMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.common.machine.trait.LayeredRecipeLogic;

import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;

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
        var numParallels = 0;
        var subtickParallels = 0;
        var batchParallels = 0;
        var totalRuns = 0;
        var exact = false;

        if (logic.isActive() && logic.getLastRecipe() != null) {
            numParallels = logic.getLastRecipe().parallels;
            subtickParallels = logic.getLastRecipe().subtickParallels;
            batchParallels = logic.getLastRecipe().batchParallels;
            totalRuns = logic.getLastRecipe().getTotalRuns();
            exact = true;
        } else if (logic.getLayeredRecipe() != null) {
            numParallels = logic.getLayeredRecipe().get(0).parallels;
            subtickParallels = logic.getLayeredRecipe().get(0).subtickParallels;
            batchParallels = logic.getLayeredRecipe().get(0).batchParallels;
            totalRuns = logic.getLayeredRecipe().get(0).getTotalRuns();
            exact = true;
        } else {
            numParallels = getParallelHatch().map(IParallelHatch::getCurrentParallel).orElse(0);
        }

        MultiblockDisplayText.builder(textList, isFormed())
                .setWorkingStatus(logic.isWorkingEnabled(), logic.isActive())
                .addEnergyUsageLine(energyContainer)
                .addEnergyTierLine(tier)
                .addMachineModeLine(getRecipeType(), getRecipeTypes().length > 1)
                .addTotalRunsLine(totalRuns)
                .addParallelsLine(numParallels, exact)
                .addSubtickParallelsLine(subtickParallels)
                .addBatchModeLine(isBatchEnabled(), batchParallels)
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
