package com.gregtechceu.gtceu.common.item.armor;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;

import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

public interface IElectricJetpack extends IJetpack {

    int getTier();

    @Override
    default boolean canUseEnergy(@NotNull ItemStack stack, int amount) {
        IElectricItem container = GTCapabilityHelper.getElectricItem(stack);
        return container != null && container.canUse(amount);
    }

    @Override
    default void drainEnergy(@NotNull ItemStack stack, int amount) {
        IElectricItem container = GTCapabilityHelper.getElectricItem(stack);
        if (container != null) {
            container.discharge(amount, getTier(), true, false, false);
        }
    }

    @Override
    default boolean hasEnergy(@NotNull ItemStack stack) {
        IElectricItem container = GTCapabilityHelper.getElectricItem(stack);
        return container != null && container.getCharge() > 0;
    }
}
