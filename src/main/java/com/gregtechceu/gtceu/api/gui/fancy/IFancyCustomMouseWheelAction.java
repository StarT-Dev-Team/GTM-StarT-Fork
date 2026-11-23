package com.gregtechceu.gtceu.api.gui.fancy;

public interface IFancyCustomMouseWheelAction {

    default boolean mouseWheelMove(double wheelDelta) {
        return false;
    }
}
