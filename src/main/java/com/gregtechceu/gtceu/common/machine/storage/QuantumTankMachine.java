package com.gregtechceu.gtceu.common.machine.storage;

import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.PhantomFluidSlot;
import com.gregtechceu.gtceu.api.gui.widget.ToggleButton;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.machine.*;
import com.gregtechceu.gtceu.api.machine.feature.IAutoOutputFluid;
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine;
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import com.gregtechceu.gtceu.api.transfer.fluid.IFluidHandlerModifiable;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTMath;
import com.gregtechceu.gtceu.utils.GTTransferUtils;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.RequireRerender;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import dev.vfyjxf.taffy.style.TaffyPosition;
import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class QuantumTankMachine extends TieredMachine implements IAutoOutputFluid, IInteractedMachine, IControllable,
                                IDropSaveMachine, IFancyUIMachine {

    public static Object2LongMap<MachineDefinition> TANK_CAPACITY = new Object2LongArrayMap<>();

    @Getter
    @Persisted
    @DescSynced
    @RequireRerender
    protected Direction outputFacingFluids;
    @Getter
    @Persisted
    @DescSynced
    @RequireRerender
    protected boolean autoOutputFluids;
    @Getter
    @Setter
    @Persisted
    protected boolean allowInputFromOutputSideFluids;
    @Persisted
    private boolean isVoiding;

    @Getter
    private final long maxAmount;
    protected final FluidCache cache;
    @DescSynced
    private final CustomFluidTank lockedFluid;

    @Getter
    @DescSynced
    protected FluidStack stored = FluidStack.EMPTY;
    @Getter
    @DescSynced
    protected long storedAmount = 0;

    @Nullable
    protected TickableSubscription autoOutputSubs;

    public QuantumTankMachine(IMachineBlockEntity holder, int tier, long maxAmount, Object... args) {
        super(holder, tier);
        this.outputFacingFluids = getFrontFacing().getOpposite();
        this.maxAmount = maxAmount;
        this.cache = createCacheFluidHandler(args);
        this.lockedFluid = new CustomFluidTank(1000);
    }

    //////////////////////////////////////
    // ***** Initialization ******//
    //////////////////////////////////////

    protected FluidCache createCacheFluidHandler(Object... args) {
        return new FluidCache(this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(0, this::updateAutoOutputSubscription));
        }
    }

    protected void onFluidChanged() {
        if (!isRemote()) {
            markDirty();
            updateAutoOutputSubscription();
        }
    }

    @Override
    public boolean savePickClone() {
        return false;
    }

    @Override
    public boolean saveBreak() {
        return !stored.isEmpty();
    }

    @Override
    public void saveCustomPersistedData(@NotNull CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);
        if (!forDrop) tag.put("lockedFluid", lockedFluid.writeToNBT(new CompoundTag()));
        tag.put("stored", stored.writeToNBT(new CompoundTag()));
        tag.putLong("storedAmount", storedAmount);
    }

    @Override
    public void saveToItem(CompoundTag tag) {
        IDropSaveMachine.super.saveToItem(tag);
        tag.put("stored", stored.writeToNBT(new CompoundTag()));
        tag.putLong("storedAmount", storedAmount);
    }

    @Override
    public void loadFromItem(CompoundTag tag) {
        IDropSaveMachine.super.loadFromItem(tag);
        if (tag.contains("stored") && tag.contains("storedAmount")) {
            var fluid = FluidStack.loadFluidStackFromNBT(tag.getCompound("stored"));
            stored = new FluidStack(fluid, 1000);
            storedAmount = tag.getLong("storedAmount");
        }
    }

    @Override
    public void loadCustomPersistedData(@NotNull CompoundTag tag) {
        super.loadCustomPersistedData(tag);

        var from = tag.contains("cache") ? tag.getCompound("cache") : tag;
        this.lockedFluid.readFromNBT(from.getCompound("lockedFluid"));

        var stored = FluidStack.loadFluidStackFromNBT(tag.getCompound("stored"));
        this.stored = new FluidStack(stored, 1000);

        if (!tag.contains("storedAmount")) this.storedAmount = stored.getAmount();
        else this.storedAmount = tag.getLong("storedAmount");
        if (storedAmount == 0 && !stored.isEmpty()) this.storedAmount = stored.getAmount();
    }

    //////////////////////////////////////
    // ****** Capability ********//
    //////////////////////////////////////

    @Override
    public @Nullable IItemHandlerModifiable getItemHandlerCap(@Nullable Direction side, boolean useCoverCapability) {
        if (side == getFrontFacing()) {
            return null;
        }
        return super.getItemHandlerCap(side, useCoverCapability);
    }

    @Override
    public @Nullable IFluidHandlerModifiable getFluidHandlerCap(@Nullable Direction side, boolean useCoverCapability) {
        if (side == getFrontFacing()) {
            return null;
        }
        return super.getFluidHandlerCap(side, useCoverCapability);
    }

    //////////////////////////////////////
    // ******* Auto Output *******//
    //////////////////////////////////////

    @Override
    public void setAutoOutputFluids(boolean allow) {
        this.autoOutputFluids = allow;
        updateAutoOutputSubscription();
    }

    @Override
    public void setOutputFacingFluids(@Nullable Direction outputFacing) {
        this.outputFacingFluids = outputFacing;
        updateAutoOutputSubscription();
    }

    @Override
    public boolean isWorkingEnabled() {
        return isAutoOutputFluids();
    }

    @Override
    public void setWorkingEnabled(boolean isWorkingAllowed) {
        setAutoOutputFluids(isWorkingAllowed);
    }

    @Override
    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChanged(block, fromPos, isMoving);
        updateAutoOutputSubscription();
    }

    protected void updateAutoOutputSubscription() {
        var outputFacing = getOutputFacingFluids();
        if ((isAutoOutputFluids() && !stored.isEmpty()) && outputFacing != null &&
                GTTransferUtils.hasAdjacentFluidHandler(getLevel(), getPos(), outputFacing)) {
            autoOutputSubs = subscribeServerTick(autoOutputSubs, this::checkAutoOutput);
        } else if (autoOutputSubs != null) {
            autoOutputSubs.unsubscribe();
            autoOutputSubs = null;
        }
    }

    protected void checkAutoOutput() {
        if (getOffsetTimer() % 5 == 0) {
            if (isAutoOutputFluids() && getOutputFacingFluids() != null) {
                cache.exportToNearby(getOutputFacingFluids());
            }
            updateAutoOutputSubscription();
        }
    }

    //////////////////////////////////////
    // ******* Interaction *******//
    //////////////////////////////////////

    @Override
    public boolean isFacingValid(Direction facing) {
        if (facing == outputFacingFluids) return false;
        return super.isFacingValid(facing);
    }

    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                   BlockHitResult hit) {
        if (hit.getDirection() == getFrontFacing() && !isRemote()) {
            if (FluidUtil.interactWithFluidHandler(player, hand, cache)) {
                return InteractionResult.SUCCESS;
            }
        }
        return IInteractedMachine.super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult onWrenchClick(Player playerIn, InteractionHand hand, Direction gridSide,
                                              BlockHitResult hitResult) {
        if (!playerIn.isShiftKeyDown() && !isRemote()) {
            var tool = playerIn.getItemInHand(hand);
            if (tool.getDamageValue() >= tool.getMaxDamage()) return InteractionResult.PASS;
            if (hasFrontFacing() && gridSide == getFrontFacing()) return InteractionResult.PASS;
            if (gridSide != getOutputFacingFluids()) {
                setOutputFacingFluids(gridSide);
            } else {
                setOutputFacingFluids(null);
            }
            return InteractionResult.sidedSuccess(playerIn.level().isClientSide);
        }

        return super.onWrenchClick(playerIn, hand, gridSide, hitResult);
    }

    @Override
    protected InteractionResult onScrewdriverClick(Player playerIn, InteractionHand hand, Direction gridSide,
                                                   BlockHitResult hitResult) {
        if (!isRemote()) {
            if (gridSide == getOutputFacingFluids()) {
                if (isAllowInputFromOutputSideFluids()) {
                    setAllowInputFromOutputSideFluids(false);
                    playerIn.sendSystemMessage(
                            Component.translatable("gtceu.machine.basic.input_from_output_side.disallow")
                                    .append(Component.translatable("gtceu.creative.tank.fluid")));
                } else {
                    setAllowInputFromOutputSideFluids(true);
                    playerIn.sendSystemMessage(
                            Component.translatable("gtceu.machine.basic.input_from_output_side.allow")
                                    .append(Component.translatable("gtceu.creative.tank.fluid")));
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.onScrewdriverClick(playerIn, hand, gridSide, hitResult);
    }

    public boolean isLocked() {
        return !lockedFluid.isEmpty();
    }

    protected void setLocked(boolean locked) {
        if (!stored.isEmpty() && locked) {
            var copied = new FluidStack(stored, 1000);
            lockedFluid.setFluid(copied);
        } else if (!locked) {
            lockedFluid.setFluid(FluidStack.EMPTY);
        }
    }

    protected void setLocked(FluidStack fluid) {
        if (fluid.isEmpty()) setLocked(false);
        else if (stored.isEmpty()) lockedFluid.setFluid(fluid);
        else if (stored.isFluidEqual(fluid)) setLocked(true);
    }

    public FluidStack getLockedFluid() {
        return lockedFluid.getFluid();
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////
    public UIElement createUIWidget() {
        var group = new UIElement();
        group.layout(layout -> layout.width(90).height(63));
        group.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND_INVERSE));

        var display = new UIElement();
        display.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(4).top(4).width(82).height(55));
        display.style(style -> style.backgroundTexture(GuiTextures.DISPLAY));

        var title = new Label();
        title.setText(Component.translatable("gtceu.gui.fluid_amount"));
        title.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(8).top(8));

        var amount = new Label();
        amount.textStyle(style -> style.textColor(-1).textShadow(false));
        amount.bind(DataBindingBuilder.componentS2C(
                () -> Component.literal(FormattingUtil.formatBuckets(storedAmount).getString())).build());
        amount.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(8).top(18));

        var tankSlot = new FluidSlot().bind(cache, 0);
        tankSlot.amountLabel.setDisplay(false);
        tankSlot.setAllowClickFilled(true);
        tankSlot.setAllowClickDrained(true);
        tankSlot.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(68).top(23));
        tankSlot.style(style -> style.backgroundTexture(GuiTextures.FLUID_SLOT));

        var lockedSlot = new PhantomFluidSlot(this::getLockedFluid, this::setLocked);
        lockedSlot.amountLabel.setDisplay(false);
        lockedSlot.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(68).top(41));
        lockedSlot.style(style -> style.backgroundTexture(ColorPattern.T_GRAY.rectTexture()));

        var autoOutput = new ToggleButton(GuiTextures.BUTTON_FLUID_OUTPUT, this::isAutoOutputFluids,
                this::setAutoOutputFluids)
                .setShouldUseBaseBackground()
                .setTooltipText("gtceu.gui.fluid_auto_output.tooltip");
        autoOutput.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(4).top(41));

        var lock = new ToggleButton(GuiTextures.BUTTON_LOCK, this::isLocked, this::setLocked)
                .setShouldUseBaseBackground()
                .setTooltipText("gtceu.gui.fluid_lock.tooltip");
        lock.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(22).top(41));

        var voiding = new ToggleButton(GuiTextures.BUTTON_VOID, () -> isVoiding, b -> isVoiding = b)
                .setShouldUseBaseBackground()
                .setTooltipText("gtceu.gui.fluid_voiding_partial.tooltip");
        voiding.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(40).top(41));

        group.addChildren(display, title, amount, tankSlot, lockedSlot, autoOutput, lock, voiding);
        return group;
    }

    //////////////////////////////////////
    // ******* Rendering ********//
    //////////////////////////////////////
    @Override
    public @Nullable SpriteTexture sideTips(Player player, BlockPos pos, BlockState state, Set<GTToolType> toolTypes,
                                            Direction side) {
        if (toolTypes.contains(GTToolType.WRENCH)) {
            if (!player.isShiftKeyDown()) {
                if (!hasFrontFacing() || side != getFrontFacing()) {
                    return GuiTextures.TOOL_IO_FACING_ROTATION;
                }
            }
        } else if (toolTypes.contains(GTToolType.SCREWDRIVER)) {
            if (side == getOutputFacingFluids()) {
                return GuiTextures.TOOL_ALLOW_INPUT;
            }
        } else if (toolTypes.contains(GTToolType.SOFT_MALLET)) {
            if (side == getFrontFacing()) return null;
        }
        return super.sideTips(player, pos, state, toolTypes, side);
    }

    protected class FluidCache extends MachineTrait implements IFluidHandler {

        private final Predicate<FluidStack> filter = f -> !isLocked() || getLockedFluid().isFluidEqual(f);

        public FluidCache(MetaMachine holder) {
            super(holder);
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return new FluidStack(stored, GTMath.saturatedCast(storedAmount));
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            long free = isVoiding ? Long.MAX_VALUE : maxAmount - storedAmount;
            long canFill = 0;
            if ((stored.isEmpty() || stored.isFluidEqual(resource)) && filter.test(resource)) {
                canFill = Math.min(resource.getAmount(), free);
            }
            if (action.execute() && canFill > 0) {
                if (stored.isEmpty()) stored = new FluidStack(resource, 1000);
                storedAmount = Math.min(maxAmount, storedAmount + canFill);
                onFluidChanged();
            }
            return (int) canFill;
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            if (stored.isEmpty()) return FluidStack.EMPTY;
            long toDrain = Math.min(storedAmount, maxDrain);
            var copy = new FluidStack(stored, (int) toDrain);
            if (action.execute() && toDrain > 0) {
                storedAmount -= toDrain;
                if (storedAmount == 0) stored = FluidStack.EMPTY;
                onFluidChanged();
            }
            return copy.isEmpty() ? FluidStack.EMPTY : copy;
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (!resource.isFluidEqual(stored)) return FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }

        @Override
        public int getTankCapacity(int tank) {
            return GTMath.saturatedCast(maxAmount);
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return filter.test(stack);
        }

        public void exportToNearby(@NotNull Direction... facings) {
            if (stored.isEmpty()) return;
            var level = getMachine().getLevel();
            var pos = getMachine().getPos();
            for (Direction facing : facings) {
                var filter = getMachine().getFluidCapFilter(facing, IO.OUT);
                GTTransferUtils.getAdjacentFluidHandler(level, pos, facing)
                        .ifPresent(adj -> GTTransferUtils.transferFluidsFiltered(this, adj, filter));
            }
        }
    }
}
