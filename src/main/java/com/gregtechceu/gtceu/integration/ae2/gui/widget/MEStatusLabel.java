package com.gregtechceu.gtceu.integration.ae2.gui.widget;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;

import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

public class MEStatusLabel extends Label {

    public MEStatusLabel(BooleanSupplier online) {
        textStyle(style -> style.textColor(0xFF404040).textShadow(false));
        bindDataSource(SupplierDataSource.of(() -> Component.translatable(online.getAsBoolean() ?
                "gtceu.gui.me_network.online" : "gtceu.gui.me_network.offline")));
    }
}
