package com.gregtechceu.gtceu.integration.ae2.machine;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IHasCircuitSlot;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.common.data.machines.GTMachineUtils;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.ConfigWidget;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.MEStatusLabel;
import com.gregtechceu.gtceu.integration.ae2.machine.feature.multiblock.IMEInputPart;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEFluidList;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEFluidSlot;
import com.gregtechceu.gtceu.integration.ae2.slot.IConfigurableSlotList;
import com.gregtechceu.gtceu.integration.ae2.slot.PortAEInventory;
import com.gregtechceu.gtceu.utils.GTMath;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraftforge.fluids.capability.IFluidHandler;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MEInputHatchPartMachine extends MEHatchPartMachine
                                     implements IMEInputPart, IMachineLife, IHasCircuitSlot {

    protected ExportOnlyAEFluidList aeFluidHandler;

    public MEInputHatchPartMachine(IMachineBlockEntity holder, int tier, Object... args) {
        super(holder, tier, CONFIG_SIZE, IO.IN, args);
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
    protected NotifiableFluidTank createTank(int initialCapacity, int slots, Object... args) {
        this.aeFluidHandler = new ExportOnlyAEFluidList(this, getSlotCount());

        return this.aeFluidHandler;
    }

    @Override
    public IConfigurableSlotList getSlotList() {
        return this.aeFluidHandler;
    }

    @Override
    protected void autoIO() {
        if (!this.isWorkingEnabled()) return;
        if (!this.shouldSyncME()) return;

        if (this.updateMEStatus()) {
            this.syncME();
            this.updateTankSubscription();
        }
    }

    protected void syncME() {
        MEStorage networkInv = this.getMainNode().getGrid().getStorageService().getInventory();
        for (ExportOnlyAEFluidSlot aeTank : this.aeFluidHandler.getInventory()) {
            GenericStack exceedFluid = aeTank.exceedStack();
            if (exceedFluid != null) {
                int total = GTMath.saturatedCast(exceedFluid.amount());
                int inserted = GTMath.saturatedCast(networkInv.insert(exceedFluid.what(), exceedFluid.amount(),
                        Actionable.MODULATE, this.actionSource));

                if (inserted > 0) {
                    aeTank.drain(inserted, IFluidHandler.FluidAction.EXECUTE);
                    continue;
                } else {
                    aeTank.drain(total, IFluidHandler.FluidAction.EXECUTE);
                }
            }
            GenericStack reqFluid = aeTank.requestStack();
            if (reqFluid != null) {
                long extracted = networkInv.extract(reqFluid.what(), reqFluid.amount(), Actionable.MODULATE,
                        this.actionSource);
                if (extracted > 0) {
                    aeTank.addStack(new GenericStack(reqFluid.what(), extracted));
                }
            }
        }
    }

    protected void flushInventory() {
        var grid = getMainNode().getGrid();
        if (grid != null) {
            for (var aeSlot : this.aeFluidHandler.getInventory()) {
                GenericStack stock = aeSlot.getStock();
                if (stock != null) {
                    grid.getStorageService().getInventory().insert(stock.what(), stock.amount(), Actionable.MODULATE,
                            this.actionSource);
                }
            }
        }
    }

    @Override
    public UIElement createUIWidget() {
        var group = new UIElement();
        group.layout(layout -> layout.paddingHorizontal(3).gapAll(1));
        group.addChildren(new MEStatusLabel(() -> this.isOnline),
                new ConfigWidget(this.aeFluidHandler.getInventory(), this.aeFluidHandler.isStocking(),
                        this.aeFluidHandler::isAutoPull, stack -> this.aeFluidHandler.hasStackInConfig(stack, true),
                        AEFluidKey.class));
        return group;
    }

    @Override
    public List<DataStickFormat> getDataStickFormats() {
        return List.of(new DataStickFormat("MEInputHatch", null));
    }

    @Override
    public String getDataStickNameKey() {
        return "gtceu.machine.me.fluid_import.data_stick.name";
    }
}
