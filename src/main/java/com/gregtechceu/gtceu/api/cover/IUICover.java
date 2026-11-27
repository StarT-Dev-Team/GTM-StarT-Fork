package com.gregtechceu.gtceu.api.cover;

import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.util.UICloseHandler;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;

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

    default Component getCoverUITitle() {
        return Component.empty();
    }

    default ModularUI createUI(Player entityPlayer) {
        var root = UITemplate.panel()
                .layout(l -> l.minWidth(176));

        var group = new UIElement()
                .layout(s -> s.display(TaffyDisplay.FLEX).flexDirection(FlexDirection.COLUMN).gapAll(4).paddingAll(4)
                        .alignItems(AlignItems.CENTER));
        root.addChild(group);

        var label = new Label().setText(getCoverUITitle())
                .layout(s -> s.widthPercent(100));
        group.addChild(label);

        var innerUi = createUIWidget();
        group.addChild(innerUi);

        root.addChild(UITemplate.playerInventory());
        UICloseHandler.onClose(root, this::onUIClosed);
        return UITemplate.modularUI(root, entityPlayer);
    }

    default void onUIClosed() {}

    UIElement createUIWidget();
}
