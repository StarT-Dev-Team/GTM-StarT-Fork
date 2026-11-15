package com.gregtechceu.gtceu.integration.ae2.machine.feature.multiblock;

import com.gregtechceu.gtceu.integration.ae2.machine.feature.IGridConnectedMachine;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

import appeng.api.networking.security.IActionSource;

import java.util.EnumSet;

public interface IMEIOPart extends IMEPart, IGridConnectedMachine {

    IActionSource getActionSource();

    boolean isExposeAllSides();

    void setExposeAllSides(boolean exposeAllSides);

    void updateSubscription();

    default void updateExposedSides() {
        getMainNode().setExposedOnSides(
                isExposeAllSides() ? EnumSet.allOf(Direction.class) : EnumSet.of(self().getFrontFacing()));
    }

    default InteractionResult toggleExposeSides(Player player) {
        setExposeAllSides(!isExposeAllSides());

        updateExposedSides();

        player.sendSystemMessage(Component.translatable("gtceu.machine.me.io.expose.description", isExposeAllSides()));

        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
}
