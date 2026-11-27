package com.gregtechceu.gtceu.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator;
import com.gregtechceu.gtceu.api.gui.widget.IntInput;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.ae2.machine.MEHatchPartMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.feature.multiblock.IMEPart;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;

import net.minecraft.network.chat.Component;

public class MEPartFancyConfigurator implements IFancyConfigurator {

    private final IMEPart machine;

    public MEPartFancyConfigurator(IMEPart machine) {
        this.machine = machine;
    }

    @Override
    public Component getTitle() {
        if (machine instanceof MEHatchPartMachine) {
            return Component.translatable("gtceu.gui.me_hatch_config.title");
        }
        return Component.translatable("gtceu.gui.me_bus_config.title");
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(GTItems.TOOL_DATA_STICK.asStack());
    }

    @Override
    public UIElement createConfigurator() {
        var group = new UIElement().layout(layout -> layout.minWidth(90).paddingAll(4).gapAll(2));

        group.addChild(new Label().setText(Component.translatable("gtceu.gui.title.me_config.ticks_per_cycle")));
        group.addChild(new IntInput(machine::getTicksPerCycle, machine::setTicksPerCycle)
                .setMin(ConfigHolder.INSTANCE.compat.ae2.minUpdateIntervals)
                .setTooltips("gtceu.gui.me_config.ticks_per_cycle")
                .layout(layout -> layout.width(81).height(14)));
        return group;
    }
}
