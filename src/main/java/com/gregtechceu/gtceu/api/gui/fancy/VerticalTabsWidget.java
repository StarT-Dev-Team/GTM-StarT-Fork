package com.gregtechceu.gtceu.api.gui.fancy;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;

import dev.vfyjxf.taffy.style.FlexDirection;

import java.util.function.Consumer;

public class VerticalTabsWidget extends TabsWidget {

    public VerticalTabsWidget(Consumer<IFancyUIProvider> onTabClick) {
        super(onTabClick);
        addClass("gt-tabs-vertical");
        var tabsLeft = "gtceu:textures/gui/tab/tabs_left.png";
        setTabTexture(SpriteTexture.of(tabsLeft).setSprite(0, 28, 32, 28));
        setTabHoverTexture(SpriteTexture.of(tabsLeft).setSprite(32, 28, 32, 28));
        setTabPressedTexture(SpriteTexture.of(tabsLeft).setSprite(32, 28, 32, 28));
    }

    @Override
    protected void setupLayout() {
        layout(l -> l.flexDirection(FlexDirection.COLUMN).width(TAB_SIZE).paddingTop(8));
        subTabContainer.layout(l -> l.flexDirection(FlexDirection.COLUMN));
    }
}
