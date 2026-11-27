package com.gregtechceu.gtceu.integration.rei;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;

import com.mojang.blaze3d.systems.RenderSystem;
import me.shedaniel.rei.api.client.gui.Renderer;

public class REIIcons {

    public static Renderer of(IGuiTexture texture) {
        return (graphics, bounds, mouseX, mouseY, delta) -> {
            if (texture == null) return;
            texture.draw(graphics, mouseX, mouseY, bounds.getX(), bounds.getY(), bounds.getWidth(),
                    bounds.getWidth(), delta);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
        };
    }
}
