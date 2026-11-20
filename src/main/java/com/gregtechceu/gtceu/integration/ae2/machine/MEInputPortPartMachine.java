package com.gregtechceu.gtceu.integration.ae2.machine;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.recipe.RecipeTransaction;
import com.gregtechceu.gtceu.common.data.machines.GTMachineUtils;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.ConfigWidget;
import com.gregtechceu.gtceu.integration.ae2.machine.feature.multiblock.IMEInputPart;
import com.gregtechceu.gtceu.integration.ae2.machine.trait.PortInputHandlers;
import com.gregtechceu.gtceu.integration.ae2.slot.IConfigurableSlotList;
import com.gregtechceu.gtceu.integration.ae2.slot.PortAEInventory;

import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.Position;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;

import java.util.List;

public class MEInputPortPartMachine extends MEBusPartMachine implements IMEInputPart {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MEInputPortPartMachine.class, MEBusPartMachine.MANAGED_FIELD_HOLDER);

    public static final String TAG_PORT = "MEInputPort";
    public static final String TAG_BUS = "MEInputBus";
    public static final String TAG_HATCH = "MEInputHatch";

    @Persisted
    protected PortAEInventory portSlots;

    protected PortInputHandlers.PortStockAccess access;
    protected PortInputHandlers.Items itemHandler;
    protected PortInputHandlers.Fluids fluidHandler;

    public MEInputPortPartMachine(IMachineBlockEntity holder, int tier, Object... args) {
        super(holder, tier, IO.IN, args);

        this.fluidHandler = new PortInputHandlers.Fluids(this, this.access);
    }

    @Override
    protected NotifiableItemStackHandler createInventory(Object... args) {
        this.portSlots = new PortAEInventory(this::onPortSlotsChanged, getSlotCount());
        this.access = new Access();
        this.itemHandler = new PortInputHandlers.Items(this, this.access);

        return this.itemHandler;
    }

    protected int getBaseTier() {
        return GTMachineUtils.ME_INPUT_TIERS[0];
    }

    private int getSlotCount() {
        return PortAEInventory.slotsForTier(getTier(), getBaseTier());
    }

    private void onPortSlotsChanged() {
        if (this.itemHandler != null) this.itemHandler.onContentsChanged();
        if (this.fluidHandler != null) this.fluidHandler.notifyChanged();
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public PortAEInventory getPortInventory() {
        return this.portSlots;
    }

    @Override
    public IConfigurableSlotList getSlotList() {
        return this.portSlots;
    }

    protected boolean isStocking() {
        return false;
    }

    @Override
    public void onMachineRemoved() {
        flushInventory();
    }

    protected void flushInventory() {
        var grid = getMainNode().getGrid();

        if (grid == null) return;

        for (int i = 0; i < this.portSlots.size(); i++) {
            GenericStack stock = this.portSlots.stock.getStack(i);

            if (stock != null) {
                grid.getStorageService().getInventory().insert(stock.what(), stock.amount(), Actionable.MODULATE,
                        this.actionSource);
            }
        }
    }

    @Override
    protected void autoIO() {
        if (!this.isWorkingEnabled()) return;
        if (!this.shouldSyncME()) return;

        if (this.updateMEStatus()) {
            this.syncME();
            this.updateInventorySubscription();
        }
    }

    protected void syncME() {
        MEStorage network = this.getMainNode().getGrid().getStorageService().getInventory();

        for (int i = 0; i < this.portSlots.size(); i++) {
            GenericStack config = this.portSlots.config.getStack(i);
            GenericStack stored = this.portSlots.stock.getStack(i);

            if (stored != null) {
                long excess = 0;

                if (config == null || !config.what().equals(stored.what())) {
                    excess = stored.amount();
                } else if (config.amount() < stored.amount()) {
                    excess = stored.amount() - config.amount();
                }

                if (excess > 0) {
                    long inserted = network.insert(stored.what(), excess, Actionable.MODULATE, this.actionSource);

                    if (inserted > 0) {
                        this.portSlots.stock.extract(i, stored.what(), inserted, Actionable.MODULATE);

                        continue;
                    }
                }
            }

            if (config == null) continue;

            GenericStack current = this.portSlots.stock.getStack(i);

            long request;

            if (current == null) {
                request = config.amount();
            } else if (config.what().equals(current.what()) && current.amount() < config.amount()) {
                request = config.amount() - current.amount();
            } else {
                continue;
            }

            long extracted = network.extract(config.what(), request, Actionable.MODULATE, this.actionSource);

            if (extracted > 0) {
                this.portSlots.stock.insert(i, config.what(), extracted, Actionable.MODULATE);
            }
        }
    }

    protected long extractStock(int slot, AEKey key, long amount, boolean simulate) {
        long extracted = this.portSlots.stock.extract(slot, key, amount,
                simulate ? Actionable.SIMULATE : Actionable.MODULATE);

        if (!simulate && extracted > 0 && RecipeTransaction.isActive()) {
            RecipeTransaction.record(() -> this.portSlots.stock.insert(slot, key, extracted, Actionable.MODULATE));
        }

        return extracted;
    }

    private final class Access implements PortInputHandlers.PortStockAccess {

        @Override
        public int slots() {
            return MEInputPortPartMachine.this.portSlots.size();
        }

        @Override
        public GenericStack stockAt(int slot) {
            return MEInputPortPartMachine.this.portSlots.stock.getStack(slot);
        }

        @Override
        public long extract(int slot, AEKey key, long amount, boolean simulate) {
            return extractStock(slot, key, amount, simulate);
        }
    }

    @Override
    public Widget createUIWidget() {
        WidgetGroup group = new WidgetGroup(new Position(0, 0));

        group.addWidget(new LabelWidget(3, 0,
                () -> this.isOnline ? "gtceu.gui.me_network.online" : "gtceu.gui.me_network.offline"));
        group.addWidget(new ConfigWidget(3, 10, this.portSlots.getViews(), isStocking(), this::isAutoPullEnabled,
                this::hasStackInOtherPartOrHere, null));

        return group;
    }

    protected boolean isAutoPullEnabled() {
        return false;
    }

    protected boolean hasStackInOtherPartOrHere(GenericStack stack) {
        return false;
    }

    @Override
    public List<DataStickFormat> getDataStickFormats() {
        return List.of(new DataStickFormat(TAG_PORT, null), new DataStickFormat(TAG_BUS, AEItemKey.class),
                new DataStickFormat(TAG_HATCH, AEFluidKey.class));
    }

    @Override
    public String getDataStickNameKey() {
        return "gtceu.machine.me.item_import.data_stick.name";
    }
}
