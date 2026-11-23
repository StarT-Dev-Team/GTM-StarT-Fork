package com.gregtechceu.gtceu.integration.emi.orevein;

import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidDefinition;
import com.gregtechceu.gtceu.client.ClientProxy;
import com.gregtechceu.gtceu.integration.xei.GTXEIUI;
import com.gregtechceu.gtceu.integration.xei.widgets.GTOreVeinWidget;

import com.lowdragmc.lowdraglib2.integration.xei.emi.ModularUIEMIRecipe;

import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import org.jetbrains.annotations.Nullable;

public class GTBedrockFluid extends ModularUIEMIRecipe {

    private final BedrockFluidDefinition fluid;

    public GTBedrockFluid(BedrockFluidDefinition fluid) {
        super(recipe -> GTXEIUI.of(new GTOreVeinWidget(fluid)));
        this.fluid = fluid;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return GTBedrockFluidEmiCategory.CATEGORY;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return ClientProxy.CLIENT_FLUID_VEINS.inverse().get(fluid).withPrefix("/bedrock_fluid_diagram/");
    }

    @Override
    public int getDisplayWidth() {
        return GTOreVeinWidget.width;
    }

    @Override
    public int getDisplayHeight() {
        return 140;
    }
}
