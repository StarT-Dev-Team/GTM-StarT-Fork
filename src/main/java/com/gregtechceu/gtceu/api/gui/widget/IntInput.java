package com.gregtechceu.gtceu.api.gui.widget;

import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;

import net.minecraft.util.Mth;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class IntInput extends NumberInput<Integer> {

    public IntInput(Supplier<Integer> valueSupplier, Consumer<Integer> onChanged) {
        super(valueSupplier, onChanged);
    }

    @Override
    protected Integer defaultMin() {
        return 0;
    }

    @Override
    protected Integer defaultMax() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected String toText(Integer value) {
        return String.valueOf(value);
    }

    @Override
    protected Integer fromText(String value) {
        return Integer.parseInt(value);
    }

    @Override
    protected ChangeValues<Integer> getChangeValues() {
        return new ChangeValues<>(1, 8, 64, 512);
    }

    @Override
    protected Integer getOne(boolean positive) {
        return positive ? 1 : -1;
    }

    @Override
    protected Integer add(Integer a, Integer b) {
        return a + b;
    }

    @Override
    protected Integer multiply(Integer a, Integer b) {
        return a * b;
    }

    @Override
    protected Integer clamp(Integer value, Integer min, Integer max) {
        return Mth.clamp(value, min, max);
    }

    @Override
    protected void setTextFieldRange(TextField textField, Integer min, Integer max) {
        textField.setNumbersOnlyInt(min, max);
    }
}
