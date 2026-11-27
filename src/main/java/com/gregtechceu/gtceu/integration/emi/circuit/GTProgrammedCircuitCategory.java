package com.gregtechceu.gtceu.integration.emi.circuit;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.integration.xei.GTXEIUI;
import com.gregtechceu.gtceu.integration.xei.widgets.GTProgrammedCircuitWidget;

import com.lowdragmc.lowdraglib2.integration.xei.emi.ModularUIEMIRecipe;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.IntStream;

public class GTProgrammedCircuitCategory extends EmiRecipeCategory {

    public static final GTProgrammedCircuitCategory CATEGORY = new GTProgrammedCircuitCategory();

    public GTProgrammedCircuitCategory() {
        super(GTCEu.id("programmed_circuit"), EmiStack.of(GTItems.PROGRAMMED_CIRCUIT.asItem()));
    }

    public static void registerDisplays(EmiRegistry registry) {
        registry.addRecipe(new GTProgrammedCircuitCategory.GTProgrammedCircuitWrapper());
    }

    @Override
    public Component getName() {
        return Component.translatable("gtceu.jei.programmed_circuit");
    }

    public static class GTProgrammedCircuitWrapper extends ModularUIEMIRecipe {

        public GTProgrammedCircuitWrapper() {
            super(recipe -> GTXEIUI.of(new GTProgrammedCircuitWidget()));
        }

        @Override
        public EmiRecipeCategory getCategory() {
            return CATEGORY;
        }

        @Override
        public int getDisplayWidth() {
            return 150;
        }

        @Override
        public int getDisplayHeight() {
            return 80;
        }

        @Override
        public @Nullable ResourceLocation getId() {
            return GTCEu.id("/programmed_circuit");
        }

        @Override
        public List<EmiStack> getOutputs() {
            return IntStream.range(0, 33)
                    .mapToObj(IntCircuitBehaviour::stack)
                    .map(EmiStack::of)
                    .toList();
        }

        @Override
        public boolean supportsRecipeTree() {
            return false;
        }

        @Override
        public boolean hideCraftable() {
            return true;
        }
    }
}
