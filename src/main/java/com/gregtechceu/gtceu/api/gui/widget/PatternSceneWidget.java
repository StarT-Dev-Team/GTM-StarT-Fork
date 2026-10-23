package com.gregtechceu.gtceu.api.gui.widget;

import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.function.BiConsumer;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class PatternSceneWidget extends SceneWidget {

    public static final float DEFAULT_SPIN_SPEED = 0.2f;
    public static final long DEFAULT_IDLE_DELAY = 0L;

    @Nullable
    @Setter
    private BiConsumer<MultiBufferSource, Float> beforeBatchEnd;

    @Getter
    public boolean isTurntableActive = false;
    private float spinSpeed = DEFAULT_SPIN_SPEED;
    private long idleDelay = DEFAULT_IDLE_DELAY;
    private boolean autoResume = true;
    private long lastInteractionTime = 0L;
    private boolean pausedUntilRetoggle = false;
    private long lastRenderTime = 0L;

    public PatternSceneWidget(int x, int y, int width, int height, Level world) {
        super(x, y, width, height, world);
    }

    @Override
    protected void renderBeforeBatchEnd(MultiBufferSource bufferSource, float partialTicks) {
        super.renderBeforeBatchEnd(bufferSource, partialTicks);
        if (beforeBatchEnd != null) {
            beforeBatchEnd.accept(bufferSource, partialTicks);
        }
    }

    public void setTurntableActive(boolean active) {
        this.isTurntableActive = active;
        if (active) {
            this.pausedUntilRetoggle = false;
            this.lastInteractionTime = 0L;
            this.lastRenderTime = 0L;
        }
    }

    private void updateTurntable() {
        if (!this.isTurntableActive || this.pausedUntilRetoggle) {
            this.lastRenderTime = 0L;
            return;
        }
        if (isDragging()) {
            this.lastInteractionTime = System.currentTimeMillis();
            this.lastRenderTime = 0L;
            if (!this.autoResume) {
                this.pausedUntilRetoggle = true;
            }
            return;
        }
        long now = System.currentTimeMillis();
        if (this.idleDelay > 0 && this.lastInteractionTime > 0L && now - this.lastInteractionTime < this.idleDelay) {
            this.lastRenderTime = 0L;
            return;
        }
        float dt = (lastRenderTime > 0L) ? (now - lastRenderTime) / 1000.0f : (1.0f / 60.0f);
        if (dt > 0.1f || dt <= 0.0f) {
            dt = 1.0f / 60.0f;
        }
        this.lastRenderTime = now;
        float delta = (this.spinSpeed * 60.0f) * dt;
        this.rotationPitch = ((this.rotationPitch + delta) % 360.0f + 360.0f) % 360.0f;
        if (this.renderer != null) {
            this.renderer.setCameraLookAt((this.center != null) ? this.center : new Vector3f(0, 0, 0),
                    this.camZoom(),
                    Math.toRadians(this.rotationPitch),
                    Math.toRadians(this.rotationYaw));
        }
    }

    @Override
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        updateTurntable();
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.lastInteractionTime = 0L;
        this.lastRenderTime = 0L;
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
