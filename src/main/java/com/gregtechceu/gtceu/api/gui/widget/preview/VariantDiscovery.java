package com.gregtechceu.gtceu.api.gui.widget.preview;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;

import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class VariantDiscovery {

    private VariantDiscovery() {}

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to,
                                                                     Property<?> prop) {
        Property<T> p = (Property<T>) prop;
        return to.setValue(p, from.getValue(p));
    }

    public static List<SimplePredicate> allSimplePredicates(TraceabilityPredicate predicate) {
        List<SimplePredicate> simples = new ArrayList<>(predicate.common.size() + predicate.limited.size());
        simples.addAll(predicate.common);
        simples.addAll(predicate.limited);
        return simples;
    }

    public static ItemStack stackFor(BlockState state) {
        Block block = state.getBlock();
        ItemStack stack = block instanceof MetaMachineBlock mmb ? mmb.definition.asStack() : ItemStack.EMPTY;
        if (stack.isEmpty()) {
            stack = block.getCloneItemStack(PreviewLevel.get(), BlockPos.ZERO, state);
        }
        return stack.isEmpty() ? new ItemStack(block) : stack;
    }

    public static int indexOfBlock(List<BlockState> variants, @Nullable BlockState state) {
        if (state == null) return -1;
        for (int i = 0; i < variants.size(); i++) {
            if (variants.get(i).getBlock() == state.getBlock()) return i;
        }
        return -1;
    }

    public static BlockState withSharedProperties(BlockState from, BlockState to) {
        BlockState result = to.getBlock().defaultBlockState();
        for (Property<?> p : from.getProperties()) {
            if (result.hasProperty(p)) {
                result = copyProperty(from, result, p);
            }
        }
        return result;
    }

    public static void discoverCategories(@Nullable MBPattern pattern) {
        if (pattern == null) return;
        PartAbility.syncDynamicAbilities();

        Map<PartAbility, List<BlockPos>> positionsMap = new LinkedHashMap<>();
        Map<PartAbility, BlockState> defaultStates = new LinkedHashMap<>();

        for (Map.Entry<BlockPos, BlockInfo> entry : pattern.blockMap.entrySet()) {
            BlockInfo info = entry.getValue();
            if (info == null) continue;
            BlockState state = info.getBlockState();
            Block block = state.getBlock();

            TraceabilityPredicate predicate = pattern.predicateMap.get(entry.getKey());
            if (predicate == null || predicate.isController) continue;

            PartAbility matchedAbility = null;
            for (SimplePredicate sp : allSimplePredicates(predicate)) {
                if (sp.partAbility != null && sp.partAbility.isApplicable(block)) {
                    matchedAbility = sp.partAbility;
                    break;
                }
            }

            if (matchedAbility == null && predicate.partAbility != null && predicate.partAbility.isApplicable(block)) {
                matchedAbility = predicate.partAbility;
            }

            if (matchedAbility != null &&
                    (matchedAbility.getRegistry().size() > 1 || matchedAbility.getAllBlocks().size() > 1)) {
                positionsMap.computeIfAbsent(matchedAbility, k -> new ArrayList<>()).add(entry.getKey());
                defaultStates.putIfAbsent(matchedAbility, state);
            }
        }

        pattern.variantCategories.clear();
        for (Map.Entry<PartAbility, List<BlockPos>> entry : positionsMap.entrySet()) {
            PartAbility ability = entry.getKey();

            List<BlockState> variants = new ArrayList<>();
            List<ItemStack> variantStacks = new ArrayList<>();

            Map<Integer, Set<Block>> registry = ability.getRegistry();

            for (int tier : registry.keySet().stream().mapToInt(Integer::intValue).sorted().toArray()) {
                Set<Block> blocksInTier = registry.get(tier);
                if (blocksInTier == null) continue;
                List<Block> sortedBlocks = new ArrayList<>(blocksInTier);
                sortedBlocks.sort(Comparator.comparing(b -> BuiltInRegistries.BLOCK.getKey(b).toString()));

                for (Block b : sortedBlocks) {
                    BlockState varState = b.defaultBlockState();
                    variants.add(varState);
                    variantStacks.add(stackFor(varState));
                }
            }

            if (variants.size() > 1) {
                pattern.variantCategories.add(
                        new VariantCategory(ability, Component.translatable("gtceu.part_ability." + ability.getName()),
                                variants, variantStacks, entry.getValue()));

                if (!pattern.activeAbilityChoices.containsKey(ability)) {
                    BlockState defaultState = defaultStates.get(ability);
                    pattern.activeAbilityChoices.put(ability, defaultState != null ? defaultState : variants.get(0));
                }
            }
        }
    }

    @Nullable
    public static VariantCategory findMatchingCategory(@Nullable SimplePredicate predicate,
                                                       @Nullable MBPattern pattern, @Nullable BlockPos position) {
        if (predicate == null || pattern == null) return null;
        if (predicate.partAbility != null) {
            VariantCategory category = pattern.getCategory(predicate.partAbility);
            if (category != null) {
                addPosition(category, position);
                return category;
            }
        }
        List<ItemStack> candidateStacks = predicate.getCandidates();
        if (candidateStacks != null && !candidateStacks.isEmpty()) {
            for (VariantCategory category : pattern.variantCategories) {
                for (ItemStack stack : candidateStacks) {
                    Block block = Block.byItem(stack.getItem());
                    if (block != Blocks.AIR && category.ability().isApplicable(block)) {
                        addPosition(category, position);
                        return category;
                    }
                }
            }
        }
        return null;
    }

    private static void addPosition(VariantCategory category, @Nullable BlockPos position) {
        if (position != null && !category.positions().contains(position)) {
            category.positions().add(position);
        }
    }
}
