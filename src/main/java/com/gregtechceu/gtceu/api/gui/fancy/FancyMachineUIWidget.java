package com.gregtechceu.gtceu.api.gui.fancy;

import com.gregtechceu.gtceu.api.gui.UITemplate;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Getter
public class FancyMachineUIWidget extends UIElement {

    public static final int INVENTORY_HEIGHT = 86;
    public static final String NAVIGATION_MESSAGE = "gtceu:fancy_navigation";

    protected static final int NAV_TAB = 0;
    protected static final int NAV_BACK = 1;
    protected static final int NAV_PAGE_SWITCHER = 2;
    protected static final int NAV_SWITCH_PAGE = 3;

    protected final TitleBarWidget titleBar;
    protected final VerticalTabsWidget sideTabsWidget;
    protected final UIElement pageContainer;
    protected final PageSwitcher pageSwitcher;
    protected final UIElement configuratorHost;
    protected final UIElement autoPushConfiguratorHost;
    protected final UIElement tooltipsHost;

    @Nullable
    protected final InventorySlots playerInventory;
    protected final int border = 4;
    protected final int minWidth;
    protected final int minPageHeight;

    protected final IFancyUIProvider mainPage;

    protected IFancyUIProvider currentPage;
    protected IFancyUIProvider currentHomePage;

    protected final List<IFancyUIProvider> allPages;

    protected final Deque<NavigationEntry> previousPages = new ArrayDeque<>();
    protected final Map<IFancyUIProvider, Page> pages = new IdentityHashMap<>();
    protected final Map<IFancyUIProvider, SideTabs> sideTabs = new IdentityHashMap<>();

    protected record NavigationEntry(IFancyUIProvider page, IFancyUIProvider homePage, Runnable onNavigation) {}

    protected record SideTabs(@Nullable IFancyUIProvider mainTab, List<IFancyUIProvider> subTabs) {}

    public FancyMachineUIWidget(IFancyUIProvider mainPage) {
        this(mainPage, 176, 166);
    }

    public FancyMachineUIWidget(IFancyUIProvider mainPage, int width, int height) {
        this.mainPage = mainPage;
        this.minWidth = width;
        this.minPageHeight = Math.max(86, height - INVENTORY_HEIGHT);
        addClass("gt-fancy-ui");
        style(s -> s.background(UITemplate.panelTexture()));
        layout(l -> l.minWidth(width).paddingAll(border).gapAll(2).alignItems(AlignItems.CENTER));

        this.pageContainer = new UIElement();
        pageContainer.addClass("gt-fancy-page-container");
        pageContainer.layout(l -> l.minWidth(width - border * 2).minHeight(minPageHeight - border * 2)
                .alignItems(AlignItems.CENTER).justifyContent(AlignContent.CENTER));
        addChild(pageContainer);

        if (mainPage.hasPlayerInventory()) {
            this.playerInventory = UITemplate.playerInventory();
            addChild(playerInventory);
        } else {
            this.playerInventory = null;
        }

        this.titleBar = new TitleBarWidget(e -> requestNavigation(NAV_BACK, 0),
                e -> requestNavigation(NAV_PAGE_SWITCHER, 0));
        titleBar.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(8).right(8).top(-16));

        this.sideTabsWidget = new VerticalTabsWidget(this::onSideTabClicked);
        sideTabsWidget.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(-20).top(0));

        this.tooltipsHost = new UIElement();
        tooltipsHost.addClass("gt-fancy-tooltips");
        tooltipsHost.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).right(-(TooltipsPanel.ICON_SIZE + 2)).top(2)
                .width(TooltipsPanel.ICON_SIZE));

        this.configuratorHost = new UIElement();
        configuratorHost.addClass("gt-fancy-configurators");
        configuratorHost.layout(l -> l.positionType(TaffyPosition.ABSOLUTE)
                .left(-(ConfiguratorPanel.TAB_SIZE + 2)).bottom(4).width(ConfiguratorPanel.TAB_SIZE));

        this.autoPushConfiguratorHost = new UIElement();
        autoPushConfiguratorHost.addClass("gt-fancy-auto-push-configurators");
        autoPushConfiguratorHost.layout(l -> l.positionType(TaffyPosition.ABSOLUTE)
                .right(-(ConfiguratorPanel.TAB_SIZE + 2)).bottom(4).width(ConfiguratorPanel.TAB_SIZE));

        addChildren(titleBar, sideTabsWidget, tooltipsHost, configuratorHost, autoPushConfiguratorHost);

        this.allPages = Stream.concat(Stream.of(mainPage), mainPage.getSubTabs().stream()).toList();
        this.pageSwitcher = new PageSwitcher(this::onPageSwitched);

        onMessage(NAVIGATION_MESSAGE, tag -> handleNavigation(tag.getInt("action"), tag.getInt("index")));

        performNavigation(mainPage, mainPage);
    }

    @Nullable
    public Player getPlayer() {
        var mui = getModularUI();
        return mui == null ? null : mui.player;
    }

    public ConfiguratorPanel getConfiguratorPanel() {
        return getOrCreatePage(currentPage).configuratorPanel;
    }

    public ConfiguratorPanel getAutoPushConfiguratorPanel() {
        return getOrCreatePage(currentPage).autoPushConfiguratorPanel;
    }

    public TooltipsPanel getTooltipsPanel() {
        return getOrCreatePage(currentPage).tooltipsPanel;
    }

    ////////////////////////////////////////
    // ********* NAVIGATION *********//
    ////////////////////////////////////////

    protected void onSideTabClicked(IFancyUIProvider tab) {
        requestNavigation(NAV_TAB, sideTabsWidget.getAllTabs().indexOf(tab));
    }

    protected void onPageSwitched(IFancyUIProvider page) {
        requestNavigation(NAV_SWITCH_PAGE, allPages.indexOf(page));
    }

    protected void requestNavigation(int action, int index) {
        handleNavigation(action, index);
        var tag = new CompoundTag();
        tag.putInt("action", action);
        tag.putInt("index", index);
        sendMessage(NAVIGATION_MESSAGE, tag);
    }

    protected void handleNavigation(int action, int index) {
        switch (action) {
            case NAV_TAB -> {
                var tabs = sideTabsWidget.getAllTabs();
                if (index >= 0 && index < tabs.size()) {
                    navigate(tabs.get(index));
                }
            }
            case NAV_BACK -> navigateBack();
            case NAV_PAGE_SWITCHER -> openPageSwitcher();
            case NAV_SWITCH_PAGE -> {
                if (index >= 0 && index < allPages.size()) {
                    switchPage(allPages.get(index));
                }
            }
            default -> {}
        }
    }

    protected void navigate(IFancyUIProvider newPage) {
        navigate(newPage, this.currentHomePage);
    }

    protected void navigate(IFancyUIProvider nextPage, IFancyUIProvider nextHomePage) {
        if (nextPage != mainPage) {
            if (!this.previousPages.isEmpty() && this.previousPages.peek().page == nextPage) {
                this.previousPages.pop();
            } else if (this.currentPage != null) {
                this.previousPages.push(new NavigationEntry(this.currentPage, this.currentHomePage, () -> {}));
            }
        } else {
            this.previousPages.clear();
        }

        performNavigation(nextPage, nextHomePage);
    }

    protected void navigateBack() {
        if (previousPages.isEmpty()) return;
        var navigationEntry = previousPages.pop();

        performNavigation(navigationEntry.page, navigationEntry.homePage);
        navigationEntry.onNavigation.run();
    }

    protected void performNavigation(IFancyUIProvider nextPage, IFancyUIProvider nextHomePage) {
        if (currentHomePage != nextHomePage) {
            setupSideTabs(nextHomePage);
        }

        this.currentPage = nextPage;
        this.currentHomePage = nextHomePage;

        setupFancyUI(nextPage);
    }

    ///////////////////////////////////////////////
    // *********** PAGE SWITCHER ***********//
    ///////////////////////////////////////////////

    protected void openPageSwitcher() {
        if (allPages.size() <= 1 || currentPage == pageSwitcher) return;
        pageSwitcher.setPageList(allPages, currentHomePage);

        if (currentPage != currentHomePage && !previousPages.isEmpty()) {
            previousPages.pop();
        }

        this.sideTabsWidget.setDisplay(false);

        this.previousPages.push(new NavigationEntry(currentHomePage, currentHomePage,
                () -> sideTabsWidget.setDisplay(true)));

        this.currentPage = this.pageSwitcher;
        this.currentHomePage = this.pageSwitcher;

        setupFancyUI(this.pageSwitcher);
    }

    protected void switchPage(IFancyUIProvider nextHomePage) {
        this.currentHomePage = mainPage;
        this.currentPage = mainPage;
        this.previousPages.clear();

        sideTabsWidget.setDisplay(true);

        setupSideTabs(this.currentHomePage);
        navigate(nextHomePage, nextHomePage);
    }

    //////////////////////////////////////////////
    // *********** UI RENDERING ***********//
    //////////////////////////////////////////////

    protected Page getOrCreatePage(IFancyUIProvider provider) {
        var page = pages.get(provider);
        if (page == null) {
            page = new Page(provider);
            pages.put(provider, page);
            page.attach();
        }
        return page;
    }

    protected void setupFancyUI(IFancyUIProvider fancyUI) {
        var next = getOrCreatePage(fancyUI);
        for (var page : pages.values()) {
            page.setShown(page == next);
        }

        sideTabsWidget.selectTab(fancyUI);
        titleBar.updateState(fancyUI, !this.previousPages.isEmpty(),
                this.allPages.size() > 1 && this.currentPage != this.pageSwitcher);

        if (playerInventory != null) {
            playerInventory.setDisplay(fancyUI.hasPlayerInventory());
        }
    }

    protected void setupSideTabs(IFancyUIProvider homePage) {
        var tabs = sideTabs.get(homePage);
        if (tabs == null) {
            sideTabsWidget.setTabs(null, List.of());
            homePage.attachSideTabs(sideTabsWidget);
            tabs = new SideTabs(sideTabsWidget.getMainTab(), new ArrayList<>(sideTabsWidget.getSubTabs()));
            sideTabs.put(homePage, tabs);
        } else {
            sideTabsWidget.setTabs(tabs.mainTab, tabs.subTabs);
        }
    }

    public class Page {

        @Getter
        protected final IFancyUIProvider provider;
        @Getter
        protected final UIElement container;
        @Getter
        protected final ConfiguratorPanel configuratorPanel;
        @Getter
        protected final ConfiguratorPanel autoPushConfiguratorPanel;
        @Getter
        protected final TooltipsPanel tooltipsPanel;

        protected Page(IFancyUIProvider provider) {
            this.provider = provider;
            this.container = new UIElement();
            container.addClass("gt-fancy-page");
            container.layout(l -> l.alignItems(AlignItems.CENTER).justifyContent(AlignContent.CENTER));
            this.configuratorPanel = new ConfiguratorPanel();
            this.autoPushConfiguratorPanel = new ConfiguratorPanel();
            this.tooltipsPanel = new TooltipsPanel();
        }

        protected void attach() {
            pageContainer.addChild(container);
            configuratorHost.addChild(configuratorPanel);
            autoPushConfiguratorHost.addChild(autoPushConfiguratorPanel);
            tooltipsHost.addChild(tooltipsPanel);
            container.addChild(provider.createMainPage(FancyMachineUIWidget.this));
            provider.attachConfigurators(configuratorPanel);
            provider.attachAutoPushConfigurators(autoPushConfiguratorPanel);
            provider.attachTooltips(tooltipsPanel);
        }

        protected void setShown(boolean shown) {
            container.setDisplay(shown);
            configuratorPanel.setDisplay(shown);
            autoPushConfiguratorPanel.setDisplay(shown);
            tooltipsPanel.setDisplay(shown);
            if (!shown) {
                configuratorPanel.collapseTab();
                autoPushConfiguratorPanel.collapseTab();
            }
        }
    }
}
