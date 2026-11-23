package com.gregtechceu.gtceu.integration.rei.oreprocessing;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.integration.xei.GTXEIUI;
import com.gregtechceu.gtceu.integration.xei.widgets.GTOreByProductWidget;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.integration.xei.rei.ModularUIDisplay;

import net.minecraft.resources.ResourceLocation;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;

import java.util.Optional;

public class GTOreProcessingDisplay implements ModularUIDisplay {

    private final Material material;

    public GTOreProcessingDisplay(Material material) {
        this.material = material;
    }

    @Override
    public Optional<ResourceLocation> getDisplayLocation() {
        return Optional.of(material.getResourceLocation());
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return GTOreProcessingDisplayCategory.CATEGORY;
    }

    public ModularUI createModularUI() {
        return GTXEIUI.of(new GTOreByProductWidget(material));
    }
}
