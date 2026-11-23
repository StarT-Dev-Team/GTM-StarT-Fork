package com.gregtechceu.gtceu.api.item.component;

import com.lowdragmc.lowdraglib2.gui.factory.HeldItemUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface IItemUIFactory extends IInteractionItem {

    ModularUI createUI(HeldItemUIMenuType.HeldItemUIHolder holder, Player entityPlayer);

    @Override
    default InteractionResultHolder<ItemStack> use(Item item, Level level, Player player, InteractionHand usedHand) {
        ItemStack heldItem = player.getItemInHand(usedHand);
        if (player instanceof ServerPlayer serverPlayer) {
            HeldItemUIMenuType.openUI(serverPlayer, usedHand);
        }
        return InteractionResultHolder.success(heldItem);
    }
}
