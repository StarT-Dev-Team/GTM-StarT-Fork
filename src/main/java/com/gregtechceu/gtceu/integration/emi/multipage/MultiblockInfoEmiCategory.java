package com.gregtechceu.gtceu.integration.emi.multipage;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.gui.widget.PatternPreviewWidget;
import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;

import net.minecraft.network.chat.Component;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;

public class MultiblockInfoEmiCategory extends EmiRecipeCategory {

    public static final MultiblockInfoEmiCategory CATEGORY = new MultiblockInfoEmiCategory();

    private MultiblockInfoEmiCategory() {
        super(GTCEu.id("multiblock_info"), EmiStack.of(GTMultiMachines.ELECTRIC_BLAST_FURNACE.getItem()));
    }

    public static void registerDisplays(EmiRegistry registry) {
        PatternPreviewWidget.getMultiblockPreviews().stream()
                .map(MultiblockInfoEmiRecipe::new)
                .forEach(registry::addRecipe);
    }

    @Override
    public Component getName() {
        return Component.translatable("gtceu.jei.multiblock_info");
    }
}
