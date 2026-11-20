package com.gregtechceu.gtceu.integration.ae2.machine;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IHasCircuitSlot;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.common.data.machines.GTMachineUtils;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.ConfigWidget;
import com.gregtechceu.gtceu.integration.ae2.machine.feature.multiblock.IMEInputPart;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemList;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemSlot;
import com.gregtechceu.gtceu.integration.ae2.slot.IConfigurableSlotList;
import com.gregtechceu.gtceu.integration.ae2.slot.PortAEInventory;
import com.gregtechceu.gtceu.utils.GTMath;

import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.Position;

import net.minecraft.MethodsReturnNonnullByDefault;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MEInputBusPartMachine extends MEBusPartMachine implements IMEInputPart, IMachineLife, IHasCircuitSlot {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(MEInputBusPartMachine.class,
            MEBusPartMachine.MANAGED_FIELD_HOLDER);

    protected ExportOnlyAEItemList aeItemHandler;

    public MEInputBusPartMachine(IMachineBlockEntity holder, int tier, Object... args) {
        super(holder, tier, IO.IN, args);
    }

    protected int getBaseTier() {
        return GTMachineUtils.ME_INPUT_TIERS[0];
    }

    protected int getSlotCount() {
        return PortAEInventory.slotsForTier(getTier(), getBaseTier());
    }

    @Override
    public void onMachineRemoved() {
        flushInventory();
    }

    @Override
    protected NotifiableItemStackHandler createInventory(Object... args) {
        this.aeItemHandler = new ExportOnlyAEItemList(this, getSlotCount());

        return this.aeItemHandler;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public IConfigurableSlotList getSlotList() {
        return this.aeItemHandler;
    }

    @Override
    public void autoIO() {
        if (!this.isWorkingEnabled()) return;
        if (!this.shouldSyncME()) return;

        if (this.updateMEStatus()) {
            this.syncME();
            this.updateInventorySubscription();
        }
    }

    protected void syncME() {
        MEStorage networkInv = this.getMainNode().getGrid().getStorageService().getInventory();
        for (ExportOnlyAEItemSlot aeSlot : this.aeItemHandler.getInventory()) {
            GenericStack exceedItem = aeSlot.exceedStack();
            if (exceedItem != null) {
                long total = exceedItem.amount();
                long inserted = networkInv.insert(exceedItem.what(), exceedItem.amount(), Actionable.MODULATE,
                        this.actionSource);
                if (inserted > 0) {
                    aeSlot.extractItem(0, GTMath.saturatedCast(inserted), false);
                    continue;
                } else {
                    aeSlot.extractItem(0, GTMath.saturatedCast(total), false);
                }
            }
            GenericStack reqItem = aeSlot.requestStack();
            if (reqItem != null) {
                long extracted = networkInv.extract(reqItem.what(), reqItem.amount(), Actionable.MODULATE,
                        this.actionSource);
                if (extracted != 0) {
                    aeSlot.addStack(new GenericStack(reqItem.what(), extracted));
                }
            }
        }
    }

    protected void flushInventory() {
        var grid = getMainNode().getGrid();
        if (grid != null) {
            for (var aeSlot : this.aeItemHandler.getInventory()) {
                GenericStack stock = aeSlot.getStock();
                if (stock != null) {
                    grid.getStorageService().getInventory().insert(stock.what(), stock.amount(), Actionable.MODULATE,
                            this.actionSource);
                }
            }
        }
    }

    @Override
    public Widget createUIWidget() {
        WidgetGroup group = new WidgetGroup(new Position(0, 0));

        group.addWidget(new LabelWidget(3, 0, () -> this.isOnline ?
                "gtceu.gui.me_network.online" :
                "gtceu.gui.me_network.offline"));
        group.addWidget(new ConfigWidget(3, 10, this.aeItemHandler.getInventory(), this.aeItemHandler.isStocking(),
                this.aeItemHandler::isAutoPull, stack -> this.aeItemHandler.hasStackInConfig(stack, true),
                AEItemKey.class));

        return group;
    }

    @Override
    public List<DataStickFormat> getDataStickFormats() {
        return List.of(new DataStickFormat("MEInputBus", null));
    }

    @Override
    public String getDataStickNameKey() {
        return "gtceu.machine.me.item_import.data_stick.name";
    }
}
