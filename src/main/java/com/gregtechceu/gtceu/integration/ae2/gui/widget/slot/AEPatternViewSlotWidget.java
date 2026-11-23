package com.gregtechceu.gtceu.integration.ae2.gui.widget.slot;

import com.gregtechceu.gtceu.api.gui.GuiTextures;

import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import appeng.crafting.pattern.EncodedPatternItem;

public class AEPatternViewSlotWidget extends ItemSlot {

    public AEPatternViewSlotWidget(IItemHandlerModifiable itemHandler, int slotIndex, Runnable changeListener) {
        super();
        style(style -> style.backgroundTexture(GuiTextures.SLOT));
        slotStyle(style -> style.slotOverlay(GuiTextures.PATTERN_OVERLAY).showSlotOverlayOnlyEmpty(true));
        bind(new ItemHandlerSlot(itemHandler, slotIndex).addChangeListener(changeListener));
    }

    @Override
    protected void drawItemStack(GUIContext guiContext, ItemStack itemStack) {
        var displayed = itemStack;
        if (!itemStack.isEmpty() && itemStack.getItem() instanceof EncodedPatternItem pattern) {
            var output = pattern.getOutput(itemStack);
            if (!output.isEmpty()) {
                displayed = output;
            }
        }
        DrawerHelper.drawItemStack(guiContext.graphics, displayed, 0, 0, guiContext.elementColor,
                compactCount(displayed));
    }
}
