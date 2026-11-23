package com.gregtechceu.gtceu.api.gui.fancy;

import com.gregtechceu.gtceu.api.gui.GuiTextures;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;

import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PageSwitcher implements IFancyUIProvider {

    private final Consumer<IFancyUIProvider> onPageSwitched;

    private List<IFancyUIProvider> pages = List.of();

    public PageSwitcher(Consumer<IFancyUIProvider> onPageSwitched) {
        this.onPageSwitched = onPageSwitched;
    }

    public void setPageList(List<IFancyUIProvider> allPages, IFancyUIProvider currentPage) {
        this.pages = allPages;
    }

    @Override
    public UIElement createMainPage(FancyMachineUIWidget widget) {
        var scroller = new ScrollerView();
        scroller.layout(l -> l.width(156).height(146));

        var groupedPages = pages.stream().collect(Collectors.groupingBy(
                page -> Objects.requireNonNullElse(page.getPageGroupingData(), new PageGroupingData(null, -1))));

        groupedPages.keySet().stream()
                .sorted(Comparator.comparingInt(PageGroupingData::groupPositionWeight))
                .forEachOrdered(group -> {
                    if (group.groupKey() != null) {
                        var label = new Label();
                        label.setText(Component.translatable(group.groupKey()));
                        label.textStyle(t -> t.textShadow(false).textColor(0xFF404040));
                        scroller.addScrollViewChild(label);
                    }
                    var row = new UIElement();
                    row.layout(l -> l.flexDirection(FlexDirection.ROW).flexWrap(FlexWrap.WRAP).gapAll(5)
                            .marginBottom(5));
                    for (var page : groupedPages.get(group)) {
                        var button = new Button();
                        button.noText();
                        button.layout(l -> l.width(25).height(25).paddingAll(4).alignItems(AlignItems.CENTER)
                                .justifyContent(AlignContent.CENTER));
                        button.buttonStyle(s -> s.baseTexture(GuiTextures.BACKGROUND)
                                .hoverTexture(GuiTextures.BACKGROUND.copy().setColor(0xFFDDDDDD))
                                .pressedTexture(GuiTextures.BACKGROUND.copy().setColor(0xFFBBBBBB)));
                        var icon = new UIElement();
                        icon.layout(l -> l.width(17).height(17));
                        icon.style(s -> s.background(page.getTabIcon()));
                        icon.setAllowHitTest(false);
                        button.addChild(icon);
                        button.style(s -> s.tooltips(page.getTitle()));
                        button.setOnClick(e -> onPageSwitched.accept(page));
                        row.addChild(button);
                    }
                    scroller.addScrollViewChild(row);
                });

        return scroller;
    }

    @Override
    public IGuiTexture getTabIcon() {
        return new TextTexture("+").setDropShadow(false).setColor(ChatFormatting.BLACK.getColor());
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gtceu.gui.title_bar.page_switcher");
    }

    @Override
    public boolean hasPlayerInventory() {
        return false;
    }
}
