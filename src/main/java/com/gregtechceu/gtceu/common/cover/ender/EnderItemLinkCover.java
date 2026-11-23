package com.gregtechceu.gtceu.common.cover.ender;

import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.filter.FilterHandler;
import com.gregtechceu.gtceu.api.cover.filter.FilterHandlers;
import com.gregtechceu.gtceu.api.cover.filter.ItemFilter;
import com.gregtechceu.gtceu.api.misc.virtualregistry.EntryTypes;
import com.gregtechceu.gtceu.api.misc.virtualregistry.VirtualEnderRegistry;
import com.gregtechceu.gtceu.api.misc.virtualregistry.VirtualEntry;
import com.gregtechceu.gtceu.api.misc.virtualregistry.entries.VirtualItemStorage;
import com.gregtechceu.gtceu.utils.GTTransferUtils;

import com.lowdragmc.lowdraglib2.gui.slot.LocalSlot;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class EnderItemLinkCover extends AbstractEnderLinkCover<VirtualItemStorage> {

    protected static final int TRANSFER_RATE = 8;

    @Persisted
    @DescSynced
    protected VirtualItemStorage storage;
    protected int itemsLeftToTransferLastSecond;
    @Getter
    @Persisted
    @DescSynced
    protected FilterHandler<ItemStack, ItemFilter> filterHandler;

    public EnderItemLinkCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
        itemsLeftToTransferLastSecond = TRANSFER_RATE * 20;
        filterHandler = FilterHandlers.item(this);
        if (!isRemote()) storage = VirtualEnderRegistry.getInstance().getOrCreateEntry(getOwner(),
                EntryTypes.ENDER_ITEM, getChannelName());
    }

    @Override
    public boolean canAttach() {
        return true;
    }

    @Override
    protected String identifier() {
        return "EILink#";
    }

    @Override
    protected VirtualItemStorage getEntry() {
        return storage;
    }

    @Override
    protected void setEntry(VirtualEntry entry) {
        storage = (VirtualItemStorage) entry;
    }

    @Override
    protected EntryTypes<VirtualItemStorage> getEntryType() {
        return EntryTypes.ENDER_ITEM;
    }

    @Override
    protected void transfer() {
        long timer = coverHolder.getOffsetTimer();
        if (itemsLeftToTransferLastSecond > 0) {
            itemsLeftToTransferLastSecond -= doTransferItems(itemsLeftToTransferLastSecond);
        }
        if (timer % 20 == 0) itemsLeftToTransferLastSecond = TRANSFER_RATE * 20;
    }

    private int doTransferItems(int max) {
        IItemHandler ownHandler = getOwnItemHandler();
        if (ownHandler == null) return 0;
        return switch (io) {
            case IN -> GTTransferUtils.transferItemsFiltered(ownHandler, storage.getHandler(),
                    filterHandler.getFilter(), max);
            case OUT -> GTTransferUtils.transferItemsFiltered(storage.getHandler(), ownHandler,
                    filterHandler.getFilter(), max);
            default -> 0;
        };
    }

    public @Nullable IItemHandler getOwnItemHandler() {
        return coverHolder.getItemHandlerCap(attachedSide, false);
    }

    @Override
    protected UIElement addVirtualEntryWidget(Supplier<? extends VirtualEntry> entry, boolean interactive) {
        var group = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW));
        if (!interactive) {
            var slot = new ItemSlot().bind(new LocalSlot());
            slot.addEventListener(UIEvents.TICK, event -> slot.setItem(firstStack((VirtualItemStorage) entry.get()),
                    false));
            group.addChild(slot);
            return group;
        }
        var handler = new EntryHandler(entry);
        for (int i = 0; i < handler.getSlots(); i++) {
            group.addChild(new ItemSlot().bind(handler, i));
        }
        return group;
    }

    private static ItemStack firstStack(VirtualItemStorage storage) {
        var handler = storage.getHandler();
        for (int i = 0; i < handler.getSlots(); i++) {
            if (!handler.getStackInSlot(i).isEmpty()) return handler.getStackInSlot(i);
        }
        return ItemStack.EMPTY;
    }

    private static class EntryHandler implements IItemHandlerModifiable {

        private final Supplier<? extends VirtualEntry> entry;

        private EntryHandler(Supplier<? extends VirtualEntry> entry) {
            this.entry = entry;
        }

        private IItemHandlerModifiable delegate() {
            return ((VirtualItemStorage) entry.get()).getHandler();
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            delegate().setStackInSlot(slot, stack);
        }

        @Override
        public int getSlots() {
            return delegate().getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return delegate().getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return delegate().insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return delegate().extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return delegate().getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return delegate().isItemValid(slot, stack);
        }
    }

    @Override
    protected String getUITitle() {
        return "cover.ender_item_link.title";
    }
}
