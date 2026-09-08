package com.gregtechceu.gtceu.integration.jade.provider;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.integration.ae2.machine.feature.IGridConnectedMachine;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class MEGridConnectedProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity)) return;
        if (!(blockEntity.getMetaMachine() instanceof IGridConnectedMachine)) return;

        var serverData = accessor.getServerData();
        var online = serverData.getBoolean("online");
        var key = online ? "gtceu.gui.me_network.online" : "gtceu.gui.me_network.offline";

        tooltip.add(Component.translatable(key));
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity)) return;
        if (!(blockEntity.getMetaMachine() instanceof IGridConnectedMachine machine)) return;

        compoundTag.putBoolean("online", machine.isOnline());
    }

    @Override
    public ResourceLocation getUid() {
        return GTCEu.id("me_grid_connected");
    }
}
