package com.gregtechceu.gtceu.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;

import net.minecraft.network.chat.Component;

import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.List;

@Accessors(chain = true)
public class FancyInvConfigurator implements IFancyConfigurator {

    private final CustomItemStackHandler inventory;

    @Getter
    private final Component title;

    @Getter
    @Setter
    private List<Component> tooltips = Collections.emptyList();

    public FancyInvConfigurator(CustomItemStackHandler inventory, Component title) {
        this.inventory = inventory;
        this.title = title;
    }

    @Override
    public IGuiTexture getIcon() {
        return GuiTextures.BUTTON_ITEM_OUTPUT;
    }

    @Override
    public UIElement createConfigurator() {
        int rowSize = (int) Math.sqrt(inventory.getSlots());
        int colSize = rowSize;
        if (inventory.getSlots() == 8) {
            rowSize = 4;
            colSize = 2;
        }
        var group = new UIElement().layout(layout -> layout.paddingAll(4));
        var container = new UIElement().layout(layout -> layout.paddingAll(4));
        container.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND_INVERSE));
        int index = 0;
        for (int y = 0; y < colSize; y++) {
            var row = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW));
            for (int x = 0; x < rowSize; x++) {
                var slot = new ItemSlot().bind(inventory, index++);
                slot.style(style -> style.backgroundTexture(GuiTextures.SLOT));
                slot.xeiRecipeIngredient(IngredientIO.INPUT);
                row.addChild(slot);
            }
            container.addChild(row);
        }

        group.addChild(container);

        return group;
    }
}
