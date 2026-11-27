package com.gregtechceu.gtceu.api.gui.widget;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;

import java.util.function.BooleanSupplier;

public class PredicatedButton extends Button {

    public PredicatedButton(BooleanSupplier predicate) {
        noText();
        setDisplay(predicate.getAsBoolean());
        addChild(new BindableValue<Boolean>().bind(DataBindingBuilder.boolS2C(predicate::getAsBoolean)
                .remoteSetter(this::setDisplay).build()));
    }
}
