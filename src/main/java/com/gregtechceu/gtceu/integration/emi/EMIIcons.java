package com.gregtechceu.gtceu.integration.emi;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.emi.api.render.EmiRenderable;

public class EMIIcons {

    public static EmiRenderable of(IGuiTexture texture, int width, int height) {
        return (graphics, x, y, delta) -> {
            if (texture == null) return;
            texture.draw(graphics, 0, 0, x, y, width, height, delta);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
        };
    }
}
