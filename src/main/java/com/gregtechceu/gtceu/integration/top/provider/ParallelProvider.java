package com.gregtechceu.gtceu.integration.top.provider;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.capability.IParallelHatch;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.recipe.ParallelType;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import it.unimi.dsi.fastutil.objects.Reference2IntMaps;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;

public class ParallelProvider implements IProbeInfoProvider {

    @Override
    public ResourceLocation getID() {
        return GTCEu.id("parallel");
    }

    @Override
    public void addProbeInfo(ProbeMode probeMode, IProbeInfo info, Player player, Level level,
                             BlockState blockState, IProbeHitData iProbeHitData) {
        BlockEntity blockEntity = level.getBlockEntity(iProbeHitData.getPos());
        if (blockEntity instanceof MetaMachineBlockEntity machineBlockEntity) {
            var parallels = 0;
            var parallelsByType = Reference2IntMaps.<ParallelType>emptyMap();
            var exact = false;
            IParallelHatch hatch = null;

            if (machineBlockEntity.getMetaMachine() instanceof IParallelHatch parallelHatch) {
                hatch = parallelHatch;
            } else if (machineBlockEntity.getMetaMachine() instanceof IMultiController controller) {
                hatch = controller.getParallelHatch().orElse(null);
                if (controller instanceof IRecipeLogicMachine rlm &&
                        rlm.getRecipeLogic().isActive() &&
                        rlm.getRecipeLogic().getLastRecipe() != null) {
                    parallels = rlm.getRecipeLogic().getLastRecipe().parallels;
                    parallelsByType = rlm.getRecipeLogic().getLastRecipe().parallelsByType;
                    exact = true;
                }
            }

            if (!exact && hatch != null) {
                var parallel = hatch.getCurrentParallel();
                var minParallel = hatch.getMinimumParallel();

                var comp = Component.literal(FormattingUtil.formatNumbers(parallel))
                        .withStyle(ChatFormatting.DARK_PURPLE);
                if (minParallel == parallel) {
                    info.text(Component.translatable("gtceu.multiblock.exaxctly_parallel", comp)
                            .withStyle(ChatFormatting.GRAY));
                } else if (minParallel == 1) {
                    info.text(
                            Component.translatable("gtceu.multiblock.parallel", comp).withStyle(ChatFormatting.GRAY));
                } else {
                    var compMin = Component.literal(FormattingUtil.formatNumbers(minParallel))
                            .withStyle(ChatFormatting.DARK_PURPLE);
                    info.text(Component.translatable("gtceu.multiblock.between_parallel", compMin, comp)
                            .withStyle(ChatFormatting.GRAY));
                }
            } else if (exact && parallels > 1) {
                var runs = Component.literal(FormattingUtil.formatNumbers(parallels))
                        .withStyle(ChatFormatting.DARK_PURPLE);
                info.text(Component.translatable("gtceu.multiblock.total_runs", runs));
                for (var entry : parallelsByType.reference2IntEntrySet()) {
                    if (entry.getIntValue() <= 1) continue;
                    info.text(entry.getKey().format(entry.getIntValue()));
                }
            }
        }
    }
}
