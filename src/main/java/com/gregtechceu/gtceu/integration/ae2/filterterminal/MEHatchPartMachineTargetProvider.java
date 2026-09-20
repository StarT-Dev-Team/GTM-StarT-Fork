package com.gregtechceu.gtceu.integration.ae2.filterterminal;

import com.gregtechceu.gtceu.common.machine.owner.MachineOwner;
import com.gregtechceu.gtceu.integration.ae2.machine.MEInputHatchPartMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.MEStockingHatchPartMachine;
import com.gregtechceu.gtceu.integration.ae2.slot.IConfigurableSlotList;

import appeng.api.filterterminal.FilterTerminalTargetMetadata;
import appeng.api.filterterminal.IFilterTerminalTarget;
import appeng.api.filterterminal.IFilterTerminalTargetProvider;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKeyType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BooleanSupplier;

public final class MEHatchPartMachineTargetProvider implements IFilterTerminalTargetProvider<MEInputHatchPartMachine> {

    public static final MEHatchPartMachineTargetProvider INSTANCE = new MEHatchPartMachineTargetProvider();

    private MEHatchPartMachineTargetProvider() {}

    @Override
    public Class<MEInputHatchPartMachine> getTargetType() {
        return MEInputHatchPartMachine.class;
    }

    @Override
    public @Nullable IFilterTerminalTarget getTarget(MEInputHatchPartMachine hatch) {
        var level = hatch.getLevel();
        var gridNode = hatch.getActionableNode();
        var tank = hatch.tank;
        if (level == null || gridNode == null || !(tank instanceof IConfigurableSlotList slots)) {
            return null;
        }

        var group = new PatternContainerGroup(AEItemKey.of(hatch.getDefinition().asStack()), hatch.getTitle(),
                List.of());
        var metadata = new FilterTerminalTargetMetadata(group, level.dimension(), hatch.getPos(), null);
        var stocking = hatch instanceof MEStockingHatchPartMachine;
        BooleanSupplier readOnly = hatch instanceof MEStockingHatchPartMachine stockingHatch ?
                stockingHatch::isAutoPull : () -> false;
        var configView = new GTFilterTerminal.ConfigView(slots, AEKeyType.fluids(), stocking, readOnly, () -> {
            tank.onContentsChanged();
            hatch.markDirty();
        });
        return new GTFilterTerminal.Target(hatch, gridNode, metadata, configView,
                player -> MachineOwner.canOpenOwnerMachine(player, hatch));
    }
}
