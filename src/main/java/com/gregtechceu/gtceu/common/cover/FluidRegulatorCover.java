package com.gregtechceu.gtceu.common.cover;

import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.filter.FluidFilter;
import com.gregtechceu.gtceu.api.cover.filter.SimpleFluidFilter;
import com.gregtechceu.gtceu.api.gui.widget.EnumSelector;
import com.gregtechceu.gtceu.api.gui.widget.IntInput;
import com.gregtechceu.gtceu.api.gui.widget.NumberInput;
import com.gregtechceu.gtceu.api.transfer.fluid.IFluidHandlerModifiable;
import com.gregtechceu.gtceu.common.cover.data.BucketMode;
import com.gregtechceu.gtceu.common.cover.data.TransferMode;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FluidRegulatorCover extends PumpCover {

    private static final int MAX_STACK_SIZE = 2_048_000_000; // Capacity of quantum tank IX

    @Persisted
    @DescSynced
    @Getter
    private TransferMode transferMode = TransferMode.TRANSFER_ANY;

    @Persisted
    @DescSynced
    @Getter
    private BucketMode transferBucketMode = BucketMode.MILLI_BUCKET;
    @Persisted
    @DescSynced
    @Getter
    protected int globalTransferLimit;
    protected int fluidTransferBuffered = 0;

    private NumberInput<Integer> transferSizeInput;
    private EnumSelector<BucketMode> transferBucketModeInput;
    private TransferMode appliedMode;
    private BucketMode appliedBucketMode;

    public FluidRegulatorCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide, int tier,
                               int maxTransferRate) {
        super(definition, coverHolder, attachedSide, tier, maxTransferRate);
    }

    public FluidRegulatorCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide, int tier) {
        this(definition, coverHolder, attachedSide, tier, PUMP_SCALING.applyAsInt(tier));
    }

    //////////////////////////////////////
    // ***** Transfer Logic ******//
    //////////////////////////////////////

    @Override
    protected int doTransferFluidsInternal(IFluidHandlerModifiable source, IFluidHandlerModifiable destination,
                                           int platformTransferLimit) {
        return switch (transferMode) {
            case TRANSFER_ANY -> transferAny(source, destination, platformTransferLimit);
            case TRANSFER_EXACT -> transferExact(source, destination, platformTransferLimit);
            case KEEP_EXACT -> keepExact(source, destination, platformTransferLimit);
        };
    }

    private int transferExact(IFluidHandler source, IFluidHandler destination, int platformTransferLimit) {
        int fluidLeftToTransfer = platformTransferLimit;

        for (int slot = 0; slot < source.getTanks(); slot++) {
            if (fluidLeftToTransfer <= 0)
                break;

            FluidStack sourceFluid = source.getFluidInTank(slot).copy();
            int supplyAmount = getFilteredFluidAmount(sourceFluid);

            // If the remaining transferrable amount in this operation is not enough to transfer the full stack size,
            // the remaining amount for this operation will be buffered and added to the next operation's maximum.
            if (fluidLeftToTransfer + fluidTransferBuffered < supplyAmount) {
                this.fluidTransferBuffered += fluidLeftToTransfer;
                fluidLeftToTransfer = 0;
                break;
            }

            if (sourceFluid.isEmpty() || supplyAmount <= 0)
                continue;

            sourceFluid.setAmount(supplyAmount);
            FluidStack drained = source.drain(sourceFluid, FluidAction.SIMULATE);

            if (drained.isEmpty() || drained.getAmount() < supplyAmount)
                continue;

            int insertableAmount = destination.fill(drained.copy(), FluidAction.SIMULATE);
            if (insertableAmount != supplyAmount)
                continue;

            drained.setAmount(insertableAmount);
            drained = source.drain(drained, FluidAction.EXECUTE);

            if (!drained.isEmpty()) {
                destination.fill(drained, FluidAction.EXECUTE);
                fluidLeftToTransfer -= (drained.getAmount() - fluidTransferBuffered);
            }

            fluidTransferBuffered = 0;
        }

        return platformTransferLimit - fluidLeftToTransfer;
    }

    private int keepExact(IFluidHandlerModifiable source, IFluidHandlerModifiable destination,
                          int platformTransferLimit) {
        int fluidLeftToTransfer = platformTransferLimit;

        var sourceAmounts = enumerateDistinctFluids(source, TransferDirection.EXTRACT);
        var destinationAmounts = enumerateDistinctFluids(destination, TransferDirection.INSERT);

        for (FluidStack fluidStack : sourceAmounts.keySet()) {
            if (fluidLeftToTransfer <= 0) break;

            int amountToKeep = getFilteredFluidAmount(fluidStack);
            long amountInDest = destinationAmounts.getOrDefault(fluidStack, 0);
            if (amountInDest >= amountToKeep) continue;

            FluidStack fluidToMove = fluidStack.copy();
            fluidToMove.setAmount(Math.min(fluidLeftToTransfer, (int) (amountToKeep - amountInDest)));
            if (fluidToMove.getAmount() <= 0) continue;

            FluidStack drained = source.drain(fluidToMove, FluidAction.SIMULATE);
            int fillableAmount = destination.fill(drained, FluidAction.SIMULATE);
            if (fillableAmount <= 0) continue;

            fluidToMove.setAmount(Math.min(fluidToMove.getAmount(), fillableAmount));

            drained = source.drain(fluidToMove, FluidAction.EXECUTE);
            int movedAmount = destination.fill(drained, FluidAction.EXECUTE);

            fluidLeftToTransfer -= movedAmount;
        }

        return platformTransferLimit - fluidLeftToTransfer;
    }

    private void setTransferBucketMode(BucketMode transferBucketMode) {
        var oldMultiplier = this.transferBucketMode.multiplier;
        var newMultiplier = transferBucketMode.multiplier;

        this.transferBucketMode = transferBucketMode;

        if (transferSizeInput == null) return;

        if (oldMultiplier > newMultiplier) {
            transferSizeInput.setValue(getCurrentBucketModeTransferSize());
        }
        this.transferSizeInput.setMax(MAX_STACK_SIZE / this.transferBucketMode.multiplier);
        if (newMultiplier > oldMultiplier) {
            transferSizeInput.setValue(getCurrentBucketModeTransferSize());
        }
    }

    private void setTransferMode(TransferMode transferMode) {
        this.transferMode = transferMode;

        configureTransferSizeInput();

        if (!this.isRemote()) {
            configureFilter();
        }
    }

    @Override
    protected void configureFilter() {
        if (filterHandler.getFilter() instanceof SimpleFluidFilter filter) {
            filter.setMaxStackSize(transferMode == TransferMode.TRANSFER_ANY ? 1 : MAX_STACK_SIZE);
        }

        configureTransferSizeInput();
    }

    private int getFilteredFluidAmount(FluidStack fluidStack) {
        if (!filterHandler.isFilterPresent())
            return globalTransferLimit;

        FluidFilter filter = filterHandler.getFilter();
        return (filter.supportsAmounts() ? filter.testFluidAmount(fluidStack) : globalTransferLimit);
    }

    ///////////////////////////
    // ***** GUI ******//
    ///////////////////////////

    @Override
    protected @NotNull String getUITitle() {
        return "cover.fluid_regulator.title";
    }

    @Override
    protected void buildAdditionalUI(UIElement group) {
        this.transferSizeInput = new IntInput(this::getCurrentBucketModeTransferSize,
                this::setCurrentBucketModeTransferSize);
        this.transferSizeInput.setMin(0);
        this.transferSizeInput.setMax(Integer.MAX_VALUE);
        this.transferSizeInput.layout(layout -> layout.flex(1).height(20));
        this.transferSizeInput.addEventListener(UIEvents.TICK, event -> {
            if (appliedMode != transferMode) {
                configureTransferSizeInput();
            }
            if (appliedBucketMode != transferBucketMode) {
                appliedBucketMode = transferBucketMode;
                transferSizeInput.setMax(MAX_STACK_SIZE / transferBucketMode.multiplier);
            }
        });

        this.transferBucketModeInput = new EnumSelector<>(BucketMode.values(), () -> transferBucketMode,
                this::setTransferBucketMode);
        configureTransferSizeInput();

        group.addChild(this.transferSizeInput);
        group.addChild(this.transferBucketModeInput);
        group.addChild(new EnumSelector<>(TransferMode.values(), () -> transferMode, this::setTransferMode));
    }

    private int getCurrentBucketModeTransferSize() {
        return this.globalTransferLimit / this.transferBucketMode.multiplier;
    }

    private void setCurrentBucketModeTransferSize(int transferSize) {
        this.globalTransferLimit = Math.min(Math.max(transferSize * this.transferBucketMode.multiplier, 0),
                MAX_STACK_SIZE);
    }

    private void configureTransferSizeInput() {
        if (this.transferSizeInput == null || transferBucketModeInput == null)
            return;

        this.appliedMode = this.transferMode;
        this.transferSizeInput.setDisplay(shouldShowTransferSize());
        this.transferBucketModeInput.setDisplay(shouldShowTransferSize());
    }

    private boolean shouldShowTransferSize() {
        if (this.transferMode == TransferMode.TRANSFER_ANY)
            return false;

        if (!this.filterHandler.isFilterPresent())
            return true;

        return !this.filterHandler.getFilter().supportsAmounts();
    }
}
