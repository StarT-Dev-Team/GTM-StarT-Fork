package com.gregtechceu.gtceu.integration.emi.recipe;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.category.GTRecipeCategory;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.integration.emi.EMIIcons;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class GTRecipeEMICategory extends EmiRecipeCategory {

    public static final Function<GTRecipeCategory, GTRecipeEMICategory> CATEGORIES = Util
            .memoize(GTRecipeEMICategory::new);
    private final GTRecipeCategory category;

    private GTRecipeEMICategory(GTRecipeCategory category) {
        super(category.registryKey, EMIIcons.of(category.getIcon(), 16, 16));
        this.category = category;
    }

    public static void registerDisplays(EmiRegistry registry) {
        List<GTRecipeCategory> subCategories = new ArrayList<>();
        // run main categories first
        for (GTRecipeCategory category : GTRegistries.RECIPE_CATEGORIES) {
            if (!category.shouldRegisterDisplays()) continue;
            var type = category.getRecipeType();
            if (category == type.getCategory()) {
                type.buildRepresentativeRecipes();
            } else {
                subCategories.add(category);
                continue;
            }
            EmiRecipeCategory emiCategory = CATEGORIES.apply(category);
            type.getRecipesInCategory(category).stream()
                    .map(recipe -> new GTEmiRecipe(recipe, emiCategory))
                    .forEach(recipe -> addSafely(registry, recipe));
        }
        // run subcategories
        for (var subCategory : subCategories) {
            if (!subCategory.shouldRegisterDisplays()) continue;
            var type = subCategory.getRecipeType();
            EmiRecipeCategory emiCategory = CATEGORIES.apply(subCategory);
            type.getRecipesInCategory(subCategory).stream()
                    .map(recipe -> new GTEmiRecipe(recipe, emiCategory))
                    .forEach(recipe -> addSafely(registry, recipe));
        }
    }

    public static void addSafely(EmiRegistry registry, EmiRecipe recipe) {
        try {
            registry.addRecipe(recipe);
        } catch (RuntimeException e) {
            GTCEu.LOGGER.error("Failed to add EMI recipe {}", recipe.getId(), e);
        }
    }

    public static Comparator<MachineDefinition> sortDefinition = (a, b) -> {
        boolean isAMulti = a instanceof MultiblockMachineDefinition;
        boolean isBMulti = b instanceof MultiblockMachineDefinition;
        if (isAMulti && !isBMulti) {
            return 1;
        } else if (!isAMulti && isBMulti) {
            return -1;
        } else {
            return a.getTier() - b.getTier();
        }
    };

    public static void registerWorkStations(EmiRegistry registry) {
        for (MachineDefinition machine : GTRegistries.MACHINES.values()
                .stream()
                .sorted(sortDefinition)
                .toList()) {
            for (GTRecipeType type : machine.getRecipeTypes()) {
                for (GTRecipeCategory category : type.getCategoriesWithMain()) {
                    if (!category.isXEIVisible() && !GTCEu.isDev()) continue;
                    registry.addWorkstation(machineCategory(category), EmiStack.of(machine.asStack()));
                }
            }
        }
    }

    public static EmiRecipeCategory machineCategory(GTRecipeCategory category) {
        if (category == GTRecipeTypes.FURNACE_RECIPES.getCategory()) return VanillaEmiRecipeCategories.SMELTING;
        else if (category == GTRecipeTypes.BLAST_FURNACE_RECIPES.getCategory())
            return VanillaEmiRecipeCategories.BLASTING;
        else if (category == GTRecipeTypes.SMOKING_FURNACE_RECIPES.getCategory())
            return VanillaEmiRecipeCategories.SMOKING;
        else return CATEGORIES.apply(category);
    }

    @Override
    public Component getName() {
        return Component.translatable(category.getLanguageKey());
    }
}
