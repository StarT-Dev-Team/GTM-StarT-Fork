package com.gregtechceu.gtceu.integration.jei.circuit;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.integration.xei.GTXEIUI;
import com.gregtechceu.gtceu.integration.xei.widgets.GTProgrammedCircuitWidget;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.integration.xei.jei.ModularUIRecipeCategory;

import net.minecraft.network.chat.Component;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.RecipeType;
import org.jetbrains.annotations.Nullable;

public class GTProgrammedCircuitCategory extends
                                         ModularUIRecipeCategory<GTProgrammedCircuitCategory.GTProgrammedCircuitWrapper> {

    public final static RecipeType<GTProgrammedCircuitWrapper> RECIPE_TYPE = new RecipeType<>(
            GTCEu.id("programmed_circuit"), GTProgrammedCircuitWrapper.class);

    private final IDrawable background;
    private final IDrawable icon;

    public GTProgrammedCircuitCategory(IJeiHelpers helpers) {
        super(GTProgrammedCircuitWrapper::createModularUI);
        background = helpers.getGuiHelper().createBlankDrawable(150, 80);
        icon = helpers.getGuiHelper().createDrawableItemStack(GTItems.PROGRAMMED_CIRCUIT.asStack());
    }

    @Override
    public RecipeType<GTProgrammedCircuitWrapper> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gtceu.jei.programmed_circuit");
    }

    @Override
    public @Nullable IDrawable getBackground() {
        return background;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 150;
    }

    @Override
    public int getHeight() {
        return 80;
    }

    public static class GTProgrammedCircuitWrapper {

        public ModularUI createModularUI() {
            return GTXEIUI.of(new GTProgrammedCircuitWidget());
        }
    }
}
