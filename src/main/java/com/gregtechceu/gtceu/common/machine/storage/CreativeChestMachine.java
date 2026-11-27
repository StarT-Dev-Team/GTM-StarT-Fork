package com.gregtechceu.gtceu.common.machine.storage;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.gui.widget.IntInput;
import com.gregtechceu.gtceu.api.gui.widget.PhantomItemSlot;
import com.gregtechceu.gtceu.api.gui.widget.TextToggleButton;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DropSaved;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.items.ItemHandlerHelper;

import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeChestMachine extends QuantumChestMachine {

    @Getter
    @Persisted
    @DropSaved
    private int itemsPerCycle = 1;
    @Getter
    @Persisted
    @DropSaved
    private int ticksPerCycle = 1;

    public CreativeChestMachine(IMachineBlockEntity holder) {
        super(holder, GTValues.MAX, -1);
    }

    @Override
    protected ItemCache createCacheItemHandler(Object... args) {
        return new InfiniteCache(this);
    }

    protected void checkAutoOutput() {
        if (getOffsetTimer() % ticksPerCycle == 0) {
            if (isAutoOutputItems() && getOutputFacingItems() != null) {
                cache.exportToNearby(getOutputFacingItems());
            }
            updateAutoOutputSubscription();
        }
    }

    private InteractionResult updateStored(ItemStack item) {
        stored = item.copyWithCount(1);
        onItemChanged();
        return InteractionResult.SUCCESS;
    }

    private void setTicksPerCycle(int value) {
        ticksPerCycle = value;
        onItemChanged();
    }

    private void setItemsPerCycle(int value) {
        itemsPerCycle = value;
        onItemChanged();
    }

    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                   BlockHitResult hit) {
        var heldItem = player.getItemInHand(hand);
        if (hit.getDirection() == getFrontFacing() && !isRemote()) {
            // Clear item if empty hand + shift-rclick
            if (heldItem.isEmpty() && player.isCrouching() && !stored.isEmpty()) {
                return updateStored(ItemStack.EMPTY);
            }

            // If held item can stack with stored item, delete held item
            if (!heldItem.isEmpty() && ItemHandlerHelper.canItemStacksStack(stored, heldItem)) {
                player.setItemInHand(hand, ItemStack.EMPTY);
                return InteractionResult.SUCCESS;
            } else if (!heldItem.isEmpty()) { // If held item is different than stored item, update stored item
                return updateStored(heldItem);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public UIElement createUIWidget() {
        var group = new UIElement();
        group.layout(layout -> layout.width(176).height(131));

        var itemSlot = new PhantomItemSlot(cache, 0);
        itemSlot.setClearSlotOnRightClick(true);
        itemSlot.setMaxStackSize(1);
        itemSlot.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(36).top(6));

        var itemLabel = new Label();
        itemLabel.setText(Component.translatable("gtceu.creative.chest.item"));
        itemLabel.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(7).top(9));
        var ipcLabel = new Label();
        ipcLabel.setText(Component.translatable("gtceu.creative.chest.ipc"));
        ipcLabel.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(7).top(28));
        var ipcInput = new IntInput(() -> itemsPerCycle, this::setItemsPerCycle);
        ipcInput.setMin(1);
        ipcInput.setMax(Integer.MAX_VALUE);
        ipcInput.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(7).top(43).width(152).height(16));
        var tpcLabel = new Label();
        tpcLabel.setText(Component.translatable("gtceu.creative.chest.tpc"));
        tpcLabel.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(7).top(65));
        var tpcInput = new IntInput(() -> ticksPerCycle, this::setTicksPerCycle);
        tpcInput.setMin(1);
        tpcInput.setMax(Integer.MAX_VALUE);
        tpcInput.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(7).top(80).width(152).height(16));
        var activity = new TextToggleButton("gtceu.creative.activity.on", "gtceu.creative.activity.off",
                this::isWorkingEnabled, this::setWorkingEnabled);
        activity.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(7).top(101).width(162).height(20));

        group.addChildren(itemSlot, itemLabel, ipcLabel, ipcInput, tpcLabel, tpcInput, activity);
        return group;
    }

    private class InfiniteCache extends ItemCache {

        public InfiniteCache(MetaMachine holder) {
            super(holder);
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return stored;
        }

        @Override
        public void setStackInSlot(int index, ItemStack stack) {
            updateStored(stack);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!stored.isEmpty() && GTUtil.isSameItemSameTags(stored, stack)) return ItemStack.EMPTY;
            return stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!stored.isEmpty()) return stored.copyWithCount(itemsPerCycle);
            return ItemStack.EMPTY;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return true;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    }
}
