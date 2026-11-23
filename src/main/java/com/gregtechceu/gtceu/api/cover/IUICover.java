package com.gregtechceu.gtceu.api.cover;

import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.util.UICloseHandler;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import net.minecraft.world.entity.player.Player;

public interface IUICover {

    default CoverBehavior self() {
        return (CoverBehavior) this;
    }

    default boolean isStillValid() {
        return !self().coverHolder.isInValid() && self().coverHolder.getCoverAtSide(self().attachedSide) == self();
    }

    default boolean isInvalid() {
        return !isStillValid();
    }

    default boolean isRemote() {
        return self().coverHolder.isRemote();
    }

    default void markAsDirty() {
        self().coverHolder.markDirty();
    }

    default ModularUI createUI(Player entityPlayer) {
        var root = UITemplate.panel();
        root.layout(l -> l.minWidth(176));
        root.addChildren(createUIWidget(), UITemplate.playerInventory());
        UICloseHandler.onClose(root, this::onUIClosed);
        return UITemplate.modularUI(root, entityPlayer);
    }

    default void onUIClosed() {}

    UIElement createUIWidget();
}
