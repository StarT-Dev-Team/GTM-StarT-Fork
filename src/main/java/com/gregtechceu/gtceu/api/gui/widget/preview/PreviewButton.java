package com.gregtechceu.gtceu.api.gui.widget.preview;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import dev.vfyjxf.taffy.style.TaffyPosition;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class PreviewButton extends Button {

    public static final int SIZE = 18;

    @Nullable
    private Supplier<List<Component>> dynamicTooltip;

    public PreviewButton(int x, int y, Supplier<String> label, Runnable onPress) {
        var base = ColorPattern.T_GRAY.rectTexture();
        var hover = IGuiTexture.group(base, new ColorBorderTexture(1, -1));
        buttonStyle(style -> style.baseTexture(base).hoverTexture(hover).pressedTexture(hover));
        textStyle(style -> style.textColor(-1).textShadow(true));
        layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(x).top(y).width(SIZE).height(SIZE)
                .paddingAll(0));
        setText(label.get(), false);
        setOnClick(event -> onPress.run());
        addEventListener(UIEvents.TICK, event -> setText(label.get(), false));
        addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
            if (dynamicTooltip != null) {
                event.hoverTooltips = new HoverTooltips(dynamicTooltip.get(), null, null, null);
            }
        });
    }

    public PreviewButton onRightClick(Runnable action) {
        addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 1) {
                action.run();
            }
        });
        return this;
    }

    public PreviewButton onWheel(DoubleConsumer action) {
        addEventListener(UIEvents.MOUSE_WHEEL, event -> {
            action.accept(event.deltaY);
            event.stopPropagation();
        });
        return this;
    }

    public PreviewButton tooltip(String translationKey) {
        style(style -> style.tooltips(Component.translatable(translationKey)));
        return this;
    }

    public PreviewButton dynamicTooltip(Supplier<List<Component>> supplier) {
        this.dynamicTooltip = supplier;
        return this;
    }

    public PreviewButton visible(boolean visible) {
        setDisplay(visible);
        return this;
    }
}
