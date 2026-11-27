package com.gregtechceu.gtceu.integration.emi.orevein;

import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.client.ClientProxy;
import com.gregtechceu.gtceu.integration.xei.GTXEIUI;
import com.gregtechceu.gtceu.integration.xei.widgets.GTOreVeinWidget;

import com.lowdragmc.lowdraglib2.integration.xei.emi.ModularUIEMIRecipe;

import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GTEmiOreVein extends ModularUIEMIRecipe {

    private final GTOreDefinition oreDefinition;

    public GTEmiOreVein(GTOreDefinition oreDefinition) {
        super(recipe -> GTXEIUI.of(new GTOreVeinWidget(oreDefinition)));
        this.oreDefinition = oreDefinition;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return GTOreVeinEmiCategory.CATEGORY;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return ClientProxy.CLIENT_ORE_VEINS.inverse().get(oreDefinition).withPrefix("/ore_vein_diagram/");
    }

    @Override
    public List<EmiStack> getOutputs() {
        return GTOreVeinWidget.getContainedOresAndBlocks(oreDefinition)
                .stream()
                .map(EmiStack::of)
                .toList();
    }

    @Override
    public int getDisplayWidth() {
        return GTOreVeinWidget.width;
    }

    @Override
    public int getDisplayHeight() {
        return 160;
    }
}
