package com.gregtechceu.gtceu.api.gui.widget.preview;

import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public record VariantCategory(PartAbility ability, Component displayName, List<BlockState> variants,
                              List<ItemStack> variantStacks, List<BlockPos> positions) {}
