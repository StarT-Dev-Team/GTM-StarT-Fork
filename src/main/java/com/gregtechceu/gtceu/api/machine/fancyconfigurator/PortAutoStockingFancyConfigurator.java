package com.gregtechceu.gtceu.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.ae2.machine.MEStockingInputPortPartMachine;

import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.network.chat.Component;

public class PortAutoStockingFancyConfigurator extends MEPartFancyConfigurator {

    private final MEStockingInputPortPartMachine machine;

    public PortAutoStockingFancyConfigurator(MEStockingInputPortPartMachine machine) {
        super(machine);

        this.machine = machine;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gtceu.gui.adv_stocking_config.title");
    }

    @Override
    public Widget createConfigurator() {
        var group = new WidgetGroup(0, 0, 90, 100);

        group.addWidget(new LabelWidget(4, 2, "gtceu.gui.title.adv_stocking_config.min_item_count"));
        group.addWidget(new IntInputWidget(4, 12, 81, 14, machine::getMinStackSize,
                machine::setMinStackSize).setMin(1)
                .appendHoverTooltips(Component.translatable("gtceu.gui.adv_stocking_config.min_item_count")));
        group.addWidget(new LabelWidget(4, 32, "gtceu.gui.title.adv_stocking_config.min_fluid_count"));
        group.addWidget(new IntInputWidget(4, 42, 81, 14, machine::getMinFluidStackSize,
                machine::setMinFluidStackSize).setMin(1)
                .appendHoverTooltips(Component.translatable("gtceu.gui.adv_stocking_config.min_fluid_count")));
        group.addWidget(new LabelWidget(4, 66, "gtceu.gui.title.adv_stocking_config.ticks_per_cycle"));
        group.addWidget(new IntInputWidget(4, 76, 81, 14, machine::getTicksPerCycle,
                machine::setTicksPerCycle).setMin(ConfigHolder.INSTANCE.compat.ae2.minUpdateIntervals)
                .setHoverTooltips(Component.translatable("gtceu.gui.adv_stocking_config.ticks_per_cycle")));

        return group;
    }
}
