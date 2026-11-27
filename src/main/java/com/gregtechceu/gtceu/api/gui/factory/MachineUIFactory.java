package com.gregtechceu.gtceu.api.gui.factory;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IUIMachine;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;

import net.minecraft.server.level.ServerPlayer;

public class MachineUIFactory {

    public static final MachineUIFactory INSTANCE = new MachineUIFactory();

    private MachineUIFactory() {}

    public boolean openUI(MetaMachine machine, ServerPlayer player) {
        return BlockUIMenuType.openUI(player, machine.getPos());
    }

    public static ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        if (MetaMachine.getMachine(holder.player.level(), holder.pos) instanceof IUIMachine uiMachine) {
            return uiMachine.createUI(holder.player);
        }
        return new ModularUI(UI.empty(), holder.player);
    }

    public static boolean stillValid(BlockUIMenuType.BlockUIHolder holder) {
        if (!holder.blockState.is(holder.player.level().getBlockState(holder.pos).getBlock())) {
            return false;
        }
        var machine = MetaMachine.getMachine(holder.player.level(), holder.pos);
        return machine instanceof IUIMachine && !machine.isInValid();
    }
}
