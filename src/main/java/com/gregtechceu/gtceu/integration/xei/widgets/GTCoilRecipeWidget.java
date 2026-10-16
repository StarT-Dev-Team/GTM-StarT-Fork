package com.gregtechceu.gtceu.integration.xei.widgets;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.data.lang.LangHandler;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.List;

public class GTCoilRecipeWidget extends GTRecipeWidget {

    private List<ICoilType> validCoils;
    private ICoilType selectedCoil;
    private ButtonWidget coilButton;
    private LabelWidget recipeTemperatureText;
    private LabelWidget recipeCoilText;

    public GTCoilRecipeWidget(GTRecipe recipe) {
        super(recipe);

        if (voltageTextWidget != null && recipe.data.contains("ebf_temp")) {
            validCoils = calculateValidCoils();

            if (!validCoils.isEmpty()) {
                selectedCoil = validCoils.get(0);
                coilButton = new ButtonWidget(getCoilXOffset(), voltageTextWidget.getSelfPositionY() - 6, 16, 16,
                        cd -> setRecipeCoil(cd.button));

                addWidget(coilButton);
                updateOC(false);
            }
        }
    }

    private List<ICoilType> calculateValidCoils() {
        int recipeTemp = recipe.data.getInt("ebf_temp");

        return Arrays.stream(ICoilType.ALL_COILS_TEMPERATURE_SORTED.get())
                .filter(coil -> GTRecipeModifiers.ebfMachineTemp(coil, getTier()) >= recipeTemp).toList();
    }

    private int getCoilXOffset() {
        return getVoltageXOffset(getTier(), getSize().width) - getXOffset() - 24;
    }

    private void setRecipeCoil(int button) {
        int index = validCoils.indexOf(selectedCoil);

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            index++;
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            index--;
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            index = 0;
        }

        selectedCoil = validCoils.get(Mth.clamp(index, 0, validCoils.size() - 1));

        updateOC(false);
    }

    @Override
    protected OverclockingLogic createOCLogic(boolean perfect) {
        if (selectedCoil == null) return super.createOCLogic(perfect);

        validCoils = calculateValidCoils();

        if (!validCoils.contains(selectedCoil)) {
            selectedCoil = validCoils.get(0);
        }

        int recipeTemp = recipe.data.getInt("ebf_temp");
        int machineTemp = GTRecipeModifiers.ebfMachineTemp(selectedCoil, getTier());

        return (params, maxVoltage) -> OverclockingLogic.heatingCoilOC(params, maxVoltage, recipeTemp, machineTemp);
    }

    @Override
    protected double getEUtMultiplier() {
        if (selectedCoil == null) return 1;

        return OverclockingLogic.getCoilEUtDiscount(recipe.data.getInt("ebf_temp"),
                GTRecipeModifiers.ebfMachineTemp(selectedCoil, getTier()));
    }

    @Override
    protected List<Component> getEUtMultiplierTexts() {
        double discount = getEUtMultiplier();

        if (discount >= 1) return List.of();

        return List.of(Component.translatable("gtceu.recipe.oc.usage.discount",
                Component.literal(Math.round((1 - discount) * 100) + "%").withStyle(ChatFormatting.GREEN)));
    }

    @Override
    protected int getPerfectOCs(OverclockingLogic.OCResult result) {
        if (selectedCoil == null) return super.getPerfectOCs(result);

        int temperatureGap = GTRecipeModifiers.ebfMachineTemp(selectedCoil, getTier()) - recipe.data.getInt("ebf_temp");

        return Math.min(result.ocLevel(),
                Math.max(0, temperatureGap / OverclockingLogic.COIL_EUT_DISCOUNT_TEMPERATURE) / 2);
    }

    @Override
    protected void onRecipeOverclockWidgetUpdated() {
        if (coilButton == null) return;

        var coilStack = new ItemStack(GTCEuAPI.HEATING_COILS.get(selectedCoil).get());

        coilButton.setSelfPositionX(getCoilXOffset());
        coilButton.setButtonTexture(new ItemStackTexture(coilStack));
        coilButton.setHoverTooltips(LangHandler.getMultiLang("gtceu.recipe.coil.tooltip",
                new ItemStack(GTCEuAPI.HEATING_COILS.get(validCoils.get(0)).get()).getHoverName(),
                selectedCoil.getCoilTemperature())
                .toArray(Component[]::new));

        updateRecipeTemperatureText();
        updateRecipeCoilText();
    }

    public void setRecipeTemperatureText(LabelWidget text) {
        recipeTemperatureText = text;

        updateRecipeTemperatureText();
    }

    private void updateRecipeTemperatureText() {
        if (recipeTemperatureText == null || selectedCoil == null) return;

        int machineTemp = GTRecipeModifiers.ebfMachineTemp(selectedCoil, getTier());

        recipeTemperatureText.setComponent(Component.translatable("gtceu.recipe.temperature.value",
                Component.literal(FormattingUtil.formatNumbers(machineTemp) + "K")
                        .withStyle(ChatFormatting.AQUA))
                .append(Component.literal("/" + FormattingUtil.formatNumbers(recipe.data.getInt("ebf_temp")) + "K")));
        recipeTemperatureText.setHoverTooltips(LangHandler.getMultiLang("gtceu.recipe.temperature.tooltip",
                FormattingUtil.formatNumbers(selectedCoil.getCoilTemperature()), GTValues.VNF[getTier()],
                Component
                        .literal("+" + FormattingUtil.formatNumbers(machineTemp - selectedCoil.getCoilTemperature()) +
                                "K")
                        .withStyle(ChatFormatting.GREEN))
                .toArray(Component[]::new));
    }

    public void setRecipeCoilText(LabelWidget text) {
        recipeCoilText = text;

        updateRecipeCoilText();
    }

    private void updateRecipeCoilText() {
        if (recipeCoilText == null || selectedCoil == null) return;

        recipeCoilText.setComponent(Component.translatable("gtceu.recipe.coil.tier",
                new ItemStack(GTCEuAPI.HEATING_COILS.get(selectedCoil).get()).getHoverName()));
    }
}
