package com.gregtechceu.gtceu.api.gui;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.config.ConfigHolder;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class UITemplate {

    public static final ResourceLocation GT_STYLESHEET = GTCEu.id("lss/gtceu.lss");

    public static List<Stylesheet> stylesheets() {
        return List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC),
                StylesheetManager.INSTANCE.getStylesheetSafe(GT_STYLESHEET));
    }

    public static UI ui(UIElement root) {
        return UI.of(root, stylesheets());
    }

    public static ModularUI modularUI(UIElement root, Player player) {
        return new ModularUI(ui(root), player);
    }

    public static int defaultUIColor() {
        return Long.decode(ConfigHolder.INSTANCE.client.defaultUIColor).intValue() | 0xFF000000;
    }

    public static IGuiTexture panelTexture() {
        return GuiTextures.BACKGROUND.copy().setColor(defaultUIColor());
    }

    public static UIElement panel() {
        var panel = new UIElement();
        panel.addClass("gt-panel");
        panel.style(s -> s.background(panelTexture()));
        return panel;
    }

    public static ScrollerView displayScreen(IGuiTexture background) {
        var screen = new ScrollerView();
        screen.style(style -> style.backgroundTexture(background));
        screen.viewPort(viewPort -> {
            viewPort.layout(layout -> layout.paddingAll(0));
            viewPort.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        });
        return screen;
    }

    public static InventorySlots playerInventory() {
        return bindPlayerInventory(GuiTextures.SLOT);
    }

    public static InventorySlots bindPlayerInventory(IGuiTexture slotTexture) {
        var inventory = new InventorySlots();
        inventory.addClass("gt-player-inventory");
        inventory.apply(slot -> slot.style(s -> s.background(slotTexture)));
        return inventory;
    }
}
