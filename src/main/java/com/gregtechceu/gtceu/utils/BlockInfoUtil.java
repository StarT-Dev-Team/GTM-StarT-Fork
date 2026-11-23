package com.gregtechceu.gtceu.utils;

import com.lowdragmc.lowdraglib2.utils.data.BlockInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;

public class BlockInfoUtil {

    @Nullable
    public static BlockEntity createBlockEntity(BlockInfo info, BlockPos pos) {
        var state = info.getBlockState();
        if (!info.hasBlockEntity() || !(state.getBlock() instanceof EntityBlock entityBlock)) return null;
        var blockEntity = entityBlock.newBlockEntity(pos, state);
        info.postEntity(blockEntity);
        return blockEntity;
    }
}
