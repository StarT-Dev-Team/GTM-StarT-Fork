package com.gregtechceu.gtceu.api.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class GridRenderer {

    public static final int COLOR_GRID_LINES = 0x50404020;
    public static final int COLOR_GRID_MARGINS = 0xFF5C5C5C;

    private record LineSegment(float x1, float y1, float z1, float x2, float y2, float z2) {}

    public static void render(PatternPreviewWidget.MBPattern pattern, MultiBufferSource bufferSource) {
        render(pattern, bufferSource, 0);
    }

    public static void render(PatternPreviewWidget.MBPattern pattern, MultiBufferSource bufferSource,
                              int graduationStep) {
        if (pattern == null || pattern.blockMap.isEmpty()) return;

        int xMin = pattern.xMin;
        int xMax = pattern.xMax;
        int yMin = pattern.yMin;
        int yMax = pattern.yMax;
        int zMin = pattern.zMin;
        int zMax = pattern.zMax;

        int x0 = xMin - 1;
        int x1 = xMax + 1;
        int z0 = zMin - 1;
        int z1 = zMax + 1;
        int y0 = yMin;
        int y1 = yMax + 1;

        float centerX = (x0 + x1) / 2.0f;
        float centerY = (y0 + y1) / 2.0f;
        float centerZ = (z0 + z1) / 2.0f;

        Vector3f cameraPos = new Vector3f(centerX + 10.0f, centerY + 10.0f, centerZ - 10.0f);
        try {
            Matrix4f inv = new Matrix4f(RenderSystem.getModelViewMatrix()).invert();
            Vector3f testPos = new Vector3f();
            inv.transformPosition(testPos);
            if (!Float.isNaN(testPos.x) && !Float.isNaN(testPos.y) && !Float.isNaN(testPos.z)) {
                cameraPos = testPos;
            }
        } catch (Exception e) {}

        float yGrid = (cameraPos.y >= centerY) ? (yMin - 0.015f) : (yMax + 1.015f);
        float zWall = (cameraPos.z >= centerZ) ? (zMin - 1.015f) : (zMax + 1.015f);
        float xWall = (cameraPos.x >= centerX) ? (xMin - 1.015f) : (xMax + 1.015f);

        int gridSizeX = x1 - x0;
        int gridSizeY = y1 - y0;
        int gridSizeZ = z1 - z0;

        boolean enableGradX = graduationStep > 0 && gridSizeX >= 2 * graduationStep;
        boolean enableGradY = graduationStep > 0 && gridSizeY >= 2 * graduationStep;
        boolean enableGradZ = graduationStep > 0 && gridSizeZ >= 2 * graduationStep;

        List<LineSegment> gridLines = new ArrayList<>();
        List<LineSegment> gridMargins = new ArrayList<>();

        // floor grid
        for (int x = x0; x <= x1; x++) {
            boolean isGradOrMarginX = x == x0 || x == x1 ||
                    (enableGradX && Math.floorMod(x - xMin, graduationStep) == 0);
            for (int z = z0; z < z1; z++) {
                LineSegment segment = new LineSegment(x, yGrid, z, x, yGrid, z + 1);
                if (isGradOrMarginX) gridMargins.add(segment);
                else gridLines.add(segment);
            }
        }
        for (int z = z0; z <= z1; z++) {
            boolean isGradOrMarginZ = z == z0 || z == z1 ||
                    (enableGradZ && Math.floorMod(z - zMin, graduationStep) == 0);
            for (int x = x0; x < x1; x++) {
                LineSegment segment = new LineSegment(x, yGrid, z, x + 1, yGrid, z);
                if (isGradOrMarginZ) gridMargins.add(segment);
                else gridLines.add(segment);
            }
        }

        // wall grid
        for (int x = x0; x <= x1; x++) {
            boolean isGradOrMarginX = x == x0 || x == x1 ||
                    (enableGradX && Math.floorMod(x - xMin, graduationStep) == 0);
            for (int y = y0; y < y1; y++) {
                LineSegment segment = new LineSegment(x, y, zWall, x, y + 1, zWall);
                if (isGradOrMarginX) gridMargins.add(segment);
                else gridLines.add(segment);
            }
        }
        for (int y = y0; y <= y1; y++) {
            boolean isGradOrMarginY = y == y0 || y == y1 ||
                    (enableGradY && Math.floorMod(y - yMin, graduationStep) == 0);
            for (int x = x0; x < x1; x++) {
                LineSegment segment = new LineSegment(x, y, zWall, x + 1, y, zWall);
                if (isGradOrMarginY) gridMargins.add(segment);
                else gridLines.add(segment);
            }
        }

        for (int z = z0; z <= z1; z++) {
            boolean isGradOrMarginZ = z == z0 || z == z1 ||
                    (enableGradZ && Math.floorMod(z - zMin, graduationStep) == 0);
            for (int y = y0; y < y1; y++) {
                LineSegment segment = new LineSegment(xWall, y, z, xWall, y + 1, z);
                if (isGradOrMarginZ) gridMargins.add(segment);
                else gridLines.add(segment);
            }
        }
        for (int y = y0; y <= y1; y++) {
            boolean isGradOrMarginY = y == y0 || y == y1 ||
                    (enableGradY && Math.floorMod(y - yMin, graduationStep) == 0);
            for (int z = z0; z < z1; z++) {
                LineSegment segment = new LineSegment(xWall, y, z, xWall, y, z + 1);
                if (isGradOrMarginY) gridMargins.add(segment);
                else gridLines.add(segment);
            }
        }

        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);

        PoseStack poseStack = new PoseStack();

        float guiScale = (float) Math.max(1.0, Minecraft.getInstance().getWindow().getGuiScale());
        float minorWidth = Math.max(3.0f, guiScale * 1.25f);
        float majorWidth = Math.max(5.0f, guiScale * 2.25f);

        // grid lines
        if (!gridLines.isEmpty()) {
            RenderSystem.lineWidth(minorWidth);
            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            for (LineSegment line : gridLines) {
                putLine(builder, poseStack, line.x1, line.y1, line.z1, line.x2, line.y2, line.z2, COLOR_GRID_LINES);
            }
            BufferUploader.drawWithShader(builder.end());
        }

        // margin lines
        if (!gridMargins.isEmpty()) {
            RenderSystem.lineWidth(majorWidth);
            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            for (LineSegment line : gridMargins) {
                putLine(builder, poseStack, line.x1, line.y1, line.z1, line.x2, line.y2, line.z2, COLOR_GRID_MARGINS);
            }
            BufferUploader.drawWithShader(builder.end());
        }

        RenderSystem.lineWidth(1.0f);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    private static void putLine(VertexConsumer consumer, PoseStack poseStack,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-5f) return;
        float nx = dx / len;
        float ny = dy / len;
        float nz = dz / len;

        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        consumer.vertex(pose, x1, y1, z1).color(r, g, b, a).normal(normalMatrix, nx, ny, nz).endVertex();
        consumer.vertex(pose, x2, y2, z2).color(r, g, b, a).normal(normalMatrix, nx, ny, nz).endVertex();
    }
}
