package com.gregtechceu.gtceu.common.item.modules;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.item.component.IMonitorModuleItem;
import com.gregtechceu.gtceu.client.renderer.monitor.IMonitorRenderer;
import com.gregtechceu.gtceu.client.renderer.monitor.MonitorImageRenderer;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.CentralMonitorMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.monitor.MonitorGroup;
import com.gregtechceu.gtceu.common.network.GTNetwork;
import com.gregtechceu.gtceu.common.network.packets.SCPacketMonitorGroupNBTChange;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;

import net.minecraft.world.item.ItemStack;

public class ImageModuleBehaviour implements IMonitorModuleItem {

    @Override
    public IMonitorRenderer getRenderer(ItemStack stack) {
        return new MonitorImageRenderer(getUrl(stack));
    }

    @Override
    public UIElement createUIWidget(ItemStack stack, CentralMonitorMachine machine, MonitorGroup group) {
        var builder = new UIElement().layout(layout -> layout.gapAll(3));
        var textField = new TextField().setAnyString();
        textField.layout(layout -> layout.width(100).height(10));
        textField.setValue(getUrl(stack), false);

        var saveButton = new Button().noText();
        saveButton.layout(layout -> layout.width(20).height(20));
        saveButton.buttonStyle(style -> style.baseTexture(GuiTextures.BUTTON_CHECK));
        saveButton.setOnClick(event -> {
            setUrl(stack, textField.getValue());
            GTNetwork.sendToServer(new SCPacketMonitorGroupNBTChange(stack, group, machine));
        });
        builder.addChildren(textField, saveButton);
        return builder;
    }

    @Override
    public String getType() {
        return "image";
    }

    public String getUrl(ItemStack stack) {
        return stack.getOrCreateTag().getString("url");
    }

    public void setUrl(ItemStack stack, String url) {
        stack.getOrCreateTag().putString("url", url);
    }
}
