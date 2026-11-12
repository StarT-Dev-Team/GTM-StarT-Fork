package com.gregtechceu.gtceu.api.machine.trait;

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;

public class RecipeAmperageEnergyContainer extends NotifiableEnergyContainer {

    protected IRecipeLogicMachine machine;

    public RecipeAmperageEnergyContainer(long maxCapacity,
                                         long maxInputVoltage, long maxInputAmperage,
                                         long maxOutputVoltage, long maxOutputAmperage) {
        super(maxCapacity, maxInputVoltage, maxInputAmperage, maxOutputVoltage, maxOutputAmperage);
    }

    public static RecipeAmperageEnergyContainer makeEmitterContainer(long maxCapacity,
                                                                     long maxOutputVoltage, long maxOutputAmperage) {
        return new RecipeAmperageEnergyContainer(maxCapacity, 0L, 0L, maxOutputVoltage, maxOutputAmperage);
    }

    public static RecipeAmperageEnergyContainer makeReceiverContainer(long maxCapacity,
                                                                      long maxInputVoltage, long maxInputAmperage) {
        return new RecipeAmperageEnergyContainer(maxCapacity, maxInputVoltage, maxInputAmperage, 0L, 0L);
    }

    @Override
    public void onTraitAttached() {
        super.onTraitAttached();
        if (!(getMachine() instanceof IRecipeLogicMachine rlMachine))
            throw new IllegalStateException("Invalid machine machine type for recipe logic");
        machine = rlMachine;
    }

    @Override
    public long getInputAmperage() {
        var lastRecipe = machine.getRecipeLogic().getLastRecipe();
        long amperage;
        if (lastRecipe != null) {
            amperage = lastRecipe.getInputEUt().amperage();
        } else {
            amperage = super.getInputAmperage();
        }
        if (getEnergyCapacity() / 2 > getEnergyStored() && machine.getRecipeLogic().isActive()) {
            return amperage + 1;
        } else {
            return amperage;
        }
    }

    @Override
    public long getOutputAmperage() {
        var lastRecipe = machine.getRecipeLogic().getLastRecipe();
        if (lastRecipe != null) {
            return lastRecipe.getOutputEUt().amperage();
        } else {
            return super.getOutputAmperage();
        }
    }
}
