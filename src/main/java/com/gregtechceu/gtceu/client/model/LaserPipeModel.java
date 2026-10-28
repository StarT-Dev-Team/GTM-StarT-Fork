package com.gregtechceu.gtceu.client.model;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.common.pipelike.laser.LaserPipeType;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.client.bakedpipeline.FaceQuad;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class LaserPipeModel extends PipeModel {

    private static final ResourceLocation SIDE_OVERLAY = GTCEu.id("block/pipe/pipe_laser_side_overlay");
    private static final ResourceLocation END_OVERLAY = GTCEu.id("block/pipe/pipe_laser_in_overlay");
    private static final ResourceLocation MIRROR_CORE = GTCEu.id("block/pipe/pipe_laser_mirror_core");
    private static final int MIRROR_ITEM_CONNECTIONS = 0b101000;

    private final boolean mirror;
    private final Map<ResourceLocation, TextureAtlasSprite> sprites = new ConcurrentHashMap<>();

    public LaserPipeModel(boolean mirror) {
        super(LaserPipeType.NORMAL.getThickness(), () -> GTCEu.id("block/pipe/pipe_laser_side"),
                () -> GTCEu.id("block/pipe/pipe_laser_in"), null, null);

        this.mirror = mirror;
    }

    private static ResourceLocation mirrorCore(int[] signs, boolean overlay) {
        return GTCEu.id("block/pipe/pipe_laser_mirror_core_" + (signs[1] < 0 ? "up" : "down") +
                (signs[0] < 0 ? "_left" : "_right") + (overlay ? "_overlay" : ""));
    }

    @Nullable
    private static int[] mirrorSigns(Direction face, int connections) {
        if (Integer.bitCount(connections) != 2) return null;

        int x = 0, y = 0, z = 0;

        for (Direction dir : GTUtil.DIRECTIONS) {
            if (PipeBlockEntity.isConnected(connections, dir)) {
                x += dir.getStepX();
                y += dir.getStepY();
                z += dir.getStepZ();
            }
        }

        if (face.getAxis().choose(x, y, z) != 0) return null;

        Direction.Axis horizontalAxis = face.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        Direction.Axis verticalAxis = face.getAxis() == Direction.Axis.Y ? Direction.Axis.Z : Direction.Axis.Y;

        int horizontal = horizontalAxis.choose(x, y, z);
        int vertical = verticalAxis.choose(x, y, z);

        if (horizontal == 0 || vertical == 0) return null;

        return new int[] { horizontal, vertical };
    }

    @OnlyIn(Dist.CLIENT)
    private TextureAtlasSprite sprite(ResourceLocation location) {
        return sprites.computeIfAbsent(location,
                loc -> Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(loc));
    }

    @Override
    public int getItemConnections() {
        return mirror ? MIRROR_ITEM_CONNECTIONS : ITEM_CONNECTIONS;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void bakeCoreFace(List<BakedQuad> quads, Direction face, int connections) {
        if (!mirror) {
            super.bakeCoreFace(quads, face, connections);

            return;
        }

        int[] signs = mirrorSigns(face, connections);

        ResourceLocation texture = signs == null ? MIRROR_CORE : mirrorCore(signs, false);

        quads.add(FaceQuad.builder(face, sprite(texture)).cube(coreCube).cubeUV().tintIndex(0).bake());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public List<BakedQuad> bakeQuads(@Nullable Direction side, int connections, int blockedConnections) {
        List<BakedQuad> quads = new ArrayList<>(super.bakeQuads(side, connections, blockedConnections));

        if (side != null) {
            if (PipeBlockEntity.isConnected(connections, side)) {
                quads.add(FaceQuad.builder(side, sprite(END_OVERLAY)).cube(sideCubes.get(side)).cubeUV().tintIndex(2)
                        .bake());
            }

            return quads;
        }

        for (Direction face : GTUtil.DIRECTIONS) {
            if (!PipeBlockEntity.isConnected(connections, face)) {
                ResourceLocation overlay = SIDE_OVERLAY;

                if (mirror) {
                    int[] signs = mirrorSigns(face, connections);

                    overlay = signs == null ? null : mirrorCore(signs, true);
                }

                if (overlay != null) {
                    quads.add(FaceQuad.builder(face, sprite(overlay)).cube(coreCube.inflate(0.001)).cubeUV()
                            .tintIndex(2).bake());
                }
            }

            for (Direction facing : GTUtil.DIRECTIONS) {
                if (facing.getAxis() != face.getAxis() && PipeBlockEntity.isConnected(connections, facing)) {
                    quads.add(FaceQuad.builder(face, sprite(SIDE_OVERLAY))
                            .cube(sideCubes.get(facing).inflate(0.001)).cubeUV().tintIndex(2).bake());
                }
            }
        }

        return quads;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void registerTextureAtlas(Consumer<ResourceLocation> register) {
        super.registerTextureAtlas(register);

        sprites.clear();
        register.accept(SIDE_OVERLAY);
        register.accept(END_OVERLAY);

        if (mirror) {
            register.accept(MIRROR_CORE);

            for (int i : new int[] { -1, 1 }) {
                for (int j : new int[] { -1, 1 }) {
                    register.accept(mirrorCore(new int[] { i, j }, false));
                    register.accept(mirrorCore(new int[] { i, j }, true));
                }
            }
        }
    }
}
