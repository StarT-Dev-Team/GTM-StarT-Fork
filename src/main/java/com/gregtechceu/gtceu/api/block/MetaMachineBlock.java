package com.gregtechceu.gtceu.api.block;

import com.gregtechceu.gtceu.api.gui.factory.MachineUIFactory;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.feature.*;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@SuppressWarnings("deprecation")
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MetaMachineBlock extends Block implements IMachineBlock, BlockUIMenuType.BlockUI {

    @Getter
    public final MachineDefinition definition;

    private final MachineBlockHelper helper = new MachineBlockHelper(this);

    public MetaMachineBlock(Properties properties, MachineDefinition definition) {
        super(properties);
        this.definition = definition;

        var defaultBlockState = helper.prepareDefaultState();
        if (defaultBlockState != null) {
            registerDefaultState(defaultBlockState);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        helper.createBlockStateDefinition(builder);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return helper.getShape(state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return helper.getRenderShape();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        helper.animateTick(level, pos, random);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity player,
                            ItemStack stack) {
        helper.setPlacedBy(level, pos, player, stack);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        helper.onPlace(level, pos);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return helper.getStateForPlacement(context);
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return MachineUIFactory.createUI(holder);
    }

    @Override
    public boolean stillValid(BlockUIMenuType.BlockUIHolder holder) {
        return MachineUIFactory.stillValid(holder);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        ItemStack itemStack = super.getCloneItemStack(level, pos, state);
        return helper.getCloneItemStack(itemStack, level, pos);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip,
                                TooltipFlag flag) {
        helper.appendHoverText(stack, tooltip);
    }

    @Override
    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int pId, int param) {
        return helper.triggerEvent(level, pos, pId, param);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return helper.rotate(state, rotation);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        var drops = super.getDrops(state, builder);
        return helper.getDrops(drops, builder);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        helper.onRemove(state, level, pos, newState);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        return helper.use(state, world, pos, player, hand, hit);
    }

    @Override
    @SuppressWarnings("deprecation") // This is fine to override, just not to be called.
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return helper.getSignal(state, level, pos, direction);
    }

    @Override
    @SuppressWarnings("deprecation") // This is fine to override, just not to be called.
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return helper.getDirectSignal(state, level, pos, direction);
    }

    @Override
    @SuppressWarnings("deprecation") // This is fine to override, just not to be called.
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return helper.getAnalogOutputSignal(state, level, pos);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
                                boolean isMoving) {
        helper.neighborChanged(level, pos, block, fromPos, isMoving);
    }

    @Override
    public BlockState getAppearance(BlockState state, BlockAndTintGetter level, BlockPos pos, Direction side,
                                    @Nullable BlockState sourceState, @Nullable BlockPos sourcePos) {
        var appearance = helper.getAppearance(state, level, pos, side, sourceState, sourcePos);
        if (appearance != null) {
            return appearance;
        }
        return super.getAppearance(state, level, pos, side, sourceState, sourcePos);
    }

    @Override
    public boolean isValidSpawn(BlockState state, BlockGetter level, BlockPos pos, SpawnPlacements.Type type,
                                EntityType<?> entityType) {
        return helper.isValidSpawn(state, level, pos, type, entityType);
    }
}
