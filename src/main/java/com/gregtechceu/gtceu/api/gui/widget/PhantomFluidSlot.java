package com.gregtechceu.gtceu.api.gui.widget;

import com.gregtechceu.gtceu.api.transfer.fluid.IFluidHandlerModifiable;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class PhantomFluidSlot extends FluidSlot {

    public PhantomFluidSlot(Supplier<FluidStack> getter, Consumer<FluidStack> setter) {
        setAllowClickFilled(false);
        setAllowClickDrained(false);
        slotStyle(style -> style.fillDirection(FillDirection.ALWAYS_FULL));
        bind(DataBindingBuilder.fluidStack(getter, setter).build());
        xeiPhantom();
    }

    public PhantomFluidSlot scrollable(IFluidHandlerModifiable tank, int tankIndex) {
        addServerEventListener(UIEvents.MOUSE_WHEEL, event -> scroll(tank, tankIndex, event));
        return this;
    }

    private static void scroll(IFluidHandlerModifiable tank, int tankIndex, UIEvent event) {
        var fluid = tank.getFluidInTank(tankIndex);
        if (fluid.isEmpty()) return;
        var delta = event.deltaY > 0 ? 1 : -1;
        if (event.isShiftDown()) delta *= 10;
        if (event.isCtrlDown()) delta *= 100;
        if (!event.isAltDown()) delta *= 1000;
        var copy = fluid.copy();
        copy.setAmount(Math.max(0, Math.min(tank.getTankCapacity(tankIndex), fluid.getAmount() + delta)));
        tank.setFluidInTank(tankIndex, copy.getAmount() <= 0 ? FluidStack.EMPTY : copy);
    }

    @Override
    protected void onMouseDown(UIEvent event) {
        if (!isActiveInHierarchy()) return;
        var modularUI = getModularUI();
        if (modularUI == null) return;
        var menu = modularUI.getMenu();
        var carried = menu == null ? ItemStack.EMPTY : menu.getCarried();
        var fluid = FluidUtil.getFluidContained(carried)
                .map(contained -> new FluidStack(contained, FluidType.BUCKET_VOLUME))
                .orElse(FluidStack.EMPTY);
        setValue(fluid, true);
    }

    public static FluidStack drainFrom(@Nullable Object ingredient) {
        if (ingredient instanceof Ingredient ing) {
            var items = ing.getItems();
            if (items.length > 0) {
                ingredient = items[0];
            }
        }
        if (ingredient instanceof ItemStack itemStack) {
            return FluidUtil.getFluidHandler(itemStack)
                    .map(handler -> handler.drain(Integer.MAX_VALUE, FluidAction.SIMULATE))
                    .orElse(FluidStack.EMPTY);
        }
        return FluidStack.EMPTY;
    }

    @Override
    public String name() {
        return "fluid-slot";
    }
}
