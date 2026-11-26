package com.gregtechceu.gtceu.api.machine.feature.multiblock;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.widget.ComponentPanel;
import com.gregtechceu.gtceu.api.machine.feature.IUIMachine;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.util.ClickData;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface IDisplayUIMachine extends IUIMachine, IMultiController {

    default void addDisplayText(@NotNull List<Component> textList) {
        for (var part : this.getParts()) {
            part.addMultiText(textList);
        }
    }

    default void handleDisplayClick(String componentData, ClickData clickData) {}

    default IGuiTexture getScreenTexture() {
        return GuiTextures.DISPLAY;
    }

    @Override
    default ModularUI createUI(Player entityPlayer) {
        var content = new UIElement();
        content.layout(l -> l.paddingAll(4).gapAll(3));
        var title = new Label();
        title.setText(Component.translatable(self().getBlockState().getBlock().getDescriptionId()));
        content.addChildren(title, new ComponentPanel(this::addDisplayText)
                .setMaxWidthLimit(154)
                .clickHandler(this::handleDisplayClick));

        var screen = UITemplate.displayScreen(getScreenTexture());
        screen.layout(l -> l.width(162).height(121));
        screen.addScrollViewChild(content);

        var root = UITemplate.panel();
        root.layout(l -> l.width(176));
        root.addChildren(screen, UITemplate.playerInventory());
        return UITemplate.modularUI(root, entityPlayer);
    }
}
