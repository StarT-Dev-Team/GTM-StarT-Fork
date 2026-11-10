package com.gregtechceu.gtceu.common.pipelike.laser;

import com.gregtechceu.gtceu.api.capability.ILaserContainer;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.gregtechceu.gtceu.api.pipenet.PipeNetWalker;
import com.gregtechceu.gtceu.common.blockentity.LaserPipeBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LaserNetWalker extends PipeNetWalker<LaserPipeBlockEntity, LaserPipeProperties, LaserPipeNet> {

    public static final LaserRoutePath FAILED_MARKER = new LaserRoutePath(null, null, 0);

    @Nullable
    public static LaserRoutePath createNetData(LaserPipeNet world, BlockPos sourcePipe, Direction faceToSourceHandler) {
        try {
            LaserNetWalker walker = new LaserNetWalker(world, sourcePipe, 1);
            walker.sourcePipe = sourcePipe;
            walker.facingToHandler = faceToSourceHandler;
            walker.traversePipeNet();
            return walker.routePath;
        } catch (Exception e) {
            return FAILED_MARKER;
        }
    }

    private LaserRoutePath routePath;
    private BlockPos sourcePipe;
    private Direction facingToHandler;

    protected LaserNetWalker(LaserPipeNet world, BlockPos sourcePipe, int distance) {
        super(world, sourcePipe, distance);
    }

    @NotNull
    @Override
    protected PipeNetWalker<LaserPipeBlockEntity, LaserPipeProperties, LaserPipeNet> createSubWalker(LaserPipeNet net,
                                                                                                     Direction direction,
                                                                                                     BlockPos nextPos,
                                                                                                     int walkedBlocks) {
        LaserNetWalker walker = new LaserNetWalker(net, nextPos, walkedBlocks);
        walker.facingToHandler = facingToHandler;
        walker.sourcePipe = sourcePipe;
        return walker;
    }

    @Override
    protected Class<LaserPipeBlockEntity> getBasePipeClass() {
        return LaserPipeBlockEntity.class;
    }

    @Override
    protected void checkPipe(LaserPipeBlockEntity pipeTile, BlockPos pos) {}

    @Override
    protected void checkNeighbour(LaserPipeBlockEntity pipeNode, BlockPos pipePos, Direction faceToNeighbour,
                                  @org.jetbrains.annotations.Nullable BlockEntity neighbourTile) {
        if (neighbourTile == null || (pipePos.equals(sourcePipe) && faceToNeighbour == facingToHandler)) {
            return;
        }

        if (((LaserNetWalker) root).routePath == null) {
            ILaserContainer handler = neighbourTile
                    .getCapability(GTCapability.CAPABILITY_LASER, faceToNeighbour.getOpposite()).resolve().orElse(null);
            if (handler != null) {
                ((LaserNetWalker) root).routePath = new LaserRoutePath(pipePos.immutable(), faceToNeighbour,
                        getWalkedBlocks());
                stop();
            }
        }
    }
}
