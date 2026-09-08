package com.gregtechceu.gtceu.integration.jade.provider;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.research.NetworkSwitchMachine;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class NetworkSwitchProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    @Override
    public ResourceLocation getUid() {
        return GTCEu.id("network_switch");
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor blockAccessor, IPluginConfig config) {
        if (blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity) {
            MetaMachine machine = blockEntity.getMetaMachine();
            if (machine instanceof NetworkSwitchMachine && blockAccessor.getServerData().getBoolean("isFormed")) {
                int receiversCount = blockAccessor.getServerData().getInt("receiversCount");
                int transmittersCount = blockAccessor.getServerData().getInt("transmittersCount");
                int cwut = blockAccessor.getServerData().getInt("cwut");

                // wrap in text component to keep it from being formatted
                var receivers = Component.literal(Integer.toString(receiversCount)).withStyle(ChatFormatting.WHITE);
                var transmitters = Component.literal(Integer.toString(transmittersCount))
                        .withStyle(ChatFormatting.WHITE);
                var cwutText = Component.literal(Integer.toString(cwut)).withStyle(ChatFormatting.AQUA);

                tooltip.add(Component.translatable("gtceu.multiblock.network_switch.receivers", receivers));
                tooltip.add(Component.translatable("gtceu.multiblock.network_switch.transmitters", transmitters));
                tooltip.add(Component.translatable("gtceu.multiblock.computation.max", cwutText));
            }
        }
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        if (blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity) {
            MetaMachine machine = blockEntity.getMetaMachine();
            if (machine instanceof NetworkSwitchMachine networkSwitch) {
                compoundTag.putBoolean("isFormed", networkSwitch.isFormed());
                compoundTag.putInt("receiversCount", networkSwitch.getReceiversCount());
                compoundTag.putInt("transmittersCount", networkSwitch.getTransmittersCount());
                compoundTag.putInt("cwut", networkSwitch.getMaxCWUtForDisplay());
            }
        }
    }
}
