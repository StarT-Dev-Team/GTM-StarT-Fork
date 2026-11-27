package com.gregtechceu.gtceu.integration.jei;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;

import net.minecraft.client.gui.GuiGraphics;

import com.mojang.blaze3d.systems.RenderSystem;
import mezz.jei.api.gui.drawable.IDrawable;

public class JEIIcons {

    public static IDrawable of(IGuiTexture texture, int width, int height) {
        return new IDrawable() {

            @Override
            public int getWidth() {
                return width;
            }

            @Override
            public int getHeight() {
                return height;
            }

            @Override
            public void draw(GuiGraphics graphics, int x, int y) {
                if (texture == null) return;
                texture.draw(graphics, 0, 0, x, y, width, height, 0);
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(true);
            }
        };
    }
}
