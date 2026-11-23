package com.gregtechceu.gtceu.integration.ae2.gui.widget;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.slot.AEConfigSlotWidget;
import com.gregtechceu.gtceu.integration.ae2.slot.IConfigurableSlot;
import com.gregtechceu.gtceu.integration.ae2.slot.PortAEInventory;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import net.minecraft.network.chat.Component;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public class ConfigWidget extends UIElement {

    protected final IConfigurableSlot[] config;
    protected final IConfigurableSlot[] displayList;

    @Getter
    protected final boolean isStocking;

    private final BooleanSupplier autoPull;
    private final Predicate<GenericStack> hasStackInConfig;
    @Nullable
    private final Class<? extends AEKey> keyType;

    private static final int SLOTS_PER_ROW = 8;
    private static final int ROWS_PER_PAGE = 3;
    private static final int SLOTS_PER_PAGE = SLOTS_PER_ROW * ROWS_PER_PAGE;
    private static final int ROW_HEIGHT = 18 * 2 + 2;
    private static final int PAGER_HEIGHT = 14;
    private static final int PAGER_BUTTON_SIZE = 12;

    private final List<UIElement> pages = new ArrayList<>();
    private final int pageCount;
    private int currentPage;

    public ConfigWidget(IConfigurableSlot[] config, boolean isStocking, BooleanSupplier autoPull,
                        Predicate<GenericStack> hasStackInConfig, @Nullable Class<? extends AEKey> keyType) {
        this.pageCount = calculatePageCount(config.length);
        this.config = config;
        this.isStocking = isStocking;
        this.autoPull = autoPull;
        this.hasStackInConfig = hasStackInConfig;
        this.keyType = keyType;
        this.displayList = new IConfigurableSlot[config.length];

        var width = Math.min(config.length, SLOTS_PER_ROW) * 18;
        var height = calculateHeight(config.length);
        layout(layout -> layout.width(width).height(height));

        var pagesHeight = Math.min((config.length + SLOTS_PER_ROW - 1) / SLOTS_PER_ROW, ROWS_PER_PAGE) * ROW_HEIGHT;
        for (int page = 0; page < this.pageCount; page++) {
            var pageElement = new UIElement();
            pageElement.layout(layout -> layout.width(width).height(pagesHeight));
            this.pages.add(pageElement);
            addChild(pageElement);
        }

        UIElement row = null;
        for (int index = 0; index < config.length; index++) {
            this.displayList[index] = new PortAEInventory.Snapshot();

            if (index % SLOTS_PER_ROW == 0) {
                row = new UIElement();
                row.layout(layout -> layout.flexDirection(FlexDirection.ROW).height(ROW_HEIGHT));
                this.pages.get(index / SLOTS_PER_PAGE).addChild(row);
            }
            row.addChild(new AEConfigSlotWidget(this, index));
        }

        if (this.pageCount > 1) {
            createPageControls(width);
            showPage(0);
        }
    }

    private static int calculatePageCount(int slots) {
        return (slots + SLOTS_PER_PAGE - 1) / SLOTS_PER_PAGE;
    }

    private static int calculateHeight(int slots) {
        int rows = Math.min((slots + SLOTS_PER_ROW - 1) / SLOTS_PER_ROW, ROWS_PER_PAGE);

        return rows * ROW_HEIGHT + (calculatePageCount(slots) > 1 ? PAGER_HEIGHT : 0);
    }

    private void createPageControls(int width) {
        var controls = new UIElement();
        controls.layout(layout -> layout.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER)
                .width(width).height(PAGER_HEIGHT));

        var label = new Label();
        label.layout(layout -> layout.flex(1).heightPercent(100));
        label.textStyle(style -> style.textColor(0xFF404040).textShadow(false)
                .textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER));
        label.addEventListener(UIEvents.TICK,
                event -> label.setText(Component.literal(this.currentPage + 1 + " / " + this.pageCount)));

        controls.addChildren(createPageButton(GuiTextures.BUTTON_PAGE_LEFT, -1), label,
                createPageButton(GuiTextures.BUTTON_PAGE_RIGHT, 1));
        addChild(controls);
    }

    private Button createPageButton(IGuiTexture icon, int direction) {
        var button = new Button();
        button.noText();
        button.layout(layout -> layout.width(PAGER_BUTTON_SIZE).height(PAGER_BUTTON_SIZE).paddingAll(0));
        button.buttonStyle(style -> style.baseTexture(IGuiTexture.group(GuiTextures.BUTTON, icon)));
        button.setOnClick(event -> showPage(this.currentPage + direction));
        return button;
    }

    private void showPage(int newPage) {
        this.currentPage = Math.floorMod(newPage, this.pageCount);

        for (int index = 0; index < this.pages.size(); index++) {
            this.pages.get(index).setDisplay(index == this.currentPage);
        }
    }

    public boolean hasStackInConfig(GenericStack stack) {
        return this.hasStackInConfig.test(stack);
    }

    public boolean isAutoPull() {
        return this.autoPull.getAsBoolean();
    }

    public boolean acceptsKey(AEKey key) {
        return this.keyType == null || this.keyType.isInstance(key);
    }

    public boolean acceptsItems() {
        return this.keyType == null || this.keyType == AEItemKey.class;
    }

    public boolean acceptsFluids() {
        return this.keyType == null || this.keyType == AEFluidKey.class;
    }

    public final IConfigurableSlot getConfig(int index) {
        return this.config[index];
    }

    public final IConfigurableSlot getDisplay(int index) {
        return this.displayList[index];
    }
}
