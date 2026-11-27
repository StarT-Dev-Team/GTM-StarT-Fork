package com.gregtechceu.gtceu.api.cover.filter;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.PhantomItemSlot;
import com.gregtechceu.gtceu.api.gui.widget.ToggleButton;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;

import java.util.Arrays;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SimpleItemFilter implements ItemFilter {

    @Getter
    protected boolean isBlackList;
    @Getter
    protected boolean ignoreNbt;
    @Getter
    protected ItemStack[] matches = new ItemStack[9];

    protected Consumer<ItemFilter> itemWriter = filter -> {};
    protected Consumer<ItemFilter> onUpdated = filter -> itemWriter.accept(filter);

    @Getter
    protected int maxStackSize;

    protected SimpleItemFilter() {
        Arrays.fill(matches, ItemStack.EMPTY);
        maxStackSize = 1;
    }

    public static SimpleItemFilter loadFilter(ItemStack itemStack) {
        return loadFilter(itemStack.getOrCreateTag(), filter -> itemStack.setTag(filter.saveFilter()));
    }

    private static SimpleItemFilter loadFilter(CompoundTag tag, Consumer<ItemFilter> itemWriter) {
        var handler = new SimpleItemFilter();
        handler.itemWriter = itemWriter;
        handler.isBlackList = tag.getBoolean("isBlackList");
        handler.ignoreNbt = tag.getBoolean("matchNbt");
        var list = tag.getList("matches", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            handler.matches[i] = ItemStack.of((CompoundTag) list.get(i));
        }
        return handler;
    }

    @Override
    public void setOnUpdated(Consumer<ItemFilter> onUpdated) {
        this.onUpdated = filter -> {
            this.itemWriter.accept(filter);
            onUpdated.accept(filter);
        };
    }

    @Override
    public boolean isBlank() {
        return !isBlackList && !ignoreNbt && Arrays.stream(matches).allMatch(ItemStack::isEmpty);
    }

    public CompoundTag saveFilter() {
        if (isBlank()) {
            return null;
        }
        var tag = new CompoundTag();
        tag.putBoolean("isBlackList", isBlackList);
        tag.putBoolean("matchNbt", ignoreNbt);
        var list = new ListTag();
        for (var match : matches) {
            list.add(match.save(new CompoundTag()));
        }
        tag.put("matches", list);
        return tag;
    }

    public void setBlackList(boolean blackList) {
        isBlackList = blackList;
        onUpdated.accept(this);
    }

    public void setIgnoreNbt(boolean ingoreNbt) {
        this.ignoreNbt = ingoreNbt;
        onUpdated.accept(this);
    }

    public UIElement openConfigurator() {
        var group = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(5));
        var grid = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW));
        for (int i = 0; i < 3; i++) {
            var column = new UIElement();
            for (int j = 0; j < 3; j++) {
                final int index = i * 3 + j;
                var slot = new PhantomItemSlot(() -> matches[index], stack -> {
                    matches[index] = stack;
                    onUpdated.accept(this);
                });
                slot.setMaxStackSize(() -> maxStackSize);
                slot.style(style -> style.backgroundTexture(GuiTextures.SLOT));
                column.addChild(slot);
            }
            grid.addChild(column);
        }
        var buttons = new UIElement();
        buttons.addChildren(
                new ToggleButton(GuiTextures.BUTTON_BLACKLIST, this::isBlackList, this::setBlackList),
                new ToggleButton(GuiTextures.BUTTON_FILTER_NBT, this::isIgnoreNbt, this::setIgnoreNbt));
        group.addChildren(grid, buttons);
        return group;
    }

    @Override
    public boolean test(ItemStack itemStack) {
        return testItemCount(itemStack) > 0;
    }

    @Override
    public int testItemCount(ItemStack itemStack) {
        int totalItemCount = getTotalConfiguredItemCount(itemStack);

        if (isBlackList) {
            return (totalItemCount > 0) ? 0 : Integer.MAX_VALUE;
        }

        return totalItemCount;
    }

    public int getTotalConfiguredItemCount(ItemStack itemStack) {
        int totalCount = 0;

        for (var candidate : matches) {
            if (ignoreNbt && ItemStack.isSameItem(candidate, itemStack)) {
                totalCount += candidate.getCount();
            }
            if (!ignoreNbt && GTUtil.isSameItemSameTags(candidate, itemStack)) {
                totalCount += candidate.getCount();
            }
        }

        return totalCount;
    }

    public void setMaxStackSize(int maxStackSize) {
        this.maxStackSize = maxStackSize;

        for (ItemStack match : matches) {
            match.setCount(Math.min(match.getCount(), maxStackSize));
        }
    }
}
