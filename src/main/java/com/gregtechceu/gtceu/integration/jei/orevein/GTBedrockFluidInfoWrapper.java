package com.gregtechceu.gtceu.integration.jei.orevein;

import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidDefinition;
import com.gregtechceu.gtceu.integration.xei.GTXEIUI;
import com.gregtechceu.gtceu.integration.xei.widgets.GTOreVeinWidget;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

public class GTBedrockFluidInfoWrapper {

    public final BedrockFluidDefinition fluid;

    public GTBedrockFluidInfoWrapper(BedrockFluidDefinition fluid) {
        this.fluid = fluid;
    }

    public ModularUI createModularUI() {
        return GTXEIUI.of(new GTOreVeinWidget(fluid));
    }
}
