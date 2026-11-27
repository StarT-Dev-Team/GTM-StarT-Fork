package com.gregtechceu.gtceu.api.gui.texture;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class UVSpriteTexture extends SpriteTexture {

    private final float u, v, uWidth, vHeight;
    private boolean resolved;

    protected UVSpriteTexture(float u, float v, float uWidth, float vHeight) {
        this.u = u;
        this.v = v;
        this.uWidth = uWidth;
        this.vHeight = vHeight;
    }

    public static UVSpriteTexture of(ResourceLocation imageLocation, float u, float v, float uWidth, float vHeight) {
        var texture = new UVSpriteTexture(u, v, uWidth, vHeight);
        texture.setImageLocation(imageLocation);
        return texture;
    }

    public static UVSpriteTexture of(String imageLocation, float u, float v, float uWidth, float vHeight) {
        return of(new ResourceLocation(imageLocation), u, v, uWidth, vHeight);
    }

    public static UVSpriteTexture of(SpriteTexture base, float u, float v, float uWidth, float vHeight) {
        var texture = of(base.getImageLocation(), u, v, uWidth, vHeight);
        texture.setColor(base.color);
        texture.copyTransform(base);
        return texture;
    }

    @Override
    public UVSpriteTexture copy() {
        var copied = of(getImageLocation(), u, v, uWidth, vHeight);
        copied.setBorder(borderLT.getX(), borderLT.getY(), borderRB.getX(), borderRB.getY());
        copied.setColor(color);
        copied.setWrapMode(wrapMode);
        copied.copyTransform(this);
        return copied;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void drawInternal(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width,
                                float height, float partialTicks) {
        if (!resolved) {
            var size = getImageSize();
            setSprite(Math.round(u * size.getWidth()), Math.round(v * size.getHeight()),
                    Math.max(1, Math.round(uWidth * size.getWidth())),
                    Math.max(1, Math.round(vHeight * size.getHeight())));
            resolved = size.getWidth() > 1 || size.getHeight() > 1;
        }
        super.drawInternal(graphics, mouseX, mouseY, x, y, width, height, partialTicks);
    }
}
