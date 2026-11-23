package com.gregtechceu.gtceu.integration.xei.widgets;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;

import dev.vfyjxf.taffy.style.TaffyPosition;

public class GTProgrammedCircuitWidget extends UIElement {

    public GTProgrammedCircuitWidget() {
        layout(layout -> layout.width(150).height(80));
        setRecipe();
    }

    public void setRecipe() {
        var frame = new UIElement();
        frame.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(39).top(0).width(36).height(36));
        frame.style(style -> style.backgroundTexture(GuiTextures.SLOT));
        addChild(frame);

        for (int j = 0; j < 4; j++) {
            for (int i = 0; i < 8; i++) {
                var index = i + j * 8;
                var io = index == 31 ? IngredientIO.OUTPUT : IngredientIO.NONE;
                var slot = new ItemSlot().setItem(IntCircuitBehaviour.stack(1 + index));
                slot.xeiRecipeIngredient(io).xeiRecipeSlot(io, 1);
                var x = 3 + 18 * i;
                var y = 18 * j;
                slot.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(x).top(y));
                addChild(slot);
            }
        }
    }
}
