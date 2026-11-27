package com.gregtechceu.gtceu.integration.jei.oreprocessing;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.integration.xei.GTXEIUI;
import com.gregtechceu.gtceu.integration.xei.widgets.GTOreByProductWidget;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

public class GTOreProcessingInfoWrapper {

    public final Material material;

    public GTOreProcessingInfoWrapper(Material mat) {
        this.material = mat;
    }

    public ModularUI createModularUI() {
        return GTXEIUI.of(new GTOreByProductWidget(material));
    }
}
