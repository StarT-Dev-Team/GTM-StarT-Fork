package com.gregtechceu.gtceu.api.machine.trait;

public interface IMachineTrait {

    default MachineTrait self() {
        return (MachineTrait) this;
    }
}
