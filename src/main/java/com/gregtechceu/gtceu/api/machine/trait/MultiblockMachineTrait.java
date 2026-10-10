package com.gregtechceu.gtceu.api.machine.trait;

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import lombok.Getter;

public abstract class MultiblockMachineTrait extends MachineTrait {

    @Getter
    protected final MultiblockControllerMachine machine;

    public MultiblockMachineTrait(MultiblockControllerMachine machine) {
        super(machine);
        this.machine = machine;
    }

    public void onMultiblockStructureFormed() {}

    public void onMultiblockStructureInvalid() {}
}
