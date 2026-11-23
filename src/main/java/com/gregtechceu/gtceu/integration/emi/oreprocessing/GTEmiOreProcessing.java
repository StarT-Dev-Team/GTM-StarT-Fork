package com.gregtechceu.gtceu.integration.emi.oreprocessing;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.integration.xei.GTXEIUI;
import com.gregtechceu.gtceu.integration.xei.widgets.GTOreByProductWidget;

import com.lowdragmc.lowdraglib2.integration.xei.emi.ModularUIEMIRecipe;

import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import org.jetbrains.annotations.Nullable;

public class GTEmiOreProcessing extends ModularUIEMIRecipe {

    final Material material;

    public GTEmiOreProcessing(Material material) {
        super(recipe -> GTXEIUI.of(new GTOreByProductWidget(material)));
        this.material = material;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return GTOreProcessingEmiCategory.CATEGORY;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return material.getResourceLocation().withPrefix("/");
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public int getDisplayWidth() {
        return 176;
    }

    @Override
    public int getDisplayHeight() {
        return 166;
    }
}
