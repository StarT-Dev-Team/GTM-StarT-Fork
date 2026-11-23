package com.gregtechceu.gtceu.common.machine.multiblock.primitive;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.widget.ProgressTextureBar;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IFluidRenderMulti;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.RequireRerender;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PrimitiveBlastFurnaceMachine extends PrimitiveWorkableMachine implements IUIMachine, IFluidRenderMulti {

    private TickableSubscription hurtSubscription;

    @Getter
    @Setter
    @DescSynced
    @RequireRerender
    private @NotNull Set<BlockPos> fluidBlockOffsets = new HashSet<>();

    public PrimitiveBlastFurnaceMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    @Override
    protected NotifiableItemStackHandler createImportItemHandler(Object... args) {
        return new NotifiableItemStackHandler(this, getRecipeType().getMaxInputs(ItemRecipeCapability.CAP), IO.IN,
                IO.NONE);
    }

    @Override
    protected NotifiableItemStackHandler createExportItemHandler(Object... args) {
        return new NotifiableItemStackHandler(this, getRecipeType().getMaxOutputs(ItemRecipeCapability.CAP), IO.OUT,
                IO.NONE);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        unsubscribe(hurtSubscription);
        hurtSubscription = null;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        IFluidRenderMulti.super.onStructureFormed();
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        IFluidRenderMulti.super.onStructureInvalid();
    }

    @Override
    public void notifyStatusChanged(RecipeLogic.Status oldStatus, RecipeLogic.Status newStatus) {
        super.notifyStatusChanged(oldStatus, newStatus);
        if (newStatus == RecipeLogic.Status.WORKING) {
            this.hurtSubscription = subscribeServerTick(this.hurtSubscription, this::hurtEntitiesAndBreakSnow);
        } else if (oldStatus == RecipeLogic.Status.WORKING && hurtSubscription != null) {
            unsubscribe(hurtSubscription);
            hurtSubscription = null;
        }
    }

    @Override
    public @NotNull Set<BlockPos> saveOffsets() {
        return Collections.singleton(new BlockPos(getFrontFacing().getOpposite().getNormal()));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void clientTick() {
        super.clientTick();
        if (isFormed) {
            var pos = this.getPos();
            var facing = this.getFrontFacing().getOpposite();
            float xPos = facing.getStepX() * 0.76F + pos.getX() + 0.5F;
            float yPos = facing.getStepY() * 0.76F + pos.getY() + 0.25F;
            float zPos = facing.getStepZ() * 0.76F + pos.getZ() + 0.5F;

            var up = RelativeDirection.UP.getRelative(getFrontFacing(), getUpwardsFacing(), isFlipped());
            var sign = up.getAxisDirection().getStep();
            var shouldX = up.getAxis() == Direction.Axis.X;
            var shouldY = up.getAxis() == Direction.Axis.Y;
            var shouldZ = up.getAxis() == Direction.Axis.Z;
            var speed = ((shouldY ? facing.getStepY() : shouldX ? facing.getStepX() : facing.getStepZ()) * 0.1F + 0.2F +
                    0.1F * GTValues.RNG.nextFloat()) * sign;
            if (getOffsetTimer() % 20 == 0) {
                getLevel().addParticle(ParticleTypes.LAVA, xPos, yPos, zPos,
                        shouldX ? speed * 2 : 0,
                        shouldY ? speed * 2 : 0,
                        shouldZ ? speed * 2 : 0);
            }
            if (isActive()) {
                getLevel().addParticle(ParticleTypes.LARGE_SMOKE, xPos, yPos, zPos,
                        shouldX ? speed : 0,
                        shouldY ? speed : 0,
                        shouldZ ? speed : 0);
            }
        }
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        var area = new UIElement();
        area.layout(layout -> layout.height(82));

        var title = new Label();
        title.setText(Component.translatable(getBlockState().getBlock().getDescriptionId()));
        title.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(5).top(5));

        var in0 = new ItemSlot().bind(importItems.storage, 0);
        in0.style(style -> style.backgroundTexture(
                new GuiTextureGroup(GuiTextures.PRIMITIVE_SLOT, GuiTextures.PRIMITIVE_INGOT_OVERLAY)));
        in0.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(52).top(20));
        var in1 = new ItemSlot().bind(importItems.storage, 1);
        in1.style(style -> style.backgroundTexture(
                new GuiTextureGroup(GuiTextures.PRIMITIVE_SLOT, GuiTextures.PRIMITIVE_DUST_OVERLAY)));
        in1.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(52).top(38));
        var in2 = new ItemSlot().bind(importItems.storage, 2);
        in2.style(style -> style.backgroundTexture(
                new GuiTextureGroup(GuiTextures.PRIMITIVE_SLOT, GuiTextures.PRIMITIVE_FURNACE_OVERLAY)));
        in2.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(52).top(56));

        var progress = ProgressTextureBar.of(GuiTextures.PRIMITIVE_BLAST_FURNACE_PROGRESS_BAR,
                FillDirection.LEFT_TO_RIGHT, 20, 15);
        progress.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(77).top(39));
        progress.bindDataSource(SupplierDataSource.of(() -> (float) recipeLogic.getProgressPercent()));

        var out0 = new ItemSlot().bind(new ItemHandlerSlot(exportItems.storage, 0).setCanPlace(stack -> false));
        out0.style(style -> style.backgroundTexture(
                new GuiTextureGroup(GuiTextures.PRIMITIVE_SLOT, GuiTextures.PRIMITIVE_INGOT_OVERLAY)));
        out0.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(104).top(38));
        var out1 = new ItemSlot().bind(new ItemHandlerSlot(exportItems.storage, 1).setCanPlace(stack -> false));
        out1.style(style -> style.backgroundTexture(
                new GuiTextureGroup(GuiTextures.PRIMITIVE_SLOT, GuiTextures.PRIMITIVE_DUST_OVERLAY)));
        out1.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(122).top(38));
        var out2 = new ItemSlot().bind(new ItemHandlerSlot(exportItems.storage, 2).setCanPlace(stack -> false));
        out2.style(style -> style.backgroundTexture(
                new GuiTextureGroup(GuiTextures.PRIMITIVE_SLOT, GuiTextures.PRIMITIVE_DUST_OVERLAY)));
        out2.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(140).top(38));

        area.addChildren(title, in0, in1, in2, progress, out0, out1, out2);

        var root = UITemplate.panel();
        root.style(style -> style.background(GuiTextures.PRIMITIVE_BACKGROUND));
        root.layout(layout -> layout.width(176).paddingAll(0).paddingBottom(7));
        root.addChildren(area, UITemplate.bindPlayerInventory(GuiTextures.PRIMITIVE_SLOT));
        return UITemplate.modularUI(root, entityPlayer);
    }

    @Override
    public void animateTick(RandomSource random) {
        if (this.isActive()) {
            final BlockPos pos = getPos();
            float x = pos.getX() + 0.5F;
            float z = pos.getZ() + 0.5F;

            final var facing = getFrontFacing();
            final float horizontalOffset = GTValues.RNG.nextFloat() * 0.6F - 0.3F;
            final float y = pos.getY() + GTValues.RNG.nextFloat() * 0.375F + 0.3F;

            if (facing.getAxis() == Direction.Axis.X) {
                if (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE) x += 0.52F;
                else x -= 0.52F;
                z += horizontalOffset;
            } else if (facing.getAxis() == Direction.Axis.Z) {
                if (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE) z += 0.52F;
                else z -= 0.52F;
                x += horizontalOffset;
            }
            if (ConfigHolder.INSTANCE.machines.machineSounds && GTValues.RNG.nextDouble() < 0.1) {
                getLevel().playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE,
                        SoundSource.BLOCKS, 1.0F, 1.0F, false);
            }
            getLevel().addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0, 0, 0);
            getLevel().addParticle(ParticleTypes.FLAME, x, y, z, 0, 0, 0);
        }
    }

    private void hurtEntitiesAndBreakSnow() {
        BlockPos middlePos = self().getPos().offset(getFrontFacing().getOpposite().getNormal());
        getLevel().getEntities(null, new AABB(middlePos)).forEach(e -> e.hurt(e.damageSources().lava(), 3.0f));

        if (getOffsetTimer() % 10 == 0) {
            BlockState state = getLevel().getBlockState(middlePos);
            GTUtil.tryBreakSnow(getLevel(), middlePos, state, true);
        }
    }
}
