package com.gregtechceu.gtceu.common.machine.storage;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.*;
import com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.common.data.GTItems;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.RequireRerender;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CrateMachine extends MetaMachine implements IUIMachine, IMachineLife,
                          IDropSaveMachine, IInteractedMachine {

    public static final BooleanProperty TAPED_PROPERTY = GTMachineModelProperties.IS_TAPED;

    @Getter
    private final Material material;
    @Getter
    private final int inventorySize;
    @Getter
    @RequireRerender
    @Persisted
    @DescSynced
    private boolean isTaped;

    @Persisted
    public final NotifiableItemStackHandler inventory;

    public CrateMachine(IMachineBlockEntity holder, Material material, int inventorySize) {
        super(holder);
        this.material = material;
        this.inventorySize = inventorySize;
        this.inventory = new NotifiableItemStackHandler(this, inventorySize, IO.BOTH);
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        int xOffset = inventorySize >= 90 ? 162 : 0;
        int yOverflow = xOffset > 0 ? 18 : 9;

        var title = new Label();
        title.setText(Component.translatable(getBlockState().getBlock().getDescriptionId()));
        title.layout(layout -> layout.alignSelf(AlignItems.FLEX_START).marginLeft(1));

        var grid = new UIElement();
        UIElement row = null;
        for (int slot = 0; slot < inventorySize; slot++) {
            if (slot % yOverflow == 0) {
                row = new UIElement();
                row.layout(layout -> layout.flexDirection(FlexDirection.ROW));
                grid.addChild(row);
            }
            row.addChild(new ItemSlot().bind(inventory, slot));
        }

        var root = UITemplate.panel();
        root.layout(layout -> layout.width(176 + xOffset).paddingAll(5).paddingBottom(7));
        root.addChildren(title, grid, UITemplate.playerInventory());
        return UITemplate.modularUI(root, entityPlayer);
    }

    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                   BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isCrouching() && !isTaped) {
            if (stack.is(GTItems.DUCT_TAPE.asItem()) || stack.is(GTItems.BASIC_TAPE.asItem())) {
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                isTaped = true;
                setRenderState(getRenderState().setValue(GTMachineModelProperties.IS_TAPED, isTaped));
                return InteractionResult.sidedSuccess(world.isClientSide);
            }
        }
        return IInteractedMachine.super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    public void onMachinePlaced(@Nullable LivingEntity player, ItemStack stack) {
        IMachineLife.super.onMachinePlaced(player, stack);
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            if (tag.contains("taped") && tag.getBoolean("taped")) {
                this.inventory.storage.deserializeNBT(tag.getCompound("inventory"));
            }
            setRenderState(getRenderState().setValue(GTMachineModelProperties.IS_TAPED, isTaped));
        }
    }

    @Override
    public void saveToItem(CompoundTag tag) {
        if (isTaped) {
            IDropSaveMachine.super.saveToItem(tag);
            tag.putBoolean("taped", isTaped);
            tag.put("inventory", inventory.storage.serializeNBT());
        }
    }

    @Override
    public boolean saveBreak() {
        return isTaped;
    }

    @Override
    public void onMachineRemoved() {
        if (!isTaped) clearInventory(inventory.storage);
    }
}
