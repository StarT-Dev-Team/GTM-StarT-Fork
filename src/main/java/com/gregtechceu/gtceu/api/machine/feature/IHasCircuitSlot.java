package com.gregtechceu.gtceu.api.machine.feature;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.config.ConfigHolder;

public interface IHasCircuitSlot {

    default boolean isCircuitSlotEnabled() {
        return true;
    }

    NotifiableItemStackHandler getCircuitInventory();

    default boolean isCircuitPastingBlocked() {
        return false;
    }

    default void setCircuitPastingBlocked(boolean blocked) {}

    default boolean isCircuitPastingEnabled() {
        var config = ConfigHolder.INSTANCE.compat.ae2;
        if (!config.isCircuitPastingEnabled()) return false;
        if (this instanceof IMultiController) {
            return config.pastingOnController;
        } else if (this instanceof MultiblockPartMachine) {
            return config.pastingOnHatch || config.subnetPastingOnHatch || config.pastingOnController;
        }
        return config.pastingOnSingleblock || config.subnetPastingOnSingleblock;
    }
}
