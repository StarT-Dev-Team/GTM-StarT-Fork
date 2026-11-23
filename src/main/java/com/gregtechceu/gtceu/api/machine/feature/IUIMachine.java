package com.gregtechceu.gtceu.api.machine.feature;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIFactory;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A machine that has gui. can be opened via right click.
 */
public interface IUIMachine extends IMachineFeature {

    ModularUI createUI(Player entityPlayer);

    default boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        return true;
    }

    default InteractionResult tryToOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        if (this.shouldOpenUI(player, hand, hit)) {
            if (player instanceof ServerPlayer serverPlayer) {
                MachineUIFactory.INSTANCE.openUI(self(), serverPlayer);
            }
        } else {
            return InteractionResult.PASS;
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    default boolean isInvalid() {
        return self().isInValid();
    }

    default boolean isRemote() {
        var level = self().getLevel();
        return level == null ? GTCEu.isClientThread() : level.isClientSide;
    }

    default void markAsDirty() {
        self().markDirty();
    }
}
