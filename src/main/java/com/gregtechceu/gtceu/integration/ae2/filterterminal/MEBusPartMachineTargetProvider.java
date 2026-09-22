package com.gregtechceu.gtceu.integration.ae2.filterterminal;

import com.gregtechceu.gtceu.common.machine.owner.MachineOwner;
import com.gregtechceu.gtceu.integration.ae2.machine.MEInputBusPartMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.MEStockingBusPartMachine;
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

public final class MEBusPartMachineTargetProvider implements IFilterTerminalTargetProvider<MEInputBusPartMachine> {

    public static final MEBusPartMachineTargetProvider INSTANCE = new MEBusPartMachineTargetProvider();

    private MEBusPartMachineTargetProvider() {}

    @Override
    public Class<MEInputBusPartMachine> getTargetType() {
        return MEInputBusPartMachine.class;
    }

    @Override
    public @Nullable IFilterTerminalTarget getTarget(MEInputBusPartMachine bus) {
        var level = bus.getLevel();
        var gridNode = bus.getActionableNode();
        var inventory = bus.getInventory();
        if (level == null || gridNode == null || !(inventory instanceof IConfigurableSlotList slots)) {
            return null;
        }

        var group = new PatternContainerGroup(AEItemKey.of(bus.getDefinition().asStack()), bus.getTitle(), List.of());
        var metadata = new FilterTerminalTargetMetadata(group, level.dimension(), bus.getPos(), null);
        var stocking = bus instanceof MEStockingBusPartMachine;
        BooleanSupplier readOnly = bus instanceof MEStockingBusPartMachine stockingBus ?
                stockingBus::isAutoPull : () -> false;
        var configView = new GTFilterTerminal.ConfigView(slots, AEKeyType.items(), stocking, readOnly, () -> {
            inventory.onContentsChanged();
            bus.markDirty();
        });
        return new GTFilterTerminal.Target(bus, gridNode, metadata, configView,
                player -> MachineOwner.canOpenOwnerMachine(player, bus));
    }
}
