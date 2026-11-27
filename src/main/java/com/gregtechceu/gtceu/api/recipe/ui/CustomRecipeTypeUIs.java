package com.gregtechceu.gtceu.api.recipe.ui;

import com.gregtechceu.gtceu.api.gui.texture.UVSpriteTexture;
import com.gregtechceu.gtceu.api.gui.widget.DualProgressBar;
import com.gregtechceu.gtceu.api.gui.widget.ProgressTextureBar;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;

import net.minecraft.resources.ResourceLocation;

import dev.vfyjxf.taffy.style.TaffyPosition;
import org.jetbrains.annotations.Nullable;

public final class CustomRecipeTypeUIs {

    private CustomRecipeTypeUIs() {}

    public static boolean has(ResourceLocation recipeType) {
        return switch (recipeType.toString()) {
            case "gtceu:assembly_line" -> true;
            case "gtceu:distillation_tower" -> true;
            case "gtceu:forge_hammer" -> true;
            case "gtceu:lathe" -> true;
            case "gtceu:research_station" -> true;
            default -> false;
        };
    }

    @Nullable
    public static RecipeUILayout create(ResourceLocation recipeType) {
        return switch (recipeType.toString()) {
            case "gtceu:assembly_line" -> assemblyLine();
            case "gtceu:distillation_tower" -> distillationTower();
            case "gtceu:forge_hammer" -> forgeHammer();
            case "gtceu:lathe" -> lathe();
            case "gtceu:research_station" -> researchStation();
            default -> null;
        };
    }

    private static UIElement place(UIElement element, int x, int y, int width, int height) {
        return element.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(x).top(y).width(width)
                .height(height));
    }

    private static UIElement group(int x, int y, int width, int height) {
        return place(new UIElement(), x, y, width, height);
    }

    private static UIElement image(int x, int y, int width, int height, IGuiTexture texture) {
        return place(new UIElement(), x, y, width, height).style(style -> style.backgroundTexture(texture));
    }

    private static UIElement itemSlot(String id, int x, int y, @Nullable IGuiTexture background) {
        var slot = new ItemSlot();
        slot.setId(id);
        place(slot, x, y, 18, 18);
        if (background != null) {
            slot.style(style -> style.backgroundTexture(background));
        }
        return slot;
    }

    private static UIElement fluidSlot(String id, int x, int y, @Nullable IGuiTexture background) {
        var slot = new FluidSlot();
        slot.slotStyle(style -> style.fillDirection(FillDirection.ALWAYS_FULL));
        slot.setId(id);
        place(slot, x, y, 18, 18);
        if (background != null) {
            slot.style(style -> style.backgroundTexture(background));
        }
        return slot;
    }

    private static ProgressTextureBar progress(String id, int x, int y, int width, int height, IGuiTexture empty,
                                               IGuiTexture filled, FillDirection direction) {
        var bar = new ProgressTextureBar(empty, filled, direction, width, height);
        bar.setId(id);
        place(bar, x, y, width, height);
        return bar;
    }

    private static UIElement dualProgress(String id, int x, int y, int width, int height, float splitPoint,
                                          ProgressTextureBar first, ProgressTextureBar second) {
        var dual = new DualProgressBar(first, second, splitPoint);
        dual.setId(id);
        return place(dual, x, y, width, height);
    }

    private static RecipeUILayout assemblyLine() {
        var root = new UIElement();
        root.layout(layout -> layout.width(154).height(80));
        var e1 = progress("progress", 76, 5, 53, 69,
                UVSpriteTexture.of("gtceu:textures/gui/progress_bar/progress_bar_assembly_line.png", 0f, 0f, 1f, 0.5f),
                UVSpriteTexture.of("gtceu:textures/gui/progress_bar/progress_bar_assembly_line.png", 0f, 0.5f, 1f,
                        0.5f),
                FillDirection.LEFT_TO_RIGHT);
        root.addChild(e1);
        var e2 = itemSlot("item_in_0", 4, 4, SpriteTexture.of("gtceu:textures/gui/base/slot.png").setBorder(1));
        root.addChild(e2);
        var e3 = itemSlot("item_in_1", 22, 4, SpriteTexture.of("gtceu:textures/gui/base/slot.png").setBorder(1));
        root.addChild(e3);
        var e4 = itemSlot("item_in_2", 40, 4, SpriteTexture.of("gtceu:textures/gui/base/slot.png").setBorder(1));
        root.addChild(e4);
        var e5 = itemSlot("item_in_3", 58, 4, SpriteTexture.of("gtceu:textures/gui/base/slot.png").setBorder(1));
        root.addChild(e5);
        var e6 = itemSlot("item_in_4", 4, 22, SpriteTexture.of("gtceu:textures/gui/base/slot.png").setBorder(1));
        root.addChild(e6);
        var e7 = itemSlot("item_in_5", 22, 22, SpriteTexture.of("gtceu:textures/gui/base/slot.png").setBorder(1));
        root.addChild(e7);
        var e8 = itemSlot("item_in_6", 40, 22, SpriteTexture.of("gtceu:textures/gui/base/slot.png").setBorder(1));
        root.addChild(e8);
        var e9 = itemSlot("item_in_7", 58, 22, SpriteTexture.of("gtceu:textures/gui/base/slot.png").setBorder(1));
        root.addChild(e9);
        var e10 = itemSlot("item_in_8", 4, 40, SpriteTexture.of("gtceu:textures/gui/base/slot.png").setBorder(1));
        root.addChild(e10);
        var e11 = itemSlot("item_in_9", 22, 40, SpriteTexture.of("gtceu:textures/gui/base/slot.png").setBorder(1));
        root.addChild(e11);
        var e12 = itemSlot("item_in_10", 40, 40, SpriteTexture.of("gtceu:textures/gui/base/slot.png").setBorder(1));
        root.addChild(e12);
        var e13 = itemSlot("item_in_11", 58, 40, SpriteTexture.of("gtceu:textures/gui/base/slot.png").setBorder(1));
        root.addChild(e13);
        var e14 = itemSlot("item_in_12", 4, 58, SpriteTexture.of("gtceu:textures/gui/base/slot.png").setBorder(1));
        root.addChild(e14);
        var e15 = itemSlot("item_in_13", 22, 58, SpriteTexture.of("gtceu:textures/gui/base/slot.png").setBorder(1));
        root.addChild(e15);
        var e16 = itemSlot("item_in_14", 40, 58, SpriteTexture.of("gtceu:textures/gui/base/slot.png").setBorder(1));
        root.addChild(e16);
        var e17 = itemSlot("item_in_15", 58, 58, SpriteTexture.of("gtceu:textures/gui/base/slot.png").setBorder(1));
        root.addChild(e17);
        var e18 = fluidSlot("fluid_in_0", 93, 4,
                SpriteTexture.of("gtceu:textures/gui/base/fluid_slot.png").setBorder(1));
        root.addChild(e18);
        var e19 = fluidSlot("fluid_in_1", 93, 22,
                SpriteTexture.of("gtceu:textures/gui/base/fluid_slot.png").setBorder(1));
        root.addChild(e19);
        var e20 = fluidSlot("fluid_in_2", 93, 40,
                SpriteTexture.of("gtceu:textures/gui/base/fluid_slot.png").setBorder(1));
        root.addChild(e20);
        var e21 = fluidSlot("fluid_in_3", 93, 58,
                SpriteTexture.of("gtceu:textures/gui/base/fluid_slot.png").setBorder(1));
        root.addChild(e21);
        var e22 = itemSlot("item_out_0", 130, 4, SpriteTexture.of("gtceu:textures/gui/base/slot.png").setBorder(1));
        root.addChild(e22);
        var e23 = itemSlot("item_in_16", 130, 41,
                IGuiTexture.group(SpriteTexture.of("gtceu:textures/gui/base/slot.png"),
                        SpriteTexture.of("gtceu:textures/gui/overlay/data_orb_overlay.png")));
        root.addChild(e23);
        var e24 = progress("progress", 134, 22, 10, 18,
                UVSpriteTexture.of("gtceu:textures/gui/progress_bar/progress_bar_assembly_line_arrow.png", 0f, 0f, 1f,
                        0.5f),
                UVSpriteTexture.of("gtceu:textures/gui/progress_bar/progress_bar_assembly_line_arrow.png", 0f, 0.5f, 1f,
                        0.5f),
                FillDirection.DOWN_TO_UP);
        root.addChild(e24);
        return new RecipeUILayout(root, 154, 80);
    }

    private static RecipeUILayout distillationTower() {
        var root = new UIElement();
        root.layout(layout -> layout.width(148).height(96));
        var e1 = dualProgress("progress", 27, 8, 90, 82, 0.4000000059604645f,
                progress("", -8, 52, 12, 18,
                        UVSpriteTexture.of("gtceu:textures/gui/progress_bar/progress_bar_distillation_tower_coil.png",
                                0f, 0f, 1f, 0.5f),
                        UVSpriteTexture.of("gtceu:textures/gui/progress_bar/progress_bar_distillation_tower_coil.png",
                                0f, 0.5f, 1f, 0.49900001287460327f),
                        FillDirection.DOWN_TO_UP),
                progress("", 5, 3, 78, 76,
                        UVSpriteTexture.of("gtceu:textures/gui/progress_bar/progress_bar_distillation_tower.png", 0f,
                                0f, 0.9800000190734863f, 0.5f),
                        UVSpriteTexture.of("gtceu:textures/gui/progress_bar/progress_bar_distillation_tower.png", 0f,
                                0.5099999904632568f, 0.9800000190734863f, 0.5f),
                        FillDirection.LEFT_TO_RIGHT));
        root.addChild(e1);
        var e2 = fluidSlot("fluid_in_0", 16, 39,
                IGuiTexture.group(SpriteTexture.of("ldlib:textures/gui/fluid_slot.png").setBorder(1),
                        SpriteTexture.of("gtceu:textures/gui/overlay/beaker_overlay_1.png")));
        root.addChild(e2);
        var e3 = group(-12, -5, 19, 17);
        var e4 = fluidSlot("fluid_out_9", 90, 8,
                IGuiTexture.group(SpriteTexture.of("ldlib:textures/gui/fluid_slot.png").setBorder(1),
                        SpriteTexture.of("gtceu:textures/gui/overlay/beaker_overlay_2.png")));
        e3.addChild(e4);
        var e5 = fluidSlot("fluid_out_10", 108, 8,
                IGuiTexture.group(SpriteTexture.of("ldlib:textures/gui/fluid_slot.png").setBorder(1),
                        SpriteTexture.of("gtceu:textures/gui/overlay/beaker_overlay_3.png")));
        e3.addChild(e5);
        var e6 = fluidSlot("fluid_out_11", 126, 8,
                IGuiTexture.group(SpriteTexture.of("ldlib:textures/gui/fluid_slot.png").setBorder(1),
                        SpriteTexture.of("gtceu:textures/gui/overlay/beaker_overlay_4.png")));
        e3.addChild(e6);
        var e7 = fluidSlot("fluid_out_6", 90, 26,
                IGuiTexture.group(SpriteTexture.of("ldlib:textures/gui/fluid_slot.png").setBorder(1),
                        SpriteTexture.of("gtceu:textures/gui/overlay/beaker_overlay_2.png")));
        e3.addChild(e7);
        var e8 = fluidSlot("fluid_out_7", 108, 26,
                IGuiTexture.group(SpriteTexture.of("ldlib:textures/gui/fluid_slot.png").setBorder(1),
                        SpriteTexture.of("gtceu:textures/gui/overlay/beaker_overlay_3.png")));
        e3.addChild(e8);
        var e9 = fluidSlot("fluid_out_8", 126, 26,
                IGuiTexture.group(SpriteTexture.of("ldlib:textures/gui/fluid_slot.png").setBorder(1),
                        SpriteTexture.of("gtceu:textures/gui/overlay/beaker_overlay_4.png")));
        e3.addChild(e9);
        var e10 = fluidSlot("fluid_out_3", 90, 44,
                IGuiTexture.group(SpriteTexture.of("ldlib:textures/gui/fluid_slot.png").setBorder(1),
                        SpriteTexture.of("gtceu:textures/gui/overlay/beaker_overlay_2.png")));
        e3.addChild(e10);
        var e11 = fluidSlot("fluid_out_4", 108, 44,
                IGuiTexture.group(SpriteTexture.of("ldlib:textures/gui/fluid_slot.png").setBorder(1),
                        SpriteTexture.of("gtceu:textures/gui/overlay/beaker_overlay_3.png")));
        e3.addChild(e11);
        var e12 = fluidSlot("fluid_out_5", 126, 44,
                IGuiTexture.group(SpriteTexture.of("ldlib:textures/gui/fluid_slot.png").setBorder(1),
                        SpriteTexture.of("gtceu:textures/gui/overlay/beaker_overlay_4.png")));
        e3.addChild(e12);
        var e13 = fluidSlot("fluid_out_0", 90, 62,
                IGuiTexture.group(SpriteTexture.of("ldlib:textures/gui/fluid_slot.png").setBorder(1),
                        SpriteTexture.of("gtceu:textures/gui/overlay/beaker_overlay_2.png")));
        e3.addChild(e13);
        var e14 = fluidSlot("fluid_out_1", 108, 62,
                IGuiTexture.group(SpriteTexture.of("ldlib:textures/gui/fluid_slot.png").setBorder(1),
                        SpriteTexture.of("gtceu:textures/gui/overlay/beaker_overlay_3.png")));
        e3.addChild(e14);
        var e15 = fluidSlot("fluid_out_2", 126, 62,
                IGuiTexture.group(SpriteTexture.of("ldlib:textures/gui/fluid_slot.png").setBorder(1),
                        SpriteTexture.of("gtceu:textures/gui/overlay/beaker_overlay_4.png")));
        e3.addChild(e15);
        var e16 = itemSlot("item_out_0", 90, 80,
                IGuiTexture.group(SpriteTexture.of("ldlib:textures/gui/slot.png").setBorder(1),
                        SpriteTexture.of("gtceu:textures/gui/overlay/dust_overlay.png").setColor(0xB2FFFFFF)));
        e3.addChild(e16);
        root.addChild(e3);
        var e17 = dualProgress("progress", -9, 13, 15, 34, 0.4000000059604645f,
                progress("", 0, 0, 16, 16, IGuiTexture.EMPTY, IGuiTexture.EMPTY, FillDirection.LEFT_TO_RIGHT),
                progress("", 30, -9, 7, 32,
                        UVSpriteTexture.of(
                                "gtceu:textures/gui/progress_bar/progress_bar_distillation_tower_bubbles.png", 0f, 0f,
                                1f, 0.5f),
                        UVSpriteTexture.of(
                                "gtceu:textures/gui/progress_bar/progress_bar_distillation_tower_bubbles.png", 0f, 0.5f,
                                1f, 0.5f),
                        FillDirection.DOWN_TO_UP));
        root.addChild(e17);
        return new RecipeUILayout(root, 148, 96);
    }

    private static RecipeUILayout forgeHammer() {
        var root = new UIElement();
        root.layout(layout -> layout.width(92).height(26));
        var e1 = group(0, 0, 26, 26);
        var e2 = itemSlot("item_in_0", 4, 4,
                IGuiTexture.group(SpriteTexture.of("gtceu:textures/gui/base/slot.png").setBorder(1),
                        SpriteTexture.of("gtceu:textures/gui/overlay/hammer_overlay.png")));
        e1.addChild(e2);
        root.addChild(e1);
        var e3 = group(66, 0, 26, 26);
        var e4 = itemSlot("item_out_0", 4, 4, SpriteTexture.of("gtceu:textures/gui/base/slot.png").setBorder(1));
        e3.addChild(e4);
        root.addChild(e3);
        var e5 = progress("progress", 36, 3, 20, 20,
                UVSpriteTexture.of("gtceu:textures/gui/progress_bar/progress_bar_hammer.png", 0f, 0f, 1f, 0.5f),
                UVSpriteTexture.of("gtceu:textures/gui/progress_bar/progress_bar_hammer.png", 0f, 0.5f, 1f, 0.5f),
                FillDirection.UP_TO_DOWN);
        root.addChild(e5);
        var e6 = image(35, 20, 21, 6, SpriteTexture.of("gtceu:textures/gui/progress_bar/progress_bar_hammer_base.png"));
        root.addChild(e6);
        return new RecipeUILayout(root, 92, 26);
    }

    private static RecipeUILayout lathe() {
        var root = new UIElement();
        root.layout(layout -> layout.width(110).height(26));
        var e1 = group(0, 0, 26, 26);
        var e2 = itemSlot("item_in_0", 4, 4,
                IGuiTexture.group(SpriteTexture.of("gtceu:textures/gui/base/slot.png").setBorder(1),
                        SpriteTexture.of("gtceu:textures/gui/overlay/pipe_overlay_1.png")));
        e1.addChild(e2);
        root.addChild(e1);
        var e3 = group(66, 0, 44, 26);
        var e4 = itemSlot("item_out_0", 4, 4,
                IGuiTexture.group(SpriteTexture.of("gtceu:textures/gui/base/slot.png").setBorder(1),
                        SpriteTexture.of("gtceu:textures/gui/overlay/pipe_overlay_2.png")));
        e3.addChild(e4);
        var e5 = itemSlot("item_out_1", 22, 4,
                IGuiTexture.group(SpriteTexture.of("gtceu:textures/gui/base/slot.png").setBorder(1),
                        SpriteTexture.of("gtceu:textures/gui/overlay/dust_overlay.png")));
        e3.addChild(e5);
        root.addChild(e3);
        var e6 = progress("progress", 36, 3, 20, 20,
                UVSpriteTexture.of("gtceu:textures/gui/progress_bar/progress_bar_lathe.png", 0f, 0f, 1f, 0.5f),
                UVSpriteTexture.of("gtceu:textures/gui/progress_bar/progress_bar_lathe.png", 0f, 0.5f, 1f, 0.5f),
                FillDirection.LEFT_TO_RIGHT);
        root.addChild(e6);
        var e7 = image(54, 4, 5, 18, SpriteTexture.of("gtceu:textures/gui/progress_bar/progress_bar_lathe_base.png"));
        root.addChild(e7);
        return new RecipeUILayout(root, 110, 26);
    }

    private static RecipeUILayout researchStation() {
        var root = new UIElement();
        root.layout(layout -> layout.width(136).height(72));
        var e1 = dualProgress("progress", 62, 28, 56, 19, 0.75f,
                progress("", 0, 0, 54, 5,
                        UVSpriteTexture.of("gtceu:textures/gui/progress_bar/progress_bar_research_station_1.png", 0f,
                                0f, 1f, 0.5f),
                        UVSpriteTexture.of("gtceu:textures/gui/progress_bar/progress_bar_research_station_1.png", 0f,
                                0.5f, 1f, 0.5f),
                        FillDirection.LEFT_TO_RIGHT),
                progress("", 47, 3, 10, 16,
                        UVSpriteTexture.of("gtceu:textures/gui/progress_bar/progress_bar_research_station_2.png", 0f,
                                0f, 1f, 0.5f),
                        UVSpriteTexture.of("gtceu:textures/gui/progress_bar/progress_bar_research_station_2.png", 0f,
                                0.5f, 1f, 0.5f),
                        FillDirection.UP_TO_DOWN));
        root.addChild(e1);
        var e2 = group(0, 0, 105, 60);
        var e3 = image(0, 0, 84, 60,
                SpriteTexture.of("gtceu:textures/gui/progress_bar/progress_bar_research_station_base.png"));
        e2.addChild(e3);
        var e4 = itemSlot("item_in_0", 87, 21, IGuiTexture.group(SpriteTexture.of("gtceu:textures/gui/base/slot.png"),
                SpriteTexture.of("gtceu:textures/gui/overlay/research_station_overlay.png")));
        e2.addChild(e4);
        var e5 = itemSlot("item_in_1", 33, 21, IGuiTexture.group(SpriteTexture.of("gtceu:textures/gui/base/slot.png"),
                SpriteTexture.of("gtceu:textures/gui/overlay/scanner_overlay.png")));
        e2.addChild(e5);
        root.addChild(e2);
        var e6 = group(105, 50, 18, 18);
        var e7 = itemSlot("item_out_0", 0, 0, IGuiTexture.group(SpriteTexture.of("gtceu:textures/gui/base/slot.png"),
                SpriteTexture.of("gtceu:textures/gui/overlay/data_orb_overlay.png")));
        e6.addChild(e7);
        root.addChild(e6);
        return new RecipeUILayout(root, 136, 72);
    }
}
