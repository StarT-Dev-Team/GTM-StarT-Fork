package com.gregtechceu.gtceu.api.gui.widget;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.texture.UVSpriteTexture;
import com.gregtechceu.gtceu.data.lang.LangHandler;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SimpleBinding;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableUIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import net.minecraft.network.chat.Component;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.function.BooleanSupplier;

public class ToggleButton extends BindableUIElement<Boolean> {

    private IGuiTexture offTexture;
    private IGuiTexture onTexture;
    private final IGuiTexture icon;
    private boolean pressed;
    @Nullable
    private String tooltipText;
    private boolean multiLang;
    @Nullable
    private SimpleBinding<Boolean> binding;

    public ToggleButton(BooleanSupplier isPressed, BooleanConsumer setPressed) {
        this(GuiTextures.VANILLA_BUTTON, isPressed, setPressed);
    }

    public ToggleButton(IGuiTexture buttonTexture, BooleanSupplier isPressed, BooleanConsumer setPressed) {
        this.icon = buttonTexture;
        layout(layout -> layout.width(18).height(18));
        if (buttonTexture instanceof SpriteTexture sheet) {
            offTexture = half(sheet, false);
            onTexture = half(sheet, true);
        } else {
            offTexture = buttonTexture;
            onTexture = buttonTexture;
        }
        pressed = isPressed.getAsBoolean();
        updateTexture();
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.HOVER_TOOLTIPS, this::onHoverTooltips);
        rebind(isPressed, setPressed);
    }

    public ToggleButton rebind(BooleanSupplier isPressed, BooleanConsumer setPressed) {
        if (binding != null) {
            unbind(binding);
        }
        binding = DataBindingBuilder.bool(isPressed::getAsBoolean, setPressed::accept).build();
        bind(binding);
        setValue(isPressed.getAsBoolean(), false);
        return this;
    }

    private static SpriteTexture half(SpriteTexture sheet, boolean lower) {
        var texture = UVSpriteTexture.of(sheet, 0, lower ? 0.5f : 0, 1, 0.5f);
        texture.setBorder(sheet.borderLT.getX(), sheet.borderLT.getY(), sheet.borderRB.getX(), sheet.borderRB.getY());
        return texture;
    }

    public ToggleButton setShouldUseBaseBackground() {
        offTexture = IGuiTexture.group(half(GuiTextures.TOGGLE_BUTTON_BACK, false), icon);
        onTexture = IGuiTexture.group(half(GuiTextures.TOGGLE_BUTTON_BACK, true), icon);
        updateTexture();
        return this;
    }

    public ToggleButton setTooltipText(String tooltipText) {
        this.tooltipText = tooltipText;
        return this;
    }

    public ToggleButton isMultiLang() {
        multiLang = true;
        return this;
    }

    private void updateTexture() {
        var texture = pressed ? onTexture : offTexture;
        style(style -> style.backgroundTexture(texture));
    }

    @Override
    public Boolean getValue() {
        return pressed;
    }

    @Override
    public BindableUIElement<Boolean> setValue(@Nullable Boolean value, boolean notify) {
        var newValue = value != null && value;
        if (newValue == pressed) return this;
        pressed = newValue;
        updateTexture();
        if (notify) notifyListeners();
        return this;
    }

    private void onMouseDown(UIEvent event) {
        if (!isActiveInHierarchy() || event.button != 0) return;
        setValue(!pressed, true);
    }

    private void onHoverTooltips(UIEvent event) {
        var tooltips = new ArrayList<Component>();
        if (tooltipText != null) {
            var key = tooltipText + (pressed ? ".enabled" : ".disabled");
            if (multiLang) {
                tooltips.addAll(LangHandler.getMultiLang(key));
            } else {
                tooltips.add(Component.translatable(key));
            }
        }
        tooltips.addAll(getStyle().tooltips().asList());
        if (!tooltips.isEmpty()) {
            event.hoverTooltips = new HoverTooltips(tooltips, null, null, null);
        }
    }
}
