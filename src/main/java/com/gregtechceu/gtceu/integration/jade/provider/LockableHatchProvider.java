package com.gregtechceu.gtceu.integration.jade.provider;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.ILockableHatch;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class LockableHatchProvider extends CapabilityBlockProvider<ILockableHatch> {

    public LockableHatchProvider() {
        super(GTCEu.id("lockable_hatch_provider"));
    }

    @Nullable
    @Override
    protected ILockableHatch getCapability(Level level, BlockPos pos, @Nullable Direction side) {
        return GTCapabilityHelper.getLockableHatch(level, pos, side);
    }

    @Override
    protected void write(CompoundTag data, ILockableHatch capability) {
        data.putBoolean("locked", capability.isLocked());
    }

    @Override
    protected void addTooltip(CompoundTag capData, ITooltip tooltip, Player player, BlockAccessor block,
                              BlockEntity blockEntity, IPluginConfig config) {
        if (capData.getBoolean("locked")) {
            tooltip.add(
                    Component.translatable("gtceu.jade.hatch_locked").withStyle(ChatFormatting.RED));
        }
    }
}
