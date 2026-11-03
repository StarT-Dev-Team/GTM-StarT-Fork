package com.gregtechceu.gtceu.api.gui.widget.preview;

import com.gregtechceu.gtceu.api.gui.widget.PatternSceneWidget;

import com.lowdragmc.lowdraglib.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib.client.utils.RenderUtils;
import com.lowdragmc.lowdraglib.utils.BlockPosFace;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class PreviewSceneWidget extends PatternSceneWidget {

    public interface Host {

        boolean hasSelection();

        boolean isOverVariantUi(double mouseX, double mouseY);

        void clearBlockSelection();

        void stepLayer(int delta);
    }

    private final Host host;

    public PreviewSceneWidget(int x, int y, int width, int height, Level world, Host host) {
        super(x, y, width, height, world);
        this.host = host;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean result = super.mouseClicked(mouseX, mouseY, button);
        if ((button == 0 || button == 1) && host.hasSelection() && isMouseOverElement(mouseX, mouseY) &&
                !host.isOverVariantUi(mouseX, mouseY)) {
            BlockHitResult hit = renderer.getLastTraceResult();
            if (hit == null || hit.getType() == HitResult.Type.MISS) {
                host.clearBlockSelection();
            }
        }
        return result;
    }

    @Override
    public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
        if (isMouseOverElement(mouseX, mouseY) && (Screen.hasShiftDown() || Screen.hasControlDown())) {
            host.stepLayer(wheelDelta > 0 ? 1 : -1);
            return true;
        }
        return super.mouseWheelMove(mouseX, mouseY, wheelDelta);
    }

    @Override
    public void renderBlockOverLay(WorldSceneRenderer renderer) {
        PoseStack poseStack = new PoseStack();
        hoverPosFace = null;
        hoverItem = null;
        if (isMouseOverElement(currentMouseX, currentMouseY)) {
            hoverPosFace = resolveHover(renderer);
        }
        if (hoverPosFace != null) {
            var state = getDummyWorld().getBlockState(hoverPosFace.pos);
            hoverItem = state.getBlock().getCloneItemStack(getDummyWorld(), hoverPosFace.pos, state);
        }
        BlockPosFace tmp = dragging ? clickPosFace : hoverPosFace;
        if (renderFacing) {
            if (selectedPosFace != null) {
                drawFacingBorder(poseStack, selectedPosFace, 0xff00ff00);
            }
            if (tmp != null && !tmp.equals(selectedPosFace)) {
                drawFacingBorder(poseStack, tmp, 0xffffffff);
            }
        }
        if (selectedPosFace != null && renderSelect) {
            RenderUtils.renderBlockOverLay(poseStack, selectedPosFace.pos, 0.6f, 0, 0, 1.03f);
        }

        if (this.afterWorldRender != null) {
            this.afterWorldRender.accept(this);
        }
    }

    @Nullable
    private BlockPosFace resolveHover(WorldSceneRenderer renderer) {
        BlockHitResult hit = renderer.getLastTraceResult();
        if (hit == null) return null;
        if (core.contains(hit.getBlockPos())) {
            return new BlockPosFace(hit.getBlockPos(), hit.getDirection());
        }
        if (useOrtho) return null;

        Vector3f hitPos = hit.getLocation().toVector3f();
        Level world = renderer.world;
        Vec3 eyePos = new Vec3(renderer.getEyePos());
        hitPos.mul(2);
        Vec3 endPos = new Vec3((hitPos.x - eyePos.x), (hitPos.y - eyePos.y), (hitPos.z - eyePos.z));
        double min = Float.MAX_VALUE;
        BlockPosFace result = null;
        for (BlockPos pos : core) {
            BlockState blockState = world.getBlockState(pos);
            if (blockState.getBlock() == Blocks.AIR) continue;
            BlockHitResult candidate = world.clipWithInteractionOverride(eyePos, endPos, pos,
                    blockState.getShape(world, pos), blockState);
            if (candidate != null && candidate.getType() != HitResult.Type.MISS) {
                double dist = eyePos.distanceToSqr(candidate.getLocation());
                if (dist < min) {
                    min = dist;
                    result = new BlockPosFace(candidate.getBlockPos(), candidate.getDirection());
                }
            }
        }
        return result;
    }
}
