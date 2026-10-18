package com.gregtechceu.gtceu.api.gui.widget;

import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

@OnlyIn(Dist.CLIENT)
public class PatternSceneWidget extends SceneWidget {

    @Nullable
    private BiConsumer<MultiBufferSource, Float> beforeBatchEnd;

    public PatternSceneWidget(int x, int y, int width, int height, Level world) {
        super(x, y, width, height, world);
    }

    public void setBeforeBatchEnd(@Nullable BiConsumer<MultiBufferSource, Float> beforeBatchEnd) {
        this.beforeBatchEnd = beforeBatchEnd;
    }

    @Override
    protected void renderBeforeBatchEnd(MultiBufferSource bufferSource, float partialTicks) {
        super.renderBeforeBatchEnd(bufferSource, partialTicks);
        if (beforeBatchEnd != null) {
            beforeBatchEnd.accept(bufferSource, partialTicks);
        }
    }
}
