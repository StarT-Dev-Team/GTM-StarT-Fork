package com.gregtechceu.gtceu.integration.ae2.machine;

import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.AutoStockingFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.recipe.RecipeTransaction;
import com.gregtechceu.gtceu.common.data.machines.GTMachineUtils;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.ae2.machine.feature.multiblock.IMEStockingPart;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEFluidList;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEFluidSlot;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAESlot;
import com.gregtechceu.gtceu.integration.ae2.utils.AEUtil;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.DropSaved;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
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
public class MEStockingHatchPartMachine extends MEInputHatchPartMachine implements IMEStockingPart {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MEStockingHatchPartMachine.class, MEInputHatchPartMachine.MANAGED_FIELD_HOLDER);

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

    public MEStockingHatchPartMachine(IMachineBlockEntity holder, int tier, Object... args) {
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
    protected NotifiableFluidTank createTank(int initialCapacity, int slots, Object... args) {
        this.aeFluidHandler = new ExportOnlyAEStockingFluidList(this, getSlotCount());

        return this.aeFluidHandler;
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

        if (this.autoPull) {
            refreshList();
        }

        syncME();
        updateTankSubscription();
    }

    @Override
    protected void syncME() {
        MEStorage networkInv = this.getMainNode().getGrid().getStorageService().getInventory();

        for (ExportOnlyAEFluidSlot slot : this.aeFluidHandler.getInventory()) {
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
    public long getMinSizeFor(AEKey key) {
        return key instanceof AEFluidKey ? this.minStackSize : Long.MAX_VALUE;
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

        attachHatchConfigurators(configuratorPanel);

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

    public class ExportOnlyAEStockingFluidList extends ExportOnlyAEFluidList {

        public ExportOnlyAEStockingFluidList(MetaMachine holder, int slots) {
            super(holder, slots, ExportOnlyAEStockingFluidSlot::new);
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

    public class ExportOnlyAEStockingFluidSlot extends ExportOnlyAEFluidSlot {

        public ExportOnlyAEStockingFluidSlot() {
            super();
        }

        public ExportOnlyAEStockingFluidSlot(@Nullable GenericStack config, @Nullable GenericStack stock) {
            super(config, stock);
        }

        @Override
        public ExportOnlyAEFluidSlot copy() {
            return new ExportOnlyAEStockingFluidSlot(
                    this.config == null ? null : copy(this.config),
                    this.stock == null ? null : copy(this.stock));
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (this.stock != null && this.config != null) {
                // Extract the items from the real net to either validate (simulate)
                // or extract (modulate) when this is called
                if (!isOnline()) return FluidStack.EMPTY;

                MEStorage aeNetwork = getMainNode().getGrid().getStorageService().getInventory();
                Actionable actionable = action.simulate() ? Actionable.SIMULATE : Actionable.MODULATE;
                var key = config.what();
                long extracted = aeNetwork.extract(key, maxDrain, actionable, actionSource);

                if (extracted > 0) {
                    FluidStack resultStack = key instanceof AEFluidKey fluidKey ?
                            AEUtil.toFluidStack(fluidKey, extracted) : FluidStack.EMPTY;

                    if (action.execute()) {
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

            return FluidStack.EMPTY;
        }
    }
}
