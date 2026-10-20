package com.gregtechceu.gtceu.api.machine.trait;

import net.minecraft.core.Direction;

public interface IFrontFacingTrait extends IMachineTrait {

    default boolean isValidFrontFace(Direction direction) {
        return true;
    }

}
