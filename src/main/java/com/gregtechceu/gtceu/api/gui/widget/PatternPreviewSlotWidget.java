package com.gregtechceu.gtceu.api.gui.widget;

import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public class PatternPreviewSlotWidget extends ItemSlot {

    private static int countOffset(int count) {
        if (count / 100_000 != 0) return 9;
        if (count / 10_000 != 0) return 6;
        if (count / 1000 != 0) return 3;
        return 0;
    }

    @Override
    protected void drawItemStack(GUIContext guiContext, ItemStack itemStack) {
        var graphics = guiContext.graphics;
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 100);
        graphics.renderItem(itemStack, 0, 0);
        graphics.pose().translate(0, 0, 100);
        graphics.renderItemDecorations(Minecraft.getInstance().font, itemStack, countOffset(itemStack.getCount()), 0,
                null);
        graphics.pose().popPose();
    }

    @Override
    public String name() {
        return "item-slot";
    }
}
