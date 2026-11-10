package com.gregtechceu.gtceu.api.gui.widget.preview;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.block.property.GTBlockStateProperties;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMufflerMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.client.model.machine.MachineRenderState;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.item.TurbineRotorBehaviour;
import com.gregtechceu.gtceu.common.machine.multiblock.part.RotorHolderPartMachine;

import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.ItemStackKey;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public class MBPattern {

    public final MultiblockShapeInfo shapeInfo;
    public final BlockPos multiPos;
    public final BlockPos controllerPatternPos;
    @NotNull
    public List<List<ItemStack>> parts;
    @NotNull
    public Map<BlockPos, TraceabilityPredicate> predicateMap;
    @NotNull
    public Map<BlockPos, BlockInfo> blockMap;
    @NotNull
    public final IMultiController controllerBase;

    public final Map<PartAbility, BlockState> activeAbilityChoices = new LinkedHashMap<>();
    public final List<VariantCategory> variantCategories = new ArrayList<>();

    public int maxY, minY;
    public int xMin, xMax, yMin, yMax, zMin, zMax;

    public Direction currentFront;
    public Direction currentUp;
    public boolean currentFlipped;

    public MBPattern(@NotNull MultiblockShapeInfo shapeInfo, @NotNull BlockPos multiPos,
                     @NotNull BlockPos controllerPatternPos, @NotNull IMultiController controllerBase) {
        this.shapeInfo = shapeInfo;
        this.multiPos = multiPos;
        this.controllerPatternPos = controllerPatternPos;
        this.controllerBase = controllerBase;
        this.parts = new ArrayList<>();
        this.predicateMap = new HashMap<>();
        this.blockMap = new HashMap<>();
    }

    @Nullable
    public VariantCategory getCategory(PartAbility ability) {
        for (VariantCategory cat : variantCategories) {
            if (cat.ability() == ability) return cat;
        }
        return null;
    }

    public int maxLayer() {
        return Math.max(-1, maxY - minY);
    }

    public void applyTransformation(Direction front, Direction up, boolean isFlipped) {
        TrackedDummyWorld level = PreviewLevel.get();
        synchronized (level != null ? level : this) {
            if (level != null) {
                for (BlockPos pos : this.blockMap.keySet()) {
                    level.removeBlock(pos);
                }
            }

            Direction.Axis mirrorAxis = PatternTransforms.getMirrorAxis(front, up);
            this.blockMap = rebuildBlockMap(front, up, isFlipped, mirrorAxis);
            this.currentFront = front;
            this.currentUp = up;
            this.currentFlipped = isFlipped;

            BlockPos controllerPos = multiPos.offset(controllerPatternPos);
            BlockInfo cInfo = this.blockMap.get(controllerPos);
            BlockState cState = cInfo != null ? cInfo.getBlockState() : null;
            if (cState != null) {
                controllerBase.self().holder.getSelf().setBlockState(cState);
            }

            if (level != null) {
                level.addBlocks(this.blockMap);
                level.setInnerBlockEntity(controllerBase.self().holder.getSelf());
            }

            recalculateBounds();
            syncController(controllerPos, cState, front, up, isFlipped, mirrorAxis);
            rebuildParts();
        }
    }

    private Map<BlockPos, BlockInfo> rebuildBlockMap(Direction front, Direction up, boolean isFlipped,
                                                     Direction.Axis mirrorAxis) {
        Map<BlockPos, BlockInfo> newBlockMap = new HashMap<>();
        BlockInfo[][][] blocks = shapeInfo.getBlocks();
        for (VariantCategory cat : this.variantCategories) {
            cat.positions().clear();
        }
        for (int x = 0; x < blocks.length; x++) {
            BlockInfo[][] aisle = blocks[x];
            for (int y = 0; y < aisle.length; y++) {
                BlockInfo[] column = aisle[y];
                for (int z = 0; z < column.length; z++) {
                    BlockInfo info = column[z];
                    if (info == null) continue;
                    BlockPos relP = new BlockPos(x - controllerPatternPos.getX(),
                            y - controllerPatternPos.getY(),
                            z - controllerPatternPos.getZ());

                    BlockPos offset = PatternTransforms.transformOffset(relP, front, up, isFlipped, mirrorAxis);
                    BlockPos worldPos = multiPos.offset(controllerPatternPos).offset(offset);

                    BlockState rawState = applyVariantChoice(info.getBlockState());
                    for (VariantCategory cat : this.variantCategories) {
                        if (cat.ability().isApplicable(rawState.getBlock())) {
                            cat.positions().add(worldPos);
                        }
                    }
                    BlockState transformedState = PatternTransforms.rotateBlockState(rawState, front, up, isFlipped,
                            mirrorAxis);
                    newBlockMap.put(worldPos, BlockInfo.fromBlockState(transformedState));
                }
            }
        }
        return newBlockMap;
    }

    private BlockState applyVariantChoice(BlockState rawState) {
        for (Map.Entry<PartAbility, BlockState> choice : this.activeAbilityChoices.entrySet()) {
            if (choice.getKey().isApplicable(rawState.getBlock())) {
                return VariantDiscovery.withSharedProperties(rawState, choice.getValue());
            }
        }
        return rawState;
    }

    private void syncController(BlockPos controllerPos, @Nullable BlockState cState, Direction front, Direction up,
                                boolean isFlipped, Direction.Axis mirrorAxis) {
        var machine = controllerBase.self();
        Direction initialFacing = machine.getDefinition().getRotationState().defaultDirection;
        Direction controllerFacing = PatternTransforms.rotateFacing(initialFacing, front, up, isFlipped, mirrorAxis);
        if (controllerFacing == null) controllerFacing = front;

        Direction targetTop = PatternTransforms.rotateFacing(Direction.UP, front, up, isFlipped, mirrorAxis);
        Direction controllerUpwards = PatternTransforms.getUpwardsFacing(controllerFacing, targetTop);

        if (machine.isFacingValid(controllerFacing)) {
            machine.setFrontFacing(controllerFacing);
        }
        if (machine.allowExtendedFacing()) {
            machine.setUpwardsFacing(controllerUpwards);
        }
        if (controllerBase instanceof MultiblockControllerMachine mcm) {
            mcm.setFlipped(isFlipped);
        }
        if (cState != null) {
            machine.holder.getSelf().setBlockState(cState);
        }

        BlockPattern pattern = controllerBase.getPattern();
        if (pattern != null) {
            controllerBase.getMultiblockState().clean();

            if (pattern.checkPatternAt(controllerBase.getMultiblockState(), controllerPos, controllerFacing,
                    controllerUpwards, isFlipped, true) ||
                    pattern.checkPatternAt(controllerBase.getMultiblockState(), true)) {
                controllerBase.onStructureFormed();
            } else {
                controllerBase.onStructureInvalid();
            }

            Map<BlockPos, TraceabilityPredicate> predicates = controllerBase.getMultiblockState()
                    .getMatchContext().get("predicates");
            this.predicateMap = predicates != null ? predicates : new HashMap<>();
        }
    }

    private void rebuildParts() {
        Map<ItemStackKey, PartInfo> drops = PartInfo.gather(this.blockMap);
        Comparator<PartInfo> order = Comparator
                .comparingInt((PartInfo p) -> p.isController ? 0 : 1)
                .thenComparingInt(p -> p.isTile ? 0 : 1)
                .thenComparing((a, b) -> Integer.compare(b.blockId, a.blockId))
                .thenComparing((a, b) -> Integer.compare(b.amount, a.amount));
        this.parts = drops.values().stream()
                .sorted(order)
                .map(PartInfo::getItemStack)
                .filter(list -> !list.isEmpty())
                .collect(java.util.stream.Collectors.toList());
    }

    public void recalculateBounds() {
        int minXVal = Integer.MAX_VALUE, maxXVal = Integer.MIN_VALUE;
        int minYVal = Integer.MAX_VALUE, maxYVal = Integer.MIN_VALUE;
        int minZVal = Integer.MAX_VALUE, maxZVal = Integer.MIN_VALUE;
        for (BlockPos pos : blockMap.keySet()) {
            minXVal = Math.min(minXVal, pos.getX());
            maxXVal = Math.max(maxXVal, pos.getX());
            minYVal = Math.min(minYVal, pos.getY());
            maxYVal = Math.max(maxYVal, pos.getY());
            minZVal = Math.min(minZVal, pos.getZ());
            maxZVal = Math.max(maxZVal, pos.getZ());
        }

        if (minXVal == Integer.MAX_VALUE) {
            minXVal = maxXVal = minYVal = maxYVal = minZVal = maxZVal = 0;
        }

        minY = minYVal;
        maxY = maxYVal;
        xMin = minXVal;
        xMax = maxXVal + 1;
        yMin = minYVal;
        yMax = maxYVal + 1;
        zMin = minZVal;
        zMax = maxZVal + 1;
    }

    public boolean setCoilsActive(boolean active) {
        boolean changed = false;
        TrackedDummyWorld level = PreviewLevel.get();
        for (Map.Entry<BlockPos, BlockInfo> entry : blockMap.entrySet()) {
            BlockState state = entry.getValue().getBlockState();
            if (state.hasProperty(GTBlockStateProperties.ACTIVE)) {
                if (state.getValue(GTBlockStateProperties.ACTIVE) != active) {
                    BlockState newState = state.setValue(GTBlockStateProperties.ACTIVE, active);
                    BlockInfo newInfo = BlockInfo.fromBlockState(newState);
                    entry.setValue(newInfo);
                    if (level != null) {
                        level.addBlock(entry.getKey(), newInfo);
                    }
                    changed = true;
                }
            }
        }
        return changed;
    }

    public List<IMufflerMachine> getMufflers() {
        return getMachinesOfType(IMufflerMachine.class, IMufflerMachine::self);
    }

    public List<RotorHolderPartMachine> getRotorHolders() {
        return getMachinesOfType(RotorHolderPartMachine.class, RotorHolderPartMachine::self);
    }

    private <T> List<T> getMachinesOfType(Class<T> type, Function<T, MetaMachine> selfGetter) {
        List<T> result = new ArrayList<>();
        Set<MetaMachine> seen = new HashSet<>();
        if (controllerBase instanceof MultiblockControllerMachine controller) {
            for (IMultiPart part : controller.getParts()) {
                if (type.isInstance(part) && seen.add(selfGetter.apply(type.cast(part)))) {
                    result.add(type.cast(part));
                }
            }
        }
        TrackedDummyWorld level = PreviewLevel.get();
        if (level != null) {
            for (BlockPos pos : blockMap.keySet()) {
                if (level.getBlockEntity(pos) instanceof IMachineBlockEntity holder &&
                        type.isInstance(holder.getMetaMachine())) {
                    T machine = type.cast(holder.getMetaMachine());
                    if (seen.add(selfGetter.apply(machine))) {
                        result.add(machine);
                    }
                }
            }
        }
        return result;
    }

    public void setRotorHoldersState(boolean hasRotor, boolean spinning) {
        for (RotorHolderPartMachine rotorHolder : getRotorHolders()) {
            if (hasRotor) {
                Material rotorMat = switch (rotorHolder.getTier()) {
                    case GTValues.EV -> GTMaterials.StainlessSteel;
                    case GTValues.IV -> GTMaterials.Titanium;
                    case GTValues.LuV -> GTMaterials.TungstenSteel;
                    case GTValues.ZPM -> GTMaterials.RhodiumPlatedPalladium;
                    case GTValues.UV -> GTMaterials.NaquadahAlloy;
                    default -> GTMaterials.Steel;
                };
                if (rotorHolder.getRotorStack().isEmpty()) {
                    ItemStack rotor = GTItems.TURBINE_ROTOR.asStack();
                    var behaviour = TurbineRotorBehaviour.getBehaviour(rotor);
                    if (behaviour != null) {
                        behaviour.setPartMaterial(rotor, rotorMat);
                    }
                    rotorHolder.setRotorStack(rotor);
                }
                rotorHolder.setRotorMaterial(rotorMat);
                rotorHolder.setRotorSpeed(spinning ? rotorHolder.getMaxRotorHolderSpeed() : 0);
            } else {
                rotorHolder.setRotorStack(ItemStack.EMPTY);
                rotorHolder.setRotorMaterial(GTMaterials.NULL);
                rotorHolder.setRotorSpeed(0);
            }
            MachineRenderState renderState = rotorHolder.self().getRenderState();
            if (renderState.hasProperty(GTMachineModelProperties.IS_FORMED)) {
                renderState = renderState.setValue(GTMachineModelProperties.IS_FORMED, hasRotor);
            }
            if (renderState.hasProperty(GTMachineModelProperties.HAS_ROTOR)) {
                renderState = renderState.setValue(GTMachineModelProperties.HAS_ROTOR, hasRotor);
            }
            if (renderState.hasProperty(GTMachineModelProperties.IS_ROTOR_SPINNING)) {
                renderState = renderState.setValue(GTMachineModelProperties.IS_ROTOR_SPINNING, spinning);
            }
            if (renderState.hasProperty(GTMachineModelProperties.IS_EMISSIVE_ROTOR)) {
                renderState = renderState.setValue(GTMachineModelProperties.IS_EMISSIVE_ROTOR, false);
            }
            rotorHolder.self().setRenderState(renderState);
        }
    }

    public void formStructure() {
        BlockPos controllerPos = multiPos.offset(controllerPatternPos);
        var machine = controllerBase.self();
        BlockPattern pattern = controllerBase.getPattern();
        if (pattern != null) {
            controllerBase.getMultiblockState().clean();
            if (pattern.checkPatternAt(controllerBase.getMultiblockState(), controllerPos, machine.getFrontFacing(),
                    machine.getUpwardsFacing(), currentFlipped, true) ||
                    pattern.checkPatternAt(controllerBase.getMultiblockState(), true)) {
                controllerBase.onStructureFormed();
            }
        }
    }

    public void invalidateStructure() {
        controllerBase.onStructureInvalid();
    }
}
