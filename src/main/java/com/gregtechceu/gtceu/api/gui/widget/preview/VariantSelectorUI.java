package com.gregtechceu.gtceu.api.gui.widget.preview;

import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;
import com.gregtechceu.gtceu.integration.xei.handlers.item.CycleItemStackHandler;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.NotNull;
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

        Position widgetPosition();

        void clearBlockSelection();

        void selectVariant(PartAbility ability, BlockState chosenState);
    }

    private static final int LIST_X = 3;
    private static final int LIST_Y = 14;
    private static final int LIST_WIDTH = 24;
    private static final int LIST_HEIGHT = 162;
    private static final int MAX_UNSCROLLED_ENTRIES = 8;
    private static final int ENTRY_SPACING = 20;

    private final Host host;
    private final int widgetWidth;
    private final int widgetHeight;

    private final DraggableScrollableWidgetGroup listGroup;
    private final WidgetGroup flyoutGroup;
    @Nullable
    private VariantCategory activeFlyoutCategory = null;
    @Nullable
    private WidgetGroup activeFlyoutPopup = null;

    public VariantSelectorUI(Host host, int widgetWidth, int widgetHeight) {
        this.host = host;
        this.widgetWidth = widgetWidth;
        this.widgetHeight = widgetHeight;

        this.listGroup = new DraggableScrollableWidgetGroup(LIST_X, LIST_Y, LIST_WIDTH, LIST_HEIGHT) {

            @Override
            public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
                closeFlyout();
                return super.mouseWheelMove(mouseX, mouseY, wheelDelta);
            }

            @Override
            public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
                closeFlyout();
                return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            }
        };
        listGroup.setYScrollBarWidth(3)
                .setYBarStyle(new ColorRectTexture(0x30000000), ColorPattern.T_WHITE.rectTexture().setRadius(1))
                .setScrollable(true)
                .setDraggable(false)
                .setScrollWheelDirection(DraggableScrollableWidgetGroup.ScrollWheelDirection.VERTICAL)
                .setUseScissor(true);
        listGroup.setVisible(false);

        this.flyoutGroup = new WidgetGroup(0, 0, widgetWidth, widgetHeight) {

            @Nullable
            @Override
            public Widget getHoverElement(double mouseX, double mouseY) {
                for (int i = widgets.size() - 1; i >= 0; i--) {
                    Widget widget = widgets.get(i);
                    if (widget.isVisible()) {
                        Widget hover = widget.getHoverElement(mouseX, mouseY);
                        if (hover != null) {
                            return hover;
                        }
                    }
                }
                return null;
            }
        };
        flyoutGroup.setVisible(false);
    }

    public DraggableScrollableWidgetGroup getListGroup() {
        return listGroup;
    }

    public WidgetGroup getFlyoutGroup() {
        return flyoutGroup;
    }

    public boolean isFlyoutOpen() {
        return activeFlyoutCategory != null;
    }

    public boolean isMouseOverFlyout(double mouseX, double mouseY) {
        return activeFlyoutCategory != null && activeFlyoutPopup != null && activeFlyoutPopup.isVisible() &&
                activeFlyoutPopup.isMouseOverElement(mouseX, mouseY);
    }

    public boolean isMouseOverList(double mouseX, double mouseY) {
        return listGroup.isVisible() && listGroup.isMouseOverElement(mouseX, mouseY);
    }

    public boolean isMouseOverAny(double mouseX, double mouseY) {
        return isMouseOverFlyout(mouseX, mouseY) || isMouseOverList(mouseX, mouseY);
    }

    public void clear() {
        closeFlyout();
        listGroup.clearAllWidgets();
        listGroup.setScrollYOffset(0);
        listGroup.setVisible(false);
    }

    public void resetScroll() {
        listGroup.setScrollYOffset(0);
    }

    public void update() {
        MBPattern pattern = host.currentPattern();
        if (pattern == null || host.selectedPos() == null) {
            clear();
            return;
        }

        int prevScroll = listGroup.getScrollYOffset();
        listGroup.clearAllWidgets();
        listGroup.setVisible(true);
        build(pattern);
        listGroup.setScrollYOffset(prevScroll);
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

        listGroup.setYScrollBarWidth(total > MAX_UNSCROLLED_ENTRIES ? 3 : 0);
        listGroup.setScrollable(total > MAX_UNSCROLLED_ENTRIES);

        for (int i = 0; i < total; i++) {
            CandidateEntry entry = entries.get(i);
            int y = 1 + i * ENTRY_SPACING;
            if (entry.category() != null) {
                listGroup.addWidget(createCategoryButton(entry.category(), pattern, 1, y, entry.tooltips()));
            } else {
                listGroup.addWidget(createCandidateSlot(entry.simplePredicate(), 1, y, entry.tooltips()));
            }
        }

        listGroup.addWidget(new Widget(0, 1 + (total - 1) * ENTRY_SPACING + 19, 1, 2));
    }

    private static class StackTooltipButton extends ButtonWidget {

        private final ItemStack tooltipStack;
        private final List<Component> tooltip;

        StackTooltipButton(int x, int y, IGuiTexture texture, ItemStack tooltipStack, List<Component> tooltip) {
            super(x, y, 18, 18, texture, cd -> {});
            this.tooltipStack = tooltipStack;
            this.tooltip = tooltip;
            setHoverBorderTexture(1, -1);
        }

        @Override
        public List<Component> getTooltipTexts() {
            return tooltip;
        }

        @Override
        protected void drawTooltipTexts(int mouseX, int mouseY) {
            this.tooltipTexts.clear();
            this.tooltipTexts.addAll(tooltip);
            if (isMouseOverElement(mouseX, mouseY) && getHoverElement(mouseX, mouseY) == this &&
                    gui != null && gui.getModularUIGui() != null) {
                gui.getModularUIGui().setHoverTooltip(tooltip, tooltipStack, null, null);
            }
        }
    }

    private Widget createCategoryButton(VariantCategory cat, MBPattern pattern, int itemX, int itemY,
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

        return new StackTooltipButton(itemX, itemY,
                new GuiTextureGroup(ColorPattern.T_GRAY.rectTexture(), new ItemStackTexture(displayStack)),
                displayStack, tooltip) {

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (isMouseOverElement(mouseX, mouseY)) {
                    if (button == 1) {
                        closeFlyout();
                        cycleVariant(cat);
                        playButtonClickSound();
                        return true;
                    } else if (button == 0) {
                        toggleFlyout(cat, this);
                        playButtonClickSound();
                        return true;
                    }
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
                super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
                if (activeFlyoutCategory == cat) {
                    Position pos = getPosition();
                    Size size = getSize();
                    new ColorBorderTexture(1, 0xff00ffff).draw(graphics, mouseX, mouseY, pos.x, pos.y, size.width,
                            size.height);
                }
            }
        };
    }

    private Widget createCandidateSlot(SimplePredicate sp, int itemX, int itemY, List<Component> tips) {
        CycleItemStackHandler handler = new CycleItemStackHandler(List.of(sp.getCandidates()));

        return new SlotWidget(handler, 0, itemX, itemY, false, false) {

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (isMouseOverElement(mouseX, mouseY)) {
                    closeFlyout();
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }
        }
                .setIngredientIO(IngredientIO.INPUT)
                .setBackgroundTexture(ColorPattern.T_GRAY.rectTexture())
                .setOnAddedTooltips((s, list) -> {
                    if (tips != null) list.addAll(tips);
                })
                .setHoverTexture(new ColorBorderTexture(1, -1));
    }

    private void cycleVariant(VariantCategory category) {
        MBPattern pattern = host.currentPattern();
        if (pattern == null || category.variants().isEmpty()) return;
        int current = VariantDiscovery.indexOfBlock(category.variants(),
                pattern.activeAbilityChoices.get(category.ability()));
        int next = (Math.max(0, current) + 1) % category.variants().size();
        host.selectVariant(category.ability(), category.variants().get(next));
    }

    private void toggleFlyout(VariantCategory category, @Nullable Widget sourceWidget) {
        if (this.activeFlyoutCategory == category) {
            closeFlyout();
            return;
        }
        openFlyout(category, sourceWidget);
    }

    private void openFlyout(VariantCategory category, @Nullable Widget sourceWidget) {
        MBPattern pattern = host.currentPattern();
        if (pattern == null) return;

        this.activeFlyoutCategory = category;
        flyoutGroup.clearAllWidgets();
        flyoutGroup.setVisible(true);

        BlockState currentChoice = pattern.activeAbilityChoices.get(category.ability());

        int count = category.variants().size();
        int cols = Math.min(count, 6);
        WidgetGroup popup = createPopup(sourceWidget, count, cols);

        for (int i = 0; i < count; i++) {
            int x = 2 + (i % cols) * 18;
            int y = 2 + (i / cols) * 18;

            BlockState vState = category.variants().get(i);
            ItemStack vStack = category.variantStacks().get(i);
            boolean isSelected = currentChoice != null && currentChoice.getBlock() == vState.getBlock();

            StackTooltipButton vBtn = new StackTooltipButton(x, y,
                    new GuiTextureGroup(
                            isSelected ? new ColorRectTexture(0x4000ff00) : new ColorRectTexture(0x20ffffff),
                            new ItemStackTexture(vStack)),
                    vStack, List.of(vStack.getHoverName())) {

                @Override
                public boolean mouseClicked(double mouseX, double mouseY, int button) {
                    if (isMouseOverElement(mouseX, mouseY)) {
                        closeFlyout();
                        host.selectVariant(category.ability(), vState);
                        playButtonClickSound();
                        return true;
                    }
                    return false;
                }
            };
            vBtn.setHoverBorderTexture(1, isSelected ? 0xff00ff00 : -1);
            popup.addWidget(vBtn);
        }

        flyoutGroup.addWidget(popup);
        this.activeFlyoutPopup = popup;
    }

    private @NotNull WidgetGroup createPopup(@Nullable Widget sourceWidget, int count, int cols) {
        int rows = (count + cols - 1) / cols;

        int flyoutWidth = cols * 18 + 4;
        int flyoutHeight = rows * 18 + 4;

        int flyoutX = LIST_X + LIST_WIDTH + 1;
        int sourceY = sourceWidget != null ? sourceWidget.getPosition().y - host.widgetPosition().y : LIST_Y;
        int flyoutY = Mth.clamp(sourceY, LIST_Y, Math.max(LIST_Y, widgetHeight - 32 - flyoutHeight));

        return new WidgetGroup(flyoutX, flyoutY, flyoutWidth, flyoutHeight) {

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return super.mouseClicked(mouseX, mouseY, button) || isMouseOverElement(mouseX, mouseY);
            }

            @Override
            public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
                return super.mouseWheelMove(mouseX, mouseY, wheelDelta) || isMouseOverElement(mouseX, mouseY);
            }
        };
    }

    public void closeFlyout() {
        activeFlyoutCategory = null;
        activeFlyoutPopup = null;
        flyoutGroup.clearAllWidgets();
        flyoutGroup.setVisible(false);
    }
}
