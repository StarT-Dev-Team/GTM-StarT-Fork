package com.gregtechceu.gtceu.api.machine.trait.lifecycle;

import com.gregtechceu.gtceu.api.machine.trait.IMachineTrait;

public interface IMultiblockMachineTrait extends IMachineTrait {

    default void onMultiblockStructureFormed() {}

    default void onMultiblockStructureInvalid() {}
}
