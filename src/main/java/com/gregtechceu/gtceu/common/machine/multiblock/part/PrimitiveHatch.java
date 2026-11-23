package com.gregtechceu.gtceu.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.FluidTankProxyTrait;
import com.gregtechceu.gtceu.api.machine.trait.ItemHandlerProxyTrait;
import com.gregtechceu.gtceu.api.transfer.fluid.FluidHandlerList;
import com.gregtechceu.gtceu.common.machine.multiblock.primitive.PrimitiveWorkableMachine;
import com.gregtechceu.gtceu.utils.GTTransferUtils;

import com.lowdragmc.lowdraglib2.syncdata.ISubscription;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PrimitiveHatch extends MultiblockPartMachine {

    private final ItemHandlerProxyTrait inputInventory;
    private final List<IItemHandlerModifiable> inputInventoryList = new ArrayList<>();

    private final ItemHandlerProxyTrait outputInventory;
    private final List<IItemHandlerModifiable> outputInventoryList = new ArrayList<>();

    private final FluidTankProxyTrait inputTank;
    private final List<IFluidHandler> inputTankList = new ArrayList<>();

    private final FluidTankProxyTrait outputTank;
    private final List<IFluidHandler> outputTankList = new ArrayList<>();

    @Nullable
    protected TickableSubscription autoIOSubs;
    protected final Map<PrimitiveWorkableMachine, ISubscription> outputInventorySubs = new Object2ObjectOpenHashMap<>();
    protected final Map<PrimitiveWorkableMachine, ISubscription> outputTankSubs = new Object2ObjectOpenHashMap<>();

    public PrimitiveHatch(IMachineBlockEntity holder, Object... args) {
        super(holder);
        inputInventory = new ItemHandlerProxyTrait(this, IO.IN);
        outputInventory = new ItemHandlerProxyTrait(this, IO.OUT);
        inputTank = new FluidTankProxyTrait(this, IO.IN);
        outputTank = new FluidTankProxyTrait(this, IO.OUT);
    }

    @Override
    public void addedToController(IMultiController controller) {
        super.addedToController(controller);
        if (!(controller instanceof PrimitiveWorkableMachine primitiveMachine)) {
            return;
        }

        outputInventorySubs.put(primitiveMachine,
                primitiveMachine.exportItems.addChangedListener(this::updateAutoIOSubscription));
        outputTankSubs.put(primitiveMachine,
                primitiveMachine.exportFluids.addChangedListener(this::updateAutoIOSubscription));
        inputInventoryList.add(primitiveMachine.importItems);
        outputInventoryList.add(primitiveMachine.exportItems);
        inputTankList.add(primitiveMachine.importFluids);
        outputTankList.add(primitiveMachine.exportFluids);
        updateHandlersProxies();
        updateAutoIOSubscription();
    }

    @Override
    public void removedFromController(IMultiController controller) {
        super.removedFromController(controller);
        if (!(controller instanceof PrimitiveWorkableMachine primitiveMachine)) return;

        outputInventorySubs.computeIfPresent(primitiveMachine, (machine, sub) -> {
            sub.unsubscribe();
            return null;
        });
        outputTankSubs.computeIfPresent(primitiveMachine, (machine, sub) -> {
            sub.unsubscribe();
            return null;
        });
        inputInventoryList.remove(primitiveMachine.importItems);
        outputInventoryList.remove(primitiveMachine.exportItems);
        inputTankList.remove(primitiveMachine.importFluids);
        outputTankList.remove(primitiveMachine.exportFluids);
        updateHandlersProxies();
        updateAutoIOSubscription();
    }

    private void updateHandlersProxies() {
        inputInventory.setProxy(new CombinedInvWrapper(inputInventoryList.toArray(IItemHandlerModifiable[]::new)));
        outputInventory.setProxy(new CombinedInvWrapper(outputInventoryList.toArray(IItemHandlerModifiable[]::new)));
        inputTank.setProxy(new FluidHandlerList(inputTankList.toArray(IFluidHandler[]::new)));
        outputTank.setProxy(new FluidHandlerList(outputTankList.toArray(IFluidHandler[]::new)));
    }

    protected void updateAutoIOSubscription() {
        var canAutoOutputItems = !outputInventory.isEmpty() &&
                GTTransferUtils.hasAdjacentItemHandler(getLevel(), getPos(), getFrontFacing());
        var canAutoOutputFluids = !outputTank.isEmpty() &&
                GTTransferUtils.hasAdjacentFluidHandler(getLevel(), getPos(), getFrontFacing());

        if (canAutoOutputItems || canAutoOutputFluids) {
            autoIOSubs = subscribeServerTick(autoIOSubs, this::autoIO);
        } else if (autoIOSubs != null) {
            autoIOSubs.unsubscribe();
            autoIOSubs = null;
        }
    }

    protected void autoIO() {
        if (getOffsetTimer() % 5 == 0) {
            outputInventory.exportToNearby(getFrontFacing());
            outputTank.exportToNearby(getFrontFacing());
            updateAutoIOSubscription();
        }
    }

    @Override
    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChanged(block, fromPos, isMoving);
        updateAutoIOSubscription();
    }

    @Override
    public void onRotated(Direction oldFacing, Direction newFacing) {
        super.onRotated(oldFacing, newFacing);
        updateAutoIOSubscription();
    }

    @Override
    public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        return false;
    }
}
