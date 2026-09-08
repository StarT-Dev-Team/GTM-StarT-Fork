package com.gregtechceu.gtceu.integration.xei.widgets;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.gui.widget.TankWidget;
import com.gregtechceu.gtceu.api.recipe.chance.boost.ChanceBoostFunction;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.data.lang.LangHandler;
import com.gregtechceu.gtceu.integration.xei.entry.fluid.FluidEntryList;
import com.gregtechceu.gtceu.integration.xei.entry.item.ItemEntryList;
import com.gregtechceu.gtceu.integration.xei.handlers.fluid.CycleFluidEntryHandler;
import com.gregtechceu.gtceu.integration.xei.handlers.item.CycleItemEntryHandler;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.jei.IngredientIO;

import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.ints.IntImmutableList;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.regex.Pattern;

public class GTOreByProductWidget extends WidgetGroup {

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
        super(0, 0, 176, 166);

        recipeWrapper = new GTOreByProduct(material);

        setClientSideWidget();
        setRecipe();
    }

    public void setRecipe() {
        this.widgets.clear();

        WidgetGroup group = new WidgetGroup();

        group.setId(ORE_CONTENT_GROUP_ID);

        getWidgetsById(ORE_CONTENT_GROUP_ID_REGEX).forEach(this::removeWidget);

        addWidget(group);

        BooleanList itemOutputExists = new BooleanArrayList();

        // only draw slot on inputs if it is the ore
        addWidget(new ImageWidget(ITEM_INPUT_LOCATIONS.getInt(0), ITEM_INPUT_LOCATIONS.getInt(1), 18, 18,
                GuiTextures.SLOT));
        boolean hasSifter = recipeWrapper.hasSifter();

        addWidget(new ImageWidget(0, 0, 176, 166, GuiTextures.OREBY_BASE));
        if (recipeWrapper.hasDirectSmelt()) {
            addWidget(new ImageWidget(0, 0, 176, 166, GuiTextures.OREBY_SMELT));
        }
        if (recipeWrapper.hasChemBath()) {
            addWidget(new ImageWidget(0, 0, 176, 166, GuiTextures.OREBY_CHEM));
        }
        if (recipeWrapper.hasSeparator()) {
            addWidget(new ImageWidget(0, 0, 176, 166, GuiTextures.OREBY_SEP));
        }
        if (hasSifter) {
            addWidget(new ImageWidget(0, 0, 176, 166, GuiTextures.OREBY_SIFT));
        }

        List<ItemEntryList> itemInputs = recipeWrapper.itemInputs;
        CycleItemEntryHandler itemInputsHandler = new CycleItemEntryHandler(itemInputs);

        for (int i = 0; i < ITEM_INPUT_LOCATIONS.size(); i += 2) {
            final int finalI = i;
            group.addWidget(new SlotWidget(itemInputsHandler, i / 2, ITEM_INPUT_LOCATIONS.getInt(i),
                    ITEM_INPUT_LOCATIONS.getInt(i + 1))
                    .setCanTakeItems(false)
                    .setCanPutItems(false)
                    .setIngredientIO(IngredientIO.INPUT)
                    .setOnAddedTooltips(
                            (slot, tooltips) -> recipeWrapper.getTooltip(finalI / 2, tooltips, MIN_OC_TIER, tier))
                    .setBackground((IGuiTexture) null));
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

            group.addWidget(new SlotWidget(itemOutputsHandler, slotIndex, ITEM_OUTPUT_LOCATIONS.getInt(i),
                    ITEM_OUTPUT_LOCATIONS.getInt(i + 1))
                    .setCanTakeItems(false)
                    .setCanPutItems(false)
                    .setIngredientIO(FINAL_OUTPUT_INDICES.contains(i) ? IngredientIO.OUTPUT : IngredientIO.BOTH)
                    .setXEIChance(xeiChance)
                    .setOnAddedTooltips(
                            (slot, tooltips) -> recipeWrapper.getTooltip(slotIndex + itemInputs.size(), tooltips,
                                    MIN_OC_TIER, tier))
                    .setBackground((IGuiTexture) null).setOverlay(overlay));
            itemOutputExists.add(true);
        }

        List<FluidEntryList> fluidInputs = recipeWrapper.fluidInputs;
        CycleFluidEntryHandler fluidInputsHandler = new CycleFluidEntryHandler(fluidInputs);

        for (int i = 0; i < FLUID_LOCATIONS.size(); i += 2) {
            int slotIndex = i / 2;
            if (!fluidInputs.get(slotIndex).isEmpty()) {
                var tank = new TankWidget(new CustomFluidTank(fluidInputsHandler.getFluidInTank(slotIndex)),
                        FLUID_LOCATIONS.getInt(i), FLUID_LOCATIONS.getInt(i + 1), false, false)
                        .setIngredientIO(IngredientIO.INPUT)
                        .setBackground(GuiTextures.FLUID_SLOT)
                        .setShowAmount(false);
                group.addWidget(tank);
            }
        }

        String tierText = GTValues.VNF[tier];
        LabelWidget voltageTextWidget = new LabelWidget(GTRecipeWidget.getVoltageXOffset(tier, getSize().width),
                getSize().height - 10, tierText).setTextColor(-1).setDropShadow(false);
        group.addWidget(new ButtonWidget(voltageTextWidget.getPositionX(), voltageTextWidget.getPositionY(),
                voltageTextWidget.getSizeWidth(), voltageTextWidget.getSizeHeight(),
                cd -> cycleTier(cd.button))
                .setHoverTooltips(LangHandler.getMultiLang("gtceu.oc.tooltip", GTValues.VNF[MIN_OC_TIER])
                        .toArray(Component[]::new)));
        group.addWidget(voltageTextWidget);

        for (int i = 0; i < ITEM_OUTPUT_LOCATIONS.size(); i += 2) {
            // stupid hack to show all sifter slots if the first one exists
            if (itemOutputExists.getBoolean(i / 2) || (i > 28 * 2 && itemOutputExists.getBoolean(28) && hasSifter)) {
                group.addWidget(this.widgets.size() - 3, new ImageWidget(ITEM_OUTPUT_LOCATIONS.getInt(i),
                        ITEM_OUTPUT_LOCATIONS.getInt(i + 1), 18, 18, GuiTextures.SLOT));
            }
        }
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
