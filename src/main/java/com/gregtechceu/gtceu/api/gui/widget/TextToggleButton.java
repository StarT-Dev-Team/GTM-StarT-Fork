package com.gregtechceu.gtceu.api.gui.widget;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;

import net.minecraft.network.chat.Component;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;

import java.util.function.BooleanSupplier;

public class TextToggleButton extends Button {

    public TextToggleButton(String onKey, String offKey, BooleanSupplier state, BooleanConsumer setter) {
        setOnServerClick(event -> setter.accept(!state.getAsBoolean()));
        setText(Component.translatable(state.getAsBoolean() ? onKey : offKey));
        addChild(new BindableValue<Boolean>().bind(DataBindingBuilder.boolS2C(state::getAsBoolean)
                .remoteSetter(value -> setText(Component.translatable(value ? onKey : offKey))).build()));
    }
}
