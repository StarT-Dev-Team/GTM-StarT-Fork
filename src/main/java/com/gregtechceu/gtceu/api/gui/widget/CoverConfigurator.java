package com.gregtechceu.gtceu.api.gui.widget;

import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.IUICover;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator;
import com.gregtechceu.gtceu.common.data.GTItems;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor
public class CoverConfigurator implements IFancyConfigurator {

    protected final ICoverable coverable;
    @Nullable
    protected final Direction side;
    @Nullable
    protected final CoverBehavior coverBehavior;

    @Override
    public Component getTitle() {
        return Component.translatable("gtceu.gui.cover_setting.title");
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(GTItems.ITEM_FILTER.get());
    }

    @Override
    public UIElement createConfigurator() {
        var group = new UIElement().layout(layout -> layout.minWidth(120).minHeight(80));
        if (side != null && coverable.getCoverAtSide(side) instanceof IUICover cover) {
            var configurator = cover.createUIWidget();
            configurator.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND));
            configurator.layout(layout -> layout.marginLeft(4).marginTop(-4));
            group.addChild(configurator);
        }
        return group;
    }
}
