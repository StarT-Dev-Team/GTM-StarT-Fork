package com.gregtechceu.gtceu.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.blockentity.IPaintable;
import com.gregtechceu.gtceu.api.capability.ILockableHatch;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.widget.BlockableFluidSlot;
import com.gregtechceu.gtceu.api.gui.widget.PhantomFluidSlot;
import com.gregtechceu.gtceu.api.gui.widget.ToggleButton;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CircuitFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.IHasCircuitSlot;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.utils.GTTransferUtils;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;

import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FluidHatchPartMachine extends TieredIOPartMachine
                                   implements IMachineLife, IHasCircuitSlot, IPaintable, ILockableHatch {

    public static final int INITIAL_TANK_CAPACITY_1X = 8 * FluidType.BUCKET_VOLUME;
    public static final int INITIAL_TANK_CAPACITY_4X = 2 * FluidType.BUCKET_VOLUME;
    public static final int INITIAL_TANK_CAPACITY_9X = FluidType.BUCKET_VOLUME;

    @Persisted
    public final NotifiableFluidTank tank;
    private final int slots;
    @Nullable
    protected TickableSubscription autoIOSubs;
    @Nullable
    protected ISubscription tankSubs;
    @Getter
    @Setter
    @Persisted
    @DescSynced
    protected boolean circuitSlotEnabled;
    @Getter
    @Setter
    @Persisted
    @DescSynced
    protected boolean circuitPastingBlocked;
    @Getter
    @Persisted
    protected final NotifiableItemStackHandler circuitInventory;
    @Getter
    @Persisted
    @DescSynced
    protected boolean locked;

    // The `Object... args` parameter is necessary in case a superclass needs to pass any args along to createTank().
    // We can't use fields here because those won't be available while createTank() is called.
    public FluidHatchPartMachine(IMachineBlockEntity holder, int tier, IO io, int initialCapacity, int slots,
                                 Object... args) {
        super(holder, tier, io);
        this.slots = slots;
        this.tank = createTank(initialCapacity, slots, args);
        this.tank.setIsIODisabled(this::isLocked);
        this.circuitSlotEnabled = true;
        this.circuitInventory = createCircuitItemHandler(io).shouldSearchContent(false);
    }

    //////////////////////////////////////
    // ***** Initialization ******//
    //////////////////////////////////////
    protected NotifiableFluidTank createTank(int initialCapacity, int slots, Object... args) {
        return new NotifiableFluidTank(this, slots, getTankCapacity(initialCapacity, getTier()), io);
    }

    public static int getTankCapacity(int initialCapacity, int tier) {
        return initialCapacity * (1 << Math.min(9, tier));
    }

    protected NotifiableItemStackHandler createCircuitItemHandler(Object... args) {
        if (args.length > 0 && args[0] instanceof IO io && io == IO.IN) {
            return new NotifiableItemStackHandler(this, 1, IO.IN, IO.NONE)
                    .setFilter(IntCircuitBehaviour::isIntegratedCircuit);
        } else {
            return new NotifiableItemStackHandler(this, 0, IO.NONE);
        }
    }

    @Override
    public void onMachineRemoved() {
        if (!ConfigHolder.INSTANCE.machines.ghostCircuit) {
            clearInventory(circuitInventory.storage);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(0, this::updateTankSubscription));
        }
        getHandlerList().setColor(getPaintingColor());
        tankSubs = tank.addChangedListener(this::updateTankSubscription);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (tankSubs != null) {
            tankSubs.unsubscribe();
            tankSubs = null;
        }
    }

    @Override
    public void onPaintingColorChanged(int color) {
        getHandlerList().setColor(color, true);
    }

    @Override
    public void addedToController(IMultiController controller) {
        if (!controller.allowCircuitSlots()) {
            if (!ConfigHolder.INSTANCE.machines.ghostCircuit) {
                clearInventory(circuitInventory.storage);
            } else {
                circuitInventory.setStackInSlot(0, ItemStack.EMPTY);
            }
            setCircuitSlotEnabled(false);
        }
        super.addedToController(controller);
    }

    @Override
    public void removedFromController(IMultiController controller) {
        super.removedFromController(controller);
        for (var c : controllers) {
            if (!c.allowCircuitSlots()) {
                return;
            }
        }
        setCircuitSlotEnabled(true);
    }

    @Override
    public int tintColor(int index) {
        if (index == 9) return getRealColor();
        return -1;
    }

    //////////////////////////////////////
    // ******** Auto IO *********//
    //////////////////////////////////////

    @Override
    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChanged(block, fromPos, isMoving);
        updateTankSubscription();
    }

    @Override
    public void onRotated(Direction oldFacing, Direction newFacing) {
        super.onRotated(oldFacing, newFacing);
        updateTankSubscription(newFacing);
    }

    protected boolean canAutoIO() {
        return isWorkingEnabled() && !isLocked();
    }

    protected void updateTankSubscription() {
        updateTankSubscription(getFrontFacing());
    }

    protected void updateTankSubscription(Direction newFacing) {
        if (canAutoIO() && ((io.support(IO.OUT) && !tank.isEmpty()) || io.support(IO.IN)) &&
                GTTransferUtils.hasAdjacentFluidHandler(getLevel(), getPos(), newFacing)) {
            autoIOSubs = subscribeServerTick(autoIOSubs, this::autoIO);
        } else if (autoIOSubs != null) {
            autoIOSubs.unsubscribe();
            autoIOSubs = null;
        }
    }

    protected void autoIO() {
        if (getOffsetTimer() % 5 == 0) {
            if (canAutoIO()) {
                if (io == IO.OUT) {
                    tank.exportToNearby(getFrontFacing());
                } else if (io == IO.IN) {
                    tank.importFromNearby(getFrontFacing());
                } else if (io == IO.BOTH) {
                    tank.importFromNearby(getFrontFacing());
                    tank.exportToNearby(getFrontFacing().getOpposite());
                }
            }
            updateTankSubscription();
        }
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        updateTankSubscription();
    }

    @Override
    public void setWorkingEnabled(boolean workingEnabled) {
        super.setWorkingEnabled(workingEnabled);
        updateTankSubscription();
    }

    @Override
    protected InteractionResult onScrewdriverClick(Player playerIn, InteractionHand hand, Direction gridSide,
                                                   BlockHitResult hitResult) {
        InteractionResult superResult = super.onScrewdriverClick(playerIn, hand, gridSide, hitResult);
        if (superResult != InteractionResult.PASS) return superResult;
        if (io == IO.BOTH) return InteractionResult.PASS;
        if (playerIn.isShiftKeyDown()) {
            if (swapIO()) {
                return InteractionResult.sidedSuccess(playerIn.level().isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    public boolean swapIO() {
        BlockPos blockPos = getHolder().pos();
        MachineDefinition newDefinition = null;

        if (io == IO.IN) {
            if (this.slots == 1) newDefinition = GTMachines.FLUID_EXPORT_HATCH[this.getTier()];
            else if (this.slots == 4) newDefinition = GTMachines.FLUID_EXPORT_HATCH_4X[this.getTier()];
            else if (this.slots == 9) newDefinition = GTMachines.FLUID_EXPORT_HATCH_9X[this.getTier()];
        } else if (io == IO.OUT) {
            if (this.slots == 1) newDefinition = GTMachines.FLUID_IMPORT_HATCH[this.getTier()];
            else if (this.slots == 4) newDefinition = GTMachines.FLUID_IMPORT_HATCH_4X[this.getTier()];
            else if (this.slots == 9) newDefinition = GTMachines.FLUID_IMPORT_HATCH_9X[this.getTier()];
        }
        if (newDefinition == null) return false;

        BlockState newBlockState = newDefinition.getBlock().defaultBlockState();

        getLevel().setBlockAndUpdate(blockPos, newBlockState);

        if (getLevel().getBlockEntity(blockPos) instanceof IMachineBlockEntity newHolder) {
            if (newHolder.getMetaMachine() instanceof FluidHatchPartMachine newMachine) {
                newMachine.setFrontFacing(this.getFrontFacing());
                newMachine.setUpwardsFacing(this.getUpwardsFacing());
                newMachine.setPaintingColor(this.getPaintingColor());
                for (int i = 0; i < this.tank.getTanks(); i++) {
                    newMachine.tank.setFluidInTank(i, this.tank.getFluidInTank(i));
                }
            }
        }
        return true;
    }

    //////////////////////////////////////
    // ********** GUI ***********//
    //////////////////////////////////////

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        super.attachConfigurators(configuratorPanel);
        if (isCircuitSlotEnabled() && this.io == IO.IN) {
            configuratorPanel.attachConfigurators(new CircuitFancyConfigurator(this));
        }
    }

    @Override
    public UIElement createUIWidget() {
        if (slots == 1) {
            return createSingleSlotGUI();
        } else {
            return createMultiSlotGUI();
        }
    }

    protected UIElement createSingleSlotGUI() {
        var group = new UIElement();
        group.layout(layout -> layout.width(89).height(63));
        group.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND_INVERSE));

        var display = new UIElement();
        display.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(4).top(4).width(81).height(55));
        display.style(style -> style.backgroundTexture(GuiTextures.DISPLAY));
        group.addChild(display);

        if (this.io == IO.OUT) {
            var locked = new PhantomFluidSlot(() -> this.tank.getLockedFluid().getFluid(), f -> {
                if (!this.tank.getFluidInTank(0).isEmpty()) {
                    return;
                }
                if (f == null || f.isEmpty()) {
                    this.tank.setLocked(false);
                } else {
                    var newFluid = f.copy();
                    newFluid.setAmount(1);
                    this.tank.setLocked(true, newFluid);
                }
            });
            locked.amountLabel.setDisplay(false);
            locked.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(67).top(40));
            locked.style(style -> style.backgroundTexture(GuiTextures.FLUID_SLOT));
            group.addChild(locked);

            var lockButton = new ToggleButton(GuiTextures.BUTTON_LOCK, this.tank::isLocked, this.tank::setLocked)
                    .setTooltipText("gtceu.gui.fluid_lock.tooltip")
                    .setShouldUseBaseBackground();
            lockButton.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(7).top(40));
            group.addChild(lockButton);
        }

        var tankSlot = new BlockableFluidSlot();
        tankSlot.setIsBlocked(this::isLocked);
        tankSlot.bind(tank.getStorages()[0], 0);
        tankSlot.setAllowClickFilled(io.support(IO.IN));
        tankSlot.setAllowClickDrained(true);
        tankSlot.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(67).top(22));
        tankSlot.style(style -> style.backgroundTexture(GuiTextures.FLUID_SLOT));
        group.addChild(tankSlot);

        var amountTitle = new Label();
        amountTitle.setText(Component.translatable("gtceu.gui.fluid_amount"));
        amountTitle.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(8).top(8));

        var amount = new Label();
        amount.bind(DataBindingBuilder.componentS2C(() -> Component.literal(getFluidAmountText())).build());
        amount.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(8).top(18));

        var name = new Label();
        name.bind(DataBindingBuilder.componentS2C(this::getFluidNameText).build());
        name.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(8).top(28).width(56));

        group.addChildren(amountTitle, amount, name);
        return group;
    }

    private Component getFluidNameText() {
        Component translation;
        if (!tank.getFluidInTank(0).isEmpty()) {
            translation = tank.getFluidInTank(0).getDisplayName();
        } else {
            translation = this.tank.getLockedFluid().getFluid().getDisplayName();
        }
        return translation;
    }

    private String getFluidAmountText() {
        String fluidAmount = "";
        if (!tank.getFluidInTank(0).isEmpty()) {
            fluidAmount = getFormattedFluidAmount(tank.getFluidInTank(0));
        } else {
            if (!this.tank.getLockedFluid().getFluid().isEmpty()) {
                fluidAmount = "0";
            }
        }
        return fluidAmount;
    }

    public String getFormattedFluidAmount(FluidStack fluidStack) {
        return String.format("%,d", fluidStack.isEmpty() ? 0 : fluidStack.getAmount());
    }

    protected UIElement createMultiSlotGUI() {
        int rowSize = (int) Math.sqrt(slots);
        int colSize = rowSize;
        if (slots == 8) {
            rowSize = 4;
            colSize = 2;
        }

        var container = new UIElement();
        container.layout(layout -> layout.paddingAll(4));
        container.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND_INVERSE));

        int index = 0;
        for (int y = 0; y < colSize; y++) {
            var row = new UIElement();
            row.layout(layout -> layout.flexDirection(FlexDirection.ROW));
            for (int x = 0; x < rowSize; x++) {
                var slot = new BlockableFluidSlot();
                slot.setIsBlocked(this::isLocked);
                slot.bind(tank.getStorages()[index++], 0);
                slot.setAllowClickFilled(io.support(IO.IN));
                slot.setAllowClickDrained(true);
                slot.style(style -> style.backgroundTexture(GuiTextures.FLUID_SLOT));
                row.addChild(slot);
            }
            container.addChild(row);
        }

        var group = new UIElement();
        group.layout(layout -> layout.alignItems(AlignItems.CENTER).paddingAll(4));
        group.addChild(container);
        return group;
    }
}
