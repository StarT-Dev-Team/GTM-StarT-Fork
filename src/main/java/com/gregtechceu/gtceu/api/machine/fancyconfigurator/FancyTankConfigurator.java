package com.gregtechceu.gtceu.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;

import net.minecraft.network.chat.Component;

import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.List;

@Accessors(chain = true)
public class FancyTankConfigurator implements IFancyConfigurator {

    private final CustomFluidTank[] tanks;

    @Getter
    private final Component title;

    @Getter
    @Setter
    private List<Component> tooltips = Collections.emptyList();

    public FancyTankConfigurator(CustomFluidTank[] tanks, Component title) {
        this.tanks = tanks;
        this.title = title;
    }

    @Override
    public IGuiTexture getIcon() {
        return GuiTextures.BUTTON_FLUID_OUTPUT;
    }

    @Override
    public UIElement createConfigurator() {
        int rowSize = (int) Math.sqrt(tanks.length);
        int colSize = rowSize;
        if (tanks.length == 8) {
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
                var slot = new FluidSlot().bind(tanks[index++], 0);
                slot.style(style -> style.backgroundTexture(GuiTextures.FLUID_SLOT));
                row.addChild(slot);
            }
            container.addChild(row);
        }

        group.addChild(container);

        return group;
    }
}
