package com.gregtechceu.gtceu.api.gui.widget.preview;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;

import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.ItemStackKey;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PartInfo {

    public final ItemStackKey itemStackKey;
    public final boolean isController;
    public final boolean isTile;
    public final int blockId;
    public int amount = 0;

    public PartInfo(final ItemStackKey itemStackKey, final BlockInfo blockInfo) {
        this.itemStackKey = itemStackKey;
        this.blockId = Block.getId(blockInfo.getBlockState());
        this.isTile = blockInfo.hasBlockEntity();
        this.isController = blockInfo.getBlockState().getBlock() instanceof MetaMachineBlock block &&
                block.definition instanceof MultiblockMachineDefinition;
    }

    public List<ItemStack> getItemStack() {
        return Arrays.stream(itemStackKey.getItemStack())
                .map(stack -> stack.copyWithCount(amount))
                .filter(item -> !item.isEmpty())
                .toList();
    }

    public static Map<ItemStackKey, PartInfo> gather(Map<BlockPos, BlockInfo> blocks) {
        TrackedDummyWorld level = PreviewLevel.get();
        Map<ItemStackKey, PartInfo> partsMap = new Object2ObjectOpenHashMap<>();
        for (Map.Entry<BlockPos, BlockInfo> entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState blockState = level.getBlockState(pos);
            ItemStack itemStack = blockState.getBlock().getCloneItemStack(level, pos, blockState);

            if (itemStack.isEmpty() && !blockState.getFluidState().isEmpty()) {
                Fluid fluid = blockState.getFluidState().getType();
                itemStack = fluid.getBucket().getDefaultInstance();
            }

            ItemStackKey itemStackKey = new ItemStackKey(itemStack);
            partsMap.computeIfAbsent(itemStackKey, key -> new PartInfo(key, entry.getValue())).amount++;
        }
        return partsMap;
    }
}
