package com.gregtechceu.gtceu.integration.emi.materialinfo;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;

public class GTMaterialInfoEmiCategory extends EmiRecipeCategory {

    public static final GTMaterialInfoEmiCategory CATEGORY = new GTMaterialInfoEmiCategory();

    public GTMaterialInfoEmiCategory() {
        super(GTCEu.id("material_info"), EmiStack.of(Items.IRON_INGOT));
    }

    public static void registerDisplays(EmiRegistry registry) {
        for (Material mat : GTCEuAPI.materialManager.getRegisteredMaterials()) {
            GTEmiMaterialInfo recipe = new GTEmiMaterialInfo(mat);

            if (!recipe.getInputs().isEmpty()) registry.addRecipe(recipe);
        }
    }

    @Override
    public Component getName() {
        return Component.translatable("gtceu.jei.material_info");
    }
}
