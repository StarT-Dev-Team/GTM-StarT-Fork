package com.gregtechceu.gtceu.api.gui.editor;

import com.gregtechceu.gtceu.api.gui.WidgetUtils;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import lombok.Getter;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class EditableUI<W extends UIElement, T> implements IEditableUI<W, T> {

    @Getter
    final String id;
    final Class<W> clazz;
    @Getter
    final Supplier<W> creator;
    @Getter
    final BiConsumer<W, T> binder;

    public EditableUI(String id, Class<W> clazz, Supplier<W> creator, BiConsumer<W, T> binder) {
        this.id = id;
        this.clazz = clazz;
        this.creator = creator;
        this.binder = binder;
    }

    @Override
    public W createDefault() {
        var element = creator.get();
        element.setId(id);
        return element;
    }

    @Override
    public void setupUI(UIElement template, T instance) {
        WidgetUtils.widgetByIdForEach(template, "^" + id + "$", clazz, element -> binder.accept(element, instance));
    }
}
