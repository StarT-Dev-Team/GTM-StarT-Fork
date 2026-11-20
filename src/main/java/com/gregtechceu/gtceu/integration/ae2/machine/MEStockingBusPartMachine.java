package com.gregtechceu.gtceu.integration.ae2.machine;

import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.AutoStockingFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.recipe.RecipeTransaction;
import com.gregtechceu.gtceu.common.data.machines.GTMachineUtils;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.ae2.machine.feature.multiblock.IMEStockingPart;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemList;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemSlot;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAESlot;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.DropSaved;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MEStockingBusPartMachine extends MEInputBusPartMachine implements IMEStockingPart {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MEStockingBusPartMachine.class, MEInputBusPartMachine.MANAGED_FIELD_HOLDER);

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
    private Predicate<GenericStack> autoPullTest;

    public MEStockingBusPartMachine(IMachineBlockEntity holder, int tier, Object... args) {
        super(holder, tier, args);

        this.autoPullTest = $ -> false;
    }

    @Override
    protected int getBaseTier() {
        return GTMachineUtils.ME_STOCKING_TIERS[0];
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
    protected NotifiableItemStackHandler createInventory(Object... args) {
        this.aeItemHandler = new ExportOnlyAEStockingItemList(this, getSlotCount());

        return this.aeItemHandler;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    /////////////////////////////////
    // ********** Sync ME *********//
    /////////////////////////////////

    @Override
    public void autoIO() {
        if (!this.isWorkingEnabled()) return;

        if (this.ticksPerCycle < ConfigHolder.INSTANCE.compat.ae2.minUpdateIntervals) {
            this.ticksPerCycle = ConfigHolder.INSTANCE.compat.ae2.updateIntervals;
        }

        if (getOffsetTimer() % this.ticksPerCycle != 0) return;
        if (!updateMEStatus()) return;

        if (this.autoPull) refreshList();

        syncME();
        updateInventorySubscription();
    }

    @Override
    protected void syncME() {
        // Update the visual display for the fake items. This also is important for the item handler's
        // getStackInSlot() method, as it uses the cached items set here.
        MEStorage networkInv = this.getMainNode().getGrid().getStorageService().getInventory();

        for (ExportOnlyAEItemSlot slot : this.aeItemHandler.getInventory()) {
            var config = slot.getConfig();

            if (config != null) {
                // Try to fill the slot
                var key = config.what();
                long extracted = networkInv.extract(key, Long.MAX_VALUE, Actionable.SIMULATE, this.actionSource);

                if (extracted >= this.minStackSize) {
                    slot.setStock(new GenericStack(key, extracted));

                    continue;
                }
            }

            slot.setStock(null);
        }
    }

    @Override
    public void attachSideTabs(TabsWidget sideTabs) {
        sideTabs.setMainTab(this); // removes the cover configurator, it's pointless and clashes with layout.
    }

    @Override
    protected void flushInventory() {
        // no-op, nothing to send back to the network
    }

    @Override
    public void setDistinct(boolean isDistinct) {
        super.setDistinct(isDistinct);
        if (!isRemote() && !isDistinct) {
            // Ensure that our configured items won't match any other buses in the multiblock.
            // Needed since we allow duplicates in distinct mode on, but not off
            validateConfig();
        }
    }

    @Override
    public long getMinSizeFor(AEKey key) {
        return key instanceof AEItemKey ? this.minStackSize : Long.MAX_VALUE;
    }

    @Override
    public void setAutoPull(boolean autoPull) {
        this.autoPull = autoPull;

        onAutoPullChanged();
    }

    ///////////////////////////////
    // ********** GUI ***********//
    ///////////////////////////////

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        IMEStockingPart.super.attachConfigurators(configuratorPanel);
        attachBusConfigurators(configuratorPanel);
        configuratorPanel.attachConfigurators(new AutoStockingFancyConfigurator(this));
    }

    ////////////////////////////////
    // ******* Interaction *******//
    ////////////////////////////////

    @Override
    protected InteractionResult onScrewdriverClick(Player playerIn, InteractionHand hand, Direction gridSide,
                                                   BlockHitResult hitResult) {
        var superResult = super.onScrewdriverClick(playerIn, hand, gridSide, hitResult);

        if (superResult != InteractionResult.PASS) return superResult;

        return toggleAutoPull(playerIn);
    }

    public class ExportOnlyAEStockingItemList extends ExportOnlyAEItemList {

        public ExportOnlyAEStockingItemList(MetaMachine holder, int slots) {
            super(holder, slots, ExportOnlyAEStockingItemSlot::new);
        }

        @Override
        public boolean isAutoPull() {
            return autoPull;
        }

        @Override
        public boolean isStocking() {
            return true;
        }

        @Override
        public boolean hasStackInConfig(GenericStack stack, boolean checkExternal) {
            if (super.hasStackInConfig(stack, false)) return true;

            return checkExternal && testConfiguredInOtherPart(stack);
        }
    }

    public class ExportOnlyAEStockingItemSlot extends ExportOnlyAEItemSlot {

        public ExportOnlyAEStockingItemSlot() {
            super();
        }

        public ExportOnlyAEStockingItemSlot(@Nullable GenericStack config, @Nullable GenericStack stock) {
            super(config, stock);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == 0 && this.stock != null) {
                if (this.config != null) {
                    // Extract the items from the real net to either validate (simulate)
                    // or extract (modulate) when this is called
                    if (!isOnline()) return ItemStack.EMPTY;

                    MEStorage aeNetwork = getMainNode().getGrid().getStorageService().getInventory();
                    Actionable action = simulate ? Actionable.SIMULATE : Actionable.MODULATE;
                    var key = config.what();
                    long extracted = aeNetwork.extract(key, amount, action, actionSource);

                    if (extracted > 0) {
                        ItemStack resultStack = key instanceof AEItemKey itemKey ?
                                itemKey.toStack((int) extracted) : ItemStack.EMPTY;

                        if (!simulate) {
                            var previousStock = this.stock;

                            RecipeTransaction.record(() -> {
                                aeNetwork.insert(key, extracted, Actionable.MODULATE, actionSource);

                                this.stock = previousStock;

                                if (this.onContentsChanged != null) {
                                    this.onContentsChanged.run();
                                }
                            });

                            // may as well update the display here
                            this.stock = ExportOnlyAESlot.copy(stock, stock.amount() - extracted);

                            if (this.stock.amount() == 0) {
                                this.stock = null;
                            }

                            if (this.onContentsChanged != null) {
                                this.onContentsChanged.run();
                            }
                        }

                        return resultStack;
                    }
                }
            }

            return ItemStack.EMPTY;
        }

        @Override
        public ExportOnlyAEStockingItemSlot copy() {
            return new ExportOnlyAEStockingItemSlot(
                    this.config == null ? null : copy(this.config),
                    this.stock == null ? null : copy(this.stock));
        }
    }
}
