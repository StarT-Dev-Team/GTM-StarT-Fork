package com.gregtechceu.gtceu.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.ae2.machine.MEHatchPartMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.feature.multiblock.IMEPart;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

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
    public Widget createConfigurator() {
        var group = new WidgetGroup(0, 0, 90, 35);

        group.addWidget(new LabelWidget(4, 2, "gtceu.gui.title.me_config.ticks_per_cycle"));
        group.addWidget(new IntInputWidget(4, 12, 81, 14, machine::getTicksPerCycle,
                machine::setTicksPerCycle).setMin(ConfigHolder.INSTANCE.compat.ae2.minUpdateIntervals)
                .setHoverTooltips(Component.translatable("gtceu.gui.me_config.ticks_per_cycle")));
        return group;
    }
}
