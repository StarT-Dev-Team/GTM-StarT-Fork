package com.gregtechceu.gtceu.api.gui.widget;

import com.gregtechceu.gtceu.utils.GTMath;

import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class LongInput extends NumberInput<Long> {

    public LongInput(Supplier<Long> valueSupplier, Consumer<Long> onChanged) {
        super(valueSupplier, onChanged);
    }

    @Override
    protected Long defaultMin() {
        return 0L;
    }

    @Override
    protected Long defaultMax() {
        return Long.MAX_VALUE;
    }

    @Override
    protected String toText(Long value) {
        return String.valueOf(value);
    }

    @Override
    protected Long fromText(String value) {
        return Long.parseLong(value);
    }

    @Override
    protected ChangeValues<Long> getChangeValues() {
        return new ChangeValues<>(1L, 8L, 64L, 512L);
    }

    @Override
    protected Long add(Long a, Long b) {
        return a + b;
    }

    @Override
    protected Long multiply(Long a, Long b) {
        return a * b;
    }

    @Override
    protected Long clamp(Long value, Long min, Long max) {
        return GTMath.clamp(value, min, max);
    }

    @Override
    protected void setTextFieldRange(TextField textField, Long min, Long max) {
        textField.setNumbersOnlyLong(min, max);
    }

    @Override
    protected Long getOne(boolean positive) {
        return positive ? 1L : -1L;
    }
}
