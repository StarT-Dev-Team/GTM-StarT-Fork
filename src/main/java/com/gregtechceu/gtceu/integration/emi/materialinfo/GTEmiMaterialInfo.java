package com.gregtechceu.gtceu.integration.emi.materialinfo;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.integration.xei.widgets.GTMaterialInfoWidget;

import com.lowdragmc.lowdraglib.emi.ModularEmiRecipe;

import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import org.jetbrains.annotations.Nullable;

public class GTEmiMaterialInfo extends ModularEmiRecipe<GTMaterialInfoWidget> {

    final Material material;

    public GTEmiMaterialInfo(Material material) {
        super(() -> new GTMaterialInfoWidget(material));

        this.material = material;

        outputs.clear();

        for (EmiIngredient ingredient : inputs) outputs.addAll(ingredient.getEmiStacks());
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
}
