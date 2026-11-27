package com.gregtechceu.gtceu.api.cover.filter;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.gui.widget.EnumSelector;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.lookup.GTRecipeLookup;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.MapIngredientTypeManager;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.utils.ItemStackHashStrategy;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import dev.vfyjxf.taffy.style.AlignItems;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;

import java.util.List;
import java.util.function.Consumer;

public class SmartItemFilter implements ItemFilter {

    protected Consumer<ItemFilter> itemWriter = filter -> {};
    protected Consumer<ItemFilter> onUpdated = filter -> itemWriter.accept(filter);

    private SmartFilteringMode filterMode = SmartFilteringMode.ELECTROLYZER;

    protected SmartItemFilter() {}

    public static SmartItemFilter loadFilter(ItemStack itemStack) {
        return loadFilter(itemStack.getOrCreateTag(), filter -> itemStack.setTag(filter.saveFilter()));
    }

    private static SmartItemFilter loadFilter(CompoundTag tag, Consumer<ItemFilter> itemWriter) {
        var handler = new SmartItemFilter();
        handler.itemWriter = itemWriter;
        handler.filterMode = SmartFilteringMode.VALUES[tag.getInt("filterMode")];
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
        return filterMode.ordinal() == 0;
    }

    @Override
    public CompoundTag saveFilter() {
        if (isBlank()) {
            return null;
        }
        var tag = new CompoundTag();
        tag.putInt("filterMode", filterMode.ordinal());
        return tag;
    }

    private void setFilterMode(SmartFilteringMode filterMode) {
        this.filterMode = filterMode;
        onUpdated.accept(this);
    }

    @Override
    public UIElement openConfigurator() {
        var group = new UIElement().layout(layout -> layout.alignItems(AlignItems.CENTER));
        var selector = new EnumSelector<>(SmartFilteringMode.VALUES, () -> filterMode, this::setFilterMode);
        selector.layout(layout -> layout.width(32).height(32));
        group.addChild(selector);
        return group;
    }

    @Override
    public boolean test(ItemStack itemStack) {
        return testItemCount(itemStack) > 0;
    }

    @Override
    public int testItemCount(ItemStack itemStack) {
        return filterMode.cache.computeIfAbsent(itemStack, this::lookup);
    }

    private int lookup(ItemStack itemStack) {
        ItemStack copy = itemStack.copyWithCount(Integer.MAX_VALUE);
        var ingredients = MapIngredientTypeManager.getFrom(copy, ItemRecipeCapability.CAP);
        var recipe = filterMode.lookup.recurseIngredientTreeFindRecipe(List.of(ingredients),
                filterMode.lookup.getLookup(), r -> true);
        if (recipe == null) return 0;
        for (Content content : recipe.getInputContents(ItemRecipeCapability.CAP)) {
            var stacks = ItemRecipeCapability.CAP.of(content.getContent()).getItems();
            for (var stack : stacks) {
                if (ItemStack.isSameItem(stack, itemStack)) return stack.getCount();
            }
        }
        return 0;
    }

    public void setModeFromMachine(String machineName) {
        for (SmartFilteringMode mode : SmartFilteringMode.VALUES) {
            if (machineName.contains(mode.localeName)) {
                setFilterMode(mode);
                return;
            }
        }
    }

    @MethodsReturnNonnullByDefault
    private enum SmartFilteringMode implements EnumSelector.SelectableEnum {

        ELECTROLYZER("electrolyzer", GTRecipeTypes.ELECTROLYZER_RECIPES),
        CENTRIFUGE("centrifuge", GTRecipeTypes.CENTRIFUGE_RECIPES),
        SIFTER("sifter", GTRecipeTypes.SIFTER_RECIPES);

        private static final SmartFilteringMode[] VALUES = values();
        private final String localeName;
        private final GTRecipeLookup lookup;
        private final Object2IntOpenCustomHashMap<ItemStack> cache = new Object2IntOpenCustomHashMap<>(
                ItemStackHashStrategy.comparingAllButCount());

        SmartFilteringMode(String localeName, GTRecipeType type) {
            this.localeName = localeName;
            this.lookup = type.getLookup();
        }

        @Override
        public String getTooltip() {
            return "cover.item_smart_filter.filtering_mode." + localeName;
        }

        @Override
        public IGuiTexture getIcon() {
            return SpriteTexture.of("gtceu:textures/block/machines/" + localeName + "/overlay_front.png");
        }
    }
}
