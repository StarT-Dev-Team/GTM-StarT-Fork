package com.gregtechceu.gtceu.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.common.data.GTItems;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class MachineModeFancyConfigurator implements IFancyUIProvider {

    protected IRecipeLogicMachine machine;

    public MachineModeFancyConfigurator(IRecipeLogicMachine machine) {
        this.machine = machine;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gtceu.gui.machinemode.title");
    }

    @Override
    public IGuiTexture getTabIcon() {
        return new ItemStackTexture(GTItems.ROBOT_ARM_LV.get());
    }

    @Override
    public UIElement createMainPage(FancyMachineUIWidget widget) {
        var group = new UIElement().layout(layout -> layout.width(140).paddingAll(2));
        group.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND_INVERSE));
        group.addChild(new BindableValue<Integer>().bind(DataBindingBuilder
                .intValS2C(machine::getActiveRecipeType)
                .remoteSetter(machine::setActiveRecipeType)
                .build()));
        for (int i = 0; i < machine.getRecipeTypes().length; i++) {
            int finalI = i;
            var button = new Button().noText();
            button.layout(layout -> layout.width(136).height(20));
            button.buttonStyle(style -> {
                var texture = DynamicTexture.of(() -> new GuiTextureGroup(
                        GuiTextures.VANILLA_BUTTON.copy()
                                .setColor(machine.getActiveRecipeType() == finalI ? ColorPattern.CYAN.color : -1),
                        new TextTexture(machine.getRecipeTypes()[finalI].registryName.toLanguageKey())
                                .setWidth(136).setType(TextTexture.TextType.ROLL)));
                style.baseTexture(texture).hoverTexture(texture).pressedTexture(texture);
            });
            button.setOnServerClick(event -> setActiveRecipeTypeAndUpdateTickSubs(finalI));
            group.addChild(button);
        }
        return group;
    }

    @Override
    public List<Component> getTabTooltips() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("gtceu.gui.machinemode.tab_tooltip"));
        return tooltip;
    }

    private void setActiveRecipeTypeAndUpdateTickSubs(int activeRecipeType) {
        boolean isNewType = activeRecipeType != machine.getActiveRecipeType();

        if (isNewType) {
            boolean needUpdateTickSubs = !machine.keepSubscribing();

            machine.setActiveRecipeType(activeRecipeType);
            machine.getRecipeLogic().markLastRecipeDirty();

            if (needUpdateTickSubs) {
                machine.getRecipeLogic().updateTickSubscription();
            }
        }
    }
}
