package com.gregtechceu.gtceu.integration.ae2.machine;

import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.PortAutoStockingFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.recipe.RecipeTransaction;
import com.gregtechceu.gtceu.common.data.machines.GTMachineUtils;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.ae2.machine.feature.multiblock.IMEStockingPart;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.DropSaved;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Predicate;

public class MEStockingInputPortPartMachine extends MEInputPortPartMachine implements IMEStockingPart {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MEStockingInputPortPartMachine.class, MEInputPortPartMachine.MANAGED_FIELD_HOLDER);

    @DescSynced
    @Persisted
    @Getter
    private boolean autoPull;

    @Getter
    @Setter
    @Persisted
    @DropSaved
    private int minStackSize = 1;

    @Getter
    @Setter
    @Persisted
    @DropSaved
    private int minFluidStackSize = 1;

    @Getter
    @Setter
    private Predicate<GenericStack> autoPullTest;

    public MEStockingInputPortPartMachine(IMachineBlockEntity holder, int tier, Object... args) {
        super(holder, tier, args);

        this.autoPullTest = $ -> false;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected int getBaseTier() {
        return GTMachineUtils.ME_STOCKING_TIERS[0];
    }

    @Override
    protected boolean isStocking() {
        return true;
    }

    @Override
    public long getMinSizeFor(AEKey key) {
        if (key instanceof AEItemKey) return this.minStackSize;
        if (key instanceof AEFluidKey) return this.minFluidStackSize;

        return Long.MAX_VALUE;
    }

    @Override
    public void addedToController(IMultiController controller) {
        super.addedToController(controller);

        IMEStockingPart.super.addedToController(controller);
    }

    @Override
    public void removedFromController(IMultiController controller) {
        IMEStockingPart.super.removedFromController(controller);

        super.removedFromController(controller);
    }

    @Override
    protected void flushInventory() {}

    @Override
    protected void autoIO() {
        if (!this.isWorkingEnabled()) return;

        if (this.ticksPerCycle < ConfigHolder.INSTANCE.compat.ae2.minUpdateIntervals) {
            this.ticksPerCycle = ConfigHolder.INSTANCE.compat.ae2.updateIntervals;
        }

        if (getOffsetTimer() % this.ticksPerCycle != 0) return;
        if (!updateMEStatus()) return;

        if (this.autoPull) {
            refreshList();
        }

        syncME();
        updateInventorySubscription();
    }

    @Override
    protected void syncME() {
        var network = this.getMainNode().getGrid().getStorageService().getInventory();

        for (int i = 0; i < this.portSlots.size(); i++) {
            GenericStack config = this.portSlots.config.getStack(i);

            if (config != null) {
                var key = config.what();
                long extracted = network.extract(key, Long.MAX_VALUE, Actionable.SIMULATE, this.actionSource);

                if (extracted >= getMinSizeFor(key)) {
                    this.portSlots.stock.setStack(i, new GenericStack(key, extracted));

                    continue;
                }
            }

            this.portSlots.stock.setStack(i, null);
        }
    }

    @Override
    protected long extractStock(int slot, AEKey key, long amount, boolean simulate) {
        GenericStack config = this.portSlots.config.getStack(slot);

        if (config == null || !config.what().equals(key) || !isOnline()) return 0;

        var grid = getMainNode().getGrid();

        if (grid == null) return 0;

        var network = grid.getStorageService().getInventory();
        long extracted = network.extract(key, amount, simulate ? Actionable.SIMULATE : Actionable.MODULATE,
                this.actionSource);

        if (!simulate && extracted > 0) {
            GenericStack previous = this.portSlots.stock.getStack(slot);

            if (previous != null) {
                long left = previous.amount() - extracted;

                this.portSlots.stock.setStack(slot, left > 0 ? new GenericStack(previous.what(), left) : null);
            }

            RecipeTransaction.record(() -> {
                network.insert(key, extracted, Actionable.MODULATE, this.actionSource);
                this.portSlots.stock.setStack(slot, previous);
            });
        }

        return extracted;
    }

    @Override
    protected boolean isAutoPullEnabled() {
        return this.autoPull;
    }

    @Override
    protected boolean hasStackInOtherPartOrHere(GenericStack stack) {
        return this.portSlots.hasStackInConfig(stack, false) || testConfiguredInOtherPart(stack);
    }

    @Override
    public void setAutoPull(boolean autoPull) {
        this.autoPull = autoPull;

        onAutoPullChanged();
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        IMEStockingPart.super.attachConfigurators(configuratorPanel);

        attachBusConfigurators(configuratorPanel);

        configuratorPanel.attachConfigurators(new PortAutoStockingFancyConfigurator(this));
    }

    @Override
    protected InteractionResult onScrewdriverClick(Player playerIn, InteractionHand hand, Direction gridSide,
                                                   BlockHitResult hitResult) {
        var superResult = super.onScrewdriverClick(playerIn, hand, gridSide, hitResult);

        if (superResult != InteractionResult.PASS) return superResult;

        return toggleAutoPull(playerIn);
    }
}
