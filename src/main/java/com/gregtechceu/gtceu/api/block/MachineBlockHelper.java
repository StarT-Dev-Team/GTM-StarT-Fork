package com.gregtechceu.gtceu.api.block;

import com.gregtechceu.gtceu.api.block.property.GTBlockStateProperties;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.item.IGTTool;
import com.gregtechceu.gtceu.api.item.MetaMachineItem;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.item.tool.ToolHelper;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.feature.*;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.machine.owner.MachineOwner;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class MachineBlockHelper {

    private final IMachineBlock machineBlock;
    private final Block block;
    private final MachineDefinition definition;

    public MachineBlockHelper(IMachineBlock block) {
        this.machineBlock = block;
        this.block = block.self();
        this.definition = block.getDefinition();
    }

    public BlockState prepareDefaultState() {
        RotationState rotationState = definition.getRotationState();
        if (rotationState != RotationState.NONE) {
            BlockState defaultState = block.defaultBlockState().setValue(rotationState.property,
                    rotationState.defaultDirection);
            if (definition.isAllowExtendedFacing()) {
                defaultState = defaultState.setValue(GTBlockStateProperties.UPWARDS_FACING, Direction.NORTH);
            }
            return defaultState;
        }
        return null;
    }

    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        RotationState rotationState = MachineDefinition.getBuilt().getRotationState();
        if (rotationState != RotationState.NONE) {
            builder.add(rotationState.property);
            if (MachineDefinition.getBuilt().isAllowExtendedFacing()) {
                builder.add(GTBlockStateProperties.UPWARDS_FACING);
            }
        }
    }

    public VoxelShape getShape(BlockState pState) {
        return machineBlock.getRotationState() == RotationState.NONE ? definition.getShape(Direction.NORTH) :
                definition.getShape(pState.getValue(machineBlock.getRotationState().property));
    }

    public RenderShape getRenderShape() {
        return RenderShape.MODEL;
    }

    public void animateTick(Level level, BlockPos pos, RandomSource random) {
        var machine = machineBlock.getMachine(level, pos);
        if (machine != null) {
            machine.animateTick(random);
        }
    }

    public void setPlacedBy(Level level, BlockPos pos, @Nullable LivingEntity player, ItemStack stack) {
        if (!level.isClientSide) {
            var machine = machineBlock.getMachine(level, pos);
            if (machine != null) {
                if (player instanceof ServerPlayer sPlayer) {
                    machine.setOwnerUUID(sPlayer.getUUID());
                    machine.markDirty();
                }
            }
            if (machine instanceof IDropSaveMachine dropSaveMachine) {
                CompoundTag tag = stack.getTag();
                if (tag != null) {
                    dropSaveMachine.loadFromItem(tag);
                }
            }
            if (machine instanceof IMachineLife machineLife) {
                machineLife.onMachinePlaced(player, stack);
            }
        }
    }

    public void onPlace(Level level, BlockPos pos) {
        // needed to trigger block updates so machines connect to open cables properly.
        level.updateNeighbourForOutputSignal(pos, block);
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        RotationState rotationState = machineBlock.getRotationState();
        var player = context.getPlayer();
        var blockPos = context.getClickedPos();
        var state = block.defaultBlockState();
        if (player != null && rotationState != RotationState.NONE) {
            if (rotationState == RotationState.Y_AXIS) {
                state = state.setValue(rotationState.property, Direction.UP);
            } else {
                state = state.setValue(rotationState.property, player.getDirection().getOpposite());
            }
            Vec3 pos = player.position();
            if (Math.abs(pos.x - (double) ((float) blockPos.getX() + 0.5F)) < 2.0D &&
                    Math.abs(pos.z - (double) ((float) blockPos.getZ() + 0.5F)) < 2.0D) {
                double d0 = pos.y + (double) player.getEyeHeight();
                if (d0 - (double) blockPos.getY() > 2.0D && rotationState.test(Direction.UP)) {
                    state = state.setValue(rotationState.property, Direction.UP);
                }
                if ((double) blockPos.getY() - d0 > 0.0D && rotationState.test(Direction.DOWN)) {
                    state = state.setValue(rotationState.property, Direction.DOWN);
                }
            }
            if (definition.isAllowExtendedFacing()) {
                Direction frontFacing = state.getValue(rotationState.property);
                if (frontFacing == Direction.UP) {
                    state = state.setValue(GTBlockStateProperties.UPWARDS_FACING, player.getDirection());
                } else if (frontFacing == Direction.DOWN) {
                    state = state.setValue(GTBlockStateProperties.UPWARDS_FACING, player.getDirection().getOpposite());
                }
            }
        }
        return state;
    }

    public ItemStack getCloneItemStack(ItemStack superItemStack, BlockGetter level, BlockPos pos) {
        if (machineBlock.getMachine(level, pos) instanceof IDropSaveMachine dropSaveMachine &&
                dropSaveMachine.savePickClone()) {
            dropSaveMachine.saveToItem(superItemStack.getOrCreateTag());
        }
        return superItemStack;
    }

    public void appendHoverText(ItemStack stack, List<Component> tooltip) {
        definition.getTooltipBuilder().accept(stack, tooltip);
        String mainKey = String.format("%s.machine.%s.tooltip", definition.getId().getNamespace(),
                definition.getId().getPath());
        if (GTUtil.isShiftDown()) {
            if (definition instanceof MultiblockMachineDefinition multiblockDefinition) {
                var pattern = multiblockDefinition.getPatternFactory().get();
                if (pattern != null) {
                    var aisleDims = pattern.getDimensions();
                    assert aisleDims.length == 3;
                    tooltip.add(Component.translatable("gtceu.multiblock.dimension", aisleDims[0], aisleDims[1],
                            aisleDims[2]));
                }
            }
        }
        if (Language.getInstance().has(mainKey)) {
            tooltip.add(1, Component.translatable(mainKey));
        }
    }

    public boolean triggerEvent(Level level, BlockPos pos, int id, int param) {
        BlockEntity tile = level.getBlockEntity(pos);
        if (tile != null) {
            return tile.triggerEvent(id, param);
        }
        return false;
    }

    public BlockState rotate(BlockState state, Rotation rotation) {
        if (machineBlock.getRotationState() == RotationState.NONE) {
            return state;
        }
        return state.setValue(machineBlock.getRotationState().property,
                rotation.rotate(state.getValue(machineBlock.getRotationState().property)));
    }

    public List<ItemStack> getDrops(List<ItemStack> superDrops, LootParams.Builder builder) {
        BlockEntity tileEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (tileEntity instanceof IMachineBlockEntity holder) {
            var machine = holder.getMetaMachine();
            if (machine instanceof IMachineModifyDrops machineModifyDrops) {
                machineModifyDrops.onDrops(superDrops);
            }
            if (machine instanceof IDropSaveMachine dropSaveMachine && dropSaveMachine.saveBreak()) {
                for (ItemStack drop : superDrops) {
                    if (drop.getItem() instanceof MetaMachineItem item && item.getBlock() == block) {
                        dropSaveMachine.saveToItem(drop.getOrCreateTag());
                        // break here to not dupe contents if a machine drops multiple of itself for whatever reason.
                        break;
                    }
                }
            }
        }
        return superDrops;
    }

    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState) {
        if (state.hasBlockEntity()) {
            if (!state.is(newState.getBlock())) { // new block
                MetaMachine machine = machineBlock.getMachine(level, pos);
                if (machine instanceof IMachineLife machineLife) {
                    machineLife.onMachineRemoved();
                }
                if (machine != null) {
                    for (Direction direction : GTUtil.DIRECTIONS) {
                        machine.getCoverContainer().removeCover(direction, null);
                    }
                }

                level.updateNeighbourForOutputSignal(pos, block);
                level.removeBlockEntity(pos);
            } else if (machineBlock.getRotationState() != RotationState.NONE) { // old block different facing
                var oldFacing = state.getValue(machineBlock.getRotationState().property);
                var newFacing = newState.getValue(machineBlock.getRotationState().property);
                if (newFacing != oldFacing) {
                    var machine = machineBlock.getMachine(level, pos);
                    if (machine != null) {
                        machine.onRotated(oldFacing, newFacing);
                    }
                }
            }
        }
    }

    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        var machine = machineBlock.getMachine(world, pos);
        ItemStack itemStack = player.getItemInHand(hand);
        boolean shouldOpenUi = true;

        if (machine != null && machine.getOwnerUUID() == null && player instanceof ServerPlayer sPlayer) {
            machine.setOwnerUUID(sPlayer.getUUID());
            machine.markDirty();
        }

        Set<GTToolType> types = ToolHelper.getToolTypes(itemStack);
        if (machine != null &&
                (!types.isEmpty() && ToolHelper.canUse(itemStack) || types.isEmpty() && player.isShiftKeyDown())) {
            var result = machine.onToolClick(types, itemStack, new UseOnContext(player, hand, hit));
            if (result.getSecond() == InteractionResult.CONSUME && player instanceof ServerPlayer serverPlayer) {
                ToolHelper.playToolSound(result.getFirst(), serverPlayer);

                if (!serverPlayer.isCreative()) {
                    ToolHelper.damageItem(itemStack, serverPlayer, 1);
                }
            }
            if (result.getSecond() != InteractionResult.PASS) return result.getSecond();
        }

        if (itemStack.is(GTItems.PORTABLE_SCANNER.get())) {
            return itemStack.getItem().use(world, player, hand).getResult();
        }

        if (itemStack.getItem() instanceof IGTTool gtToolItem) {
            shouldOpenUi = gtToolItem.definition$shouldOpenUIAfterUse(new UseOnContext(player, hand, hit));
        }

        if (machine instanceof IInteractedMachine interactedMachine) {
            var result = interactedMachine.onUse(state, world, pos, player, hand, hit);
            if (result != InteractionResult.PASS) return result;
        }
        if (shouldOpenUi && machine instanceof IUIMachine uiMachine &&
                MachineOwner.canOpenOwnerMachine(player, machine)) {
            return uiMachine.tryToOpenUI(player, hand, hit);
        }
        return shouldOpenUi ? InteractionResult.PASS : InteractionResult.CONSUME;
    }

    @SuppressWarnings("deprecation") // This is fine to override, just not to be called.
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return machineBlock.getMachine(level, pos).getOutputSignal(direction);
    }

    @SuppressWarnings("deprecation") // This is fine to override, just not to be called.
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return machineBlock.getMachine(level, pos).getOutputDirectSignal(direction);
    }

    @SuppressWarnings("deprecation") // This is fine to override, just not to be called.
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return machineBlock.getMachine(level, pos).getAnalogOutputSignal();
    }

    public void neighborChanged(Level level, BlockPos pos, Block block, BlockPos fromPos,
                                boolean isMoving) {
        var machine = machineBlock.getMachine(level, pos);
        if (machine != null) {
            machine.onNeighborChanged(block, fromPos, isMoving);
        }
    }

    public @Nullable BlockState getAppearance(BlockState state, BlockAndTintGetter level, BlockPos pos, Direction side,
                                              @Nullable BlockState sourceState, @Nullable BlockPos sourcePos) {
        var machine = machineBlock.getMachine(level, pos);
        if (machine != null) {
            return machine.getBlockAppearance(state, level, pos, side, sourceState, sourcePos);
        }
        return null;
    }

    public boolean isValidSpawn(BlockState state, BlockGetter level, BlockPos pos, SpawnPlacements.Type type,
                                EntityType<?> entityType) {
        return false;
    }
}
