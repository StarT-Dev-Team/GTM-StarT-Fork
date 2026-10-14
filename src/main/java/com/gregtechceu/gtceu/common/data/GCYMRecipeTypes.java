package com.gregtechceu.gtceu.common.data;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.utils.LocalizationUtils;

import net.minecraft.network.chat.Component;

import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.MULTIBLOCK;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.register;
import static com.lowdragmc.lowdraglib.gui.texture.ProgressTexture.FillDirection.LEFT_TO_RIGHT;

public class GCYMRecipeTypes {

    //////////////////////////////////////
    // ******* Multiblock *******//
    //////////////////////////////////////
    public final static GTRecipeType ALLOY_BLAST_RECIPES = register("alloy_blast_smelter", MULTIBLOCK)
            .setMaxIOSize(9, 0, 3, 1)
            .setEUIO(IO.IN)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT)
            .setSlotOverlay(false, false, false, GuiTextures.FURNACE_OVERLAY_1)
            .setSlotOverlay(false, false, true, GuiTextures.FURNACE_OVERLAY_1)
            .setSlotOverlay(false, true, false, GuiTextures.FURNACE_OVERLAY_2)
            .setSlotOverlay(false, true, true, GuiTextures.FURNACE_OVERLAY_2)
            .setSlotOverlay(true, true, false, GuiTextures.FURNACE_OVERLAY_2)
            .setSlotOverlay(true, true, true, GuiTextures.FURNACE_OVERLAY_2)
            .addDataInfoFull(config -> new GTRecipeType.CustomDataInfoResult(
                    Component.literal(LocalizationUtils.format("gtceu.recipe.temperature",
                            FormattingUtil.formatNumbers(config.recipe().data.getInt("ebf_temp")))),
                    (label, widget) -> widget.setTemperatureLabel(label)))
            .addDataInfoFull(config -> new GTRecipeType.CustomDataInfoResult(Component.empty(),
                    (label, widget) -> widget.setCoilLabel(label)))
            .setSound(GTSoundEntries.ARC);

    public static void init() {}
}
