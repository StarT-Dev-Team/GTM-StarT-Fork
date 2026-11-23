package com.gregtechceu.gtceu.api.gui.widget.preview;

import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;
import com.gregtechceu.gtceu.integration.xei.handlers.item.CycleItemStackHandler;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import dev.vfyjxf.taffy.style.TaffyPosition;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class VariantSelectorUI {

    public interface Host {

        @Nullable
        MBPattern currentPattern();

        @Nullable
        BlockPos selectedPos();

        float widgetPositionY();

        void clearBlockSelection();

        void selectVariant(PartAbility ability, BlockState chosenState);
    }

    private static final int LIST_X = 3;
    private static final int LIST_Y = 14;
    private static final int LIST_WIDTH = 24;
    private static final int LIST_HEIGHT = 162;
    private static final int ENTRY_SPACING = 20;
    private static final int MAX_VISIBLE_FLYOUT_ROWS = 6;
    private static final int FLYOUT_SCROLLBAR_WIDTH = 3;

    private final Host host;
    private final int widgetHeight;

    private final ScrollerView listGroup;
    private final UIElement flyoutGroup;
    @Nullable
    private VariantCategory activeFlyoutCategory = null;
    @Nullable
    private UIElement activeFlyoutPopup = null;
    @Nullable
    private UIElement activeFlyoutSource = null;

    public VariantSelectorUI(Host host, int widgetWidth, int widgetHeight) {
        this.host = host;
        this.widgetHeight = widgetHeight;

        this.listGroup = new ScrollerView();
        listGroup.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(LIST_X).top(LIST_Y)
                .width(LIST_WIDTH).height(LIST_HEIGHT));
        listGroup.scrollerStyle(style -> style.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO));
        listGroup.viewPort(viewPort -> {
            viewPort.layout(layout -> layout.paddingAll(0));
            viewPort.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        });
        listGroup.addEventListener(UIEvents.MOUSE_WHEEL, event -> closeFlyout(), true);
        listGroup.setDisplay(false);

        this.flyoutGroup = new UIElement();
        flyoutGroup.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(0).top(0).width(0).height(0));
        flyoutGroup.setDisplay(false);
    }

    public ScrollerView getListGroup() {
        return listGroup;
    }

    public UIElement getFlyoutGroup() {
        return flyoutGroup;
    }

    public boolean isFlyoutOpen() {
        return activeFlyoutCategory != null;
    }

    public boolean isMouseOverFlyout(double mouseX, double mouseY) {
        return activeFlyoutCategory != null && activeFlyoutPopup != null && activeFlyoutPopup.isDisplayed() &&
                activeFlyoutPopup.isMouseOverElement((float) mouseX, (float) mouseY);
    }

    public boolean isMouseOverList(double mouseX, double mouseY) {
        return listGroup.isDisplayed() && listGroup.isMouseOverElement((float) mouseX, (float) mouseY);
    }

    public boolean isMouseOverAny(double mouseX, double mouseY) {
        return isMouseOverFlyout(mouseX, mouseY) || isMouseOverList(mouseX, mouseY);
    }

    public void clear() {
        closeFlyout();
        listGroup.clearAllScrollViewChildren();
        listGroup.verticalScroller.setValue(0f, true);
        listGroup.setDisplay(false);
    }

    public void resetScroll() {
        listGroup.verticalScroller.setValue(0f, true);
    }

    public void update() {
        MBPattern pattern = host.currentPattern();
        if (pattern == null || host.selectedPos() == null) {
            clear();
            return;
        }

        Float prevScroll = listGroup.verticalScroller.getValue();
        listGroup.clearAllScrollViewChildren();
        listGroup.setDisplay(true);
        build(pattern);
        listGroup.verticalScroller.setValue(prevScroll == null ? 0f : prevScroll, true);
    }

    private record CandidateEntry(@Nullable VariantCategory category, SimplePredicate simplePredicate,
                                  List<Component> tooltips) {}

    private void build(MBPattern pattern) {
        BlockPos selectedPos = host.selectedPos();
        TraceabilityPredicate predicate = pattern.predicateMap.get(selectedPos);
        if (predicate == null) {
            host.clearBlockSelection();
            return;
        }

        List<SimplePredicate> simples = VariantDiscovery.allSimplePredicates(predicate);
        simples.removeIf(p -> p == null || p.candidates == null);

        Set<VariantCategory> seenCategories = new HashSet<>();
        List<CandidateEntry> entries = new ArrayList<>();
        for (SimplePredicate sp : simples) {
            VariantCategory cat = VariantDiscovery.findMatchingCategory(sp, pattern, selectedPos);
            if (cat != null) {
                if (seenCategories.add(cat)) {
                    entries.add(new CandidateEntry(cat, sp, sp.getToolTips(predicate)));
                }
            } else if (!sp.getCandidates().isEmpty()) {
                entries.add(new CandidateEntry(null, sp, sp.getToolTips(predicate)));
            }
        }

        int total = entries.size();
        if (total == 0) {
            host.clearBlockSelection();
            return;
        }

        var content = new UIElement();
        content.layout(layout -> layout.width(LIST_WIDTH - 4).height(total * ENTRY_SPACING + 2));
        for (int i = 0; i < total; i++) {
            CandidateEntry entry = entries.get(i);
            int y = 1 + i * ENTRY_SPACING;
            if (entry.category() != null) {
                content.addChild(place(createCategoryButton(entry.category(), pattern, entry.tooltips()), 1, y));
            } else {
                content.addChild(place(createCandidateSlot(entry.simplePredicate(), entry.tooltips()), 1, y));
            }
        }
        listGroup.addScrollViewChild(content);
    }

    private static <T extends UIElement> T place(T element, int x, int y) {
        element.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(x).top(y));
        return element;
    }

    private Button stackButton(IGuiTexture texture, ItemStack tooltipStack, List<Component> tooltip) {
        var button = new Button().noText();
        button.layout(layout -> layout.width(18).height(18).paddingAll(0));
        var hover = IGuiTexture.group(texture, new ColorBorderTexture(1, -1));
        button.buttonStyle(style -> style.baseTexture(texture).hoverTexture(hover).pressedTexture(hover));
        button.addEventListener(UIEvents.HOVER_TOOLTIPS,
                event -> event.hoverTooltips = new HoverTooltips(tooltip, null, null, tooltipStack));
        return button;
    }

    private Button createCategoryButton(VariantCategory cat, MBPattern pattern,
                                        @Nullable List<Component> extraTips) {
        BlockState activeState = pattern.activeAbilityChoices.get(cat.ability());
        int vIdx = VariantDiscovery.indexOfBlock(cat.variants(), activeState);
        final ItemStack displayStack = vIdx >= 0 ? cat.variantStacks().get(vIdx) :
                activeState != null ? VariantDiscovery.stackFor(activeState) : cat.variantStacks().get(0);

        List<Component> tooltip = new ArrayList<>();
        tooltip.add(cat.displayName().copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(displayStack.getHoverName().copy().withStyle(ChatFormatting.WHITE));
        if (extraTips != null) {
            tooltip.addAll(extraTips);
        }
        tooltip.add(Component.translatable("gtceu.multiblock.preview.variant_selector.hint")
                .withStyle(ChatFormatting.GRAY));

        var button = stackButton(IGuiTexture.group(ColorPattern.T_GRAY.rectTexture(),
                new ItemStackTexture(displayStack)), displayStack, tooltip);
        button.setOnClick(event -> toggleFlyout(cat, button));
        button.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 1) {
                closeFlyout();
                cycleVariant(cat);
            }
        });
        return button;
    }

    private ItemSlot createCandidateSlot(SimplePredicate sp, @Nullable List<Component> tips) {
        CycleItemStackHandler handler = new CycleItemStackHandler(List.of(sp.getCandidates()));

        var slot = new ItemSlot();
        slot.bind(new ItemHandlerSlot(handler, 0).setCanTake(player -> false).setCanPlace(stack -> false));
        slot.xeiRecipeIngredient(IngredientIO.INPUT).xeiRecipeSlot(IngredientIO.INPUT, 1);
        slot.style(style -> {
            style.backgroundTexture(ColorPattern.T_GRAY.rectTexture());
            if (tips != null) style.appendTooltips(tips.toArray(Component[]::new));
        });
        slot.slotStyle(style -> style.hoverOverlay(new ColorBorderTexture(1, -1)));
        slot.addEventListener(UIEvents.MOUSE_DOWN, event -> closeFlyout());
        return slot;
    }

    private void cycleVariant(VariantCategory category) {
        MBPattern pattern = host.currentPattern();
        if (pattern == null || category.variants().isEmpty()) return;
        int current = VariantDiscovery.indexOfBlock(category.variants(),
                pattern.activeAbilityChoices.get(category.ability()));
        int next = (Math.max(0, current) + 1) % category.variants().size();
        host.selectVariant(category.ability(), category.variants().get(next));
    }

    private void toggleFlyout(VariantCategory category, @Nullable UIElement sourceWidget) {
        if (this.activeFlyoutCategory == category) {
            closeFlyout();
            return;
        }
        openFlyout(category, sourceWidget);
    }

    private void openFlyout(VariantCategory category, @Nullable UIElement sourceWidget) {
        MBPattern pattern = host.currentPattern();
        if (pattern == null) return;

        closeFlyout();
        this.activeFlyoutCategory = category;
        this.activeFlyoutSource = sourceWidget;
        if (sourceWidget != null) {
            sourceWidget.style(style -> style.overlay(new ColorBorderTexture(1, 0xff00ffff)));
        }
        flyoutGroup.setDisplay(true);

        BlockState currentChoice = pattern.activeAbilityChoices.get(category.ability());

        int count = category.variants().size();
        int cols = Math.max(1, Math.min(count, 6));
        int rows = (count + cols - 1) / cols;
        int visibleRows = Math.max(1, Math.min(rows, MAX_VISIBLE_FLYOUT_ROWS));
        boolean needScroll = rows > MAX_VISIBLE_FLYOUT_ROWS;

        int flyoutWidth = cols * 18 + 4 + (needScroll ? FLYOUT_SCROLLBAR_WIDTH + 1 : 0);
        int flyoutHeight = visibleRows * 18 + 4;
        int flyoutX = LIST_X + LIST_WIDTH + 1;
        int sourceY = sourceWidget != null ? (int) (sourceWidget.getPositionY() - host.widgetPositionY()) : LIST_Y;
        int flyoutY = Mth.clamp(sourceY, LIST_Y, Math.max(LIST_Y, widgetHeight - 32 - flyoutHeight));

        var content = new UIElement();
        content.layout(layout -> layout.width(cols * 18 + 4).height(rows * 18 + 4));

        for (int i = 0; i < count; i++) {
            int x = 2 + (i % cols) * 18;
            int y = 2 + (i / cols) * 18;

            BlockState vState = category.variants().get(i);
            ItemStack vStack = category.variantStacks().get(i);
            boolean isSelected = currentChoice != null && currentChoice.getBlock() == vState.getBlock();

            var texture = IGuiTexture.group(
                    isSelected ? new ColorRectTexture(0x4000ff00) : new ColorRectTexture(0x20ffffff),
                    new ItemStackTexture(vStack));
            var vBtn = stackButton(texture, vStack, List.of(vStack.getHoverName()));
            var hover = IGuiTexture.group(texture, new ColorBorderTexture(1, isSelected ? 0xff00ff00 : -1));
            vBtn.buttonStyle(style -> style.hoverTexture(hover).pressedTexture(hover));
            vBtn.setOnClick(event -> {
                closeFlyout();
                host.selectVariant(category.ability(), vState);
            });
            content.addChild(place(vBtn, x, y));
        }

        UIElement popup;
        var background = IGuiTexture.group(new ColorRectTexture(0xEE8F8F8F), new ColorBorderTexture(1, 0x50A0A0A0));
        if (needScroll) {
            var scroller = new ScrollerView();
            scroller.scrollerStyle(style -> style.mode(ScrollerMode.VERTICAL)
                    .verticalScrollDisplay(ScrollDisplay.ALWAYS));
            scroller.viewPort(viewPort -> {
                viewPort.layout(layout -> layout.paddingAll(0));
                viewPort.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
            });
            scroller.addScrollViewChild(content);
            int selectedIndex = VariantDiscovery.indexOfBlock(category.variants(), currentChoice);
            if (selectedIndex >= 0) {
                int selectedRow = selectedIndex / cols;
                int maxScroll = (rows - visibleRows) * 18;
                int targetScroll = Mth.clamp((selectedRow - visibleRows / 2) * 18, 0, maxScroll);
                if (targetScroll > 0 && maxScroll > 0) {
                    scroller.verticalScroller.setValue((float) targetScroll / maxScroll, true);
                }
            }
            popup = scroller;
        } else {
            popup = content;
        }
        popup.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(flyoutX).top(flyoutY)
                .width(flyoutWidth).height(flyoutHeight));
        popup.style(style -> style.backgroundTexture(background));
        popup.addEventListener(UIEvents.MOUSE_DOWN, UIEvent::stopPropagation);
        popup.addEventListener(UIEvents.MOUSE_WHEEL, UIEvent::stopPropagation);

        flyoutGroup.addChild(popup);
        this.activeFlyoutPopup = popup;
    }

    public void closeFlyout() {
        if (activeFlyoutSource != null) {
            activeFlyoutSource.style(style -> style.overlay(IGuiTexture.EMPTY));
        }
        activeFlyoutCategory = null;
        activeFlyoutPopup = null;
        activeFlyoutSource = null;
        flyoutGroup.clearAllChildren();
        flyoutGroup.setDisplay(false);
    }
}
