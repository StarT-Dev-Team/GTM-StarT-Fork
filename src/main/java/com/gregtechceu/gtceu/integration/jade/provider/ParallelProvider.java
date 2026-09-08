package com.gregtechceu.gtceu.integration.jade.provider;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.capability.IParallelHatch;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.recipe.GTRecipeSerializer;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class ParallelProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    private void tooltipHatch(ITooltip tooltip, CompoundTag tag) {
        var hatchParallel = tag.getInt("hatchParallel");
        if (hatchParallel <= 0) return;

        var hatchMinParallel = tag.getInt("hatchMinParallel");

        var parallels = Component.literal(FormattingUtil.formatNumbers(hatchParallel))
                .withStyle(ChatFormatting.DARK_PURPLE);
        var minParallels = Component.literal(FormattingUtil.formatNumbers(hatchMinParallel))
                .withStyle(ChatFormatting.DARK_PURPLE);

        if (hatchMinParallel == hatchParallel) {
            tooltip.add(Component.translatable("gtceu.multiblock.exaxctly_parallel", parallels));
        } else if (hatchMinParallel == 1) {
            tooltip.add(Component.translatable("gtceu.multiblock.parallel", parallels));
        } else {
            tooltip.add(Component.translatable("gtceu.multiblock.between_parallel", minParallels, parallels));
        }
    }

    private void tooltipRecipe(ITooltip tooltip, CompoundTag tag) {
        var parallel = tag.getInt("parallel");
        if (parallel <= 1) return;

        var parallelsByType = GTRecipeSerializer.PARALLELS_BY_TYPE_CODEC
                .parse(NbtOps.INSTANCE, tag.get("parallelsByType")).result().orElse(null);
        if (parallelsByType == null) return;

        var runs = Component.literal(FormattingUtil.formatNumbers(parallel))
                .withStyle(ChatFormatting.DARK_PURPLE);
        tooltip.add(Component.translatable("gtceu.multiblock.total_runs", runs));
        for (var entry : parallelsByType.reference2IntEntrySet()) {
            if (entry.getIntValue() <= 1) continue;
            tooltip.add(entry.getKey().format(entry.getIntValue()));
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        var tag = blockAccessor.getServerData();

        if (tag.getBoolean("exact")) {
            tooltipRecipe(tooltip, tag);
        } else {
            tooltipHatch(tooltip, tag);
        }
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor blockAccessor) {
        if (blockAccessor.getBlockEntity() instanceof MetaMachineBlockEntity blockEntity) {
            if (blockEntity.getMetaMachine() instanceof IParallelHatch parallelHatch) {
                tag.putInt("hatchParallel", parallelHatch.getCurrentParallel());
                tag.putInt("hatchMinParallel", parallelHatch.getMinimumParallel());
            } else if (blockEntity.getMetaMachine() instanceof IMultiController controller) {
                if (controller instanceof IRecipeLogicMachine rlm &&
                        rlm.getRecipeLogic().isActive() &&
                        rlm.getRecipeLogic().getLastRecipe() != null) {
                    var recipe = rlm.getRecipeLogic().getLastRecipe();
                    tag.putInt("parallel", recipe.parallels);
                    tag.put("parallelsByType", GTRecipeSerializer.PARALLELS_BY_TYPE_CODEC
                            .encodeStart(NbtOps.INSTANCE, recipe.parallelsByType).result().orElse(new CompoundTag()));
                    tag.putBoolean("exact", true);
                }

                controller.getParallelHatch().ifPresent(hatch -> {
                    tag.putInt("hatchParallel", hatch.getCurrentParallel());
                    tag.putInt("hatchMinParallel", hatch.getMinimumParallel());
                });
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return GTCEu.id("parallel_info");
    }
}
