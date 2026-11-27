package com.gregtechceu.gtceu.api.gui.widget;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.data.lang.LangHandler;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableUIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EnumSelector<T extends Enum<T> & EnumSelector.SelectableEnum> extends BindableUIElement<T> {

    public interface SelectableEnum {

        String getTooltip();

        IGuiTexture getIcon();
    }

    public final List<T> values;
    private int selected;

    private BiFunction<T, IGuiTexture, IGuiTexture> textureSupplier = (value, texture) -> IGuiTexture.group(
            GuiTextures.VANILLA_BUTTON, texture);
    private BiFunction<T, String, List<Component>> tooltipSupplier = (value, key) -> List
            .copyOf(LangHandler.getSingleOrMultiLang(key));

    public EnumSelector(T[] values, Supplier<T> getter, Consumer<T> setter) {
        this(Arrays.asList(values), getter, setter);
    }

    public EnumSelector(List<T> values, Supplier<T> getter, Consumer<T> setter) {
        this.values = values;
        layout(layout -> layout.width(20).height(20));
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.HOVER_TOOLTIPS, this::onHoverTooltips);
        setSelected(getter.get(), false);
        bind(DataBindingBuilder.enumVal(values.get(0).getDeclaringClass(), getter, setter).build());
    }

    public EnumSelector<T> setTextureSupplier(BiFunction<T, IGuiTexture, IGuiTexture> textureSupplier) {
        this.textureSupplier = textureSupplier;
        updateTexture();
        return this;
    }

    public EnumSelector<T> setTooltipSupplier(BiFunction<T, String, List<Component>> tooltipSupplier) {
        this.tooltipSupplier = tooltipSupplier;
        return this;
    }

    public IGuiTexture getTexture(int index) {
        var value = values.get(index);
        return textureSupplier.apply(value, value.getIcon());
    }

    public int getSelected() {
        return selected;
    }

    public void setSelected(T value, boolean notify) {
        var index = values.indexOf(value);
        if (index == -1) {
            throw new NoSuchElementException(value + " is not a possible value for this selector.");
        }
        selected = index;
        updateTexture();
        if (notify) notifyListeners();
    }

    private void updateTexture() {
        var texture = getTexture(selected);
        style(style -> style.backgroundTexture(texture));
    }

    @Override
    public T getValue() {
        return values.get(selected);
    }

    @Override
    public BindableUIElement<T> setValue(@Nullable T value, boolean notify) {
        if (value == null || value == getValue()) return this;
        setSelected(value, notify);
        return this;
    }

    private void onMouseDown(UIEvent event) {
        if (!isActiveInHierarchy() || event.button != 0) return;
        setValue(values.get((selected + 1) % values.size()), true);
    }

    private void onHoverTooltips(UIEvent event) {
        var value = getValue();
        var tooltips = new ArrayList<>(tooltipSupplier.apply(value, value.getTooltip()));
        tooltips.addAll(getStyle().tooltips().asList());
        if (!tooltips.isEmpty()) {
            event.hoverTooltips = new HoverTooltips(tooltips, null, null, null);
        }
    }
}
