package com.gregtechceu.gtceu.api.gui.fancy;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.config.ConfigHolder;

import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.util.UISoundUtils;
import com.lowdragmc.lowdraglib2.gui.util.WindowDragHelper;
import com.lowdragmc.lowdraglib2.math.interpolate.Eases;

import dev.vfyjxf.taffy.style.*;
import it.unimi.dsi.fastutil.floats.FloatObjectPair;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ConfiguratorPanel extends UIElement {

    public static final int TAB_SIZE = 24;

    @Getter
    protected List<Tab> tabs = new ArrayList<>();
    @Getter
    @Nullable
    protected Tab expanded;
    @Setter
    protected int border = 4;
    @Setter
    protected IGuiTexture texture = GuiTextures.BACKGROUND;

    public ConfiguratorPanel() {
        addClass("gt-configurator-panel");
        layout(l -> l.width(TAB_SIZE).gapAll(2).justifyContent(AlignContent.FLEX_END));
    }

    public void clear() {
        collapseTab();
        for (var tab : new ArrayList<>(getChildren())) {
            removeChild(tab);
        }
        tabs.clear();
    }

    public int getTabSize() {
        return TAB_SIZE;
    }

    public void attachConfigurators(IFancyConfigurator... fancyConfigurators) {
        for (IFancyConfigurator fancyConfigurator : fancyConfigurators) {
            var tab = new Tab(fancyConfigurator);
            tabs.add(tab);
            addChild(tab);
        }
    }

    public void expandTab(Tab tab) {
        if (expanded != null && expanded != tab) {
            var previous = expanded;
            expanded = null;
            previous.collapse();
        }
        expanded = tab;
        tab.expand();
    }

    public void collapseTab() {
        var tab = expanded;
        expanded = null;
        if (tab != null) {
            tab.collapse();
        }
    }

    public FloatingTab createFloatingTab(IFancyConfigurator configurator) {
        return new FloatingTab(configurator);
    }

    public class Tab extends UIElement {

        @Getter
        protected final IFancyConfigurator configurator;
        @Getter
        protected final UIElement button;
        @Nullable
        @Getter
        protected final UIElement view;

        public Tab(IFancyConfigurator configurator) {
            this.configurator = configurator;
            addClass("gt-configurator-tab");
            layout(l -> l.width(getTabSize()).height(getTabSize()));
            style(s -> s.background(texture));

            this.button = createIcon();
            addChild(button);

            if (configurator instanceof IFancyConfiguratorButton fancyButton) {
                this.view = null;
                button.addEventListener(UIEvents.MOUSE_DOWN, e -> UISoundUtils.playButtonClickSound());
                button.addServerEventListener(UIEvents.MOUSE_DOWN, e -> {
                    if (e.button == 2 && configurator instanceof IFancyCustomMiddleClickAction middleAction) {
                        middleAction.onMiddleClick();
                    } else {
                        fancyButton.onClick(e);
                    }
                });
            } else {
                this.view = createView();
                addChild(view);
                button.addEventListener(UIEvents.MOUSE_DOWN, this::onIconClicked);
                if (configurator instanceof IFancyCustomMiddleClickAction middleAction) {
                    button.addServerEventListener(UIEvents.MOUSE_DOWN, e -> {
                        if (e.button == 2) {
                            middleAction.onMiddleClick();
                        }
                    });
                }
            }

            if (configurator instanceof IFancyCustomMouseWheelAction wheelAction) {
                button.addEventListener(UIEvents.MOUSE_WHEEL, UIEvent::stopPropagation);
                button.addServerEventListener(UIEvents.MOUSE_WHEEL, e -> wheelAction.mouseWheelMove(e.deltaY));
            }

            configurator.bindTab(this);
        }

        protected UIElement createIcon() {
            var icon = new UIElement();
            icon.addClass("gt-configurator-tab-icon");
            icon.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).right(4).top(4).width(16).height(16));
            icon.style(s -> s.background(DynamicTexture.of(configurator::getIcon)));
            icon.addEventListener(UIEvents.HOVER_TOOLTIPS,
                    e -> e.hoverTooltips = new HoverTooltips(configurator.getTooltips(), null, null, null));
            return icon;
        }

        protected UIElement createView() {
            var content = configurator.createConfigurator();

            var wrapper = new UIElement();
            wrapper.addClass("gt-configurator-view-wrapper");
            wrapper.getLayoutStyle().positionType(TaffyPosition.ABSOLUTE).flexDirection(FlexDirection.ROW)
                    .right(viewOffset());

            var view = new UIElement();
            view.addClass("gt-configurator-view");
            view.layout(l -> l.paddingAll(border).gapAll(2).minWidth(getTabSize() * 2));
            view.style(s -> s.background(texture).zIndex(1));
            wrapper.addChild(view);

            var header = new UIElement();
            header.addClass("gt-configurator-view-header");
            header.layout(l -> l.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).height(16).gapAll(2));
            var title = new Label();
            title.setText(configurator.getTitle());
            title.textStyle(
                    t -> t.textWrap(TextWrap.ROLL).adaptiveWidth(false).textShadow(false).textColor(0xFF404040));
            title.layout(l -> l.flex(1).minWidth(0).paddingLeft(4));
            title.setOverflowVisible(false);
            var close = new UIElement();
            close.layout(l -> l.width(16).height(16));
            close.style(s -> s.background(DynamicTexture.of(configurator::getIcon)));
            close.addEventListener(UIEvents.MOUSE_DOWN, this::onIconClicked);
            close.addEventListener(UIEvents.HOVER_TOOLTIPS,
                    e -> e.hoverTooltips = new HoverTooltips(configurator.getTooltips(), null, null, null));
            header.addChildren(title, close);

            view.addChildren(header, content);

            wrapper.setVisible(false);

            WindowDragHelper.setDragMove(header, view, null, null);
            return wrapper;
        }

        protected float viewOffset() {
            return tabs.size() > 1 ? getTabSize() + 2 : 0;
        }

        protected void onIconClicked(UIEvent event) {
            if (event.button != 0) return;
            event.stopPropagation();
            UISoundUtils.playButtonClickSound();
            if (expanded == this) {
                collapseTab();
            } else {
                expandTab(this);
            }
        }

        protected void expand() {
            if (view == null) return;
            view.layout(l -> l.bottom(0).right(viewOffset()));
            view.setVisible(true);
            style(s -> s.zIndex(10));
            playViewAnimation(true, null);
        }

        protected void collapse() {
            if (view == null) return;
            style(s -> s.zIndex(0));
            if (animationSeconds() <= 0 || !view.isDisplayed()) {
                view.setVisible(false);
                return;
            }
            playViewAnimation(false, () -> {
                if (expanded != this) {
                    view.setVisible(false);
                }
            });
        }

        protected void hideViewImmediately() {
            if (view != null) {
                view.setVisible(false);
            }
        }

        protected void playViewAnimation(boolean opening, @Nullable Runnable onFinished) {
            var seconds = animationSeconds();
            if (view == null || seconds <= 0) {
                if (onFinished != null) onFinished.run();
                return;
            }
            var hidden = Transform2D.identity().scale(0.6f).pivot(1f, 1f);
            var shown = Transform2D.identity().pivot(1f, 1f);
            var animation = view.animation();
            animation.duration(seconds).ease(Eases.QUAD_OUT);
            animation.style(PropertyRegistry.OPACITY,
                    FloatObjectPair.of(0f, opening ? 0f : 1f), FloatObjectPair.of(1f, opening ? 1f : 0f));
            animation.style(PropertyRegistry.TRANSFORM_2D,
                    FloatObjectPair.of(0f, opening ? hidden : shown), FloatObjectPair.of(1f, opening ? shown : hidden));
            if (onFinished != null) {
                var done = new boolean[1];
                animation.onFinished(e -> {
                    if (!done[0]) {
                        done[0] = true;
                        onFinished.run();
                    }
                });
            }
            animation.start();
        }

        protected float animationSeconds() {
            return ConfigHolder.INSTANCE.client.animationTime / 1000f;
        }
    }

    public class FloatingTab extends Tab {

        protected Runnable closeCallback = () -> {};

        public FloatingTab(IFancyConfigurator configurator) {
            super(configurator);
            layout(l -> l.positionType(TaffyPosition.ABSOLUTE).top(0).right(0).widthAuto().heightAuto());
            style(s -> s.background(IGuiTexture.EMPTY));
            button.setVisible(false);
        }

        @Override
        protected float viewOffset() {
            return getTabSize() + 2;
        }

        @Override
        protected void collapse() {
            hideViewImmediately();
            super.collapse();
            removeSelf();
            closeCallback.run();
        }

        public void onClose(Runnable closeCallback) {
            this.closeCallback = closeCallback;
        }
    }
}
