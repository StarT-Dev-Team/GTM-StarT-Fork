package com.gregtechceu.gtceu.integration.xei;

import com.gregtechceu.gtceu.api.gui.UITemplate;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

public class GTXEIUI {

    public static ModularUI of(UIElement content) {
        var root = new UIElement();
        root.layout(layout -> layout.widthPercent(100).heightPercent(100));
        root.addChild(content);
        return ModularUI.of(UITemplate.ui(root));
    }
}
