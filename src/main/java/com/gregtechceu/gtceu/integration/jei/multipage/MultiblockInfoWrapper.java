package com.gregtechceu.gtceu.integration.jei.multipage;

import com.gregtechceu.gtceu.api.gui.widget.PatternPreviewWidget;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.integration.xei.GTXEIUI;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

public class MultiblockInfoWrapper {

    public final MultiblockMachineDefinition definition;

    public MultiblockInfoWrapper(MultiblockMachineDefinition definition) {
        this.definition = definition;
    }

    public ModularUI createModularUI() {
        return GTXEIUI.of(PatternPreviewWidget.getPatternWidget(definition));
    }
}
