package com.gregtechceu.gtceu.common.machine.multiblock.primitive;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.widget.ProgressTextureBar;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IUIMachine;
import com.gregtechceu.gtceu.config.ConfigHolder;

import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidUtil;

import dev.vfyjxf.taffy.style.TaffyPosition;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CokeOvenMachine extends PrimitiveWorkableMachine implements IUIMachine {

    public CokeOvenMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        var area = new UIElement();
        area.layout(layout -> layout.height(82));

        var title = new Label();
        title.setText(Component.translatable(getBlockState().getBlock().getDescriptionId()));
        title.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(5).top(5));

        var input = new ItemSlot().bind(importItems.storage, 0);
        input.style(style -> style.backgroundTexture(
                new GuiTextureGroup(GuiTextures.PRIMITIVE_SLOT, GuiTextures.PRIMITIVE_FURNACE_OVERLAY)));
        input.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(52).top(30));

        var progress = ProgressTextureBar.of(GuiTextures.PRIMITIVE_BLAST_FURNACE_PROGRESS_BAR,
                FillDirection.LEFT_TO_RIGHT, 20, 15);
        progress.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(76).top(32));
        progress.bindDataSource(SupplierDataSource.of(() -> (float) recipeLogic.getProgressPercent()));

        var output = new ItemSlot().bind(new ItemHandlerSlot(exportItems.storage, 0).setCanPlace(stack -> false));
        output.style(style -> style.backgroundTexture(
                new GuiTextureGroup(GuiTextures.PRIMITIVE_SLOT, GuiTextures.PRIMITIVE_FURNACE_OVERLAY)));
        output.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(103).top(30));

        var tank = new FluidSlot().bind(exportFluids.getStorages()[0], 0);
        tank.setAllowClickFilled(false);
        tank.setAllowClickDrained(true);
        tank.slotStyle(style -> style.fillDirection(FillDirection.DOWN_TO_UP)
                .slotOverlay(GuiTextures.PRIMITIVE_LARGE_FLUID_TANK_OVERLAY));
        tank.amountLabel.setDisplay(false);
        tank.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(134).top(13).width(20).height(58));
        tank.style(style -> style.backgroundTexture(GuiTextures.PRIMITIVE_LARGE_FLUID_TANK));

        area.addChildren(title, input, progress, output, tank);

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
                getLevel().playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F,
                        false);
            }
            getLevel().addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0, 0, 0);
            getLevel().addParticle(ParticleTypes.FLAME, x, y, z, 0, 0, 0);
        }
    }

    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                   BlockHitResult hit) {
        if (!isRemote()) {
            if (super.onUse(state, world, pos, player, hand, hit) == InteractionResult.SUCCESS) {
                return InteractionResult.SUCCESS;
            }
            if (FluidUtil.interactWithFluidHandler(player, hand, exportFluids)) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        return super.onUse(state, world, pos, player, hand, hit);
    }
}
