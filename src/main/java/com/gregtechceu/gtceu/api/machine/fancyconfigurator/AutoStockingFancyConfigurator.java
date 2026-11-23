package com.gregtechceu.gtceu.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.widget.IntInput;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.ae2.machine.MEStockingBusPartMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.feature.multiblock.IMEStockingPart;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;

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
    public UIElement createConfigurator() {
        var group = new UIElement().layout(layout -> layout.minWidth(90).paddingAll(4).gapAll(2));

        String suffix = machine instanceof MEStockingBusPartMachine ? "min_item_count" : "min_fluid_count";

        group.addChild(new Label().setText(Component.translatable("gtceu.gui.title.adv_stocking_config." + suffix)));
        group.addChild(new IntInput(machine::getMinStackSize, machine::setMinStackSize)
                .setMin(1)
                .setTooltips("gtceu.gui.adv_stocking_config." + suffix)
                .layout(layout -> layout.width(81).height(14)));
        group.addChild(new Label()
                .setText(Component.translatable("gtceu.gui.title.adv_stocking_config.ticks_per_cycle")));
        group.addChild(new IntInput(machine::getTicksPerCycle, machine::setTicksPerCycle)
                .setMin(ConfigHolder.INSTANCE.compat.ae2.minUpdateIntervals)
                .setTooltips("gtceu.gui.adv_stocking_config.ticks_per_cycle")
                .layout(layout -> layout.width(81).height(14)));

        return group;
    }
}
