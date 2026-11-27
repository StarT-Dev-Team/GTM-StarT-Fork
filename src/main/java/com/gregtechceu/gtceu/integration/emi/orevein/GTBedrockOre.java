package com.gregtechceu.gtceu.integration.emi.orevein;

import com.gregtechceu.gtceu.api.data.worldgen.bedrockore.BedrockOreDefinition;
import com.gregtechceu.gtceu.client.ClientProxy;
import com.gregtechceu.gtceu.integration.xei.GTXEIUI;
import com.gregtechceu.gtceu.integration.xei.widgets.GTOreVeinWidget;

import com.lowdragmc.lowdraglib2.integration.xei.emi.ModularUIEMIRecipe;

import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import org.jetbrains.annotations.Nullable;

public class GTBedrockOre extends ModularUIEMIRecipe {

    private final BedrockOreDefinition bedrockOre;

    public GTBedrockOre(BedrockOreDefinition bedrockOre) {
        super(recipe -> GTXEIUI.of(new GTOreVeinWidget(bedrockOre)));
        this.bedrockOre = bedrockOre;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return GTBedrockOreEmiCategory.CATEGORY;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return ClientProxy.CLIENT_BEDROCK_ORE_VEINS.inverse().get(bedrockOre).withPrefix("/bedrock_ore_diagram/");
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
