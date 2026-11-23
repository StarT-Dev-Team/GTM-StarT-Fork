package com.gregtechceu.gtceu.integration.emi.materialinfo;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.integration.xei.GTXEIUI;
import com.gregtechceu.gtceu.integration.xei.widgets.GTMaterialInfoWidget;

import com.lowdragmc.lowdraglib2.integration.xei.emi.ModularUIEMIRecipe;

import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GTEmiMaterialInfo extends ModularUIEMIRecipe {

    final Material material;
    private final int height;

    public GTEmiMaterialInfo(Material material) {
        super(recipe -> GTXEIUI.of(new GTMaterialInfoWidget(material)));

        this.material = material;
        this.height = new GTMaterialInfoWidget(material).getInfoHeight();
    }

    @Override
    public List<EmiStack> getOutputs() {
        return getInputs().stream().flatMap(ingredient -> ingredient.getEmiStacks().stream()).toList();
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return GTMaterialInfoEmiCategory.CATEGORY;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return material.getResourceLocation().withPrefix("/material_info/");
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public int getDisplayWidth() {
        return GTMaterialInfoWidget.WIDTH;
    }

    @Override
    public int getDisplayHeight() {
        return height;
    }
}
