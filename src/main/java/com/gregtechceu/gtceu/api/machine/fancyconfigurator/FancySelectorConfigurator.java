package com.gregtechceu.gtceu.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton;
import com.gregtechceu.gtceu.api.gui.widget.EnumSelector;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;

import net.minecraft.network.chat.Component;

import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class FancySelectorConfigurator<T extends Enum<T> & EnumSelector.SelectableEnum>
                                      implements IFancyConfiguratorButton {

    private final List<T> values;
    private final Consumer<T> onChanged;
    private int selected;

    @Setter
    @Accessors(chain = true)
    private Function<T, List<Component>> tooltip = t -> Collections.singletonList(Component.empty());

    public FancySelectorConfigurator(T[] values, T initialValue, Consumer<T> onChanged) {
        this.values = Arrays.asList(values);
        this.onChanged = onChanged;
        this.selected = Math.max(0, this.values.indexOf(initialValue));
    }

    @Override
    public void bindTab(UIElement tab) {
        tab.addChild(new BindableValue<Integer>().bind(DataBindingBuilder.intValS2C(() -> selected)
                .remoteSetter(value -> selected = value)
                .build()));
    }

    @Override
    public IGuiTexture getIcon() {
        return IGuiTexture.group(GuiTextures.VANILLA_BUTTON, values.get(selected).getIcon());
    }

    @Override
    public List<Component> getTooltips() {
        return this.tooltip.apply(values.get(selected));
    }

    @Override
    public void onClick(UIEvent event) {
        ++selected;
        if (selected >= values.size()) {
            selected = 0;
        }

        onChanged.accept(values.get(selected));
    }
}
