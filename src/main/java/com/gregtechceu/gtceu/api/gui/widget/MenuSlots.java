package com.gregtechceu.gtceu.api.gui.widget;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.slot.LocalSlot;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;

import java.util.ArrayList;
import java.util.List;

public final class MenuSlots {

    private MenuSlots() {}

    public static void detach(UIElement root) {
        var modularUI = root.getModularUI();
        if (modularUI == null) return;
        var menu = modularUI.getMenu();
        if (menu == null) return;

        var found = new ArrayList<ItemSlot>();
        collect(root, found);

        var removed = false;
        for (var itemSlot : found) {
            if (menu.slots.remove(itemSlot.getSlot())) {
                removed = true;
            }
        }
        if (!removed) return;

        for (int i = 0; i < menu.slots.size(); i++) {
            menu.slots.get(i).index = i;
        }
        if (!LDLib2.isRemote()) {
            menu.sendAllDataToRemote();
        }
    }

    public static void clearChildren(UIElement parent) {
        detach(parent);
        parent.clearAllChildren();
    }

    private static void collect(UIElement element, List<ItemSlot> out) {
        if (element instanceof ItemSlot itemSlot && !(itemSlot.getSlot() instanceof LocalSlot)) {
            out.add(itemSlot);
        }
        for (var child : element.getChildren()) {
            collect(child, out);
        }
    }
}
