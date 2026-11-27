package com.gregtechceu.gtceu.common.cover.data;

import com.gregtechceu.gtceu.api.gui.widget.EnumSelector;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;

public enum ManualIOMode implements EnumSelector.SelectableEnum {

    DISABLED("disabled"),
    FILTERED("filtered"),
    UNFILTERED("unfiltered");

    public static final ManualIOMode[] VALUES = values();

    public final String localeName;

    ManualIOMode(String localeName) {
        this.localeName = localeName;
    }

    @Override
    public String getTooltip() {
        return "cover.universal.manual_import_export.mode." + localeName;
    }

    @Override
    public IGuiTexture getIcon() {
        return SpriteTexture.of("gtceu:textures/gui/icon/manual_io_mode/" + localeName + ".png");
    }
}
