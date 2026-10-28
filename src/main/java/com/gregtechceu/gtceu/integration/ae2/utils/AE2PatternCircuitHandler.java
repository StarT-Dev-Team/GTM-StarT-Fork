package com.gregtechceu.gtceu.integration.ae2.utils;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IHasCircuitSlot;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.parts.storagebus.StorageBusPart;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AE2PatternCircuitHandler {

    public static void handlePatternPush(PatternProviderLogicHost host, IPatternDetails patternDetails,
                                         KeyCounter[] inputHolder) {
        if (host == null || !ConfigHolder.INSTANCE.compat.ae2.isCircuitPastingEnabled()) return;

        int circuitConfig = getCircuitConfig(patternDetails, inputHolder);
        if (circuitConfig < 0) return;

        stripCircuitFromInputHolder(inputHolder);

        BlockEntity be = host.getBlockEntity();
        if (be == null || be.getLevel() == null) return;

        Level level = be.getLevel();
        Set<MetaMachine> visitedMachines = new HashSet<>();
        Set<IGrid> visitedGrids = new HashSet<>();
        if (host.getGrid() != null) {
            visitedGrids.add(host.getGrid());
        }

        for (Direction side : host.getTargets()) {
            BlockPos targetPos = be.getBlockPos().relative(side);
            BlockEntity targetBe = level.getBlockEntity(targetPos);

            if (targetBe != null) {
                MetaMachine machine = MetaMachine.getMachine(level, targetPos);
                if (machine != null && visitedMachines.add(machine)) {
                    applyCircuitToMachine(machine, circuitConfig, false);
                }
            }

            if (ConfigHolder.INSTANCE.compat.ae2.subnetPastingOnSingleblock ||
                    ConfigHolder.INSTANCE.compat.ae2.subnetPastingOnHatch ||
                    ConfigHolder.INSTANCE.compat.ae2.pastingOnController) {
                IGridNode gridNode = GridHelper.getExposedNode(level, targetPos, side.getOpposite());
                if (gridNode == null) {
                    IInWorldGridNodeHost nodeHost = GridHelper.getNodeHost(level, targetPos);
                    if (nodeHost != null) {
                        gridNode = nodeHost.getGridNode(side.getOpposite());
                        if (gridNode == null) {
                            gridNode = nodeHost.getGridNode(null);
                        }
                    }
                }

                if (gridNode != null && gridNode.getGrid() != null) {
                    applyCircuitToSubnet(gridNode.getGrid(), circuitConfig, visitedMachines, visitedGrids, 0);
                }
            }
        }
    }

    public static void applyCircuitToMachine(MetaMachine machine, int circuitConfig) {
        applyCircuitToMachine(machine, circuitConfig, false);
    }

    public static void applyCircuitToMachine(MetaMachine machine, int circuitConfig, boolean fromSubnet) {
        if (machine instanceof IMultiController controller) {
            if (!ConfigHolder.INSTANCE.compat.ae2.pastingOnController) return;
            boolean controllerBlocked = controller instanceof IHasCircuitSlot controllerCircuit &&
                    controllerCircuit.isCircuitPastingBlocked();
            // case 1: multis with pc (custom primitive ones)
            if (!controllerBlocked && controller instanceof IHasCircuitSlot circuitMachine &&
                    circuitMachine.isCircuitSlotEnabled()) {
                circuitMachine.getCircuitInventory().setStackInSlot(0, IntCircuitBehaviour.stack(circuitConfig));
            }
            // case 2: apply on all hatches of a multi
            if (!controllerBlocked) {
                for (IMultiPart part : controller.getParts()) {
                    if (part instanceof IHasCircuitSlot circuitPart && circuitPart.isCircuitSlotEnabled() &&
                            !circuitPart.isCircuitPastingBlocked()) {
                        circuitPart.getCircuitInventory().setStackInSlot(0, IntCircuitBehaviour.stack(circuitConfig));
                    }
                }
            }
        } else if (machine instanceof MultiblockPartMachine) {
            if (!(fromSubnet ? ConfigHolder.INSTANCE.compat.ae2.subnetPastingOnHatch :
                    ConfigHolder.INSTANCE.compat.ae2.pastingOnHatch))
                return;
            // case 3: apply on hatch directly
            if (machine instanceof IHasCircuitSlot circuitMachine && circuitMachine.isCircuitSlotEnabled() &&
                    !circuitMachine.isCircuitPastingBlocked()) {
                circuitMachine.getCircuitInventory().setStackInSlot(0, IntCircuitBehaviour.stack(circuitConfig));
            }
        } else if (machine instanceof IHasCircuitSlot circuitMachine && circuitMachine.isCircuitSlotEnabled() &&
                !circuitMachine.isCircuitPastingBlocked()) {
                    if (!(fromSubnet ? ConfigHolder.INSTANCE.compat.ae2.subnetPastingOnSingleblock :
                            ConfigHolder.INSTANCE.compat.ae2.pastingOnSingleblock))
                        return;
                    // case 4: singleblock machine
                    circuitMachine.getCircuitInventory().setStackInSlot(0, IntCircuitBehaviour.stack(circuitConfig));
                }
    }

    // for pattern buffers. this gets the circuit
    public static int getCircuitConfig(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (patternDetails instanceof AEProcessingPattern processingPattern) {
            for (GenericStack sparseInput : processingPattern.getSparseInputs()) {
                if (sparseInput != null && sparseInput.what() instanceof AEItemKey itemKey) {
                    ItemStack stack = itemKey.toStack();
                    if (IntCircuitBehaviour.isIntegratedCircuit(stack)) {
                        return IntCircuitBehaviour.getCircuitConfiguration(stack);
                    }
                }
            }
        }
        if (inputHolder != null) {
            for (KeyCounter counter : inputHolder) {
                for (var entry : counter) {
                    if (entry.getKey() instanceof AEItemKey itemKey) {
                        ItemStack stack = itemKey.toStack();
                        if (IntCircuitBehaviour.isIntegratedCircuit(stack)) {
                            return IntCircuitBehaviour.getCircuitConfiguration(stack);
                        }
                    }
                }
            }
        }
        return -1;
    }

    // for pattern buffers. this strips the circuit from the pattern inputs
    public static void stripCircuitFromInputHolder(KeyCounter[] inputHolder) {
        if (inputHolder == null) return;
        for (KeyCounter counter : inputHolder) {
            List<AEKey> toRemove = new ArrayList<>();
            for (var entry : counter) {
                if (entry.getKey() instanceof AEItemKey itemKey) {
                    ItemStack stack = itemKey.toStack();
                    if (IntCircuitBehaviour.isIntegratedCircuit(stack)) {
                        toRemove.add(entry.getKey());
                    }
                }
            }
            for (AEKey key : toRemove) {
                counter.remove(key, Long.MAX_VALUE);
            }
        }
    }

    public static void applyCircuitToSubnet(IGrid grid, int circuitConfig, Set<MetaMachine> visitedMachines,
                                            Set<IGrid> visitedGrids) {
        applyCircuitToSubnet(grid, circuitConfig, visitedMachines, visitedGrids, 0);
    }

    public static void applyCircuitToSubnet(IGrid grid, int circuitConfig, Set<MetaMachine> visitedMachines,
                                            Set<IGrid> visitedGrids, int currentDepth) {
        if (grid == null || !visitedGrids.add(grid)) return;

        for (IGridNode node : grid.getNodes()) {
            if (node.getOwner() instanceof StorageBusPart storageBus) {
                BlockEntity hostBe = storageBus.getBlockEntity();
                if (hostBe == null || hostBe.getLevel() == null) continue;

                Level level = hostBe.getLevel();
                Direction side = storageBus.getSide();
                BlockPos targetPos = hostBe.getBlockPos().relative(side);

                MetaMachine machine = MetaMachine.getMachine(level, targetPos);
                if (machine != null && visitedMachines.add(machine)) {
                    applyCircuitToMachine(machine, circuitConfig, true);
                }

                IGridNode targetNode = GridHelper.getExposedNode(level, targetPos, side.getOpposite());
                if (targetNode == null) {
                    IInWorldGridNodeHost nodeHost = GridHelper.getNodeHost(level, targetPos);
                    if (nodeHost != null) {
                        targetNode = nodeHost.getGridNode(side.getOpposite());
                        if (targetNode == null) {
                            targetNode = nodeHost.getGridNode(null);
                        }
                    }
                }

                if (targetNode != null && targetNode.getGrid() != null) {
                    int maxDepth = ConfigHolder.INSTANCE.compat.ae2.recursiveSubnetDepthPastingAllowed;
                    if (maxDepth == -1 || currentDepth < maxDepth) {
                        applyCircuitToSubnet(targetNode.getGrid(), circuitConfig, visitedMachines, visitedGrids,
                                currentDepth + 1);
                    }
                }
            }
        }
    }
}
