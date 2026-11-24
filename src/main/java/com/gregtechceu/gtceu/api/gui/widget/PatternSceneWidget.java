package com.gregtechceu.gtceu.api.gui.widget;

import com.lowdragmc.lowdraglib2.client.scene.ISceneBlockRenderHook;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;

@OnlyIn(Dist.CLIENT)
public class PatternSceneWidget extends Scene {

    public static final float DEFAULT_SPIN_SPEED = 0.2f;

    @Nullable
    @Setter
    private BiConsumer<MultiBufferSource, Float> beforeBatchEnd;

    @Getter
    private boolean turntableActive = false;
    private float spinSpeed = DEFAULT_SPIN_SPEED;
    private long lastRenderTime = 0L;
    private long lastAtlasTickTime = 0L;

    @Getter
    @Setter
    private Vector3f focalCenter = null;
    @Getter
    @Setter
    private Float customZoom = null;
    private Vector3f targetCenter = null;

    public PatternSceneWidget(Level world) {
        createScene(world);
        setTickWorld(false);
        Objects.requireNonNull(getDummyWorld()).setBlockFilter(pos -> true);
    }

    public void clearSelection() {
        this.lastSelectedPosFace = null;
    }

    public void setCenter(Vector3f target, boolean smooth) {
        this.focalCenter = new Vector3f(target);
        if (!smooth) {
            setCenter(new Vector3f(target));
            this.targetCenter = null;
        } else {
            this.targetCenter = new Vector3f(target);
        }
    }

    public void setCenterSmooth(Vector3f target) {
        setCenter(target, true);
    }

    @Override
    public Scene setRenderedCore(Collection<BlockPos> blocks, @Nullable ISceneBlockRenderHook renderHook,
                                 boolean autoCamera) {
        if (this.renderer != null) {
            this.renderer.renderedBlocksMap.clear();
        }
        super.setRenderedCore(blocks, renderHook, autoCamera && focalCenter == null);
        if (this.focalCenter != null && this.renderer != null) {
            if (this.targetCenter == null) {
                this.center = new Vector3f(focalCenter);
            }
            if (customZoom != null) {
                this.zoom = customZoom;
                this.renderer.setCameraOrtho(range * zoom, range * zoom, range * zoom);
            }
            this.renderer.setCameraLookAt(this.center, camZoom(), Math.toRadians(rotationYaw),
                    Math.toRadians(rotationPitch));
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
        this.turntableActive = active;
        if (active) {
            this.lastRenderTime = 0L;
        }
    }

    private void updateCameraCenter(float dt) {
        if (this.targetCenter != null) {
            float distSq = this.center.distanceSquared(this.targetCenter);
            if (distSq < 0.0001f) {
                this.center.set(this.targetCenter);
                this.targetCenter = null;
            } else {
                this.center.lerp(this.targetCenter, Math.min(1.0f, dt * 10.0f));
            }
            if (this.renderer != null) {
                this.renderer.setCameraLookAt(this.center, camZoom(), Math.toRadians(this.rotationYaw),
                        Math.toRadians(this.rotationPitch));
            }
        }
    }

    private void updateTurntable(float dt) {
        if (!this.turntableActive || isDragging()) {
            return;
        }
        float delta = (this.spinSpeed * 60.0f) * dt;
        this.rotationYaw = ((this.rotationYaw + delta) % 360.0f + 360.0f) % 360.0f;
        if (this.renderer != null) {
            this.renderer.setCameraLookAt(this.center, camZoom(), Math.toRadians(this.rotationYaw),
                    Math.toRadians(this.rotationPitch));
        }
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        long now = System.currentTimeMillis();
        float dt = (this.lastRenderTime > 0L) ? (now - this.lastRenderTime) / 1000.0f : (1.0f / 60.0f);
        if (dt > 0.1f || dt <= 0.0f) {
            dt = 1.0f / 60.0f;
        }
        this.lastRenderTime = now;

        updateCameraCenter(dt);
        updateTurntable(dt);

        if (now - this.lastAtlasTickTime >= 50L) {
            this.lastAtlasTickTime = now;
            var modelManager = Minecraft.getInstance().getModelManager();
            var atlas = modelManager.getAtlas(TextureAtlas.LOCATION_BLOCKS);
            for (var loc : atlas.getTextureLocations()) {
                atlas.getSprite(loc);
            }
            atlas.cycleAnimationFrames();
            var particleManager = getParticleManager();
            if (particleManager != null) {
                particleManager.tick();
            }
        }

        super.drawBackgroundAdditional(guiContext);
    }
}
