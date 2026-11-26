package com.gregtechceu.gtceu.common.cover;

import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.IUICover;
import com.gregtechceu.gtceu.api.cover.filter.ItemFilter;
import com.gregtechceu.gtceu.api.cover.filter.SmartItemFilter;
import com.gregtechceu.gtceu.api.gui.widget.EnumSelector;
import com.gregtechceu.gtceu.api.machine.MachineCoverContainer;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.transfer.item.ItemHandlerDelegate;
import com.gregtechceu.gtceu.common.cover.data.FilterMode;
import com.gregtechceu.gtceu.common.cover.data.ManualIOMode;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ItemFilterCover extends CoverBehavior implements IUICover {

    protected ItemFilter itemFilter;
    @Persisted
    @DescSynced
    @Getter
    protected FilterMode filterMode = FilterMode.FILTER_INSERT;
    private FilteredItemHandlerWrapper itemFilterWrapper;
    @Persisted
    @Setter
    @Getter
    protected ManualIOMode allowFlow = ManualIOMode.DISABLED;

    public ItemFilterCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
    }

    public ItemFilter getItemFilter() {
        if (itemFilter == null) {
            itemFilter = ItemFilter.loadFilter(attachItem);
            if (itemFilter instanceof SmartItemFilter smart && coverHolder instanceof MachineCoverContainer mcc) {
                var machine = MetaMachine.getMachine(mcc.getLevel(), mcc.getPos());
                if (machine != null) smart.setModeFromMachine(machine.getDefinition().getName());
            }
        }
        return itemFilter;
    }

    public void setFilterMode(FilterMode filterMode) {
        this.filterMode = filterMode;
        coverHolder.markDirty();
    }

    @Override
    public boolean canAttach() {
        return super.canAttach() && coverHolder.getItemHandlerCap(attachedSide, false) != null;
    }

    @Override
    public @Nullable IItemHandlerModifiable getItemHandlerCap(IItemHandlerModifiable defaultValue) {
        if (defaultValue == null) {
            return null;
        }
        if (itemFilterWrapper == null || itemFilterWrapper.delegate != defaultValue) {
            this.itemFilterWrapper = new FilteredItemHandlerWrapper(defaultValue);
        }
        return itemFilterWrapper;
    }

    @Override
    public void onAttached(ItemStack itemStack, @Nullable ServerPlayer player) {
        super.onAttached(itemStack, player);
    }

    @Override
    public Component getCoverUITitle() {
        return Component.translatable(attachItem.getDescriptionId());
    }

    @Override
    public UIElement createUIWidget() {
        var body = new UIElement().layout(layout -> layout.minWidth(168).flexDirection(FlexDirection.ROW).gapAll(8));
        var selectors = new UIElement().layout(layout -> layout.gapAll(2));
        selectors.addChild(new EnumSelector<>(FilterMode.VALUES, () -> filterMode, this::setFilterMode)
                .layout(layout -> layout.width(18).height(18)));
        selectors.addChild(new EnumSelector<>(ManualIOMode.VALUES, () -> allowFlow, this::setAllowFlow)
                .layout(layout -> layout.width(18).height(18)));
        body.addChildren(selectors, getItemFilter().openConfigurator());

        return body;
    }

    private class FilteredItemHandlerWrapper extends ItemHandlerDelegate {

        public FilteredItemHandlerWrapper(IItemHandlerModifiable delegate) {
            super(delegate);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if ((filterMode == FilterMode.FILTER_EXTRACT) && allowFlow == ManualIOMode.UNFILTERED)
                return super.insertItem(slot, stack, simulate);
            if (filterMode != FilterMode.FILTER_EXTRACT && getItemFilter().test(stack)) {
                return super.insertItem(slot, stack, simulate);
            }
            return stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack result = super.extractItem(slot, amount, true);
            if (result.isEmpty() && (filterMode == FilterMode.FILTER_INSERT) && allowFlow == ManualIOMode.UNFILTERED) {
                return super.extractItem(slot, amount, false);
            }

            if (filterMode != FilterMode.FILTER_INSERT && getItemFilter().test(result)) {
                return super.extractItem(slot, amount, false);
            }
            return ItemStack.EMPTY;
        }
    }
}
