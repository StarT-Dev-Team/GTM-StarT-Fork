package com.gregtechceu.gtceu.integration.xei.widgets;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.recipe.chance.boost.ChanceBoostFunction;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.data.lang.LangHandler;
import com.gregtechceu.gtceu.integration.xei.entry.fluid.FluidEntryList;
import com.gregtechceu.gtceu.integration.xei.entry.item.ItemEntryList;
import com.gregtechceu.gtceu.integration.xei.handlers.fluid.CycleFluidEntryHandler;
import com.gregtechceu.gtceu.integration.xei.handlers.item.CycleItemEntryHandler;

import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;

import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import dev.vfyjxf.taffy.style.TaffyPosition;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.ints.IntImmutableList;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class GTOreByProductWidget extends UIElement {

    public static final String ORE_CONTENT_GROUP_ID = "oreContentGroup";
    public static final Pattern ORE_CONTENT_GROUP_ID_REGEX = Pattern.compile("^oreContentGroup$");

    // XY positions of every item and fluid, in three enormous lists
    protected final static IntImmutableList ITEM_INPUT_LOCATIONS = IntImmutableList.of(
            3, 3,       // ore
            23, 3,      // furnace (direct smelt)
            3, 24,      // macerator (ore -> crushed)
            23, 71,     // macerator (crushed -> impure)
            50, 80,     // centrifuge (impure -> dust)
            24, 25,     // ore washer
            97, 71,     // thermal centrifuge
            70, 80,     // macerator (centrifuged -> dust)
            114, 48,    // macerator (crushed purified -> purified)
            133, 71,    // centrifuge (purified -> dust)
            3, 123,     // cauldron / simple washer (crushed)
            41, 145,    // cauldron (impure)
            102, 145,   // cauldron (purified)
            24, 48,     // chem bath
            155, 71,    // electro separator
            101, 25     // sifter
    );

    protected final static IntImmutableList ITEM_OUTPUT_LOCATIONS = IntImmutableList.of(
            46, 3,      // smelt result: 0
            3, 47,      // ore -> crushed: 2
            3, 65,      // byproduct: 4
            23, 92,     // crushed -> impure: 6
            23, 110,    // byproduct: 8
            50, 101,    // impure -> dust: 10
            50, 119,    // byproduct: 12
            64, 25,     // crushed -> crushed purified (wash): 14
            82, 25,     // byproduct: 16
            97, 92,     // crushed/crushed purified -> centrifuged: 18
            97, 110,    // byproduct: 20
            70, 101,    // centrifuged -> dust: 22
            70, 119,    // byproduct: 24
            137, 47,    // crushed purified -> purified: 26
            155, 47,    // byproduct: 28
            133, 92,    // purified -> dust: 30
            133, 110,   // byproduct: 32
            3, 105,     // crushed cauldron: 34
            3, 145,     // -> purified crushed: 36
            23, 145,    // impure cauldron: 38
            63, 145,    // -> dust: 40
            84, 145,    // purified cauldron: 42
            124, 145,   // -> dust: 44
            64, 48,     // crushed -> crushed purified (chem bath): 46
            82, 48,     // byproduct: 48
            155, 92,    // purified -> dust (electro separator): 50
            155, 110,   // byproduct 1: 52
            155, 128,   // byproduct 2: 54
            119, 3,     // sifter outputs... : 56
            137, 3,     // 58
            155, 3,     // 60
            119, 21,    // 62
            137, 21,    // 64
            155, 21     // 66
    );

    protected final static IntImmutableList FLUID_LOCATIONS = IntImmutableList.of(
            42, 25, // washer in
            42, 48  // chem bath in
    );

    protected final static IntSet MACERATOR_BYPRODUCT_SLOTS = IntSet.of(
            2, 4, 12, 14);

    // Used to set intermediates as both input and output
    protected final static IntSet FINAL_OUTPUT_INDICES = IntSet.of(
            0, 4, 8, 10, 12, 16, 20, 22, 24, 28, 30, 32, 40, 44, 48, 50, 52, 54, 56, 58, 60, 62, 64, 66);

    private static final int MIN_OC_TIER = GTValues.LV;

    private final GTOreByProduct recipeWrapper;
    private int tier = MIN_OC_TIER;

    public GTOreByProductWidget(Material material) {
        layout(layout -> layout.width(176).height(166));

        recipeWrapper = new GTOreByProduct(material);

        setRecipe();
    }

    private static UIElement place(UIElement element, int x, int y) {
        element.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(x).top(y));
        return element;
    }

    private static UIElement image(int x, int y, int width, int height, IGuiTexture texture) {
        var image = new UIElement();
        image.layout(layout -> layout.width(width).height(height));
        image.style(style -> style.backgroundTexture(texture));
        return place(image, x, y);
    }

    private static List<Component> tooltipsOf(Consumer<List<Component>> collector) {
        var tooltips = new ArrayList<Component>();
        collector.accept(tooltips);
        return tooltips;
    }

    public void setRecipe() {
        clearAllChildren();

        var group = new UIElement();
        group.setId(ORE_CONTENT_GROUP_ID);
        group.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(0).top(0).width(176).height(166));

        BooleanList itemOutputExists = new BooleanArrayList();

        addChild(image(ITEM_INPUT_LOCATIONS.getInt(0), ITEM_INPUT_LOCATIONS.getInt(1), 18, 18, GuiTextures.SLOT));
        boolean hasSifter = recipeWrapper.hasSifter();

        addChild(image(0, 0, 176, 166, GuiTextures.OREBY_BASE));
        if (recipeWrapper.hasDirectSmelt()) {
            addChild(image(0, 0, 176, 166, GuiTextures.OREBY_SMELT));
        }
        if (recipeWrapper.hasChemBath()) {
            addChild(image(0, 0, 176, 166, GuiTextures.OREBY_CHEM));
        }
        if (recipeWrapper.hasSeparator()) {
            addChild(image(0, 0, 176, 166, GuiTextures.OREBY_SEP));
        }
        if (hasSifter) {
            addChild(image(0, 0, 176, 166, GuiTextures.OREBY_SIFT));
        }
        addChild(group);

        var slots = new ArrayList<UIElement>();

        List<ItemEntryList> itemInputs = recipeWrapper.itemInputs;
        CycleItemEntryHandler itemInputsHandler = new CycleItemEntryHandler(itemInputs);

        for (int i = 0; i < ITEM_INPUT_LOCATIONS.size(); i += 2) {
            final int finalI = i;
            var entry = itemInputs.get(i / 2);
            var slot = new ItemSlot();
            slot.bind(new ItemHandlerSlot(itemInputsHandler, i / 2).setCanTake(player -> false)
                    .setCanPlace(stack -> false));
            slot.xeiRecipeIngredient(IngredientIO.INPUT, () -> stacksOf(entry))
                    .xeiRecipeSlot(IngredientIO.INPUT, 1, 1, () -> stacksOf(entry));
            var tooltips = tooltipsOf(
                    list -> recipeWrapper.getTooltip(finalI / 2, list, MIN_OC_TIER, tier));
            slot.style(style -> style.backgroundTexture(IGuiTexture.EMPTY)
                    .appendTooltips(tooltips.toArray(Component[]::new)));
            slots.add(place(slot, ITEM_INPUT_LOCATIONS.getInt(i), ITEM_INPUT_LOCATIONS.getInt(i + 1)));
        }

        NonNullList<ItemStack> itemOutputs = recipeWrapper.itemOutputs;
        CustomItemStackHandler itemOutputsHandler = new CustomItemStackHandler(itemOutputs);

        for (int i = 0; i < ITEM_OUTPUT_LOCATIONS.size(); i += 2) {
            int slotIndex = i / 2;
            float xeiChance = 1.0f;
            Content chance = recipeWrapper.getChance(i / 2 + itemInputs.size());
            IGuiTexture overlay = null;
            if (chance != null) {
                boolean hideOC = tier < GTValues.HV && MACERATOR_BYPRODUCT_SLOTS.contains(slotIndex);
                int boostedChance = hideOC ? 0 :
                        ChanceBoostFunction.OVERCLOCK.getBoostedChance(chance, MIN_OC_TIER, tier);

                xeiChance = (float) boostedChance / chance.maxChance;

                overlay = chance.createOverlay(false, MIN_OC_TIER, tier, false,
                        hideOC ? (entry, recipeTier, chanceTier) -> boostedChance : ChanceBoostFunction.OVERCLOCK);
            }
            if (itemOutputs.get(slotIndex).isEmpty()) {
                itemOutputExists.add(false);
                continue;
            }

            var io = FINAL_OUTPUT_INDICES.contains(i) ? IngredientIO.OUTPUT : IngredientIO.NONE;
            var slot = new ItemSlot();
            slot.bind(new ItemHandlerSlot(itemOutputsHandler, slotIndex).setCanTake(player -> false)
                    .setCanPlace(stack -> false));
            slot.xeiRecipeIngredient(io).xeiRecipeSlot(io, xeiChance);
            var tooltips = tooltipsOf(list -> recipeWrapper.getTooltip(slotIndex + itemInputs.size(), list,
                    MIN_OC_TIER, tier));
            var slotOverlay = overlay;
            slot.style(style -> {
                style.backgroundTexture(IGuiTexture.EMPTY).appendTooltips(tooltips.toArray(Component[]::new));
                if (slotOverlay != null) style.overlay(slotOverlay);
            });
            slots.add(place(slot, ITEM_OUTPUT_LOCATIONS.getInt(i), ITEM_OUTPUT_LOCATIONS.getInt(i + 1)));
            itemOutputExists.add(true);
        }

        for (int i = 0; i < ITEM_OUTPUT_LOCATIONS.size(); i += 2) {
            if (itemOutputExists.getBoolean(i / 2) || (i > 28 * 2 && itemOutputExists.getBoolean(28) && hasSifter)) {
                group.addChild(image(ITEM_OUTPUT_LOCATIONS.getInt(i), ITEM_OUTPUT_LOCATIONS.getInt(i + 1), 18, 18,
                        GuiTextures.SLOT));
            }
        }
        slots.forEach(group::addChild);

        List<FluidEntryList> fluidInputs = recipeWrapper.fluidInputs;
        CycleFluidEntryHandler fluidInputsHandler = new CycleFluidEntryHandler(fluidInputs);

        for (int i = 0; i < FLUID_LOCATIONS.size(); i += 2) {
            int slotIndex = i / 2;
            if (!fluidInputs.get(slotIndex).isEmpty()) {
                var entry = fluidInputs.get(slotIndex);
                var tank = new FluidSlot();
                tank.bindDataSource(SupplierDataSource.of(() -> fluidInputsHandler.getFluidInTank(slotIndex)));
                tank.xeiRecipeIngredient(IngredientIO.INPUT, () -> entry.getStacks().stream())
                        .xeiRecipeSlot(IngredientIO.INPUT, 1, 1, () -> entry.getStacks().stream());
                tank.slotStyle(style -> style.fillDirection(FillDirection.ALWAYS_FULL));
                tank.style(style -> style.backgroundTexture(GuiTextures.FLUID_SLOT));
                tank.amountLabel.setDisplay(false);
                group.addChild(place(tank, FLUID_LOCATIONS.getInt(i), FLUID_LOCATIONS.getInt(i + 1)));
            }
        }

        String tierText = GTValues.VNF[tier];
        var voltageButton = new Button();
        voltageButton.setText(tierText, false);
        voltageButton.textStyle(style -> style.textColor(-1).textShadow(false).adaptiveWidth(true));
        voltageButton.buttonStyle(style -> style.baseTexture(IGuiTexture.EMPTY).hoverTexture(IGuiTexture.EMPTY)
                .pressedTexture(IGuiTexture.EMPTY));
        var textWidth = Minecraft.getInstance().font.width(tierText);
        voltageButton.layout(layout -> layout.width(textWidth + 4).height(10).paddingAll(0));
        voltageButton.setOnClick(event -> cycleTier(event.button));
        voltageButton.style(style -> style.tooltips(LangHandler.getMultiLang("gtceu.oc.tooltip",
                GTValues.VNF[MIN_OC_TIER]).toArray(Component[]::new)));
        group.addChild(place(voltageButton, GTRecipeWidget.getVoltageXOffset(tier, 176), 166 - 10));
    }

    private static Stream<ItemStack> stacksOf(ItemEntryList entry) {
        return entry == null ? Stream.empty() : entry.getStacks().stream();
    }

    private void cycleTier(int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            tier = Mth.clamp(tier + 1, MIN_OC_TIER, GTValues.MAX);
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            tier = Mth.clamp(tier - 1, MIN_OC_TIER, GTValues.MAX);
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            tier = MIN_OC_TIER;
        }

        setRecipe();
    }
}
