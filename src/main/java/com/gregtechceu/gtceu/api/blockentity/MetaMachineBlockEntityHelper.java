package com.gregtechceu.gtceu.api.blockentity;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.client.model.IBlockEntityRendererBakedModel;
import com.gregtechceu.gtceu.client.model.machine.MachineRenderState;
import com.gregtechceu.gtceu.common.datafixers.TagFixer;
import com.gregtechceu.gtceu.syncdata.LegacyManagedData;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.IBlockEntityManaged;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import com.lowdragmc.lowdraglib2.syncdata.storage.MultiManagedStorage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class MetaMachineBlockEntityHelper implements IBlockEntityManaged {

    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    private IMachineBlockEntity machineBlockEntity;

    private BlockEntity blockEntity;

    @Getter
    private final MetaMachine metaMachine;

    @Getter
    public final MultiManagedStorage rootStorage = new MultiManagedStorage();

    @Getter
    @Persisted
    @DescSynced
    @RequireRerender
    private MachineRenderState renderState;

    @Getter
    private final long offset = GTValues.RNG.nextInt(20);

    public MetaMachineBlockEntityHelper(IMachineBlockEntity be) {
        machineBlockEntity = be;
        blockEntity = be.self();
        metaMachine = be.getDefinition().createMetaMachine(be);
        renderState = be.getDefinition().defaultRenderState();

        rootStorage.attach(be.getSyncStorage());
        rootStorage.attach(getSyncStorage());
    }

    public boolean triggerEvent(int id, int para) {
        if (id == 1) { // chunk re-render
            if (blockEntity.getLevel() != null && blockEntity.getLevel().isClientSide) {
                machineBlockEntity.scheduleRenderUpdate();
            }
            return true;
        }
        return false;
    }

    public void setRenderState(MachineRenderState state) {
        this.renderState = state;
        machineBlockEntity.scheduleRenderUpdate();
    }

    public void setRemoved() {
        metaMachine.onUnload();
    }

    public void onLoad() {
        metaMachine.onLoad();
    }

    public boolean shouldRenderGrid(Player player, BlockPos pos, BlockState state, ItemStack held,
                                    Set<GTToolType> toolTypes) {
        return metaMachine.shouldRenderGrid(player, pos, state, held, toolTypes);
    }

    public @Nullable SpriteTexture sideTips(Player player, BlockPos pos, BlockState state, Set<GTToolType> toolTypes,
                                            Direction side) {
        return metaMachine.sideTips(player, pos, state, toolTypes, side);
    }

    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return MetaMachineBlockEntity.getCapability(getMetaMachine(), cap, side);
    }

    public void setChanged() {
        if (blockEntity.getLevel() != null) {
            blockEntity.getLevel().blockEntityChanged(blockEntity.getBlockPos());
        }
    }

    /**
     * Why, Forge, Why?
     * Why must you make me add a method for no good reason?
     */
    @SuppressWarnings("unchecked")
    @OnlyIn(Dist.CLIENT)
    public AABB getRenderBoundingBox() {
        BlockRenderDispatcher blockRenderDispatcher = Minecraft.getInstance().getBlockRenderer();
        BakedModel model = blockRenderDispatcher.getBlockModel(blockEntity.getBlockState());

        if (model instanceof IBlockEntityRendererBakedModel<?> modelWithBER) {
            if (modelWithBER.getBlockEntityType() == blockEntity.getType()) {
                return ((IBlockEntityRendererBakedModel<BlockEntity>) modelWithBER).getRenderBoundingBox(blockEntity);
            }
        }
        return new AABB(blockEntity.getBlockPos().offset(-1, 0, -1), blockEntity.getBlockPos().offset(2, 2, 2));
    }

    public void load(CompoundTag tag) {
        TagFixer.fixFluidTags(tag);
        LegacyManagedData.upgradeBlockEntityTag(getRootStorage(), tag, machineBlockEntity.getSyncTag());
    }

    @Override
    public void notifyPersistence() {
        machineBlockEntity.notifyPersistence();
    }

    @Override
    public BlockEntity asBlockEntity() {
        return blockEntity;
    }

    @Override
    public void scheduleRenderUpdate() {
        machineBlockEntity.scheduleRenderUpdate();
    }
}
