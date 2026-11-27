package com.gregtechceu.gtceu.integration.jei.orevein;

import com.gregtechceu.gtceu.api.data.worldgen.bedrockore.BedrockOreDefinition;
import com.gregtechceu.gtceu.integration.xei.GTXEIUI;
import com.gregtechceu.gtceu.integration.xei.widgets.GTOreVeinWidget;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

public class GTBedrockOreInfoWrapper {

    public final BedrockOreDefinition bedrockOre;

    public GTBedrockOreInfoWrapper(BedrockOreDefinition bedrockOre) {
        this.bedrockOre = bedrockOre;
    }

    public ModularUI createModularUI() {
        return GTXEIUI.of(new GTOreVeinWidget(bedrockOre));
    }
}
