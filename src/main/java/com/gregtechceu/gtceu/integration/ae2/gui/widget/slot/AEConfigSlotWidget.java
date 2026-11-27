package com.gregtechceu.gtceu.integration.ae2.gui.widget.slot;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.PhantomFluidSlot;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.AEAmountDialog;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.ConfigWidget;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEFluidSlot;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAESlot;
import com.gregtechceu.gtceu.integration.ae2.utils.AEUtil;
import com.gregtechceu.gtceu.utils.GTMath;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEmitter;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEventBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import dev.vfyjxf.taffy.style.TaffyPosition;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class AEConfigSlotWidget extends UIElement {

    protected final ConfigWidget parentWidget;
    protected final int index;

    private final StackCell configCell = new StackCell();
    private final StackCell stockCell = new StackCell();
    private final RPCEmitter removeEvent;
    private final RPCEmitter itemEvent;
    private final RPCEmitter fluidEvent;
    private final RPCEmitter amountEvent;
    private final RPCEmitter pickUpEvent;

    @Nullable
    private GenericStack syncedConfig;
    @Nullable
    private GenericStack syncedStock;
    private Tag syncedTag = new CompoundTag();
    @Nullable
    private Boolean lastAutoPull;
    private IGuiTexture configBackground = IGuiTexture.EMPTY;

    public AEConfigSlotWidget(ConfigWidget parentWidget, int index) {
        this.parentWidget = parentWidget;
        this.index = index;

        layout(layout -> layout.width(18).height(36));
        addChildren(configCell, stockCell);

        removeEvent = addRPCEvent(RPCEventBuilder.simple(this::serverRemove));
        itemEvent = addRPCEvent(RPCEventBuilder.simple(ItemStack.class, this::serverSetItem));
        fluidEvent = addRPCEvent(RPCEventBuilder.simple(FluidStack.class, this::serverSetFluid));
        amountEvent = addRPCEvent(RPCEventBuilder.simple(Long.class, this::serverSetAmount));
        pickUpEvent = addRPCEvent(RPCEventBuilder.simple(Boolean.class, this::serverPickUp));

        addChild(new BindableValue<Tag>().bind(DataBindingBuilder.tagS2C(this::collect)
                .remoteSetter(this::receive).build()));

        configCell.addEventListener(UIEvents.MOUSE_DOWN, this::onConfigMouseDown, true);
        configCell.addEventListener(UIEvents.MOUSE_WHEEL, this::onConfigWheel);
        configCell.addEventListener(UIEvents.HOVER_TOOLTIPS, this::onConfigTooltips);
        stockCell.addEventListener(UIEvents.MOUSE_DOWN, this::onStockMouseDown, true);

        configCell.itemSlot.xeiPhantom();
        configCell.fluidSlot.xeiPhantom();
        configCell.itemSlot.registerValueListener(this::onGhostItem);
        configCell.fluidSlot.registerValueListener(this::onGhostFluid);

        addEventListener(UIEvents.TICK, event -> refresh());
    }

    private boolean isStackValidForSlot(@Nullable GenericStack stack) {
        if (stack == null || stack.amount() < 0) return true;
        if (!this.parentWidget.acceptsKey(stack.what())) return false;
        if (!this.parentWidget.isStocking()) return true;

        return !this.parentWidget.hasStackInConfig(stack);
    }

    private static boolean areAEStackCountsNotEqual(@Nullable GenericStack first, @Nullable GenericStack second) {
        if (second == first) return false;
        if (first != null && second != null) return first.amount() != second.amount() || !first.what().matches(second);

        return true;
    }

    private Tag collect() {
        var slot = this.parentWidget.getConfig(this.index);
        var newConfig = slot.getConfig();
        var newStock = slot.getStock();

        if (areAEStackCountsNotEqual(newConfig, this.syncedConfig) ||
                areAEStackCountsNotEqual(newStock, this.syncedStock)) {
            this.syncedConfig = newConfig;
            this.syncedStock = newStock;

            var tag = new CompoundTag();

            if (newConfig != null) {
                tag.put("config", GenericStack.writeTag(newConfig));
            }

            if (newStock != null) {
                tag.put("stock", GenericStack.writeTag(newStock));
            }

            this.syncedTag = tag;
        }

        return this.syncedTag;
    }

    private void receive(Tag tag) {
        var slot = this.parentWidget.getDisplay(this.index);

        if (tag instanceof CompoundTag compound) {
            slot.setConfig(compound.contains("config") ? GenericStack.readTag(compound.getCompound("config")) : null);
            slot.setStock(compound.contains("stock") ? GenericStack.readTag(compound.getCompound("stock")) : null);
        }

        refresh();
    }

    private void refresh() {
        if (!LDLib2.isRemote()) return;

        var slot = this.parentWidget.getDisplay(this.index);
        var autoPull = this.parentWidget.isAutoPull();

        if (this.lastAutoPull == null || this.lastAutoPull != autoPull) {
            this.lastAutoPull = autoPull;
            this.configBackground = IGuiTexture.group(
                    autoPull ? GuiTextures.SLOT_DARK : GuiTextures.SLOT,
                    autoPull ? GuiTextures.CONFIG_ARROW : GuiTextures.CONFIG_ARROW_DARK);
        }

        this.configCell.update(slot.getConfig(), !this.parentWidget.isStocking(), this.configBackground,
                this.parentWidget.acceptsItems(), this.parentWidget.acceptsFluids());
        this.stockCell.update(slot.getStock(), true, GuiTextures.SLOT_DARK, true, false);
    }

    @Nullable
    private AbstractContainerMenu menu() {
        var modularUI = getModularUI();

        return modularUI == null ? null : modularUI.getMenu();
    }

    private ItemStack carried() {
        var menu = menu();

        return menu == null ? ItemStack.EMPTY : menu.getCarried();
    }

    private void onConfigTooltips(UIEvent event) {
        var slot = this.parentWidget.getDisplay(this.index);

        if (slot.getConfig() != null) return;

        var tooltips = new ArrayList<Component>();

        tooltips.add(Component.translatable("gtceu.gui.config_slot"));

        if (this.parentWidget.isAutoPull()) {
            tooltips.add(Component.translatable("gtceu.gui.config_slot.auto_pull_managed"));
        } else {
            if (!this.parentWidget.isStocking()) {
                tooltips.add(Component.translatable("gtceu.gui.config_slot.set"));
                tooltips.add(Component.translatable("gtceu.gui.config_slot.scroll"));
            } else {
                tooltips.add(Component.translatable("gtceu.gui.config_slot.set_only"));
            }

            tooltips.add(Component.translatable("gtceu.gui.config_slot.remove"));
        }

        event.hoverTooltips = new HoverTooltips(tooltips, null, null, null);
    }

    private void onConfigMouseDown(UIEvent event) {
        event.stopPropagation();

        if (this.parentWidget.isAutoPull()) return;

        if (event.button == 1) {
            this.removeEvent.send();
        } else if (event.button == 0) {
            var held = carried();

            if (!held.isEmpty()) {
                var contained = getContainedFluid(held, event.isShiftDown());

                if (!contained.isEmpty()) {
                    acceptFluid(contained);
                } else {
                    acceptItem(held);
                }
            } else {
                var config = this.parentWidget.getDisplay(this.index).getConfig();

                if (config != null) {
                    AEAmountDialog.open(this, config.what(), config.amount(), this::sendAmount);
                }
            }
        }
    }

    private void onStockMouseDown(UIEvent event) {
        event.stopPropagation();

        if (event.button != 0 || this.parentWidget.isStocking()) return;

        if (this.parentWidget.getDisplay(this.index).getStock() != null) {
            this.pickUpEvent.send(event.isShiftDown());
        }
    }

    private void onConfigWheel(UIEvent event) {
        if (this.parentWidget.isStocking()) return;

        var config = this.parentWidget.getDisplay(this.index).getConfig();

        if (config == null || event.deltaY == 0) return;

        long current = config.amount();
        long amount;

        if (event.isCtrlDown()) {
            amount = event.deltaY > 0 ? current * 2L : current / 2L;
        } else {
            amount = event.deltaY > 0 ? current + 1L : current - 1L;
        }

        if (amount > 0 && amount < Integer.MAX_VALUE + 1L) {
            this.amountEvent.send(amount);

            event.stopPropagation();
        }
    }

    private FluidStack getContainedFluid(ItemStack held, boolean shift) {
        if (!this.parentWidget.acceptsFluids()) return FluidStack.EMPTY;
        if (this.parentWidget.acceptsItems() && shift) return FluidStack.EMPTY;

        return FluidUtil.getFluidContained(held).orElse(FluidStack.EMPTY);
    }

    private void onGhostItem(ItemStack stack) {
        if (!stack.isEmpty()) {
            var drained = PhantomFluidSlot.drainFrom(stack);

            if (!this.parentWidget.acceptsItems() && !drained.isEmpty()) {
                acceptFluid(drained);
            } else {
                acceptItem(stack);
            }
        }

        refresh();
    }

    private void onGhostFluid(FluidStack stack) {
        if (!stack.isEmpty()) {
            acceptFluid(stack);
        }

        refresh();
    }

    private void acceptItem(ItemStack itemStack) {
        if (!this.parentWidget.acceptsItems() || itemStack.isEmpty()) return;

        this.itemEvent.send(itemStack.copy());
    }

    private void acceptFluid(FluidStack fluidStack) {
        if (!this.parentWidget.acceptsFluids()) return;

        var stack = fluidStack.copy();

        if (stack.getFluid() != Fluids.EMPTY && stack.getAmount() <= 0) {
            stack.setAmount(1000);
        }

        if (!stack.isEmpty()) {
            this.fluidEvent.send(stack);
        }
    }

    private void sendAmount(long amount) {
        this.amountEvent.send(amount);
    }

    private void serverRemove() {
        if (this.parentWidget.isAutoPull()) return;

        this.parentWidget.getConfig(this.index).setConfig(null);
    }

    private void serverSetItem(ItemStack item) {
        if (this.parentWidget.isAutoPull()) return;

        var stack = GenericStack.fromItemStack(item);

        if (!isStackValidForSlot(stack)) return;

        this.parentWidget.getConfig(this.index).setConfig(stack);
    }

    private void serverSetFluid(FluidStack fluid) {
        if (this.parentWidget.isAutoPull()) return;

        var stack = AEUtil.fromFluidStack(fluid);

        if (!isStackValidForSlot(stack)) return;

        this.parentWidget.getConfig(this.index).setConfig(stack);
    }

    private void serverSetAmount(Long amount) {
        if (this.parentWidget.isAutoPull()) return;

        var slot = this.parentWidget.getConfig(this.index);

        if (slot.getConfig() != null && amount >= 1 && amount <= Integer.MAX_VALUE) {
            slot.setConfig(new GenericStack(slot.getConfig().what(), amount));
        }
    }

    private void serverPickUp(Boolean shift) {
        if (this.parentWidget.isAutoPull() || this.parentWidget.isStocking()) return;

        var menu = menu();
        var modularUI = getModularUI();

        if (menu == null || modularUI == null) return;

        var slot = this.parentWidget.getConfig(this.index);
        var stock = slot.getStock();

        if (stock == null) return;

        if (stock.what() instanceof AEItemKey key) {
            if (!menu.getCarried().isEmpty()) return;

            var stack = key.toStack(Math.min(GTMath.saturatedCast(stock.amount()), key.getItem().getMaxStackSize()));

            menu.setCarried(stack);

            var remaining = ExportOnlyAESlot.copy(stock, Math.max(0, stock.amount() - stack.getCount()));

            slot.setStock(remaining.amount() == 0 ? null : remaining);
        } else if (stock.what() instanceof AEFluidKey) {
            tryClickContainer(modularUI.player, menu, shift);
        }
    }

    private void tryClickContainer(Player player, AbstractContainerMenu menu, boolean isShiftKeyDown) {
        var fluidTank = this.parentWidget
                .getConfig(this.index) instanceof ExportOnlyAEFluidSlot fluid ? fluid : null;

        if (fluidTank == null || player == null) return;

        ItemStack currentStack = menu.getCarried();
        var handler = FluidUtil.getFluidHandler(currentStack).resolve().orElse(null);

        if (handler == null) return;

        int maxAttempts = isShiftKeyDown ? currentStack.getCount() : 1;

        if (fluidTank.getFluidAmount() > 0) {
            boolean performedFill = false;
            FluidStack initialFluid = fluidTank.getFluid();

            for (int i = 0; i < maxAttempts; i++) {
                FluidActionResult result = FluidUtil.tryFillContainer(currentStack, fluidTank, Integer.MAX_VALUE, null,
                        false);

                if (!result.isSuccess()) break;

                ItemStack remainingStack = FluidUtil
                        .tryFillContainer(currentStack, fluidTank, Integer.MAX_VALUE, null, true).getResult();

                currentStack.shrink(1);

                performedFill = true;

                if (!remainingStack.isEmpty() && !player.addItem(remainingStack)) {
                    Block.popResource(player.level(), player.getOnPos(), remainingStack);

                    break;
                }
            }

            if (performedFill) {
                SoundEvent soundevent = initialFluid.getFluid().getFluidType().getSound(initialFluid,
                        SoundActions.BUCKET_FILL);

                if (soundevent != null) {
                    player.level().playSound(null, player.position().x, player.position().y + 0.5, player.position().z,
                            soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
                }

                menu.setCarried(currentStack);
            }
        }
    }

    private static class StackCell extends UIElement {

        private final ItemSlot itemSlot = new ItemSlot();
        private final FluidSlot fluidSlot = new FluidSlot();
        @Nullable
        private IGuiTexture lastBackground;
        private boolean lastItemShown = true;
        private boolean lastFluidShown = true;
        private boolean lastAmountShown = true;
        private boolean lastFluidTooltips = true;

        StackCell() {
            layout(layout -> layout.width(18).height(18));

            fluidSlot.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(0).top(0));
            fluidSlot.slotStyle(style -> style.fillDirection(FillDirection.ALWAYS_FULL));
            fluidSlot.setAllowClickFilled(false);
            fluidSlot.setAllowClickDrained(false);

            addChildren(itemSlot, fluidSlot);
        }

        void update(@Nullable GenericStack stack, boolean showAmount, IGuiTexture background, boolean acceptItems,
                    boolean acceptFluids) {
            boolean isFluid = stack != null && stack.what() instanceof AEFluidKey;
            boolean isItem = stack != null && stack.what() instanceof AEItemKey;
            boolean empty = stack == null;

            boolean showItem = isItem || (empty && acceptItems) || (empty && !acceptFluids);
            boolean showFluid = isFluid || (empty && acceptFluids);

            if (this.lastItemShown != showItem) {
                this.itemSlot.setDisplay(showItem);
            }

            if (this.lastFluidShown != showFluid) {
                this.lastFluidShown = showFluid;
                this.fluidSlot.setDisplay(showFluid);
            }

            if (this.lastBackground != background || this.lastItemShown != showItem) {
                this.lastBackground = background;
                this.lastItemShown = showItem;
                this.itemSlot.style(style -> style.backgroundTexture(background));
                this.fluidSlot.style(style -> style.backgroundTexture(showItem ? IGuiTexture.EMPTY : background));
            }

            var displayedItem = ItemStack.EMPTY;

            if (isItem && stack.what() instanceof AEItemKey key) {
                displayedItem = key.toStack(showAmount ? Math.max(1, GTMath.saturatedCast(stack.amount())) : 1);
            }

            this.itemSlot.setItem(displayedItem, false);

            var displayedFluid = isFluid ? AEUtil.toFluidStack(stack) : FluidStack.EMPTY;

            this.fluidSlot.setValue(displayedFluid, false);

            var showFluidAmount = showAmount && isFluid;

            if (this.lastAmountShown != showFluidAmount) {
                this.lastAmountShown = showFluidAmount;
                this.fluidSlot.amountLabel.setDisplay(showFluidAmount);
            }

            var fluidTooltips = !displayedFluid.isEmpty();

            if (this.lastFluidTooltips != fluidTooltips) {
                this.lastFluidTooltips = fluidTooltips;
                this.fluidSlot.slotStyle(style -> style.showFluidTooltips(fluidTooltips));
            }
        }
    }
}
