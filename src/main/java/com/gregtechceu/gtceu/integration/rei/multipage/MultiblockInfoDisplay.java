package com.gregtechceu.gtceu.integration.rei.multipage;

import com.gregtechceu.gtceu.api.gui.widget.PatternPreviewWidget;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.integration.xei.GTXEIUI;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.integration.xei.rei.ModularUIDisplay;

import net.minecraft.resources.ResourceLocation;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;

import java.util.Optional;

public class MultiblockInfoDisplay implements ModularUIDisplay {

    public final MultiblockMachineDefinition definition;

    public MultiblockInfoDisplay(MultiblockMachineDefinition definition) {
        this.definition = definition;
    }

    @Override
    public Optional<ResourceLocation> getDisplayLocation() {
        return Optional.of(definition.getId());
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return MultiblockInfoDisplayCategory.CATEGORY;
    }

    public ModularUI createModularUI() {
        return GTXEIUI.of(PatternPreviewWidget.getPatternWidget(definition));
    }
}
