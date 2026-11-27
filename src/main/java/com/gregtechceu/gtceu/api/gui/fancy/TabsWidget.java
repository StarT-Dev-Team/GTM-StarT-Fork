package com.gregtechceu.gtceu.api.gui.fancy;

import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Getter
public class TabsWidget extends UIElement {

    public static final int TAB_SIZE = 24;

    protected final Consumer<IFancyUIProvider> onTabClick;
    @Nullable
    protected IFancyUIProvider mainTab;
    protected List<IFancyUIProvider> subTabs = new ArrayList<>();
    @Nullable
    protected IFancyUIProvider selectedTab;
    @Setter
    protected IGuiTexture tabTexture = SpriteTexture.of("gtceu:textures/gui/tab/tabs_top.png")
            .setSprite(28, 0, 28, 32);
    @Setter
    protected IGuiTexture tabHoverTexture = SpriteTexture.of("gtceu:textures/gui/tab/tabs_top.png")
            .setSprite(28, 32, 28, 32);
    @Setter
    protected IGuiTexture tabPressedTexture = tabHoverTexture;
    @Setter
    @Nullable
    protected BiConsumer<IFancyUIProvider, IFancyUIProvider> onTabSwitch;

    protected final UIElement mainTabContainer;
    protected final UIElement subTabContainer;
    protected final Map<IFancyUIProvider, Button> tabButtons = new IdentityHashMap<>();

    public TabsWidget(Consumer<IFancyUIProvider> onTabClick) {
        this.onTabClick = onTabClick;
        addClass("gt-tabs");
        this.mainTabContainer = new UIElement();
        this.subTabContainer = new UIElement();
        addChildren(mainTabContainer, subTabContainer);
        setupLayout();
    }

    protected void setupLayout() {
        layout(l -> l.flexDirection(FlexDirection.ROW).height(TAB_SIZE).paddingHorizontal(8)
                .justifyContent(AlignContent.SPACE_BETWEEN));
        subTabContainer.layout(l -> l.flexDirection(FlexDirection.ROW));
    }

    public void setMainTab(IFancyUIProvider mainTab) {
        this.mainTab = mainTab;
        if (this.selectedTab == null) {
            this.selectedTab = this.mainTab;
        }
        rebuildButtons();
    }

    public void setTabs(@Nullable IFancyUIProvider mainTab, List<IFancyUIProvider> subTabs) {
        this.mainTab = mainTab;
        this.subTabs.clear();
        this.subTabs.addAll(subTabs);
        rebuildButtons();
    }

    public void clearSubTabs() {
        subTabs.clear();
        rebuildButtons();
    }

    public void attachSubTab(IFancyUIProvider subTab) {
        subTabs.add(subTab);
        rebuildButtons();
    }

    public List<IFancyUIProvider> getAllTabs() {
        var all = new ArrayList<IFancyUIProvider>();
        if (mainTab != null) all.add(mainTab);
        all.addAll(subTabs);
        return all;
    }

    public void selectTab(IFancyUIProvider selectedTab) {
        this.selectedTab = selectedTab;
        tabButtons.forEach(this::updateButtonStyle);
    }

    protected void rebuildButtons() {
        tabButtons.clear();
        for (var child : new ArrayList<>(mainTabContainer.getChildren())) {
            mainTabContainer.removeChild(child);
        }
        for (var child : new ArrayList<>(subTabContainer.getChildren())) {
            subTabContainer.removeChild(child);
        }
        if (mainTab != null) {
            mainTabContainer.addChild(createTabButton(mainTab));
        }
        for (var subTab : subTabs) {
            subTabContainer.addChild(createTabButton(subTab));
        }
    }

    protected Button createTabButton(IFancyUIProvider tab) {
        var button = new Button();
        button.noText();
        button.addClass("gt-tab");
        button.layout(l -> l.width(TAB_SIZE).height(TAB_SIZE).paddingAll(0).alignItems(AlignItems.CENTER)
                .justifyContent(AlignContent.CENTER));
        var icon = new UIElement();
        icon.layout(l -> l.width(16).height(16));
        icon.style(s -> s.background(DynamicTexture.of(tab::getTabIcon)));
        icon.setAllowHitTest(false);
        button.addChild(icon);
        button.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                tab.getTabTooltips(), tab.getTabTooltipComponent(), null, null));
        button.setOnClick(e -> onTabClicked(tab));
        tabButtons.put(tab, button);
        updateButtonStyle(tab, button);
        return button;
    }

    protected void updateButtonStyle(IFancyUIProvider tab, Button button) {
        var selected = tab == selectedTab;
        button.buttonStyle(s -> s.baseTexture(selected ? tabPressedTexture : tabTexture)
                .hoverTexture(selected ? tabPressedTexture : tabHoverTexture)
                .pressedTexture(tabPressedTexture));
    }

    protected void onTabClicked(IFancyUIProvider tab) {
        if (tab == selectedTab) return;
        if (onTabSwitch != null) {
            onTabSwitch.accept(selectedTab, tab);
        }
        onTabClick.accept(tab);
    }
}
