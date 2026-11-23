package com.gregtechceu.gtceu.api.gui.widget;

import com.gregtechceu.gtceu.api.gui.texture.UVSpriteTexture;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;

import dev.vfyjxf.taffy.style.TaffyPosition;

public class ProgressTextureBar extends ProgressBar {

    public ProgressTextureBar(IGuiTexture empty, IGuiTexture filled, FillDirection direction, int width, int height) {
        layout(layout -> layout.width(width).height(height));
        barContainer.layout(layout -> layout.paddingAll(0))
                .style(style -> style.backgroundTexture(empty));
        bar.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        bar.setOverflowVisible(false);
        label.setDisplay(false);

        var fill = new UIElement();
        fill.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE).width(width).height(height);
            switch (direction) {
                case RIGHT_TO_LEFT -> layout.right(0).top(0);
                case DOWN_TO_UP -> layout.left(0).bottom(0);
                default -> layout.left(0).top(0);
            }
        }).style(style -> style.backgroundTexture(filled));
        bar.addChild(fill);

        if (direction == FillDirection.ALWAYS_FULL) {
            bar.layout(layout -> layout.widthPercent(100).heightPercent(100));
        }
        progressBarStyle(style -> style.fillDirection(direction).interpolate(false));
    }

    public static ProgressTextureBar of(SpriteTexture sheet, FillDirection direction, int width, int height) {
        return new ProgressTextureBar(UVSpriteTexture.of(sheet, 0, 0, 1, 0.5f),
                UVSpriteTexture.of(sheet, 0, 0.5f, 1, 0.5f), direction, width, height);
    }
}
