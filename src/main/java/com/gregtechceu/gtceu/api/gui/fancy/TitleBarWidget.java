package com.gregtechceu.gtceu.api.gui.fancy;

import com.gregtechceu.gtceu.api.gui.GuiTextures;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;

import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TitleBarWidget extends UIElement {

    private static final int BORDER_WIDTH = 3;
    private static final int HEIGHT = 16;
    private static final int BTN_WIDTH = 18;
    private static final float ROLL_SPEED = 0.7f;

    private final Button backButton;
    private final Button menuButton;
    private final UIElement mainSection;
    private final UIElement tabIcon;
    private final Label tabTitle;

    public TitleBarWidget(UIEventListener onBackClicked, UIEventListener onMenuClicked) {
        addClass("gt-title-bar");
        layout(l -> l.flexDirection(FlexDirection.ROW).height(HEIGHT));

        this.backButton = createButton("<", "gtceu.gui.title_bar.back", onBackClicked);
        this.backButton.text.getTextStyle().textAlignVertical(Vertical.BOTTOM);
        this.menuButton = createButton("+", "gtceu.gui.title_bar.page_switcher", onMenuClicked);

        this.mainSection = new UIElement();
        mainSection.addClass("gt-title-bar-main");
        mainSection.setOverflowVisible(false);
        mainSection.layout(l -> l.flex(1).height(HEIGHT).flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.CENTER).paddingHorizontal(BORDER_WIDTH + 1).paddingTop(BORDER_WIDTH)
                .gapAll(2));
        mainSection.style(s -> s.background(GuiTextures.TITLE_BAR_BACKGROUND));

        this.tabIcon = new UIElement();
        tabIcon.layout(l -> l.width(HEIGHT - BORDER_WIDTH - 2).height(HEIGHT - BORDER_WIDTH - 2));
        this.tabTitle = new Label();
        tabTitle.layout(l -> l.flex(1).minWidth(0));
        tabTitle.setOverflowVisible(false);
        tabTitle.textStyle(t -> t.textWrap(TextWrap.ROLL).adaptiveWidth(false).rollSpeed(ROLL_SPEED).textShadow(false)
                .textColor(0xFF000000));
        mainSection.addChildren(tabIcon, tabTitle);

        addChildren(backButton, mainSection, menuButton);
    }

    private Button createButton(String text, String tooltip, UIEventListener onClick) {
        var button = new Button();
        button.setText(text, false);
        button.textStyle(t -> t.textShadow(false).textColor(0xFF000000)
                .textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER));
        button.layout(l -> l.width(BTN_WIDTH).height(HEIGHT - BORDER_WIDTH).marginTop(BORDER_WIDTH).paddingAll(0));
        button.buttonStyle(s -> s.baseTexture(GuiTextures.TITLE_BAR_BACKGROUND)
                .hoverTexture(GuiTextures.TITLE_BAR_BACKGROUND.copy().setColor(0xFFDDDDDD))
                .pressedTexture(GuiTextures.TITLE_BAR_BACKGROUND.copy().setColor(0xFFBBBBBB)));
        button.style(s -> s.tooltips(tooltip));
        button.setOnClick(onClick);
        return button;
    }

    public void updateState(IFancyUIProvider currentPage, boolean showBackButton, boolean showMenuButton) {
        tabTitle.setText(currentPage.getTitle().copy());
        var icon = currentPage.getTabIcon();
        tabIcon.style(s -> s.background(icon == null ? IGuiTexture.EMPTY : icon));

        backButton.setVisible(showBackButton);
        backButton.setActive(showBackButton);
        menuButton.setVisible(showMenuButton);
        menuButton.setActive(showMenuButton);
    }

    public Component getTitle() {
        return tabTitle.getText();
    }
}
