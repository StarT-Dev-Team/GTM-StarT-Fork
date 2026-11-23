package com.gregtechceu.gtceu.api.gui;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;

public class SteamTexture {

    private static final String BRONZE = "bronze";
    private static final String STEEL = "steel";

    private final SpriteTexture bronzeTexture;
    private final SpriteTexture steelTexture;

    private SteamTexture(SpriteTexture bronzeTexture, SpriteTexture steelTexture) {
        this.bronzeTexture = bronzeTexture;
        this.steelTexture = steelTexture;
    }

    public static SteamTexture fullImage(String path) {
        return new SteamTexture(
                SpriteTexture.of(String.format(path, BRONZE)),
                SpriteTexture.of(String.format(path, STEEL)));
    }

    public SpriteTexture get(boolean isHighPressure) {
        return isHighPressure ? steelTexture : bronzeTexture;
    }
}
