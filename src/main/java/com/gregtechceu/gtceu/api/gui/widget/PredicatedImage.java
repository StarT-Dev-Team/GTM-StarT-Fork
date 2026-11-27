package com.gregtechceu.gtceu.api.gui.widget;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue;

import java.util.function.BooleanSupplier;

public class PredicatedImage extends UIElement {

    public PredicatedImage(IGuiTexture texture, BooleanSupplier predicate) {
        style(style -> style.backgroundTexture(texture));
        setDisplay(predicate.getAsBoolean());
        addChild(new BindableValue<Boolean>().bind(DataBindingBuilder.boolS2C(predicate::getAsBoolean)
                .remoteSetter(this::setDisplay).build()));
    }
}
