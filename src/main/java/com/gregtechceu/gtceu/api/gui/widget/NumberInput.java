package com.gregtechceu.gtceu.api.gui.widget;

import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SimpleBinding;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class NumberInput<T extends Number> extends UIElement {

    protected abstract T defaultMin();

    protected abstract T defaultMax();

    protected abstract String toText(T value);

    protected abstract T fromText(String value);

    protected record ChangeValues<T extends Number>(T regular, T shift, T ctrl, T ctrlShift) {}

    protected abstract ChangeValues<T> getChangeValues();

    protected abstract T add(T a, T b);

    protected abstract T multiply(T a, T b);

    protected abstract T clamp(T value, T min, T max);

    protected abstract void setTextFieldRange(TextField textField, T min, T max);

    protected abstract T getOne(boolean positive);

    private final ChangeValues<T> changeValues = getChangeValues();
    private final T onePositive = getOne(true);
    private final T oneNegative = getOne(false);

    @Getter
    private Supplier<T> valueSupplier;
    @Getter
    private T min = defaultMin();
    @Getter
    private T max = defaultMax();

    private Consumer<T> onChanged;
    @Nullable
    private SimpleBinding<String> binding;
    private final TextField textField = new TextField();
    private final Button decrease = new Button();
    private final Button increase = new Button();

    public NumberInput(Supplier<T> valueSupplier, Consumer<T> onChanged) {
        this.valueSupplier = valueSupplier;
        this.onChanged = onChanged;
        layout(layout -> layout.width(100).height(20).flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER)
                .gapAll(2));

        setupButton(decrease, "-", oneNegative);
        setupButton(increase, "+", onePositive);

        textField.layout(layout -> layout.flex(1).heightPercent(100).paddingHorizontal(6));
        bindText();
        updateTextFieldRange();

        addChildren(decrease, textField, increase);
    }

    private void bindText() {
        if (binding != null) {
            textField.unbind(binding);
        }
        binding = DataBindingBuilder.string(() -> toText(valueSupplier.get()), this::onTextChanged).build();
        textField.bind(binding);
    }

    public NumberInput<T> rebind(Supplier<T> valueSupplier, Consumer<T> onChanged) {
        this.valueSupplier = valueSupplier;
        this.onChanged = onChanged;
        bindText();
        return this;
    }

    private void setupButton(Button button, String prefix, T multiplier) {
        button.layout(layout -> layout.width(18).heightPercent(100));
        button.setText(prefix + toText(changeValues.regular()), false);
        button.setOnServerClick(event -> changeValue(event, multiplier));
        button.addEventListener(UIEvents.TICK, event -> {
            var amount = GTUtil.isCtrlDown() ?
                    GTUtil.isShiftDown() ? changeValues.ctrlShift() : changeValues.ctrl() :
                    GTUtil.isShiftDown() ? changeValues.shift() : changeValues.regular();
            button.setText(prefix + toText(amount), false);
        });
        button.style(style -> style.tooltips("gui.widget.incrementButton.default_tooltip"));
    }

    private void onTextChanged(String text) {
        if (LDLib2.isRemote()) return;
        try {
            setValue(clamp(fromText(text), min, max));
        } catch (NumberFormatException ignored) {}
    }

    private void changeValue(UIEvent event, T multiplier) {
        var amount = event.isCtrlDown() ?
                event.isShiftDown() ? changeValues.ctrlShift() : changeValues.ctrl() :
                event.isShiftDown() ? changeValues.shift() : changeValues.regular();
        setValue(clamp(add(valueSupplier.get(), multiply(amount, multiplier)), min, max));
    }

    public NumberInput<T> setMin(T min) {
        this.min = min;
        updateTextFieldRange();
        return this;
    }

    public NumberInput<T> setMax(T max) {
        this.max = max;
        updateTextFieldRange();
        return this;
    }

    public NumberInput<T> setValue(T value) {
        onChanged.accept(value);
        return this;
    }

    public NumberInput<T> setTooltips(String... tooltips) {
        textField.style(style -> style.tooltips(tooltips));
        return this;
    }

    protected void updateTextFieldRange() {
        setTextFieldRange(textField, min, max);
        if (!LDLib2.isRemote()) {
            setValue(clamp(valueSupplier.get(), min, max));
        }
    }
}
