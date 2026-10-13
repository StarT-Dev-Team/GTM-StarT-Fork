package com.gregtechceu.gtceu.api.machine.feature;

import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;

public interface IHasCircuitSlot {

    default boolean isCircuitSlotEnabled() {
        return true;
    }

    NotifiableItemStackHandler getCircuitInventory();

    default boolean isCircuitPastingBlocked() {
        return false;
    }

    default void setCircuitPastingBlocked(boolean blocked) {}
}
