package com.gregtechceu.gtceu.api.machine.trait;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;

import net.minecraftforge.client.model.data.ModelData;

import javax.annotation.Nullable;
import java.util.List;

public interface IRenderOverlayTrait extends IMachineTrait {

    void getMachineOverlayQuads(List<BakedQuad> quads, @Nullable Direction side,
                                RandomSource rand, ModelData modelData, @Nullable RenderType renderType);

}
