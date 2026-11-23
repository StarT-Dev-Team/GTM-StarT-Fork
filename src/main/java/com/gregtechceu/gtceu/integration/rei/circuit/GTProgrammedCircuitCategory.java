package com.gregtechceu.gtceu.integration.rei.circuit;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.integration.rei.REIIcons;
import com.gregtechceu.gtceu.integration.xei.GTXEIUI;
import com.gregtechceu.gtceu.integration.xei.widgets.GTProgrammedCircuitWidget;

import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.integration.xei.rei.ModularUIDisplay;
import com.lowdragmc.lowdraglib2.integration.xei.rei.ModularUIDisplayCategory;
import com.lowdragmc.lowdraglib2.math.Size;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import lombok.Getter;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;

import java.util.Optional;

public class GTProgrammedCircuitCategory extends
                                         ModularUIDisplayCategory<GTProgrammedCircuitCategory.GTProgrammedCircuitDisplay> {

    public static CategoryIdentifier<GTProgrammedCircuitDisplay> CATEGORY = CategoryIdentifier
            .of(GTCEu.id("programmed_circuit"));

    @Getter
    private final Renderer icon;
    @Getter
    private final Size size;

    public GTProgrammedCircuitCategory() {
        super(GTProgrammedCircuitDisplay::createModularUI);
        this.icon = REIIcons.of(new ItemStackTexture(GTItems.PROGRAMMED_CIRCUIT.asItem()));
        this.size = Size.of(158, 80);
    }

    @Override
    public CategoryIdentifier<? extends GTProgrammedCircuitDisplay> getCategoryIdentifier() {
        return CATEGORY;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gtceu.jei.programmed_circuit");
    }

    @Override
    public int getDisplayHeight() {
        return getSize().height;
    }

    @Override
    public int getDisplayWidth(GTProgrammedCircuitDisplay display) {
        return getSize().width;
    }

    public static class GTProgrammedCircuitDisplay implements ModularUIDisplay {

        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return CATEGORY;
        }

        public ModularUI createModularUI() {
            return GTXEIUI.of(new GTProgrammedCircuitWidget());
        }

        @Override
        public Optional<ResourceLocation> getDisplayLocation() {
            return Optional.of(GTCEu.id("programmed_circuit"));
        }
    }
}
