package com.gregtechceu.gtceu.api.machine.trait.lifecycle;

import com.gregtechceu.gtceu.api.machine.trait.IMachineTrait;

public interface IMultiblockPartTrait extends IMachineTrait {

    default void addedToController() {}

    default void removedFromController() {}
}
