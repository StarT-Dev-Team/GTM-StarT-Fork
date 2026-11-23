package com.gregtechceu.gtceu.api.gui.widget;

import com.gregtechceu.gtceu.api.gui.GuiTextures;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;

import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public class ConfirmTextInput extends UIElement {

    private final Consumer<String> textResponder;
    @Nullable
    private final Function<String, String> returnValidator;
    private final Function<String, String> validator;
    private String inputText;
    private final TextField textField = new TextField();

    public ConfirmTextInput(String text, Consumer<String> textResponder, @Nullable Function<String, String> validator,
                            @Nullable Function<String, String> returnValidator) {
        this.textResponder = textResponder;
        this.returnValidator = returnValidator;
        this.validator = validator == null ? s -> s : validator;
        this.inputText = text == null ? "" : text;

        layout(layout -> layout.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(2));

        textField.layout(layout -> layout.flex(1).heightPercent(100));
        textField.bind(DataBindingBuilder.string(() -> inputText, s -> inputText = this.validator.apply(s)).build());

        var confirm = new Button().noText();
        confirm.layout(layout -> layout.aspectRatio(1).heightPercent(100));
        confirm.buttonStyle(style -> style.baseTexture(IGuiTexture.group(GuiTextures.VANILLA_BUTTON,
                GuiTextures.BUTTON_CHECK)));
        confirm.setOnServerClick(event -> {
            if (this.returnValidator != null) {
                inputText = this.returnValidator.apply(inputText);
            }
            this.textResponder.accept(inputText);
        });
        addChildren(textField, confirm);
    }

    public ConfirmTextInput setTooltip(String tooltip) {
        textField.style(style -> style.tooltips(tooltip));
        return this;
    }
}
