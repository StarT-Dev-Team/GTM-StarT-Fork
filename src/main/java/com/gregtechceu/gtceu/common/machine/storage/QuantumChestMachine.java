package com.gregtechceu.gtceu.common.machine.storage;

import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.PhantomItemSlot;
import com.gregtechceu.gtceu.api.gui.widget.ToggleButton;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.TieredMachine;
import com.gregtechceu.gtceu.api.machine.feature.IAutoOutputItem;
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine;
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait;
import com.gregtechceu.gtceu.api.transfer.fluid.IFluidHandlerModifiable;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTMath;
import com.gregtechceu.gtceu.utils.GTTransferUtils;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import dev.vfyjxf.taffy.style.TaffyPosition;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class QuantumChestMachine extends TieredMachine implements IAutoOutputItem, IInteractedMachine, IControllable,
                                 IDropSaveMachine, IFancyUIMachine {

    /**
     * Sourced from FunctionalStorage's
     * <a
     * href=https://github.com/Buuz135/FunctionalStorage/blob/1.21/src/main/java/com/buuz135/functionalstorage/block/tile/ItemControllableDrawerTile.java>
     * ItemControllerDrawerTile</a>
     */
    public static final Object2LongOpenHashMap<UUID> INTERACTION_LOGGER = new Object2LongOpenHashMap<>();

    @Getter
    @Persisted
    @DescSynced
    @RequireRerender
    protected Direction outputFacingItems;
    @Getter
    @Persisted
    @DescSynced
    @RequireRerender
    protected boolean autoOutputItems;
    @Getter
    @Setter
    @Persisted
    protected boolean allowInputFromOutputSideItems;
    @Persisted
    private boolean isVoiding;

    private final long maxAmount;
    protected final ItemCache cache;
    @DescSynced
    private final CustomItemStackHandler lockedItem;

    @Getter
    @DescSynced
    protected ItemStack stored = ItemStack.EMPTY;
    @Getter
    @DescSynced
    protected long storedAmount = 0;

    @Nullable
    protected TickableSubscription autoOutputSubs;

    public QuantumChestMachine(IMachineBlockEntity holder, int tier, long maxAmount, Object... args) {
        super(holder, tier);
        this.outputFacingItems = getFrontFacing().getOpposite();
        this.maxAmount = maxAmount;
        this.cache = createCacheItemHandler(args);
        this.lockedItem = new CustomItemStackHandler();
    }

    //////////////////////////////////////
    // ***** Initialization ******//
    //////////////////////////////////////

    protected ItemCache createCacheItemHandler(Object... args) {
        return new ItemCache(this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(0, this::updateAutoOutputSubscription));
        }
    }

    protected void onItemChanged() {
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
        if (!forDrop) tag.put("lockedItem", lockedItem.serializeNBT());
        tag.put("stored", stored.serializeNBT());
        tag.putLong("storedAmount", storedAmount);
    }

    @Override
    public void saveToItem(CompoundTag tag) {
        IDropSaveMachine.super.saveToItem(tag);
        tag.put("stored", stored.serializeNBT());
        tag.putLong("storedAmount", storedAmount);
    }

    @Override
    public void loadFromItem(CompoundTag tag) {
        IDropSaveMachine.super.loadFromItem(tag);
        if (tag.contains("stored") && tag.contains("storedAmount")) {
            stored = ItemStack.of(tag.getCompound("stored"));
            storedAmount = tag.getLong("storedAmount");
        }
    }

    @Override
    public void loadCustomPersistedData(@NotNull CompoundTag tag) {
        super.loadCustomPersistedData(tag);
        lockedItem.deserializeNBT(tag.getCompound("lockedItem"));
        stored = ItemStack.of(tag.getCompound("stored"));
        storedAmount = tag.getLong("storedAmount");
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
    public void setAutoOutputItems(boolean allow) {
        this.autoOutputItems = allow;
        updateAutoOutputSubscription();
    }

    @Override
    public void setOutputFacingItems(@Nullable Direction outputFacing) {
        this.outputFacingItems = outputFacing;
        updateAutoOutputSubscription();
    }

    @Override
    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChanged(block, fromPos, isMoving);
        updateAutoOutputSubscription();
    }

    @Override
    public boolean isWorkingEnabled() {
        return isAutoOutputItems();
    }

    @Override
    public void setWorkingEnabled(boolean isWorkingAllowed) {
        setAutoOutputItems(isWorkingAllowed);
    }

    protected void updateAutoOutputSubscription() {
        var outputFacing = getOutputFacingItems();
        if ((isAutoOutputItems() && !stored.isEmpty()) && outputFacing != null &&
                GTTransferUtils.hasAdjacentItemHandler(getLevel(), getPos(), outputFacing)) {
            autoOutputSubs = subscribeServerTick(autoOutputSubs, this::checkAutoOutput);
        } else if (autoOutputSubs != null) {
            autoOutputSubs.unsubscribe();
            autoOutputSubs = null;
        }
    }

    protected void checkAutoOutput() {
        if (getOffsetTimer() % 5 == 0) {
            if (isAutoOutputItems() && getOutputFacingItems() != null) {
                cache.exportToNearby(getOutputFacingItems());
            }
            updateAutoOutputSubscription();
        }
    }

    //////////////////////////////////////
    // ******* Interaction *******//
    //////////////////////////////////////

    @Override
    public boolean isFacingValid(Direction facing) {
        if (facing == outputFacingItems) return false;
        return super.isFacingValid(facing);
    }

    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                   BlockHitResult hit) {
        if (hit.getDirection() == getFrontFacing() && !isRemote()) {
            // Check to see if the hit is within the glass frame of the chest
            var aabb = new AABB(hit.getBlockPos()).deflate(0.12);
            var hitVector = hit.getLocation().relative(getFrontFacing(), -0.5);
            if (!aabb.contains(hitVector)) return InteractionResult.PASS;

            var held = player.getMainHandItem();
            if (!held.isEmpty() && cache.canInsert(held)) { // push
                var remaining = cache.insertItem(0, held, false);
                player.setItemInHand(InteractionHand.MAIN_HAND, remaining);
                return InteractionResult.SUCCESS;
            } else if (isDoubleHit(player.getUUID())) {
                for (var stack : player.getInventory().items) {
                    if (!stack.isEmpty() && cache.canInsert(stack)) {
                        stack.setCount(cache.insertItem(0, stack, false).getCount());
                    }
                }
            }
            INTERACTION_LOGGER.put(player.getUUID(), System.currentTimeMillis());
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static boolean isDoubleHit(UUID uuid) {
        return (System.currentTimeMillis() - INTERACTION_LOGGER.getLong(uuid)) < 300;
    }

    @Override
    public boolean onLeftClick(Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction) {
        if (direction == getFrontFacing() && !isRemote()) {
            if (GTToolType.WRENCH.matchTags.stream().anyMatch(player.getItemInHand(hand)::is)) return false;
            if (!stored.isEmpty()) { // pull
                var drained = cache.extractItem(0, player.isShiftKeyDown() ? stored.getMaxStackSize() : 1, false);
                if (!drained.isEmpty()) {
                    if (!player.addItem(drained)) {
                        Block.popResourceFromFace(world, getPos(), getFrontFacing(), drained);
                    }
                }
            }
        }
        return IInteractedMachine.super.onLeftClick(player, world, hand, pos, direction);
    }

    @Override
    protected InteractionResult onWrenchClick(Player playerIn, InteractionHand hand, Direction gridSide,
                                              BlockHitResult hitResult) {
        if (!playerIn.isShiftKeyDown() && !isRemote()) {
            var tool = playerIn.getItemInHand(hand);
            if (tool.getDamageValue() >= tool.getMaxDamage()) return InteractionResult.PASS;
            if (hasFrontFacing() && gridSide == getFrontFacing()) return InteractionResult.PASS;
            if (gridSide != getOutputFacingItems()) {
                setOutputFacingItems(gridSide);
            } else {
                setOutputFacingItems(null);
            }
            return InteractionResult.sidedSuccess(playerIn.level().isClientSide);
        }

        return super.onWrenchClick(playerIn, hand, gridSide, hitResult);
    }

    @Override
    protected InteractionResult onScrewdriverClick(Player playerIn, InteractionHand hand, Direction gridSide,
                                                   BlockHitResult hitResult) {
        if (!isRemote()) {
            if (gridSide == getOutputFacingItems()) {
                if (isAllowInputFromOutputSideItems()) {
                    setAllowInputFromOutputSideItems(false);
                    playerIn.sendSystemMessage(
                            Component.translatable("gtceu.machine.basic.input_from_output_side.disallow")
                                    .append(Component.translatable("gtceu.creative.chest.item")));
                } else {
                    setAllowInputFromOutputSideItems(true);
                    playerIn.sendSystemMessage(
                            Component.translatable("gtceu.machine.basic.input_from_output_side.allow")
                                    .append(Component.translatable("gtceu.creative.chest.item")));
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.onScrewdriverClick(playerIn, hand, gridSide, hitResult);
    }

    public boolean isLocked() {
        return !lockedItem.getStackInSlot(0).isEmpty();
    }

    protected void setLocked(boolean locked) {
        if (!stored.isEmpty() && locked) {
            var copied = stored.copyWithCount(1);
            lockedItem.setStackInSlot(0, copied);
        } else if (!locked) {
            lockedItem.setStackInSlot(0, ItemStack.EMPTY);
        }
    }

    public ItemStack getLockedItem() {
        return lockedItem.getStackInSlot(0);
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////

    public UIElement createUIWidget() {
        var group = new UIElement();
        group.layout(layout -> layout.width(109).height(63));
        group.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND_INVERSE));
        var importItems = createImportItems();

        var display = new UIElement();
        display.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(4).top(4).width(81).height(55));
        display.style(style -> style.backgroundTexture(GuiTextures.DISPLAY));

        var title = new Label();
        title.setText(Component.translatable("gtceu.machine.quantum_chest.items_stored"));
        title.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(8).top(8));

        var amount = new Label();
        amount.textStyle(style -> style.textColor(-1).textShadow(true));
        amount.bind(DataBindingBuilder.componentS2C(
                () -> Component.literal(FormattingUtil.formatNumbers(storedAmount))).build());
        amount.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(8).top(18));

        var inputSlot = new ItemSlot().bind(new ItemHandlerSlot(importItems, 0).setCanTake(player -> false));
        inputSlot.slotStyle(style -> style.slotOverlay(GuiTextures.IN_SLOT_OVERLAY));
        inputSlot.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(87).top(5));

        var cacheSlot = new ItemSlot().bind(new ItemHandlerSlot(cache, 0).setCanTake(player -> false)
                .setCanPlace(stack -> false));
        cacheSlot.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(87).top(23));

        var takeButton = new Button();
        takeButton.noText();
        setTakeTexture(takeButton);
        takeButton.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(87).top(42).width(18)
                .height(18));
        takeButton.setOnServerClick(event -> {
            var modularUI = takeButton.getModularUI();
            if (modularUI == null || stored.isEmpty()) return;
            var player = modularUI.player;
            var extracted = cache.extractItem(0, (int) Math.min(storedAmount, stored.getMaxStackSize()), false);
            if (!player.addItem(extracted)) {
                Block.popResource(player.level(), player.getOnPos(), extracted);
            }
        });

        var lockedSlot = new PhantomItemSlot(lockedItem, 0,
                stack -> stored.isEmpty() || GTUtil.isSameItemSameTags(stack, stored));
        lockedSlot.setMaxStackSize(1);
        lockedSlot.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(58).top(41));

        var autoOutput = new ToggleButton(GuiTextures.BUTTON_ITEM_OUTPUT, this::isAutoOutputItems,
                this::setAutoOutputItems)
                .setShouldUseBaseBackground()
                .setTooltipText("gtceu.gui.item_auto_output.tooltip");
        autoOutput.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(4).top(41));

        var lock = new ToggleButton(GuiTextures.BUTTON_LOCK, this::isLocked, this::setLocked)
                .setShouldUseBaseBackground()
                .setTooltipText("gtceu.gui.item_lock.tooltip");
        lock.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(22).top(41));

        var voiding = new ToggleButton(GuiTextures.BUTTON_VOID, () -> isVoiding, b -> isVoiding = b)
                .setShouldUseBaseBackground()
                .setTooltipText("gtceu.gui.item_voiding_partial.tooltip");
        voiding.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(40).top(41));

        group.addChildren(display, title, amount, inputSlot, cacheSlot, takeButton, lockedSlot, autoOutput, lock,
                voiding);
        return group;
    }

    private static void setTakeTexture(Button button) {
        var texture = new GuiTextureGroup(GuiTextures.VANILLA_BUTTON, Icons.DOWN.scale(0.7f));
        button.buttonStyle(style -> style.baseTexture(texture).hoverTexture(texture).pressedTexture(texture));
    }

    private @NotNull CustomItemStackHandler createImportItems() {
        var importItems = new CustomItemStackHandler();
        importItems.setFilter(cache::canInsert);
        importItems.setOnContentsChanged(() -> {
            var item = importItems.getStackInSlot(0).copy();
            if (!item.isEmpty()) {
                importItems.setStackInSlot(0, ItemStack.EMPTY);
                importItems.onContentsChanged(0);
                cache.insertItem(0, item.copy(), false);
            }
        });
        return importItems;
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
            if (side == getOutputFacingItems()) {
                return GuiTextures.TOOL_ALLOW_INPUT;
            }
        } else if (toolTypes.contains(GTToolType.SOFT_MALLET)) {
            if (side == getFrontFacing()) return null;
        }
        return super.sideTips(player, pos, state, toolTypes, side);
    }

    protected class ItemCache extends MachineTrait implements IItemHandlerModifiable {

        private final Predicate<ItemStack> filter = i -> !isLocked() ||
                GTUtil.isSameItemSameTags(i, getLockedItem());

        public ItemCache(MetaMachine holder) {
            super(holder);
        }

        @Override
        public void setStackInSlot(int index, ItemStack stack) {
            stored = stack.copyWithCount(1);
            storedAmount = stack.getCount();
            onItemChanged();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return stored.copyWithCount(GTMath.saturatedCast(storedAmount));
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            long free = isVoiding ? Long.MAX_VALUE : maxAmount - storedAmount;
            long canStore = 0;
            if ((stored.isEmpty() || ItemHandlerHelper.canItemStacksStack(stored, stack)) && filter.test(stack)) {
                canStore = Math.min(stack.getCount(), free);
            }
            if (!simulate && canStore > 0) {
                if (stored.isEmpty()) stored = stack.copyWithCount(1);
                storedAmount = Math.min(maxAmount, storedAmount + canStore);
                onItemChanged();
            }
            return stack.copyWithCount((int) (stack.getCount() - canStore));
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (stored.isEmpty()) return ItemStack.EMPTY;
            long toExtract = Math.min(storedAmount, amount);
            var copy = stored.copyWithCount((int) toExtract);
            if (!simulate && toExtract > 0) {
                storedAmount -= toExtract;
                if (storedAmount == 0) stored = ItemStack.EMPTY;
                onItemChanged();
            }
            return copy;
        }

        @Override
        public int getSlotLimit(int slot) {
            return GTMath.saturatedCast(maxAmount);
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return filter.test(stack);
        }

        public void exportToNearby(@NotNull Direction... facings) {
            if (stored.isEmpty()) return;
            var level = getMachine().getLevel();
            var pos = getMachine().getPos();
            for (Direction facing : facings) {
                var filter = getMachine().getItemCapFilter(facing, IO.OUT);
                GTTransferUtils.getAdjacentItemHandler(level, pos, facing)
                        .ifPresent(adj -> GTTransferUtils.transferItemsFiltered(this, adj, filter));
            }
        }

        public boolean canInsert(ItemStack stack) {
            return filter.test(stack) && (insertItem(0, stack, true).getCount() != stack.getCount());
        }
    }
}
