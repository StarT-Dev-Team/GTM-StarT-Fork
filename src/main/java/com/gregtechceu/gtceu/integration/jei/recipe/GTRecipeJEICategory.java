package com.gregtechceu.gtceu.integration.jei.recipe;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.category.GTRecipeCategory;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.core.mixins.ldlib.ModularUIRecipeCategoryMixin;

import com.lowdragmc.lowdraglib.gui.ingredient.IRecipeIngredientSlot;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.jei.IGui2IDrawable;
import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import lombok.Getter;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IClickableIngredient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class GTRecipeJEICategory extends ModularUIRecipeCategory<GTRecipe> {

    public static final Function<GTRecipeCategory, RecipeType<GTRecipe>> TYPES = Util
            .memoize(c -> new RecipeType<>(c.registryKey, GTRecipe.class));

    private final GTRecipeCategory category;
    @Getter
    private final IDrawable background;
    @Getter
    private final IDrawable icon;

    public GTRecipeJEICategory(IJeiHelpers helpers,
                               @NotNull GTRecipeCategory category) {
        super(GTRecipeWrapper::new);
        this.category = category;
        var recipeType = category.getRecipeType();
        IGuiHelper guiHelper = helpers.getGuiHelper();
        var size = recipeType.getRecipeUI().getJEISize();
        this.background = guiHelper.createBlankDrawable(size.width, size.height);
        this.icon = IGui2IDrawable.toDrawable(category.getIcon(), 16, 16);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
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
            var wrapped = List.copyOf(type.getRecipesInCategory(category));
            registration.addRecipes(TYPES.apply(category), wrapped);
        }
        // run subcategories
        for (GTRecipeCategory subCategory : subCategories) {
            if (!subCategory.shouldRegisterDisplays()) continue;
            var type = subCategory.getRecipeType();
            var wrapped = List.copyOf(type.getRecipesInCategory(subCategory));
            registration.addRecipes(TYPES.apply(subCategory), wrapped);
        }
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (MachineDefinition machine : GTRegistries.MACHINES) {
            for (GTRecipeType type : machine.getRecipeTypes()) {
                for (GTRecipeCategory category : type.getCategories()) {
                    if (!category.isXEIVisible() && !GTCEu.isDev()) continue;
                    registration.addRecipeCatalyst(machine.asStack(), machineType(category));
                }
            }
        }
    }

    public static RecipeType<?> machineType(GTRecipeCategory category) {
        if (category == GTRecipeTypes.FURNACE_RECIPES.getCategory()) return RecipeTypes.SMELTING;
        else if (category == GTRecipeTypes.BLAST_FURNACE_RECIPES.getCategory()) return RecipeTypes.BLASTING;
        else if (category == GTRecipeTypes.SMOKING_FURNACE_RECIPES.getCategory()) return RecipeTypes.SMOKING;
        return TYPES.apply(category);
    }

    @Override
    @NotNull
    public RecipeType<GTRecipe> getRecipeType() {
        return TYPES.apply(category);
    }

    @Override
    @NotNull
    public Component getTitle() {
        return Component.translatable(category.getLanguageKey());
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(@NotNull GTRecipe recipe) {
        return recipe.id;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GTRecipe recipe, IFocusGroup focuses) {
        @SuppressWarnings("unchecked")
        var wrapper = ((ModularUIRecipeCategoryMixin<GTRecipe>) this).gtceu$getModularWrapper(recipe);

        wrapper.setRecipeWidget(0, 0);
        List<Widget> flatVisibleWidgetCollection = wrapper.modularUI.getFlatWidgetCollection();
        for (int i = 0; i < flatVisibleWidgetCollection.size(); i++) {
            var widget = flatVisibleWidgetCollection.get(i);
            if (widget instanceof IRecipeIngredientSlot slot) {
                if (widget.getParent() instanceof DraggableScrollableWidgetGroup draggable &&
                        draggable.isUseScissor()) {
                    // don't add the JEI widget at all if we have a draggable group, let the draggable widget handle it
                    // instead.
                    continue;
                }

                var role = mapToRole(slot.getIngredientIO());
                if (role == null) { // both
                    ModularUIRecipeCategoryMixin.gtceu$addJEISlot(builder, slot, RecipeIngredientRole.INPUT, i);
                    ModularUIRecipeCategoryMixin.gtceu$addJEISlot(builder, slot, RecipeIngredientRole.OUTPUT, i);
                } else {
                    ModularUIRecipeCategoryMixin.gtceu$addJEISlot(builder, slot, role, i);
                }
            }
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, GTRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX,
                           double mouseY) {
        @SuppressWarnings("unchecked")
        var wrapper = ((ModularUIRecipeCategoryMixin<GTRecipe>) this).gtceu$getModularWrapper(recipe);

        if (wrapper.tooltipTexts == null || wrapper.tooltipTexts.isEmpty()) {
            super.getTooltip(tooltip, recipe, recipeSlotsView, mouseX, mouseY);
            return;
        }

        var hovered = wrapper.modularUI.mainGroup.getHoverElement(mouseX + wrapper.getLeft(),
                mouseY + wrapper.getTop());
        if (hovered instanceof IRecipeIngredientSlot hoveredSlot) {
            if (!(hovered.getParent() instanceof DraggableScrollableWidgetGroup draggable &&
                    draggable.isUseScissor())) {
                super.getTooltip(tooltip, recipe, recipeSlotsView, mouseX, mouseY);
                return;
            }

            for (Object ingredient : hoveredSlot.getXEIIngredients()) {
                if (ingredient instanceof IClickableIngredient<?> clickableIngredient) {
                    // noinspection removal
                    tooltip.setIngredient(clickableIngredient.getTypedIngredient());
                    break;
                }
            }
            // if (tooltip instanceof JeiTooltip jeiTooltip) {
            // try {
            // var field = JeiTooltip.class.getDeclaredField("lines");
            // field.setAccessible(true);
            // ((List<?>)field.get(jeiTooltip)).clear();
            // } catch (IllegalAccessException | NoSuchFieldException ignored) {
            // }
            // }
            // tooltip.addAll(hoveredSlot.getFullTooltipTexts());
        }

        tooltip.addAll(wrapper.tooltipTexts);
        if (wrapper.tooltipComponent != null) {
            tooltip.add(wrapper.tooltipComponent);
        }
    }
}
