package com.gregtechceu.gtceu.api.recipe.ui;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.SteamTexture;
import com.gregtechceu.gtceu.api.gui.WidgetUtils;
import com.gregtechceu.gtceu.api.gui.editor.IEditableUI;
import com.gregtechceu.gtceu.api.gui.widget.DualProgressBar;
import com.gregtechceu.gtceu.api.gui.widget.ProgressTextureBar;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.api.recipe.category.GTRecipeCategory;
import com.gregtechceu.gtceu.integration.emi.recipe.GTRecipeEMICategory;
import com.gregtechceu.gtceu.integration.jei.recipe.GTRecipeJEICategory;
import com.gregtechceu.gtceu.integration.rei.recipe.GTRecipeREICategory;
import com.gregtechceu.gtceu.integration.xei.widgets.GTRecipeWidget;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.integration.xei.jei.LDLibJEIPlugin;
import com.lowdragmc.lowdraglib2.math.Size;

import net.minecraft.nbt.CompoundTag;

import com.google.common.collect.Table;
import dev.emi.emi.api.EmiApi;
import dev.vfyjxf.taffy.style.TaffyPosition;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import lombok.Getter;
import lombok.Setter;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("UnusedReturnValue")
public class GTRecipeTypeUI {

    public static final int DEFAULT_MIN_WIDTH = 150;

    public static final DoubleSupplier XEI_PROGRESS = () -> Math.abs(System.currentTimeMillis() % 2000) / 2000.;

    @Getter
    @Setter
    private Byte2ObjectMap<IGuiTexture> slotOverlays = new Byte2ObjectArrayMap<>();

    private final GTRecipeType recipeType;

    @Getter
    private SpriteTexture progressBarTexture = GuiTextures.PROGRESS_BAR_ARROW;
    @Getter
    private FillDirection progressBarDirection = FillDirection.LEFT_TO_RIGHT;
    @Setter
    private SteamTexture steamProgressBarTexture = null;
    @Setter
    private FillDirection steamMoveType = FillDirection.LEFT_TO_RIGHT;
    @Setter
    @Nullable
    protected BiConsumer<GTRecipe, UIElement> uiBuilder;
    @Setter
    @Nullable
    protected Function<GTRecipe, GTRecipeWidget> widgetFactory;
    @Setter
    @Getter
    protected int maxTooltips = 3;
    @Getter
    protected int minWidth = DEFAULT_MIN_WIDTH;

    @Nullable
    private Size xeiSize;
    @Getter
    private int originalWidth;

    public GTRecipeTypeUI(@NotNull GTRecipeType recipeType) {
        this.recipeType = recipeType;
    }

    public boolean hasCustomUI() {
        return CustomRecipeTypeUIs.has(recipeType.registryName);
    }

    public void reloadCustomUI() {
        this.xeiSize = null;
    }

    public Size getJEISize() {
        var size = this.xeiSize;
        if (size == null) {
            var layout = buildLayout(false, false);
            this.originalWidth = layout.width();
            this.xeiSize = size = Size.of(Math.max(originalWidth, minWidth),
                    getPropertyHeightShift() + 5 + layout.height());
        }
        return size;
    }

    public record RecipeHolder(DoubleSupplier progressSupplier,
                               Table<IO, RecipeCapability<?>, Object> storages,
                               CompoundTag data,
                               List<RecipeCondition<?>> conditions,
                               boolean isSteam,
                               boolean isHighPressure) {}

    /**
     * Auto layout UI template for recipes.
     *
     * @param progressSupplier progress. To create a recipe viewer UI, use {@link #XEI_PROGRESS}.
     */
    public UIElement createUITemplate(DoubleSupplier progressSupplier,
                                      Table<IO, RecipeCapability<?>, Object> storages,
                                      CompoundTag data,
                                      List<RecipeCondition<?>> conditions,
                                      boolean isSteam,
                                      boolean isHighPressure) {
        var template = createEditableUITemplate(isSteam, isHighPressure);
        var root = template.createDefault();
        template.setupUI(root,
                new RecipeHolder(progressSupplier, storages, data, conditions, isSteam, isHighPressure));
        return root;
    }

    public UIElement createUITemplate(DoubleSupplier progressSupplier,
                                      Table<IO, RecipeCapability<?>, Object> storages,
                                      CompoundTag data,
                                      List<RecipeCondition<?>> conditions) {
        return createUITemplate(progressSupplier, storages, data, conditions, false, false);
    }

    /**
     * Auto layout UI template for recipes.
     */
    public IEditableUI<UIElement, RecipeHolder> createEditableUITemplate(final boolean isSteam,
                                                                         final boolean isHighPressure) {
        return new IEditableUI.Normal<>(() -> buildLayout(isSteam, isHighPressure).root(), this::bindTemplate);
    }

    protected RecipeUILayout buildLayout(boolean isSteam, boolean isHighPressure) {
        if (!isSteam) {
            var custom = CustomRecipeTypeUIs.create(recipeType.registryName);
            if (custom != null) return custom;
        }

        var inputs = addInventorySlotGroup(false, isSteam, isHighPressure);
        var outputs = addInventorySlotGroup(true, isSteam, isHighPressure);
        var maxWidth = Math.max(inputs.width(), outputs.width());
        var width = 2 * maxWidth + 40;
        var height = Math.max(inputs.height(), outputs.height());

        var root = new UIElement();
        root.layout(layout -> layout.width(width).height(height));
        place(inputs.root(), (maxWidth - inputs.width()) / 2, (height - inputs.height()) / 2, inputs.width(),
                inputs.height());
        place(outputs.root(), maxWidth + 40 + (maxWidth - outputs.width()) / 2, (height - outputs.height()) / 2,
                outputs.width(), outputs.height());
        root.addChildren(inputs.root(), outputs.root());

        var useSteam = isSteam && steamProgressBarTexture != null;
        var sheet = useSteam ? steamProgressBarTexture.get(isHighPressure) : progressBarTexture;
        var direction = useSteam ? steamMoveType : progressBarDirection;
        var progress = ProgressTextureBar.of(sheet, direction, 20, 20);
        progress.setId("progress");
        place(progress, maxWidth + 10, height / 2 - 10, 20, 20);
        root.addChild(progress);
        return new RecipeUILayout(root, width, height);
    }

    private static UIElement place(UIElement element, int x, int y, int width, int height) {
        return element.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(x).top(y).width(width)
                .height(height));
    }

    protected void bindTemplate(UIElement template, RecipeHolder recipeHolder) {
        var isXEI = recipeHolder.progressSupplier == XEI_PROGRESS;

        var progress = new ArrayList<UIElement>();
        WidgetUtils.widgetByIdForEach(template, "^progress$", ProgressBar.class, bar -> {
            bar.bindDataSource(SupplierDataSource.of(() -> (float) recipeHolder.progressSupplier.getAsDouble()));
            progress.add(bar);
        });
        WidgetUtils.widgetByIdForEach(template, "^progress$", DualProgressBar.class, dual -> {
            dual.bindDataSource(SupplierDataSource.of(() -> (float) recipeHolder.progressSupplier.getAsDouble()));
            progress.add(dual);
        });
        if (!isXEI && (GTCEu.Mods.isREILoaded() || GTCEu.Mods.isJEILoaded() || GTCEu.Mods.isEMILoaded())) {
            for (var element : progress) {
                element.addChild(createShowRecipesButton(recipeType));
            }
        }

        for (var capabilityEntry : recipeHolder.storages.rowMap().entrySet()) {
            var io = capabilityEntry.getKey();
            for (var storagesEntry : capabilityEntry.getValue().entrySet()) {
                var cap = storagesEntry.getKey();
                var storage = storagesEntry.getValue();
                var widgetClass = cap.getWidgetClass();
                if (widgetClass != null) {
                    WidgetUtils.widgetByIdForEach(template, "^%s_[0-9]+$".formatted(cap.slotName(io)), widgetClass,
                            widget -> {
                                var index = WidgetUtils.widgetIdIndex(widget);
                                cap.applyWidgetInfo(widget, index, isXEI, io, recipeHolder, recipeType, null, null,
                                        storage, 0, 0);
                            });
                }
            }
        }
    }

    public static UIElement createShowRecipesButton(GTRecipeType recipeType) {
        var button = new Button().noText();
        button.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(0).top(0).widthPercent(100)
                .heightPercent(100));
        button.buttonStyle(style -> style.baseTexture(IGuiTexture.EMPTY).hoverTexture(IGuiTexture.EMPTY)
                .pressedTexture(IGuiTexture.EMPTY));
        button.style(style -> style.tooltips("gtceu.recipe_type.show_recipes"));
        button.setOnClick(event -> openRecipeCategories(recipeType));
        return button;
    }

    public static void openRecipeCategories(GTRecipeType recipeType) {
        if (GTCEu.Mods.isREILoaded()) {
            ViewSearchBuilder.builder().addCategories(
                    recipeType.getCategories().stream()
                            .filter(GTRecipeCategory::isXEIVisible)
                            .map(GTRecipeREICategory::machineCategory)
                            .collect(Collectors.toList()))
                    .open();
        } else if (GTCEu.Mods.isJEILoaded()) {
            if (LDLibJEIPlugin.jeiRuntime == null) return;
            LDLibJEIPlugin.jeiRuntime.getRecipesGui().showTypes(
                    recipeType.getCategories().stream()
                            .filter(GTRecipeCategory::isXEIVisible)
                            .map(GTRecipeJEICategory::machineType)
                            .collect(Collectors.toList()));
        } else if (GTCEu.Mods.isEMILoaded()) {
            EmiApi.displayRecipeCategory(GTRecipeEMICategory.machineCategory(recipeType.getCategory()));
        }
    }

    protected RecipeUILayout addInventorySlotGroup(boolean isOutputs, boolean isSteam, boolean isHighPressure) {
        int maxCount = 0;
        int totalR = 0;
        Object2IntSortedMap<RecipeCapability<?>> map = new Object2IntAVLTreeMap<>(RecipeCapability.COMPARATOR);
        var maxSlots = isOutputs ? recipeType.maxOutputs : recipeType.maxInputs;
        for (var value : maxSlots.object2IntEntrySet()) {
            if (value.getKey().doRenderSlot) {
                int val = value.getIntValue();
                if (val > maxCount) {
                    maxCount = Math.min(val, 3);
                }
                totalR += (val + 2) / 3;
                map.put(value.getKey(), val);
            }
        }
        var width = maxCount * 18 + 8;
        var height = totalR * 18 + 8;
        var group = new UIElement();
        group.layout(layout -> layout.width(width).height(height));
        int index = 0;
        for (var entry : map.object2IntEntrySet()) {
            var cap = entry.getKey();
            if (cap.getWidgetClass() == null) {
                continue;
            }
            int capCount = entry.getIntValue();
            for (int slotIndex = 0; slotIndex < capCount; slotIndex++) {
                var slot = cap.createWidget();
                if (slot == null) continue;
                var background = getOverlaysForSlot(isOutputs, cap, slotIndex == capCount - 1, isSteam,
                        isHighPressure);
                slot.style(style -> style.backgroundTexture(background));
                slot.setId(cap.slotName(isOutputs ? IO.OUT : IO.IN, slotIndex));
                place(slot, (index % 3) * 18 + 4, (index / 3) * 18 + 4, 18, 18);
                group.addChild(slot);
                index++;
            }
            index += (3 - (index % 3)) % 3;
        }
        return new RecipeUILayout(group, width, height);
    }

    protected static int[] determineSlotsGrid(int itemCount) {
        int itemSlotsToLeft;
        int itemSlotsToDown;
        double sqrt = Math.sqrt(itemCount);
        if (sqrt % 1 == 0) {
            itemSlotsToLeft = itemSlotsToDown = (int) sqrt;
        } else if (itemCount == 3) {
            itemSlotsToLeft = 3;
            itemSlotsToDown = 1;
        } else {
            itemSlotsToLeft = (int) Math.ceil(sqrt);
            itemSlotsToDown = itemSlotsToLeft - 1;
            if (itemCount > itemSlotsToLeft * itemSlotsToDown) {
                itemSlotsToDown = itemSlotsToLeft;
            }
        }
        return new int[] { itemSlotsToLeft, itemSlotsToDown };
    }

    public IGuiTexture getOverlaysForSlot(boolean isOutput, RecipeCapability<?> capability, boolean isLast,
                                          boolean isSteam, boolean isHighPressure) {
        IGuiTexture base = capability == FluidRecipeCapability.CAP ? GuiTextures.FLUID_SLOT :
                (isSteam ? GuiTextures.SLOT_STEAM.get(isHighPressure) : GuiTextures.SLOT);
        byte overlayKey = (byte) ((isOutput ? 2 : 0) + (capability == FluidRecipeCapability.CAP ? 1 : 0) +
                (isLast ? 4 : 0));
        if (slotOverlays.containsKey(overlayKey)) {
            return IGuiTexture.group(base, slotOverlays.get(overlayKey));
        }
        return base;
    }

    /**
     * @return the height used to determine size of background texture in JEI
     */
    public int getPropertyHeightShift() {
        int maxPropertyCount = maxTooltips + recipeType.getDataInfos().size() + recipeType.getMinRecipeConditions();
        return maxPropertyCount * 10; // GTRecipeWidget#LINE_HEIGHT
    }

    public GTRecipeWidget createWidget(GTRecipe recipe) {
        return widgetFactory == null ? new GTRecipeWidget(recipe) : widgetFactory.apply(recipe);
    }

    public void appendJEIUI(GTRecipe recipe, UIElement root) {
        if (uiBuilder != null) {
            uiBuilder.accept(recipe, root);
        }
    }

    public GTRecipeTypeUI setSlotOverlay(boolean isOutput, boolean isFluid, IGuiTexture slotOverlay) {
        return this.setSlotOverlay(isOutput, isFluid, false, slotOverlay).setSlotOverlay(isOutput, isFluid, true,
                slotOverlay);
    }

    public GTRecipeTypeUI setSlotOverlay(boolean isOutput, boolean isFluid, boolean isLast, IGuiTexture slotOverlay) {
        this.slotOverlays.put((byte) ((isOutput ? 2 : 0) + (isFluid ? 1 : 0) + (isLast ? 4 : 0)), slotOverlay);
        return this;
    }

    public GTRecipeTypeUI setProgressBar(SpriteTexture progressBar, FillDirection moveType) {
        this.progressBarTexture = progressBar;
        this.progressBarDirection = moveType;
        return this;
    }

    public GTRecipeTypeUI setMinWidth(int minWidth) {
        if (minWidth < DEFAULT_MIN_WIDTH) {
            GTCEu.LOGGER.debug("Min width {} of {} is below the default of {}, using the default", minWidth,
                    recipeType.registryName, DEFAULT_MIN_WIDTH);
        }

        this.minWidth = Math.max(DEFAULT_MIN_WIDTH, minWidth);

        return this;
    }
}
