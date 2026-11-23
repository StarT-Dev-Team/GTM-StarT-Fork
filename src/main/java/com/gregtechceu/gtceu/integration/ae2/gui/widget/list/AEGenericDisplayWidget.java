package com.gregtechceu.gtceu.integration.ae2.gui.widget.list;

import com.gregtechceu.gtceu.api.gui.GuiTextures;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import dev.vfyjxf.taffy.style.FlexDirection;
import org.jetbrains.annotations.Nullable;

public class AEGenericDisplayWidget extends UIElement {

    private final ItemSlot itemSlot = new ItemSlot();
    private final AEFluidDisplayWidget fluidSlot = new AEFluidDisplayWidget();
    private final Label amountLabel = new Label();

    public AEGenericDisplayWidget() {
        layout(layout -> layout.flexDirection(FlexDirection.ROW).width(18 + 140).height(18));

        itemSlot.style(style -> style.backgroundTexture(GuiTextures.SLOT));
        fluidSlot.setDisplay(false);

        var number = new UIElement();
        number.layout(layout -> layout.width(140).height(18).paddingHorizontal(4));
        number.style(style -> style.backgroundTexture(GuiTextures.NUMBER_BACKGROUND));
        amountLabel.layout(layout -> layout.widthPercent(100).heightPercent(100));
        amountLabel.textStyle(style -> style.textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER).textColor(-1));
        number.addChild(amountLabel);

        addChildren(itemSlot, fluidSlot, number);
    }

    public void update(@Nullable GenericStack stack) {
        var isFluid = stack != null && stack.what() instanceof AEFluidKey;
        itemSlot.setDisplay(!isFluid);
        fluidSlot.setDisplay(isFluid);

        if (stack == null) {
            itemSlot.setItem(ItemStack.EMPTY, false);
            fluidSlot.setStack(null);
            amountLabel.setText(Component.empty());
            return;
        }

        if (isFluid) {
            fluidSlot.setStack(stack);
            itemSlot.setItem(ItemStack.EMPTY, false);
        } else {
            fluidSlot.setStack(null);
            var displayed = stack.what() instanceof AEItemKey key ? key.toStack() : ItemStack.EMPTY;
            itemSlot.setItem(displayed, false);
        }

        var locale = Minecraft.getInstance().getLanguageManager().getJavaLocale();
        amountLabel.setText(Component.literal(String.format(locale, "x%,d", stack.amount())));
    }
}
