package com.gregtechceu.gtceu.api.recipe.ingredient;

import com.gregtechceu.gtceu.common.valueprovider.StatisticalInt;
import com.gregtechceu.gtceu.common.valueprovider.WeightedInt;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.valueproviders.IntProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Describes how a weighted ranged amount is shown in the recipe widget and its tooltips. Everything takes the tier
 * difference of the displayed OC tier (see {@code ChanceBoostFunction#getTierDiff}), so it follows the OC like the
 * chance does.
 */
public final class RangedDisplay {

    public static final int MAX_LISTED_AMOUNTS = 12;
    private static final double EPSILON = 1e-9;

    /** Which way the displayed OC tier moved the average */
    public enum Shift {
        NONE,
        UP,
        DOWN
    }

    /** The average at the displayed tier, as drawn on the recipe slot */
    public record SlotAmount(double mean, Shift shift) {}

    private RangedDisplay() {}

    public static Optional<SlotAmount> slotAmount(IntProvider base, int tierDiff) {
        if (!(base instanceof StatisticalInt statistical)) return Optional.empty();

        double baseMean = statistical.mean();
        double mean = statistical.withTierDiff(tierDiff).mean();
        Shift shift = mean > baseMean + EPSILON ? Shift.UP : mean < baseMean - EPSILON ? Shift.DOWN : Shift.NONE;

        return Optional.of(new SlotAmount(mean, shift));
    }

    /** Formats an average to fit a slot, which has room for 5 characters including the leading {@code ~} */
    public static String formatMean(double mean) {
        if (mean >= 100) return String.valueOf(Math.round(mean));

        String text = String.format(Locale.ROOT, "%.1f", mean);

        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }

    public static Optional<Component> averageLine(IntProvider provider) {
        if (!(provider instanceof StatisticalInt statistical)) return Optional.empty();

        return Optional.of(Component.translatable("gtceu.gui.content.average",
                FormattingUtil.formatNumber2Places(statistical.mean())));
    }

    /** Creates the average lines, which mirror the chance tooltip: base, tier boost and value at the displayed tier */
    public static List<Component> summaryLines(IntProvider base, int tierDiff) {
        if (!(base instanceof StatisticalInt statistical)) return List.of();
        if (!statistical.hasTierWeightBoost()) return List.of(Component.translatable("gtceu.gui.content.average",
                FormattingUtil.formatNumber2Places(statistical.mean())).withStyle(ChatFormatting.YELLOW));

        double boost = statistical.getTierWeightBoost();
        String boostKey = "gtceu.gui.content.average_tier_boost_" + (boost > 0 ? "plus" : "minus");

        return List.of(
                Component.translatable("gtceu.gui.content.average_base",
                        FormattingUtil.formatNumber2Places(statistical.mean())).withStyle(ChatFormatting.YELLOW),
                FormattingUtil.formatPercentage2Places(boostKey, (float) Math.abs(100 * boost)),
                Component.translatable("gtceu.gui.content.average_boosted",
                        FormattingUtil.formatNumber2Places(statistical.withTierDiff(tierDiff).mean()))
                        .withStyle(ChatFormatting.YELLOW));
    }

    /**
     * Creates one row for each amount that is possible without a tier weight boost, with its chance at the displayed
     * tier and, if the tier changed it, the change from the base chance.
     * More than {@link #MAX_LISTED_AMOUNTS} amounts are collapsed into a single "most likely" line.
     */
    public static List<Component> oddsLines(IntProvider base, int tierDiff) {
        if (!(base instanceof WeightedInt weighted)) return List.of();

        WeightedInt shown = weighted.withTierDiff(tierDiff);
        List<Integer> amounts = new ArrayList<>();

        for (int amount = weighted.getMinValue(); amount <= weighted.getMaxValue(); amount++) {
            if (weighted.getChance(amount) > 0) amounts.add(amount);
        }

        if (amounts.size() > MAX_LISTED_AMOUNTS) {
            int best = amounts.get(0);

            for (int amount : amounts) {
                if (shown.getChance(amount) > shown.getChance(best)) best = amount;
            }

            return List.of(Component.translatable("gtceu.gui.content.odds_most_likely", best,
                    percent(shown.getChance(best))));
        }

        List<Component> lines = new ArrayList<>();

        for (int amount : amounts) {
            double chance = shown.getChance(amount);
            double delta = (chance - weighted.getChance(amount)) * 100;
            MutableComponent line = Component.translatable("gtceu.gui.content.odds_line", amount, percent(chance));

            if (shown != weighted && Math.abs(delta) >= 0.005)
                line.append(Component.literal(String.format(Locale.ROOT, " (%+.2f%%)", delta))
                        .withStyle(delta > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));

            lines.add(line);
        }

        return lines;
    }

    /**
     * Creates the summary, followed by the per-amount rows while Shift is held, or a hint otherwise.
     * The recipe viewers ask for the lines of a slot on every hover (see {@code GTEmiRecipe}), so the key is read
     * live.
     */
    public static List<Component> viewerLines(IntProvider base, int tierDiff) {
        List<Component> lines = new ArrayList<>(summaryLines(base, tierDiff));
        List<Component> odds = oddsLines(base, tierDiff);

        if (!odds.isEmpty()) {
            if (GTUtil.isShiftDown()) {
                lines.addAll(odds);
            } else {
                lines.add(Component.translatable("gtceu.gui.content.odds_hold_shift"));
            }
        }

        return lines;
    }

    // a possible amount must not read as 0%, so a tiny chance is shown as "<0.01"
    private static String percent(double chance) {
        double percent = chance * 100;

        return percent > 0 && percent < 0.005 ? "<0.01" : FormattingUtil.formatNumber2Places(percent);
    }
}
