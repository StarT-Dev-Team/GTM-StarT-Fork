package com.gregtechceu.gtceu.api.gui.widget;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import java.util.function.IntSupplier;

public class ColorBlock extends UIElement {

    private static boolean isShowAlpha = false;

    private final IntSupplier colorSupplier;

    public ColorBlock(IntSupplier colorSupplier) {
        this.colorSupplier = colorSupplier;
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.TICK, event -> updateTexture());
        updateTexture();
    }

    private void updateTexture() {
        var color = colorSupplier.getAsInt();
        var opaque = isShowAlpha ? color : color | 0xFF000000;
        var texture = IGuiTexture.group(new ColorRectTexture(opaque), new ColorBorderTexture(1, 0xFF000000));
        style(style -> style.backgroundTexture(texture));
    }

    private void onMouseDown(UIEvent event) {
        isShowAlpha = !isShowAlpha;
        updateTexture();
    }
}
