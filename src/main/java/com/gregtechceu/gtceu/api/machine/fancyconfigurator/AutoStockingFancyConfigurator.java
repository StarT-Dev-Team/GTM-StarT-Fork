package com.gregtechceu.gtceu.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.ae2.machine.MEStockingBusPartMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.feature.multiblock.IMEStockingPart;

import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.network.chat.Component;

public class AutoStockingFancyConfigurator extends MEPartFancyConfigurator {

    private final IMEStockingPart machine;

    public AutoStockingFancyConfigurator(IMEStockingPart machine) {
        super(machine);
        this.machine = machine;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gtceu.gui.adv_stocking_config.title");
    }

    @Override
    public Widget createConfigurator() {
        var group = new WidgetGroup(0, 0, 90, 70);

        String suffix = machine instanceof MEStockingBusPartMachine ? "min_item_count" : "min_fluid_count";

        group.addWidget(new LabelWidget(4, 2, "gtceu.gui.title.adv_stocking_config." + suffix));
        group.addWidget(new IntInputWidget(4, 12, 81, 14, machine::getMinStackSize,
                machine::setMinStackSize).setMin(1)
                .appendHoverTooltips(Component.translatable("gtceu.gui.adv_stocking_config." + suffix)));
        group.addWidget(new LabelWidget(4, 36, "gtceu.gui.title.adv_stocking_config.ticks_per_cycle"));
        group.addWidget(new IntInputWidget(4, 46, 81, 14, machine::getTicksPerCycle,
                machine::setTicksPerCycle).setMin(ConfigHolder.INSTANCE.compat.ae2.minUpdateIntervals)
                .setHoverTooltips(Component.translatable("gtceu.gui.adv_stocking_config.ticks_per_cycle")));

        return group;
    }
}
