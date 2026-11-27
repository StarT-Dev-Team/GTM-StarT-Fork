package com.gregtechceu.gtceu.common.cover.data;

import com.gregtechceu.gtceu.api.gui.widget.EnumSelector;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;

import lombok.Getter;

public enum VoidingMode implements EnumSelector.SelectableEnum {

    VOID_ANY("cover.voiding.voiding_mode.void_any", "void_any", 1),
    VOID_OVERFLOW("cover.voiding.voiding_mode.void_overflow", "void_overflow", 1024);

    @Getter
    public final String tooltip;
    @Getter
    public final IGuiTexture icon;
    public final int maxStackSize;

    VoidingMode(String tooltip, String textureName, int maxStackSize) {
        this.tooltip = tooltip;
        this.maxStackSize = maxStackSize;
        this.icon = SpriteTexture.of("gtceu:textures/gui/icon/voiding_mode/" + textureName + ".png");
    }
}
