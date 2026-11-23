package com.gregtechceu.gtceu.integration.ae2.gui.widget;

import com.gregtechceu.gtceu.api.gui.GuiTextures;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;

import net.minecraft.network.chat.Component;

import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.function.Consumer;

@Accessors(chain = true)
public class AETextInputButtonWidget extends UIElement {

    @Setter
    private Consumer<String> onConfirm = text -> {};

    @Getter
    @Setter
    private String text = "";

    @Getter
    private boolean isInputting;

    private final TextField textField = new TextField();
    private final Button toggle = new Button();

    public AETextInputButtonWidget() {
        layout(layout -> layout.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(2));

        textField.layout(layout -> layout.flex(1).heightPercent(100));
        textField.setAnyString();
        textField.bind(DataBindingBuilder.string(() -> text, value -> text = value).build());
        textField.setDisplay(false);

        toggle.layout(layout -> layout.aspectRatio(1).heightPercent(100));
        applyInputting(false);
        toggle.setOnServerClick(event -> toggleInputting());

        addChildren(textField, toggle);
        addChild(new BindableValue<Boolean>().bind(DataBindingBuilder.boolS2C(() -> isInputting)
                .remoteSetter(this::applyInputting).build()));
    }

    public AETextInputButtonWidget setButtonTooltips(Component... tooltipTexts) {
        toggle.style(style -> style.tooltips(tooltipTexts));
        return this;
    }

    private void toggleInputting() {
        isInputting = !isInputting;
        if (!isInputting) {
            onConfirm.accept(text);
        }
    }

    private void applyInputting(boolean inputting) {
        textField.setDisplay(inputting);
        toggle.buttonStyle(style -> style.baseTexture(GuiTextures.VANILLA_BUTTON));
        toggle.text.setText(inputting ? "✔" : "✎", false);
    }
}
