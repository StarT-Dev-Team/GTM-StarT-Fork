package com.gregtechceu.gtceu.common.cover.voiding;

import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.filter.ItemFilter;
import com.gregtechceu.gtceu.api.cover.filter.SimpleItemFilter;
import com.gregtechceu.gtceu.api.gui.widget.EnumSelector;
import com.gregtechceu.gtceu.api.gui.widget.IntInput;
import com.gregtechceu.gtceu.common.cover.data.VoidingMode;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class AdvancedItemVoidingCover extends ItemVoidingCover {

    @Persisted
    @DescSynced
    @Getter
    private VoidingMode voidingMode = VoidingMode.VOID_ANY;

    @Persisted
    @Getter
    protected int globalVoidingLimit = 1;

    private IntInput stackSizeInput;
    private VoidingMode appliedMode;

    public AdvancedItemVoidingCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
    }

    //////////////////////////////////////////////
    // *********** COVER LOGIC ***********//
    //////////////////////////////////////////////

    @Override
    protected void doVoidItems() {
        IItemHandler handler = getOwnItemHandler();
        if (handler == null) {
            return;
        }

        switch (voidingMode) {
            case VOID_ANY -> voidAny(handler);
            case VOID_OVERFLOW -> voidOverflow(handler);
        }
    }

    private void voidOverflow(IItemHandler handler) {
        Map<ItemStack, TypeItemInfo> sourceItemAmounts = countInventoryItemsByType(handler);

        for (TypeItemInfo itemInfo : sourceItemAmounts.values()) {
            int itemToVoidAmount = itemInfo.totalCount - getFilteredItemAmount(itemInfo.itemStack);

            if (itemToVoidAmount <= 0) {
                continue;
            }

            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack is = handler.getStackInSlot(slot);
                if (!is.isEmpty() && GTUtil.isSameItemSameTags(is, itemInfo.itemStack)) {
                    ItemStack extracted = handler.extractItem(slot, itemToVoidAmount, false);

                    if (!extracted.isEmpty()) {
                        itemToVoidAmount -= extracted.getCount();
                    }
                }
                if (itemToVoidAmount == 0) {
                    break;
                }
            }
        }
    }

    private int getFilteredItemAmount(ItemStack itemStack) {
        if (!filterHandler.isFilterPresent())
            return globalVoidingLimit;

        ItemFilter filter = filterHandler.getFilter();
        return filter.isBlackList() ? globalVoidingLimit : filter.testItemCount(itemStack);
    }

    public void setVoidingMode(VoidingMode voidingMode) {
        this.voidingMode = voidingMode;

        configureStackSizeInput();

        if (!this.isRemote()) {
            configureFilter();
        }
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////

    @Override
    protected @NotNull String getUITitle() {
        return "cover.item.voiding.advanced.title";
    }

    @Override
    protected void buildAdditionalUI(UIElement group) {
        this.stackSizeInput = new IntInput(() -> globalVoidingLimit, val -> globalVoidingLimit = val);
        this.stackSizeInput.layout(layout -> layout.flex(1).height(20));
        this.stackSizeInput.addEventListener(UIEvents.TICK, event -> {
            if (appliedMode != voidingMode) {
                configureStackSizeInput();
            }
        });
        configureStackSizeInput();

        group.addChild(this.stackSizeInput);
        group.addChild(new EnumSelector<>(VoidingMode.values(), () -> voidingMode, this::setVoidingMode));
    }

    @Override
    protected void configureFilter() {
        if (filterHandler.getFilter() instanceof SimpleItemFilter filter) {
            filter.setMaxStackSize(this.voidingMode.maxStackSize);
        }

        configureStackSizeInput();
    }

    private void configureStackSizeInput() {
        if (this.stackSizeInput == null)
            return;

        this.appliedMode = this.voidingMode;
        this.stackSizeInput.setDisplay(shouldShowStackSize());
        this.stackSizeInput.setMin(1);
        this.stackSizeInput.setMax(this.voidingMode.maxStackSize);
    }

    private boolean shouldShowStackSize() {
        if (this.voidingMode == VoidingMode.VOID_ANY)
            return false;

        if (!this.filterHandler.isFilterPresent())
            return true;

        return this.filterHandler.getFilter().isBlackList();
    }

    //////////////////////////////////////
    // ***** LDLib SyncData ******//
    //////////////////////////////////////
}
