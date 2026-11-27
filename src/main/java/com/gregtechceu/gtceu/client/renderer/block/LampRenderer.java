package com.gregtechceu.gtceu.client.renderer.block;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.common.block.LampBlock;

import com.lowdragmc.lowdraglib2.client.renderer.impl.IModelRenderer;
import com.lowdragmc.lowdraglib2.compat.util.TriState;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class LampRenderer extends IModelRenderer {

    public LampRenderer(LampBlock lamp, BlockState state) {
        super(GTCEu.id("block/%s%s_lamp%s%s".formatted(
                lamp.color,
                lamp.bordered ? "" : "_borderless",
                state.getValue(LampBlock.LIGHT) ? "" : "_off",
                state.getValue(LampBlock.LIGHT) && state.getValue(LampBlock.BLOOM) ? "_bloom" : "")));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public TriState useAO() {
        return TriState.TRUE;
    }
}
