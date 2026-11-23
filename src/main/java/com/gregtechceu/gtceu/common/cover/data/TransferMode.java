package com.gregtechceu.gtceu.common.cover.data;

import com.gregtechceu.gtceu.api.gui.widget.EnumSelector;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;

import lombok.Getter;

public enum TransferMode implements EnumSelector.SelectableEnum {

    TRANSFER_ANY("cover.robotic_arm.transfer_mode.transfer_any", "transfer_any", 1),
    TRANSFER_EXACT("cover.robotic_arm.transfer_mode.transfer_exact", "transfer_exact", 1024),
    KEEP_EXACT("cover.robotic_arm.transfer_mode.keep_exact", "keep_exact", 1024);

    @Getter
    public final String tooltip;
    @Getter
    public final IGuiTexture icon;
    public final int maxStackSize;

    TransferMode(String tooltip, String textureName, int maxStackSize) {
        this.tooltip = tooltip;
        this.maxStackSize = maxStackSize;
        this.icon = SpriteTexture.of("gtceu:textures/gui/icon/transfer_mode/" + textureName + ".png");
    }
}
