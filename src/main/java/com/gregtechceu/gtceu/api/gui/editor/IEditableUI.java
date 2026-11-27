package com.gregtechceu.gtceu.api.gui.editor;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public interface IEditableUI<W extends UIElement, T> {

    W createDefault();

    void setupUI(UIElement template, T instance);

    record Normal<A extends UIElement, B>(Supplier<A> supplier, BiConsumer<UIElement, B> binder)
            implements IEditableUI<A, B> {

        @Override
        public A createDefault() {
            return supplier.get();
        }

        @Override
        public void setupUI(UIElement template, B instance) {
            binder.accept(template, instance);
        }
    }
}
