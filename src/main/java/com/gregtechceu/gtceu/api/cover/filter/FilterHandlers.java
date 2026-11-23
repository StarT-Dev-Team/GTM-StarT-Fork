package com.gregtechceu.gtceu.api.cover.filter;

import com.lowdragmc.lowdraglib2.syncdata.IManaged;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public interface FilterHandlers {

    static FilterHandler<ItemStack, ItemFilter> item(IManaged container) {
        return new FilterHandler<>(container) {

            @Override
            protected ItemFilter loadFilter(ItemStack filterItem) {
                return ItemFilter.loadFilter(filterItem);
            }

            @Override
            protected ItemFilter getEmptyFilter() {
                return ItemFilter.EMPTY;
            }

            @Override
            protected boolean canInsertFilterItem(ItemStack itemStack) {
                return ItemFilter.FILTERS.containsKey(itemStack.getItem());
            }
        };
    }

    static FilterHandler<FluidStack, FluidFilter> fluid(IManaged container) {
        return new FilterHandler<>(container) {

            @Override
            protected FluidFilter loadFilter(ItemStack filterItem) {
                return FluidFilter.loadFilter(filterItem);
            }

            @Override
            protected FluidFilter getEmptyFilter() {
                return FluidFilter.EMPTY;
            }

            @Override
            protected boolean canInsertFilterItem(ItemStack itemStack) {
                return FluidFilter.FILTERS.containsKey(itemStack.getItem());
            }
        };
    }
}
