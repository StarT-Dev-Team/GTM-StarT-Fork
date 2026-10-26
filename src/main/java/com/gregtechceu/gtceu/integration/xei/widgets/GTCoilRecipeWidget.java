package com.gregtechceu.gtceu.integration.xei.widgets;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.data.lang.LangHandler;

import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import lombok.AccessLevel;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public abstract class GTCoilRecipeWidget extends GTRecipeWidget {

    @Getter(AccessLevel.PROTECTED)
    private List<ICoilType> validCoils;
    @Getter(AccessLevel.PROTECTED)
    private ICoilType selectedCoil;
    private ButtonWidget coilButton;
    private LabelWidget recipeCoilText;

    protected GTCoilRecipeWidget(GTRecipe recipe) {
        super(recipe);

        if (voltageTextWidget != null) {
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

    protected List<ICoilType> calculateValidCoils() {
        return List.of(ICoilType.ALL_COILS_TEMPERATURE_SORTED.get());
    }

    protected List<Component> getCoilTooltips() {
        return new ArrayList<>(LangHandler.getMultiLang("gtceu.recipe.coil.tooltip"));
    }

    protected List<Component> getCoilSpeedTexts(double durationMultiplier) {
        long speed = Math.round(100 / durationMultiplier);

        return List.of(Component.translatable("gtceu.recipe.oc.duration.coil",
                Component.literal(speed + "%").withStyle(speed >= 100 ? ChatFormatting.GREEN : ChatFormatting.RED)));
    }

    protected ItemStack getCoilStack(ICoilType coil) {
        return new ItemStack(GTCEuAPI.HEATING_COILS.get(coil).get());
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
    protected void updateOC(boolean perfect) {
        if (selectedCoil != null) {
            validCoils = calculateValidCoils();

            if (!validCoils.contains(selectedCoil)) {
                selectedCoil = validCoils.get(0);
            }
        }

        super.updateOC(perfect);
    }

    @Override
    protected List<Component> getEUtMultiplierTexts() {
        double discount = getEUtMultiplier();

        if (discount >= 1) return List.of();

        return List.of(Component.translatable("gtceu.recipe.oc.usage.discount",
                Component.literal(Math.round((1 - discount) * 100) + "%").withStyle(ChatFormatting.GREEN)));
    }

    @Override
    protected void onRecipeOverclockWidgetUpdated() {
        if (coilButton == null) return;

        coilButton.setSelfPositionX(getCoilXOffset());
        coilButton.setButtonTexture(new ItemStackTexture(getCoilStack(selectedCoil)));
        coilButton.setHoverTooltips(getCoilTooltips().toArray(Component[]::new));

        updateRecipeCoilText();
    }

    public void setRecipeCoilText(LabelWidget text) {
        recipeCoilText = text;

        updateRecipeCoilText();
    }

    private void updateRecipeCoilText() {
        if (recipeCoilText == null || selectedCoil == null) return;

        recipeCoilText.setComponent(Component.translatable("gtceu.recipe.coil.tier",
                getCoilStack(selectedCoil).getHoverName()));
    }
}
