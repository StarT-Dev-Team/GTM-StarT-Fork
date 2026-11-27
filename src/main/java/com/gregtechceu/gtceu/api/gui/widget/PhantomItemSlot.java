package com.gregtechceu.gtceu.api.gui.widget;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;

import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class PhantomItemSlot extends ItemSlot {

    private boolean clearSlotOnRightClick;
    private IntSupplier maxStackSize = () -> 64;
    private Predicate<ItemStack> validator = stack -> true;

    public PhantomItemSlot(IItemHandlerModifiable itemHandler, int slotIndex) {
        this(() -> itemHandler.getStackInSlot(slotIndex), stack -> itemHandler.setStackInSlot(slotIndex, stack));
    }

    public PhantomItemSlot(IItemHandlerModifiable itemHandler, int slotIndex, Predicate<ItemStack> validator) {
        this(itemHandler, slotIndex);
        this.validator = validator;
    }

    public PhantomItemSlot(Supplier<ItemStack> getter, Consumer<ItemStack> setter) {
        bind(DataBindingBuilder.itemStack(getter, stack -> {
            var sanitized = sanitize(stack);
            if (sanitized != null) setter.accept(sanitized);
        }).build());
        xeiPhantom();
    }

    public PhantomItemSlot setClearSlotOnRightClick(boolean clearSlotOnRightClick) {
        this.clearSlotOnRightClick = clearSlotOnRightClick;
        return this;
    }

    public PhantomItemSlot setMaxStackSize(int maxStackSize) {
        return setMaxStackSize(() -> maxStackSize);
    }

    public PhantomItemSlot setMaxStackSize(IntSupplier maxStackSize) {
        this.maxStackSize = maxStackSize;
        return this;
    }

    public PhantomItemSlot setValidator(Predicate<ItemStack> validator) {
        this.validator = validator;
        return this;
    }

    @Nullable
    private ItemStack sanitize(@Nullable ItemStack value) {
        if (value == null || value.isEmpty()) return ItemStack.EMPTY;
        if (!validator.test(value)) return null;
        var max = maxStackSize.getAsInt();
        return value.getCount() > max ? value.copyWithCount(max) : value;
    }

    @Override
    public ItemSlot setValue(@Nullable ItemStack value, boolean notify) {
        if (value != null && !value.isEmpty()) {
            value = sanitize(value);
            if (value == null) return this;
        }
        return super.setValue(value, notify);
    }

    @Override
    protected void onMouseDown(UIEvent event) {
        super.onMouseDown(event);
        if (!isActiveInHierarchy()) return;
        var modularUI = getModularUI();
        if (modularUI == null) return;
        var menu = modularUI.getMenu();
        var held = menu == null ? ItemStack.EMPTY : menu.getCarried();
        if (event.button == 1 && clearSlotOnRightClick && !getValue().isEmpty()) {
            setValue(ItemStack.EMPTY, true);
            return;
        }
        slotClickPhantom(event.button, event.isShiftDown() ? ClickType.QUICK_MOVE : ClickType.PICKUP, held);
    }

    private void slotClickPhantom(int mouseButton, ClickType clickType, ItemStack stackHeld) {
        var stackSlot = getValue();
        if (mouseButton == 2) {
            fillPhantomSlot(ItemStack.EMPTY, mouseButton);
        } else if (mouseButton == 0 || mouseButton == 1) {
            if (stackSlot.isEmpty()) {
                if (!stackHeld.isEmpty()) {
                    fillPhantomSlot(stackHeld, mouseButton);
                }
            } else if (stackHeld.isEmpty()) {
                adjustPhantomSlot(mouseButton, clickType);
            } else {
                if (!ItemStack.matches(stackSlot, stackHeld)) {
                    adjustPhantomSlot(mouseButton, clickType);
                }
                fillPhantomSlot(stackHeld, mouseButton);
            }
        }
    }

    private void adjustPhantomSlot(int mouseButton, ClickType clickType) {
        var stackSlot = getValue().copy();
        int stackSize;
        if (clickType == ClickType.QUICK_MOVE) {
            stackSize = mouseButton == 0 ? (stackSlot.getCount() + 1) / 2 : stackSlot.getCount() * 2;
        } else {
            stackSize = mouseButton == 0 ? stackSlot.getCount() - 1 : stackSlot.getCount() + 1;
        }
        stackSlot.setCount(Math.min(maxStackSize.getAsInt(), stackSize));
        setValue(stackSlot, true);
    }

    private void fillPhantomSlot(ItemStack stackHeld, int mouseButton) {
        if (stackHeld.isEmpty()) {
            setValue(ItemStack.EMPTY, true);
            return;
        }
        var stackSize = mouseButton == 0 ? stackHeld.getCount() : 1;
        setValue(stackHeld.copyWithCount(Math.min(maxStackSize.getAsInt(), stackSize)), true);
    }

    @Override
    public String name() {
        return "item-slot";
    }
}
