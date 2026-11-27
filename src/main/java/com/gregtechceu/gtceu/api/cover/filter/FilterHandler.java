package com.gregtechceu.gtceu.api.cover.filter;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.MenuSlots;
import com.gregtechceu.gtceu.api.machine.MachineCoverContainer;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.syncdata.IManaged;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class FilterHandler<T, F extends Filter<T, F>> implements IManaged {

    private final IManaged container;

    @Persisted
    @DescSynced
    private @NotNull ItemStack filterItem = ItemStack.EMPTY;

    private @Nullable F filter;
    private @Nullable CustomItemStackHandler filterSlot;
    private @Nullable UIElement filterGroup;

    private @NotNull Consumer<F> onFilterLoaded = (filter) -> {};
    private @NotNull Consumer<F> onFilterRemoved = (filter) -> {};
    private @NotNull Consumer<F> onFilterUpdated = (filter) -> {};

    public FilterHandler(IManaged container) {
        this.container = container;
    }

    protected abstract F loadFilter(ItemStack filterItem);

    protected abstract F getEmptyFilter();

    protected abstract boolean canInsertFilterItem(ItemStack itemStack);

    //////////////////////////////////
    // ***** PUBLIC API ******//
    //////////////////////////////////

    public ItemSlot createFilterSlotUI() {
        var slot = new ItemSlot().bind(getFilterSlot(), 0);
        slot.style(style -> style.backgroundTexture(
                IGuiTexture.group(GuiTextures.SLOT, GuiTextures.FILTER_SLOT_OVERLAY)));
        return slot;
    }

    public UIElement createFilterConfigUI() {
        this.filterGroup = new UIElement();
        if (!this.filterItem.isEmpty()) {
            this.filterGroup.addChild(getFilter().openConfigurator());
        }

        return this.filterGroup;
    }

    public boolean isFilterPresent() {
        return filter != null || !filterItem.isEmpty();
    }

    public F getFilter() {
        if (this.filter == null) {
            if (this.filterItem.isEmpty()) {
                return getEmptyFilter();
            } else {
                loadFilterFromItem();
            }
        }

        return this.filter;
    }

    public boolean test(T resource) {
        return getFilter().test(resource);
    }

    public FilterHandler<T, F> onFilterLoaded(Consumer<F> onFilterLoaded) {
        this.onFilterLoaded = onFilterLoaded;
        return this;
    }

    public FilterHandler<T, F> onFilterRemoved(Consumer<F> onFilterRemoved) {
        this.onFilterRemoved = onFilterRemoved;
        return this;
    }

    public FilterHandler<T, F> onFilterUpdated(Consumer<F> onFilterUpdated) {
        this.onFilterUpdated = onFilterUpdated;
        return this;
    }

    public @NotNull ItemStack getFilterItem() {
        return (this.filterSlot != null) ? this.filterSlot.getStackInSlot(0) : this.filterItem;
    }

    public void setFilterItem(ItemStack item) {
        getFilterSlot().setStackInSlot(0, item);
        updateFilter();
    }

    public void clear() {
        this.filterItem = ItemStack.EMPTY;
        if (this.filterSlot != null) {
            this.filterSlot.setStackInSlot(0, ItemStack.EMPTY);
        }
        if (this.filter != null) {
            var oldFilter = this.filter;
            this.filter = null;
            this.onFilterRemoved.accept(oldFilter);
        }
        this.loadFilterFromItem();
        this.notifyPersistence();
    }

    ///////////////////////////////////////
    // ***** FILTER HANDLING ******//
    ///////////////////////////////////////

    private CustomItemStackHandler getFilterSlot() {
        if (this.filterSlot == null) {
            this.filterSlot = new CustomItemStackHandler(this.filterItem) {

                @Override
                public int getSlotLimit(int slot) {
                    return 1;
                }
            };

            this.filterSlot.setFilter(this::canInsertFilterItem);
            this.filterSlot.setOnContentsChanged(this::updateFilter);
        }

        return this.filterSlot;
    }

    private void updateFilter() {
        var filterContainer = getFilterSlot();

        if (GTCEu.isClientThread()) {
            if (!filterContainer.getStackInSlot(0).isEmpty() && !this.filterItem.isEmpty()) {
                return;
            }
        }

        this.filterItem = filterContainer.getStackInSlot(0);

        if (this.filter != null) {
            var oldFilter = this.filter;
            this.filter = null;
            this.onFilterRemoved.accept(oldFilter);
        }

        loadFilterFromItem();
        this.notifyPersistence();
    }

    private void loadFilterFromItem() {
        if (!this.filterItem.isEmpty()) {
            this.filter = loadFilter(this.filterItem);
            filter.setOnUpdated(f -> {
                this.onFilterUpdated.accept(f);
                this.notifyPersistence();
            });
            if (filter instanceof SmartItemFilter smart &&
                    container instanceof CoverBehavior cover &&
                    cover.coverHolder instanceof MachineCoverContainer mcc) {
                var machine = MetaMachine.getMachine(mcc.getLevel(), mcc.getPos());
                if (machine != null) {
                    smart.setModeFromMachine(machine.getDefinition().getName());
                }
            }
            this.onFilterLoaded.accept(this.filter);
        }
        updateFilterGroupUI();
    }

    private void updateFilterGroupUI() {
        if (this.filterGroup == null)
            return;

        MenuSlots.clearChildren(this.filterGroup);

        if (!this.filterItem.isEmpty() && this.filter != null) {
            this.filterGroup.addChild(this.filter.openConfigurator());
        }
    }

    //////////////////////////////////////
    // ***** LDLib2 SyncData ******//
    //////////////////////////////////////

    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Override
    public void notifyPersistence() {
        this.container.notifyPersistence();
    }
}
