package com.gregtechceu.gtceu.common.cover.data;

import com.gregtechceu.gtceu.api.gui.widget.EnumSelector;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;

public enum DistributionMode implements EnumSelector.SelectableEnum {

    ROUND_ROBIN_GLOBAL("round_robin_global"),
    ROUND_ROBIN_PRIO("round_robin_prio"),
    INSERT_FIRST("insert_first");

    public static final DistributionMode[] VALUES = values();
    private static final float OFFSET = 1.0f / VALUES.length;

    public final String localeName;

    DistributionMode(String localeName) {
        this.localeName = localeName;
    }

    @Override
    public String getTooltip() {
        return "cover.conveyor.distribution." + localeName;
    }

    @Override
    public IGuiTexture getIcon() {
        return SpriteTexture.of("gtceu:textures/gui/icon/distribution_mode/" + localeName + ".png");
    }
}
