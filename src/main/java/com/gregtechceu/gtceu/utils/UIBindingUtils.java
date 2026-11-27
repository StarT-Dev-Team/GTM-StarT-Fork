package com.gregtechceu.gtceu.utils;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.SyncStrategy;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class UIBindingUtils {

    public static <T> BindableValue<T> s2c(Class<T> clz, Supplier<T> getter, Consumer<T> remoteSetter) {
        var binding = DataBindingBuilder
                .create(getter, x -> {})
                .syncType(clz)
                .c2sStrategy(SyncStrategy.NONE)
                .remoteSetter(remoteSetter)
                .build();
        var bindable = new BindableValue<T>();
        bindable.bind(binding);
        return bindable;
    }

    public static BindableValue<Integer> intS2C(Supplier<Integer> getter, Consumer<Integer> remoteSetter) {
        return s2c(Integer.class, getter, remoteSetter);
    }
}
