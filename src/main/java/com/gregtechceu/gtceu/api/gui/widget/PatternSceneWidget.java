package com.gregtechceu.gtceu.api.gui.widget;

import com.lowdragmc.lowdraglib.client.scene.ISceneBlockRenderHook;
import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collection;
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

    @Getter
    @Setter
    private Vector3f focalCenter = null;
    @Getter
    @Setter
    private Float customZoom = null;
    private Vector3f targetCenter = null;

    public PatternSceneWidget(int x, int y, int width, int height, Level world) {
        super(x, y, width, height, world);
    }

    public void setCenter(Vector3f target, boolean smooth) {
        this.focalCenter = new Vector3f(target);
        if (!smooth || this.center == null) {
            this.setCenter(new Vector3f(target));
            this.targetCenter = null;
        } else {
            this.targetCenter = new Vector3f(target);
        }
    }

    public void setCenterSmooth(Vector3f target) {
        this.setCenter(target, true);
    }

    @Override
    public SceneWidget setRenderedCore(Collection<BlockPos> blocks,
                                       ISceneBlockRenderHook renderHook) {
        super.setRenderedCore(blocks, renderHook);
        if (this.focalCenter != null) {
            if (this.targetCenter == null) {
                this.center = new Vector3f(focalCenter);
            }
            if (customZoom != null) {
                this.zoom = customZoom;
                if (this.renderer != null) {
                    this.renderer.setCameraOrtho(range * zoom, range * zoom, range * zoom);
                }
            }
            if (this.renderer != null) {
                this.renderer.setCameraLookAt(this.center, camZoom(), Math.toRadians(rotationPitch),
                        Math.toRadians(rotationYaw));
            }
        }
        return this;
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

    private void updateCameraCenter(float dt) {
        if (this.targetCenter != null && this.center != null) {
            float distSq = this.center.distanceSquared(this.targetCenter);
            if (distSq < 0.0001f) {
                this.center.set(this.targetCenter);
                this.targetCenter = null;
            } else {
                this.center.lerp(this.targetCenter, Math.min(1.0f, dt * 10.0f));
            }
            if (this.renderer != null) {
                this.renderer.setCameraLookAt(this.center, camZoom(),
                        Math.toRadians(this.rotationPitch),
                        Math.toRadians(this.rotationYaw));
            }
        }
    }

    private void updateTurntable(float dt) {
        if (!this.isTurntableActive || this.pausedUntilRetoggle) {
            return;
        }
        if (isDragging()) {
            this.lastInteractionTime = System.currentTimeMillis();
            if (!this.autoResume) {
                this.pausedUntilRetoggle = true;
            }
            return;
        }
        long now = System.currentTimeMillis();
        if (this.idleDelay > 0 && this.lastInteractionTime > 0L && now - this.lastInteractionTime < this.idleDelay) {
            return;
        }
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
        long now = System.currentTimeMillis();
        float dt = (this.lastRenderTime > 0L) ? (now - this.lastRenderTime) / 1000.0f : (1.0f / 60.0f);
        if (dt > 0.1f || dt <= 0.0f) {
            dt = 1.0f / 60.0f;
        }
        this.lastRenderTime = now;

        updateCameraCenter(dt);
        updateTurntable(dt);
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.lastInteractionTime = 0L;
        this.lastRenderTime = 0L;
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
