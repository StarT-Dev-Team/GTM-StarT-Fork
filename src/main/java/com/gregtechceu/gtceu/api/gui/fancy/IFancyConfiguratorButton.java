package com.gregtechceu.gtceu.api.gui.fancy;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;

import net.minecraft.network.chat.Component;

import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

public interface IFancyConfiguratorButton extends IFancyConfigurator {

    void onClick(UIEvent event);

    @Override
    default Component getTitle() {
        throw new NotImplementedException();
    }

    @Override
    default UIElement createConfigurator() {
        throw new NotImplementedException();
    }

    @Accessors(chain = true)
    class Toggle implements IFancyConfiguratorButton {

        IGuiTexture base;
        IGuiTexture pressed;
        BiConsumer<UIEvent, Boolean> onClick;
        BooleanSupplier booleanSupplier;
        boolean isPressed;
        @Setter
        Function<Boolean, List<Component>> tooltipsSupplier = isPressed -> Collections.emptyList();

        public Toggle(IGuiTexture base, IGuiTexture pressed, BooleanSupplier booleanSupplier,
                      BiConsumer<UIEvent, Boolean> onClick) {
            this.base = base;
            this.pressed = pressed;
            this.booleanSupplier = booleanSupplier;
            this.onClick = onClick;
        }

        @Override
        public List<Component> getTooltips() {
            return tooltipsSupplier.apply(isPressed);
        }

        @Override
        public void bindTab(UIElement tab) {
            isPressed = booleanSupplier.getAsBoolean();
            tab.addChild(new BindableValue<Boolean>().bind(DataBindingBuilder.boolS2C(booleanSupplier::getAsBoolean)
                    .remoteSetter(value -> isPressed = value)
                    .build()));
        }

        @Override
        public IGuiTexture getIcon() {
            return isPressed ? pressed : base;
        }

        @Override
        public void onClick(UIEvent event) {
            isPressed = booleanSupplier.getAsBoolean();
            onClick.accept(event, !isPressed);
        }
    }
}
