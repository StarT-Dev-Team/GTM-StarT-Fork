package com.gregtechceu.gtceu.integration.ae2.gui.widget;

import net.minecraft.network.chat.Component;

import appeng.api.stacks.AEKey;
import appeng.client.gui.MathExpressionParser;
import appeng.core.localization.GuiText;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.OptionalLong;

public final class AmountEntryModel {

    public static final long[] STEPS_1000 = { 1, 10, 100, 1000 };
    public static final long[] STEPS_64 = { 1, 16, 32, 64 };

    private static final long MIN_VALUE = 1;
    private static final long MAX_VALUE = Integer.MAX_VALUE;

    private final DecimalFormat decimalFormat;
    private final DecimalFormat parseFormat;

    @Getter
    private int amountPerUnit = 1;

    public AmountEntryModel(Locale locale) {
        var symbols = DecimalFormatSymbols.getInstance(locale);

        this.decimalFormat = new DecimalFormat("#.######", symbols);
        this.parseFormat = new DecimalFormat("#,##0.######", symbols);

        this.decimalFormat.setNegativePrefix("-");
        this.parseFormat.setParseBigDecimal(true);
        this.parseFormat.setNegativePrefix("-");
    }

    public void setKey(@Nullable AEKey key) {
        this.amountPerUnit = key == null ? 1 : key.getAmountPerUnit();
    }

    public String format(long external) {
        return this.decimalFormat.format(external / (double) this.amountPerUnit);
    }

    private double parse(String text) {
        if (text.startsWith("=")) {
            text = text.substring(1);
        }

        return MathExpressionParser.parse(text, this.parseFormat).map(BigDecimal::doubleValue).orElse(Double.NaN);
    }

    @Nullable
    public Component error(String text) {
        double value = parse(text);

        if (Double.isNaN(value)) {
            return GuiText.InvalidNumber.text();
        }

        if (this.amountPerUnit == 1 && value != Math.floor(value)) {
            return GuiText.NumberNonInteger.text();
        }

        long external = toExternal(value);

        if (external < MIN_VALUE) {
            return GuiText.NumberLessThanMinValue.text(format(MIN_VALUE));
        }

        if (external > MAX_VALUE) {
            return GuiText.NumberGreaterThanMaxValue.text(format(MAX_VALUE));
        }

        return null;
    }

    public OptionalLong external(String text) {
        if (error(text) != null) return OptionalLong.empty();

        return OptionalLong.of(toExternal(parse(text)));
    }

    public String add(String text, long delta) {
        double current = parse(text);

        if (Double.isNaN(current)) current = 0;

        double value = current + delta;
        double minimum = MIN_VALUE / (double) this.amountPerUnit;
        double maximum = MAX_VALUE / (double) this.amountPerUnit;

        if (value < minimum) {
            value = minimum;
        } else if (value > maximum) {
            value = maximum;
        } else if (current == 1 && delta > 0 && (delta % 10 == 0 || delta % 16 == 0)) {
            value = value - 1;
        }

        return this.decimalFormat.format(value);
    }

    private long toExternal(double internal) {
        return (long) Math.ceil(internal * this.amountPerUnit);
    }
}
