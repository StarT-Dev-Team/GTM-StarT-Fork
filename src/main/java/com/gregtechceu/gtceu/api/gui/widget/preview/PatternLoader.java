package com.gregtechceu.gtceu.api.gui.widget.preview;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;

import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public final class PatternLoader {

    private static final Map<MultiblockMachineDefinition, MBPattern[]> CACHE = new HashMap<>();
    private static final Map<MultiblockMachineDefinition, Integer> CURRENT_PAGE = new HashMap<>();

    private PatternLoader() {}

    public static int getPage(MultiblockMachineDefinition definition) {
        return CURRENT_PAGE.getOrDefault(definition, 0);
    }

    public static void setPage(MultiblockMachineDefinition definition, int page) {
        CURRENT_PAGE.put(definition, page);
    }

    @Nullable
    public static MBPattern[] getCached(MultiblockMachineDefinition definition) {
        return CACHE.get(definition);
    }

    public static MBPattern[] loadPatterns(MultiblockMachineDefinition definition,
                                           Consumer<Collection<BlockPos>> onFormed) {
        return CACHE.computeIfAbsent(definition, def -> def.getMatchingShapes().stream()
                .map(shape -> initializePattern(def, shape, onFormed))
                .filter(Objects::nonNull)
                .toArray(MBPattern[]::new));
    }

    @Nullable
    private static MBPattern initializePattern(MultiblockMachineDefinition definition, MultiblockShapeInfo shapeInfo,
                                               Consumer<Collection<BlockPos>> onFormed) {
        var level = PreviewLevel.getOrCreate();
        IMultiController controllerBase = null;
        BlockPos controllerPatternPos = BlockPos.ZERO;
        BlockPos multiPos = PreviewLevel.locateNextRegion();

        BlockInfo[][][] blocks = shapeInfo.getBlocks();
        for (int x = 0; x < blocks.length; x++) {
            BlockInfo[][] aisle = blocks[x];
            for (int y = 0; y < aisle.length; y++) {
                BlockInfo[] column = aisle[y];
                for (int z = 0; z < column.length; z++) {
                    BlockPos pos = multiPos.offset(x, y, z);
                    if (column[z].getBlockEntity(pos) instanceof IMachineBlockEntity holder &&
                            holder.getMetaMachine() instanceof IMultiController controller) {
                        holder.getSelf().setLevel(level);
                        controllerBase = controller;
                        controllerPatternPos = new BlockPos(x, y, z);
                    }
                }
            }
        }

        if (controllerBase == null) return null;

        MBPattern pattern = new MBPattern(shapeInfo, multiPos, controllerPatternPos, controllerBase);
        pattern.applyTransformation(definition.getRotationState().defaultDirection, Direction.NORTH, false);

        level.setInnerBlockEntity(controllerBase.self().holder.getSelf());

        loadControllerFormed(pattern.blockMap.keySet(), controllerBase, onFormed);
        Map<BlockPos, TraceabilityPredicate> predicates = controllerBase.getMultiblockState().getMatchContext()
                .get("predicates");
        pattern.predicateMap = predicates != null ? predicates : new HashMap<>();
        VariantDiscovery.discoverCategories(pattern);

        return pattern;
    }

    public static void loadControllerFormed(Collection<BlockPos> positions, IMultiController controllerBase,
                                            Consumer<Collection<BlockPos>> onFormed) {
        BlockPattern pattern = controllerBase.getPattern();
        if (pattern != null && pattern.checkPatternAt(controllerBase.getMultiblockState(), true)) {
            controllerBase.onStructureFormed();
        }
        if (controllerBase.isFormed()) {
            LongSet modelDisabled = controllerBase.getMultiblockState().getMatchContext().getOrDefault("renderMask",
                    LongSets.EMPTY_SET);
            if (!modelDisabled.isEmpty()) {
                positions = new HashSet<>(positions);
                positions.removeIf(pos -> modelDisabled.contains(pos.asLong()));
            }
            onFormed.accept(positions);
        } else {
            GTCEu.LOGGER.warn("Pattern formed checking failed: {}", controllerBase.self().getDefinition());
        }
    }
}
