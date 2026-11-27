package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.api.cover.filter.ItemFilter;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.item.component.IItemUIFactory;

import com.lowdragmc.lowdraglib2.gui.factory.HeldItemUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import dev.vfyjxf.taffy.style.AlignItems;

import java.util.function.Function;

public record ItemFilterBehaviour(Function<ItemStack, ItemFilter> filterCreator) implements IItemUIFactory {

    @Override
    public void onAttached(Item item) {
        IItemUIFactory.super.onAttached(item);
        ItemFilter.FILTERS.put(item, filterCreator);
    }

    @Override
    public ModularUI createUI(HeldItemUIMenuType.HeldItemUIHolder holder, Player entityPlayer) {
        var held = holder.itemStack;
        var root = UITemplate.panel();
        root.layout(layout -> layout.minWidth(176).paddingAll(5).gapAll(5).alignItems(AlignItems.CENTER));
        root.addChild(new Label().setText(Component.translatable(held.getDescriptionId())));
        root.addChild(ItemFilter.loadFilter(held).openConfigurator());
        root.addChild(UITemplate.playerInventory());
        return UITemplate.modularUI(root, entityPlayer);
    }
}
