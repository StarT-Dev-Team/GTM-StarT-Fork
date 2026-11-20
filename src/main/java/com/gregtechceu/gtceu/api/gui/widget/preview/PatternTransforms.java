package com.gregtechceu.gtceu.api.gui.widget.preview;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.block.property.GTBlockStateProperties;
import com.gregtechceu.gtceu.api.data.RotationState;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class PatternTransforms {

    private PatternTransforms() {}

    public static Direction.Axis getMirrorAxis(Direction front, Direction up) {
        if (up == Direction.NORTH || up == Direction.SOUTH) {
            if (front.getAxis() == Direction.Axis.Z || front.getAxis() == Direction.Axis.Y) {
                return Direction.Axis.X;
            } else {
                return Direction.Axis.Z;
            }
        } else {
            return Direction.Axis.Y;
        }
    }

    public static BlockPos transformOffset(BlockPos rel, Direction front, Direction up, boolean isFlipped,
                                           Direction.Axis mirrorAxis) {
        BlockPos o1 = rotateByFront(rel, front);
        BlockPos o2 = rotateByFrontAxis(o1, front, getRollRotation(up));
        return isFlipped ? mirror(o2, mirrorAxis) : o2;
    }

    public static BlockPos rotateByFront(BlockPos pos, Direction front) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        return switch (front) {
            case NORTH -> new BlockPos(x, y, z);
            case SOUTH -> new BlockPos(x, -y, -z);
            case EAST -> new BlockPos(-z, y, x);
            case WEST -> new BlockPos(z, y, -x);
            case UP -> new BlockPos(x, -z, y);
            case DOWN -> new BlockPos(x, z, -y);
        };
    }

    public static Rotation getRollRotation(Direction up) {
        return switch (up) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public static BlockPos rotateByFrontAxis(BlockPos pos, Direction frontFacing, Rotation rotation) {
        if (rotation == Rotation.NONE) return pos;

        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        if (rotation == Rotation.CLOCKWISE_180) {
            return switch (frontFacing.getAxis()) {
                case X -> new BlockPos(x, -y, -z);
                case Y -> new BlockPos(-x, y, -z);
                case Z -> new BlockPos(-x, -y, z);
            };
        }

        int step = frontFacing.getAxisDirection().getStep();
        boolean clockwise90 = rotation == Rotation.CLOCKWISE_90;

        return switch (frontFacing.getAxis()) {
            case Z -> {
                int newX = clockwise90 ? -step * y : step * y;
                int newY = clockwise90 ? step * x : -step * x;
                yield new BlockPos(newX, newY, z);
            }
            case X -> {
                int newZ = clockwise90 ? step * y : -step * y;
                int newY = clockwise90 ? -step * z : step * z;
                yield new BlockPos(x, newY, newZ);
            }
            case Y -> {
                int newX = clockwise90 ? -step * z : step * z;
                int newZ = clockwise90 ? step * x : -step * x;
                yield new BlockPos(newX, y, newZ);
            }
        };
    }

    public static BlockPos mirror(BlockPos pos, Direction.Axis axis) {
        return switch (axis) {
            case X -> new BlockPos(-pos.getX(), pos.getY(), pos.getZ());
            case Y -> new BlockPos(pos.getX(), -pos.getY(), pos.getZ());
            case Z -> new BlockPos(pos.getX(), pos.getY(), -pos.getZ());
        };
    }

    private static Direction getDirectionFromNormal(int x, int y, int z) {
        if (x > 0) return Direction.EAST;
        if (x < 0) return Direction.WEST;
        if (y > 0) return Direction.UP;
        if (y < 0) return Direction.DOWN;
        if (z > 0) return Direction.SOUTH;
        if (z < 0) return Direction.NORTH;
        return null;
    }

    public static Direction rotateFacing(Direction facing, Direction front, Direction up, boolean isFlipped,
                                         Direction.Axis mirrorAxis) {
        if (facing == null) return null;

        BlockPos normal = new BlockPos(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        BlockPos o3 = transformOffset(normal, front, up, isFlipped, mirrorAxis);

        Direction rotated = getDirectionFromNormal(o3.getX(), o3.getY(), o3.getZ());
        return (rotated != null) ? rotated : facing;
    }

    public static Direction getTopDirection(Direction facing, Direction upwardsFacing) {
        if (facing == null) facing = Direction.NORTH;
        if (upwardsFacing == null) upwardsFacing = Direction.NORTH;

        if (facing.getAxis() == Direction.Axis.Y) {
            return upwardsFacing.getAxis().isHorizontal() ? upwardsFacing : Direction.NORTH;
        }

        return switch (upwardsFacing) {
            case NORTH -> Direction.UP;
            case SOUTH -> Direction.DOWN;
            case EAST -> facing.getClockWise();
            case WEST -> facing.getCounterClockWise();
            default -> Direction.UP;
        };
    }

    public static Direction getUpwardsFacing(Direction facing, Direction top) {
        if (facing == null) facing = Direction.NORTH;
        if (top == null) top = Direction.UP;

        if (facing.getAxis() == Direction.Axis.Y) {
            return top.getAxis().isHorizontal() ? top : Direction.NORTH;
        }

        if (top == Direction.UP) {
            return Direction.NORTH;
        } else if (top == Direction.DOWN) {
            return Direction.SOUTH;
        } else if (top == facing.getClockWise()) {
            return Direction.EAST;
        } else if (top == facing.getCounterClockWise()) {
            return Direction.WEST;
        }

        return Direction.NORTH;
    }

    public static BlockState rotateBlockState(BlockState state, Direction front, Direction up, boolean isFlipped,
                                              Direction.Axis mirrorAxis) {
        if (state.getBlock() instanceof MetaMachineBlock machine) {
            var rotationState = machine.definition.getRotationState();
            boolean hasFacingProperty = rotationState != RotationState.NONE && rotationState.property != null &&
                    state.hasProperty(rotationState.property);
            Direction initialFacing = hasFacingProperty ? state.getValue(rotationState.property) : Direction.NORTH;

            Direction newFacing = rotateFacing(initialFacing, front, up, isFlipped, mirrorAxis);
            if (newFacing != null && hasFacingProperty && rotationState.test(newFacing)) {
                state = state.setValue(rotationState.property, newFacing);
            }

            if (machine.definition.isAllowExtendedFacing() &&
                    state.hasProperty(GTBlockStateProperties.UPWARDS_FACING)) {
                Direction initialUpwards = state.getValue(GTBlockStateProperties.UPWARDS_FACING);
                Direction initialTop = getTopDirection(initialFacing, initialUpwards);
                Direction newTop = rotateFacing(initialTop, front, up, isFlipped, mirrorAxis);

                Direction effectiveFacing = hasFacingProperty ? state.getValue(rotationState.property) :
                        (newFacing != null ? newFacing : front);

                Direction newUpwards = getUpwardsFacing(effectiveFacing, newTop);
                state = state.setValue(GTBlockStateProperties.UPWARDS_FACING, newUpwards);
            }
            return state;
        }

        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction facing = state.getValue(BlockStateProperties.FACING);
            Direction newFacing = rotateFacing(facing, front, up, isFlipped, mirrorAxis);
            if (newFacing != null) {
                state = state.setValue(BlockStateProperties.FACING, newFacing);
            }
        } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            Direction newFacing = rotateFacing(facing, front, up, isFlipped, mirrorAxis);
            if (newFacing != null && newFacing.getAxis().isHorizontal()) {
                state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, newFacing);
            }
        }

        return state;
    }
}
