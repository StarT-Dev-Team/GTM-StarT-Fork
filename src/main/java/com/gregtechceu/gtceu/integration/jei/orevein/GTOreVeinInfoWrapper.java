package com.gregtechceu.gtceu.integration.jei.orevein;

import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.integration.xei.GTXEIUI;
import com.gregtechceu.gtceu.integration.xei.widgets.GTOreVeinWidget;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

public class GTOreVeinInfoWrapper {

    public final GTOreDefinition oreDefinition;

    public GTOreVeinInfoWrapper(GTOreDefinition oreDefinition) {
        this.oreDefinition = oreDefinition;
    }

    public ModularUI createModularUI() {
        return GTXEIUI.of(new GTOreVeinWidget(oreDefinition));
    }
}
