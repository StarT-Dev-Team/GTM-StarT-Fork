package com.gregtechceu.gtceu.common.machine.storage;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.IntInput;
import com.gregtechceu.gtceu.api.gui.widget.PhantomFluidSlot;
import com.gregtechceu.gtceu.api.gui.widget.TextToggleButton;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DropSaved;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.items.ItemHandlerHelper;

import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class CreativeTankMachine extends QuantumTankMachine {

    @Getter
    @Persisted
    @DropSaved
    private int mBPerCycle = 1000;
    @Getter
    @Persisted
    @DropSaved
    private int ticksPerCycle = 1;

    public CreativeTankMachine(IMachineBlockEntity holder) {
        super(holder, GTValues.MAX, 1);
    }

    protected FluidCache createCacheFluidHandler(Object... args) {
        return new InfiniteCache(this);
    }

    protected void checkAutoOutput() {
        if (getOffsetTimer() % ticksPerCycle == 0) {
            if (isAutoOutputFluids() && getOutputFacingFluids() != null) {
                cache.exportToNearby(getOutputFacingFluids());
            }
            updateAutoOutputSubscription();
        }
    }

    @Override
    public long getStoredAmount() {
        return (long) Math.ceil(1d * mBPerCycle / ticksPerCycle);
    }

    private InteractionResult updateStored(FluidStack fluid) {
        stored = new FluidStack(fluid, 1000);
        onFluidChanged();
        return InteractionResult.SUCCESS;
    }

    private void setTicksPerCycle(int value) {
        ticksPerCycle = value;
        onFluidChanged();
    }

    private void setmBPerCycle(int value) {
        mBPerCycle = value;
        onFluidChanged();
    }

    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                   BlockHitResult hit) {
        var heldItem = player.getItemInHand(hand);
        if (hit.getDirection() == getFrontFacing() && !isRemote()) {
            // Clear fluid if empty + shift-rclick
            if (heldItem.isEmpty()) {
                if (player.isCrouching() && !stored.isEmpty()) {
                    return updateStored(FluidStack.EMPTY);
                }
                return InteractionResult.PASS;
            }

            // If no fluid set and held-item has fluid, set fluid
            if (stored.isEmpty()) {
                return FluidUtil.getFluidContained(heldItem)
                        .map(this::updateStored)
                        .orElse(InteractionResult.PASS);
            }

            // Need to make a fake source to fully fill held-item since our cache only allows mbPerTick extraction
            CustomFluidTank source = new CustomFluidTank(new FluidStack(stored, Integer.MAX_VALUE));
            ItemStack result = FluidUtil.tryFillContainer(heldItem, source, Integer.MAX_VALUE, player, true)
                    .getResult();
            if (!result.isEmpty() && heldItem.getCount() > 1) {
                ItemHandlerHelper.giveItemToPlayer(player, result);
                result = heldItem.copy();
                result.shrink(1);
            }

            if (!result.isEmpty()) {
                player.setItemInHand(hand, result);
                return InteractionResult.SUCCESS;
            } else {
                return FluidUtil.getFluidContained(heldItem)
                        .map(this::updateStored)
                        .orElse(InteractionResult.PASS);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public UIElement createUIWidget() {
        var group = new UIElement();
        group.layout(layout -> layout.width(176).height(131));

        var fluidSlot = new PhantomFluidSlot(this::getStored, this::updateStored);
        fluidSlot.amountLabel.setDisplay(false);
        fluidSlot.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(36).top(6));
        fluidSlot.style(style -> style.backgroundTexture(GuiTextures.FLUID_SLOT));

        var fluidLabel = new Label();
        fluidLabel.setText(Component.translatable("gtceu.creative.tank.fluid"));
        fluidLabel.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(7).top(9));
        var mbpcLabel = new Label();
        mbpcLabel.setText(Component.translatable("gtceu.creative.tank.mbpc"));
        mbpcLabel.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(7).top(28));
        var mbpcInput = new IntInput(() -> mBPerCycle, this::setmBPerCycle);
        mbpcInput.setMin(1);
        mbpcInput.setMax(Integer.MAX_VALUE);
        mbpcInput.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(7).top(43).width(152).height(16));
        var tpcLabel = new Label();
        tpcLabel.setText(Component.translatable("gtceu.creative.tank.tpc"));
        tpcLabel.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(7).top(65));
        var tpcInput = new IntInput(() -> ticksPerCycle, this::setTicksPerCycle);
        tpcInput.setMin(1);
        tpcInput.setMax(Integer.MAX_VALUE);
        tpcInput.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(7).top(80).width(152).height(16));
        var activity = new TextToggleButton("gtceu.creative.activity.on", "gtceu.creative.activity.off",
                this::isWorkingEnabled, this::setWorkingEnabled);
        activity.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(7).top(101).width(162).height(20));

        group.addChildren(fluidSlot, fluidLabel, mbpcLabel, mbpcInput, tpcLabel, tpcInput, activity);
        return group;
    }

    private class InfiniteCache extends FluidCache {

        public InfiniteCache(MetaMachine holder) {
            super(holder);
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return stored;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!stored.isEmpty() && stored.isFluidEqual(resource)) return resource.getAmount();
            return 0;
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            if (!stored.isEmpty()) return new FluidStack(stored, mBPerCycle);
            return FluidStack.EMPTY;
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (!stored.isEmpty() && stored.isFluidEqual(resource)) return new FluidStack(resource, mBPerCycle);
            return FluidStack.EMPTY;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return true;
        }

        @Override
        public int getTankCapacity(int tank) {
            return 1000;
        }
    }
}
