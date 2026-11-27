package com.gregtechceu.gtceu.api.gui.fancy;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import net.minecraft.network.chat.Component;

import java.util.List;

public interface IFancyConfigurator {

    Component getTitle();

    IGuiTexture getIcon();

    UIElement createConfigurator();

    default List<Component> getTooltips() {
        return List.of(getTitle());
    }

    default void bindTab(UIElement tab) {}
}
