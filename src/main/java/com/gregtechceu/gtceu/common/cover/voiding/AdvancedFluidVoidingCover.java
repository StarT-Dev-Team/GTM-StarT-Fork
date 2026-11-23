package com.gregtechceu.gtceu.common.cover.voiding;

import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.filter.FluidFilter;
import com.gregtechceu.gtceu.api.cover.filter.SimpleFluidFilter;
import com.gregtechceu.gtceu.api.gui.widget.EnumSelector;
import com.gregtechceu.gtceu.api.gui.widget.IntInput;
import com.gregtechceu.gtceu.api.gui.widget.NumberInput;
import com.gregtechceu.gtceu.api.transfer.fluid.IFluidHandlerModifiable;
import com.gregtechceu.gtceu.common.cover.data.BucketMode;
import com.gregtechceu.gtceu.common.cover.data.VoidingMode;
import com.gregtechceu.gtceu.utils.GTMath;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class AdvancedFluidVoidingCover extends FluidVoidingCover {

    @Persisted
    @DescSynced
    @Getter
    private VoidingMode voidingMode = VoidingMode.VOID_ANY;

    @Persisted
    @DescSynced
    @Getter
    protected int globalTransferSizeMillibuckets = 1;
    @Persisted
    @DescSynced
    @Getter
    private BucketMode transferBucketMode = BucketMode.MILLI_BUCKET;

    private NumberInput<Integer> stackSizeInput;
    private EnumSelector<BucketMode> stackSizeBucketModeInput;
    private VoidingMode appliedMode;

    public AdvancedFluidVoidingCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
    }

    //////////////////////////////////////////////
    // *********** COVER LOGIC ***********//
    //////////////////////////////////////////////

    @Override
    protected void doVoidFluids() {
        IFluidHandlerModifiable fluidHandler = getOwnFluidHandler();
        if (fluidHandler == null) {
            return;
        }

        switch (voidingMode) {
            case VOID_ANY -> voidAny(fluidHandler);
            case VOID_OVERFLOW -> voidOverflow(fluidHandler);
        }
    }

    private void voidOverflow(IFluidHandlerModifiable fluidHandler) {
        var fluidAmounts = enumerateDistinctFluids(fluidHandler, TransferDirection.EXTRACT);

        for (var entry : Object2LongMaps.fastIterable(fluidAmounts)) {
            var stack = entry.getKey();
            long presentAmount = entry.getLongValue();
            int targetAmount = getFilteredFluidAmount(stack);
            if (targetAmount <= 0L || targetAmount > presentAmount) continue;

            long diff = presentAmount - targetAmount;
            for (int op : GTMath.split(diff)) {
                var toDrain = new FluidStack(stack, op);
                fluidHandler.drain(toDrain, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    private int getFilteredFluidAmount(FluidStack fluidStack) {
        if (!filterHandler.isFilterPresent())
            return globalTransferSizeMillibuckets;

        FluidFilter filter = filterHandler.getFilter();
        return filter.isBlackList() ? globalTransferSizeMillibuckets : filter.testFluidAmount(fluidStack);
    }

    public void setVoidingMode(VoidingMode voidingMode) {
        this.voidingMode = voidingMode;

        configureStackSizeInput();

        if (!this.isRemote()) {
            configureFilter();
        }
    }

    private void setTransferBucketMode(BucketMode transferBucketMode) {
        var oldMultiplier = this.transferBucketMode.multiplier;
        var newMultiplier = transferBucketMode.multiplier;

        this.transferBucketMode = transferBucketMode;

        if (stackSizeInput == null) return;
        stackSizeInput.setValue(getCurrentBucketModeTransferSize());
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////

    @Override
    protected @NotNull String getUITitle() {
        return "cover.fluid.voiding.advanced.title";
    }

    @Override
    protected void buildAdditionalUI(UIElement group) {
        this.stackSizeInput = new IntInput(this::getCurrentBucketModeTransferSize,
                this::setCurrentBucketModeTransferSize);
        this.stackSizeInput.setMin(1);
        this.stackSizeInput.setMax(Integer.MAX_VALUE);
        this.stackSizeInput.layout(layout -> layout.flex(1).height(20));
        this.stackSizeInput.addEventListener(UIEvents.TICK, event -> {
            if (appliedMode != voidingMode) {
                configureStackSizeInput();
            }
        });

        this.stackSizeBucketModeInput = new EnumSelector<>(BucketMode.values(), () -> transferBucketMode,
                this::setTransferBucketMode);
        configureStackSizeInput();

        group.addChild(this.stackSizeInput);
        group.addChild(this.stackSizeBucketModeInput);
        group.addChild(new EnumSelector<>(VoidingMode.values(), () -> voidingMode, this::setVoidingMode));
    }

    private int getCurrentBucketModeTransferSize() {
        return this.globalTransferSizeMillibuckets / this.transferBucketMode.multiplier;
    }

    private void setCurrentBucketModeTransferSize(int transferSize) {
        this.globalTransferSizeMillibuckets = Math.max(transferSize * this.transferBucketMode.multiplier, 0);
    }

    @Override
    protected void configureFilter() {
        if (filterHandler.getFilter() instanceof SimpleFluidFilter filter) {
            filter.setMaxStackSize(voidingMode == VoidingMode.VOID_ANY ? 1 : Integer.MAX_VALUE);
        }

        configureStackSizeInput();
    }

    private void configureStackSizeInput() {
        if (this.stackSizeInput == null || stackSizeBucketModeInput == null)
            return;

        this.appliedMode = this.voidingMode;
        this.stackSizeInput.setDisplay(shouldShowStackSize());
        this.stackSizeBucketModeInput.setDisplay(shouldShowStackSize());
    }

    private boolean shouldShowStackSize() {
        if (this.voidingMode == VoidingMode.VOID_ANY)
            return false;

        if (!this.filterHandler.isFilterPresent())
            return true;

        return this.filterHandler.getFilter().isBlackList();
    }

    //////////////////////////////////////
    // ***** LDLib SyncData ******//
    //////////////////////////////////////
}
