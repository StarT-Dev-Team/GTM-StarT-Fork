package com.gregtechceu.gtceu.api.gui.widget.preview;

import com.gregtechceu.gtceu.GTCEu;

import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

import org.jetbrains.annotations.Nullable;

public final class PreviewLevel {

    private static final int REGION_SIZE = 512;

    @Nullable
    private static TrackedDummyWorld level;
    private static int lastOffsetIndex = 0;

    private PreviewLevel() {}

    @Nullable
    public static TrackedDummyWorld get() {
        return level;
    }

    public static TrackedDummyWorld getOrCreate() {
        if (level == null) {
            if (Minecraft.getInstance().level == null) {
                GTCEu.LOGGER.error("Try to init pattern previews before level load");
                throw new IllegalStateException();
            }
            level = new TrackedDummyWorld();
        }
        return level;
    }

    public static BlockPos locateNextRegion() {
        int currentIndex = lastOffsetIndex++;

        int x = 0, z = 0;
        if (currentIndex > 0) {
            int v = (int) (Mth.sqrt(currentIndex + 0.25f) - 0.5f);
            int nextV = v + 1;
            int spiralBaseIndex = v * nextV;
            int flipFlop = (v & 1) * 2 - 1;

            int offset = flipFlop * nextV / 2;
            x += offset;
            z += offset;

            int cornerIndex = spiralBaseIndex + nextV;
            if (currentIndex < cornerIndex) {
                x -= flipFlop * (currentIndex - spiralBaseIndex + 1);
            } else {
                x -= flipFlop * nextV;
                z -= flipFlop * (currentIndex - cornerIndex + 1);
            }
        }
        return new BlockPos(x * REGION_SIZE, 50, z * REGION_SIZE);
    }
}
