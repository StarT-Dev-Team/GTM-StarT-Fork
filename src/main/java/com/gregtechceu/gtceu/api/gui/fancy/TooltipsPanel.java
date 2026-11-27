package com.gregtechceu.gtceu.api.gui.fancy;

import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TooltipsPanel extends UIElement {

    public static final int ICON_SIZE = 20;

    @Getter
    protected List<IFancyTooltip> tooltips = new ArrayList<>();

    public TooltipsPanel() {
        addClass("gt-tooltips-panel");
        layout(l -> l.width(ICON_SIZE).gapAll(2));
        addEventListener(UIEvents.TICK, e -> updateIcons());
    }

    protected void updateIcons() {
        var children = getChildren();
        for (int i = 0; i < children.size() && i < tooltips.size(); i++) {
            children.get(i).setDisplay(tooltips.get(i).showFancyTooltip());
        }
    }

    public void clear() {
        tooltips.clear();
        for (var child : new ArrayList<>(getChildren())) {
            removeChild(child);
        }
    }

    public void attachTooltips(IFancyTooltip... tooltips) {
        this.tooltips.addAll(Arrays.asList(tooltips));
        for (var tooltip : tooltips) {
            addChild(createIcon(tooltip));
        }
    }

    protected UIElement createIcon(IFancyTooltip tooltip) {
        var icon = new UIElement();
        icon.addClass("gt-fancy-tooltip");
        icon.layout(l -> l.width(ICON_SIZE).height(ICON_SIZE));
        icon.style(s -> s.background(DynamicTexture.of(tooltip::getFancyTooltipIcon)));
        icon.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                tooltip.getFancyTooltip(), tooltip.getFancyComponent(), null, null));
        return icon;
    }
}
