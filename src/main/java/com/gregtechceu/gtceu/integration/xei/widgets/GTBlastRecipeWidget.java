package com.gregtechceu.gtceu.integration.xei.widgets;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.data.lang.LangHandler;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GTBlastRecipeWidget extends GTCoilRecipeWidget {

    private LabelWidget recipeTemperatureText;

    public GTBlastRecipeWidget(GTRecipe recipe) {
        super(recipe);
    }

    @Override
    protected List<ICoilType> calculateValidCoils() {
        if (!recipe.data.contains("ebf_temp")) return List.of();

        int recipeTemp = recipe.data.getInt("ebf_temp");

        return Arrays.stream(ICoilType.ALL_COILS_TEMPERATURE_SORTED.get())
                .filter(coil -> GTRecipeModifiers.ebfMachineTemp(coil, getTier()) >= recipeTemp).toList();
    }

    @Override
    protected List<Component> getCoilTooltips() {
        List<Component> tooltips = new ArrayList<>(LangHandler.getMultiLang("gtceu.recipe.blast.tooltip",
                getCoilStack(getValidCoils().get(0)).getHoverName(),
                getSelectedCoil().getCoilTemperature()));

        tooltips.addAll(super.getCoilTooltips());

        return tooltips;
    }

    @Override
    protected OverclockingLogic createOCLogic(boolean perfect) {
        if (getSelectedCoil() == null) return super.createOCLogic(perfect);

        int recipeTemp = recipe.data.getInt("ebf_temp");
        int machineTemp = GTRecipeModifiers.ebfMachineTemp(getSelectedCoil(), getTier());

        return (params, maxVoltage) -> OverclockingLogic.heatingCoilOC(params, maxVoltage, recipeTemp, machineTemp);
    }

    @Override
    protected double getEUtMultiplier() {
        if (getSelectedCoil() == null) return 1;

        return OverclockingLogic.getCoilEUtDiscount(recipe.data.getInt("ebf_temp"),
                GTRecipeModifiers.ebfMachineTemp(getSelectedCoil(), getTier()));
    }

    @Override
    protected int getPerfectOCs(OverclockingLogic.OCResult result) {
        if (getSelectedCoil() == null) return super.getPerfectOCs(result);

        int temperatureGap = GTRecipeModifiers.ebfMachineTemp(getSelectedCoil(), getTier()) -
                recipe.data.getInt("ebf_temp");

        return Math.min(result.ocLevel(),
                Math.max(0, temperatureGap / OverclockingLogic.COIL_EUT_DISCOUNT_TEMPERATURE) / 2);
    }

    @Override
    protected void onRecipeOverclockWidgetUpdated() {
        super.onRecipeOverclockWidgetUpdated();

        updateRecipeTemperatureText();
    }

    public void setRecipeTemperatureText(LabelWidget text) {
        recipeTemperatureText = text;

        updateRecipeTemperatureText();
    }

    private void updateRecipeTemperatureText() {
        if (recipeTemperatureText == null || getSelectedCoil() == null) return;

        int machineTemp = GTRecipeModifiers.ebfMachineTemp(getSelectedCoil(), getTier());

        recipeTemperatureText.setComponent(Component.translatable("gtceu.recipe.temperature.value",
                Component.literal(FormattingUtil.formatNumbers(machineTemp) + "K")
                        .withStyle(ChatFormatting.AQUA))
                .append(Component.literal("/" + FormattingUtil.formatNumbers(recipe.data.getInt("ebf_temp")) + "K")));
        recipeTemperatureText.setHoverTooltips(LangHandler.getMultiLang("gtceu.recipe.temperature.tooltip",
                FormattingUtil.formatNumbers(getSelectedCoil().getCoilTemperature()), GTValues.VNF[getTier()],
                Component
                        .literal("+" +
                                FormattingUtil.formatNumbers(machineTemp - getSelectedCoil().getCoilTemperature()) +
                                "K")
                        .withStyle(ChatFormatting.GREEN))
                .toArray(Component[]::new));
    }
}
