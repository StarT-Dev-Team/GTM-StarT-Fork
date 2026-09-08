package com.gregtechceu.gtceu.integration.xei.widgets;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.CWURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.WidgetUtils;
import com.gregtechceu.gtceu.api.gui.widget.PredicatedButtonWidget;
import com.gregtechceu.gtceu.api.recipe.*;
import com.gregtechceu.gtceu.api.recipe.chance.boost.ChanceBoostFunction;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.EnergyStack;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.FusionReactorMachine;
import com.gregtechceu.gtceu.data.lang.LangHandler;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.fml.loading.FMLLoader;

import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import lombok.Getter;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.regex.Pattern;

import static com.gregtechceu.gtceu.api.GTValues.*;

public class GTRecipeWidget extends WidgetGroup {

    public static final String RECIPE_CONTENT_GROUP_ID = "recipeContentGroup";
    public static final Pattern RECIPE_CONTENT_GROUP_ID_REGEX = Pattern.compile("^recipeContentGroup$");

    public static final int LINE_HEIGHT = 10;

    @Getter
    private final int xOffset;
    private final GTRecipe recipe;
    private LabelWidget recipeDurationText = null;
    private LabelWidget recipeTotalEUText = null;
    private LabelWidget recipeVoltageText = null;
    @Getter
    private final int minTier;
    @Getter
    private int tier;
    private int yOffset;
    @Getter
    private OverclockingLogic ocLogic = OverclockingLogic.NON_PERFECT_OVERCLOCK;
    private LabelWidget voltageTextWidget;

    public GTRecipeWidget(GTRecipe recipe) {
        super(getXOffset(recipe), 0, recipe.recipeType.getRecipeUI().getJEISize().width,
                recipe.recipeType.getRecipeUI().getJEISize().height);
        this.recipe = recipe;
        this.xOffset = getXOffset(recipe);
        this.minTier = RecipeHelper.getRecipeEUtTier(recipe);
        setRecipeWidget();
        setTierToMin();
        initializeRecipeTextWidget();
        addButtons();
    }

    private static int getXOffset(GTRecipe recipe) {
        if (recipe.recipeType.getRecipeUI().getOriginalWidth() != recipe.recipeType.getRecipeUI().getJEISize().width) {
            return (recipe.recipeType.getRecipeUI().getJEISize().width -
                    recipe.recipeType.getRecipeUI().getOriginalWidth()) / 2;
        }
        return 0;
    }

    @SuppressWarnings("UnstableApiUsage")
    private void setRecipeWidget() {
        setClientSideWidget();

        var storages = Tables.newCustomTable(new EnumMap<>(IO.class), LinkedHashMap<RecipeCapability<?>, Object>::new);
        var contents = Tables.newCustomTable(new EnumMap<>(IO.class),
                LinkedHashMap<RecipeCapability<?>, List<Content>>::new);
        collectStorage(storages, contents, recipe);

        WidgetGroup group = recipe.recipeType.getRecipeUI().createUITemplate(ProgressWidget.JEIProgress, storages,
                recipe.data.copy(), recipe.conditions);
        addSlots(contents, group, recipe);

        var size = group.getSize();

        // Ensure any previous instances of the widget are removed first. This applies when changing the recipe
        // preview's voltage tier, as this recipe widget stays the same while its contents are updated.
        group.setId(RECIPE_CONTENT_GROUP_ID);
        getWidgetsById(RECIPE_CONTENT_GROUP_ID_REGEX).forEach(this::removeWidget);

        addWidget(group);

        EnergyStack EUt = RecipeHelper.getRealEUt(recipe);
        int yOffset = 5 + size.height;
        this.yOffset = yOffset;
        yOffset += !EUt.isEmpty() ? 21 : 0;
        if (recipe.data.getBoolean("duration_is_total_cwu")) {
            yOffset -= 10;
        }

        /// add text based on i/o's
        MutableInt yOff = new MutableInt(yOffset);
        for (var capability : recipe.inputs.entrySet()) {
            capability.getKey().addXEIInfo(group, xOffset, recipe, capability.getValue(), false, true, yOff, minTier,
                    tier);
        }
        for (var capability : recipe.tickInputs.entrySet()) {
            capability.getKey().addXEIInfo(group, xOffset, recipe, capability.getValue(), true, true, yOff, minTier,
                    tier);
        }
        for (var capability : recipe.outputs.entrySet()) {
            capability.getKey().addXEIInfo(group, xOffset, recipe, capability.getValue(), false, false, yOff, minTier,
                    tier);
        }
        for (var capability : recipe.tickOutputs.entrySet()) {
            capability.getKey().addXEIInfo(group, xOffset, recipe, capability.getValue(), true, false, yOff, minTier,
                    tier);
        }

        for (RecipeCondition<?> condition : recipe.conditions) {
            if (condition.getTooltips() != null && condition.isXeiVisible()) {
                addWidget(new LabelWidget(3 - xOffset, yOffset += LINE_HEIGHT, condition.getTooltips().getString()));
            }

            var widget = condition.createCustomXeiWidget(this, recipe);
            if (widget != null) addWidget(widget);
        }

        var dataInfoConfiguration = new GTRecipeType.CustomDataInfoConfiguration(recipe);
        for (var dataInfoBuilder : recipe.recipeType.getDataInfos()) {
            var dataInfo = dataInfoBuilder.apply(dataInfoConfiguration);
            var widget = new LabelWidget(3 - xOffset, yOffset += LINE_HEIGHT, dataInfo.label());
            if (dataInfo.consumer() != null) dataInfo.consumer().accept(widget, this);
            addWidget(widget);
        }

        recipe.recipeType.getRecipeUI().appendJEIUI(recipe, this);
    }

    private void initializeRecipeTextWidget() {
        String tierText = GTValues.VNF[tier];
        int textsY = yOffset - 10;
        int duration = recipe.duration;
        var EUt = RecipeHelper.getRealEUtWithIO(recipe);
        var minVoltageTier = GTUtil.getTierByVoltage(EUt.voltage());
        float minAmperage = (float) EUt.getTotalEU() / GTValues.V[minVoltageTier];

        if (!recipe.data.getBoolean("hide_duration")) {
            textsY += 10;

            recipeDurationText = new LabelWidget(3 - xOffset, textsY,
                    Component.translatable(LayeredRecipeHelper.hasLayeredSteps(recipe) ? "gtceu.recipe.total_duration" :
                            "gtceu.recipe.duration", FormattingUtil.formatNumbers(duration / 20f))
                            .withStyle(ChatFormatting.UNDERLINE))
                    .setTextColor(-1)
                    .setDropShadow(true);
            recipeDurationText.setHoverTooltips(getRecipeDurationTexts(duration));

            if (recipeDurationText != null) addWidget(recipeDurationText);
        }

        if (EUt.voltage() > 0) {
            long euTotal = EUt.getTotalEU();
            long euTotalDuration = EUt.getTotalEU() * duration;

            textsY += 10;

            if (recipe.data.getBoolean("duration_is_total_cwu") &&
                    recipe.tickInputs.containsKey(CWURecipeCapability.CAP)) {
                int minimumCWUt = Math.max(recipe.tickInputs.get(CWURecipeCapability.CAP).stream()
                        .map(Content::getContent).mapToInt(CWURecipeCapability.CAP::of).sum(), 1);

                recipeTotalEUText = new LabelWidget(3 - xOffset, textsY,
                        Component.translatable("gtceu.recipe.max_eu",
                                FormattingUtil.formatNumberReadable(euTotalDuration / minimumCWUt))
                                .withStyle(ChatFormatting.UNDERLINE))
                        .setTextColor(-1)
                        .setDropShadow(true);
                recipeTotalEUText.setHoverTooltips(Component.translatable("gtceu.recipe.eu.pure",
                        FormattingUtil.formatNumbers(euTotalDuration / minimumCWUt)));

                if (recipeTotalEUText != null) {
                    addWidget(recipeTotalEUText);

                    textsY += 10;
                }
            } else {
                recipeTotalEUText = new LabelWidget(3 - xOffset, textsY,
                        Component.translatable("gtceu.recipe.total",
                                FormattingUtil.formatNumberReadable(euTotalDuration))
                                .withStyle(ChatFormatting.UNDERLINE))
                        .setTextColor(-1)
                        .setDropShadow(true);
                recipeTotalEUText.setHoverTooltips(Component.translatable("gtceu.recipe.eu.pure",
                        FormattingUtil.formatNumbers(euTotalDuration)));

                if (recipeTotalEUText != null) {
                    addWidget(recipeTotalEUText);

                    textsY += 10;
                }
            }

            Component text = Component.translatable(EUt.isInput() ? "gtceu.recipe.eu" : "gtceu.recipe.eu_inverted",
                    FormattingUtil.formatNumberReadable(euTotal))
                    .withStyle(ChatFormatting.UNDERLINE);

            List<Component> texts = new ArrayList<>();

            if (euTotal > 1000) texts.add(Component.translatable("gtceu.recipe.eu.total",
                    FormattingUtil.formatNumbers(euTotal)));

            texts.add(Component.translatable("gtceu.recipe.eu.amp_notation",
                    FormattingUtil.formatNumber2Places(minAmperage), GTValues.VNF[minVoltageTier]));

            recipeVoltageText = new LabelWidget(3 - xOffset, textsY, text)
                    .setTextColor(-1)
                    .setDropShadow(true);
            recipeVoltageText.setHoverTooltips(texts);

            if (recipeVoltageText != null) addWidget(recipeVoltageText);
        }

        if (EUt.isInput()) {
            LabelWidget voltageTextWidget = new LabelWidget(getVoltageXOffset(tier, getSize().width) - xOffset,
                    getSize().height - 10,
                    tierText).setTextColor(-1).setDropShadow(false);
            if (recipe.recipeType.isOffsetVoltageText()) {
                voltageTextWidget.setSelfPositionY(getSize().height - recipe.recipeType.getVoltageTextOffset());
            }
            // make it clickable
            // voltageTextWidget.setBackground(new GuiTextureGroup(GuiTextures.BUTTON));
            addWidget(new ButtonWidget(voltageTextWidget.getPositionX(), voltageTextWidget.getPositionY(),
                    voltageTextWidget.getSizeWidth(), voltageTextWidget.getSizeHeight(),
                    cd -> setRecipeOC(cd.button, cd.isShiftClick))
                    .setHoverTooltips(LangHandler.getMultiLang("gtceu.oc.tooltip", GTValues.VNF[minTier])
                            .toArray(Component[]::new)));
            addWidget(this.voltageTextWidget = voltageTextWidget);
        }
    }

    @NotNull
    private static List<Component> getRecipeDurationTexts(int duration) {
        int hours = duration / 72000;
        int minutes = (duration % 72000) / 1200;
        float seconds = (duration % 1200) / 20f;

        List<Component> texts = new ArrayList<>();

        if (hours > 0)
            texts.add(Component.translatable("gtceu.recipe.duration.hour" + (hours > 1 ? "s" : ""), hours));
        if (minutes > 0)
            texts.add(Component.translatable("gtceu.recipe.duration.minute" + (minutes > 1 ? "s" : ""), minutes));
        if (seconds > 0)
            texts.add(Component.translatable("gtceu.recipe.duration.second" + (seconds > 1 ? "s" : ""),
                    FormattingUtil.formatNumbers(seconds)));

        return texts;
    }

    private void addButtons() {
        // add a recipe id getter, btw all the things can only click within the WidgetGroup while using EMI
        int x = getSize().width - xOffset - 18;
        int y = getSize().height - 30;
        addWidget(
                new PredicatedButtonWidget(x, y, 15, 15, new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("ID")),
                        cd -> Minecraft.getInstance().keyboardHandler.setClipboard(recipe.id.toString()),
                        () -> !FMLLoader.isProduction(), !FMLLoader.isProduction())
                        .setHoverTooltips("click to copy: " + recipe.id));
    }

    public static int getVoltageXOffset(int tier, int width) {
        int x = width - switch (tier) {
            case ULV, LuV, ZPM, UHV, UEV, UXV -> 20;
            case OpV, MAX -> 22;
            case UIV -> 18;
            case IV -> 12;
            default -> 14;
        };
        if (!GTCEu.Mods.isEMILoaded()) {
            x -= 3;
        }
        return x;
    }

    public void setRecipeOC(int button, boolean isShiftClick) {
        ocLogic = OverclockingLogic.NON_PERFECT_OVERCLOCK;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            setTier(tier + 1);
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            setTier(tier - 1);
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            setTierToMin();
        }
        if (isShiftClick) {
            ocLogic = OverclockingLogic.PERFECT_OVERCLOCK;
        }
        if (recipe.recipeType == GTRecipeTypes.FUSION_RECIPES) {
            ocLogic = FusionReactorMachine.FUSION_OC;
        }
        setRecipeOverclockWidget(ocLogic);
        setRecipeWidget();
    }

    private void setRecipeOverclockWidget(OverclockingLogic logic) {
        EnergyStack inputEUt = recipe.getInputEUt();
        String tierText = GTValues.VNF[tier];
        int duration = recipe.duration;

        if (tier > minTier && !inputEUt.isEmpty()) {
            int ocs = tier - minTier;
            if (minTier == ULV) ocs--;
            var params = new OverclockingLogic.OCParams(inputEUt.voltage(), recipe.duration, ocs, ocs, 1);
            var result = logic.runOverclockingLogic(params, V[tier]);
            duration = (int) (duration * result.durationMultiplier());
            inputEUt = inputEUt.multiplyVoltage(result.eutMultiplier());
            tierText = tierText.formatted(ChatFormatting.ITALIC);
        }

        voltageTextWidget.setText(tierText);
        voltageTextWidget.setSelfPositionX(getVoltageXOffset(tier, getSize().width) - xOffset);

        var minVoltageTier = GTUtil.getTierByVoltage(inputEUt.voltage());
        long euTotal = inputEUt.getTotalEU();
        float minAmperage = (float) euTotal / GTValues.V[minVoltageTier];

        if (recipeDurationText != null) {
            recipeDurationText.setComponent(
                    Component.translatable(
                            LayeredRecipeHelper.hasLayeredSteps(recipe) ?
                                    "gtceu.recipe.total_duration" : "gtceu.recipe.duration",
                            FormattingUtil.formatNumbers(duration / 20f)).withStyle(ChatFormatting.UNDERLINE));
            recipeDurationText.setHoverTooltips(getRecipeDurationTexts(duration));
        }

        if (recipeTotalEUText != null) {
            long euTotalDuration = euTotal * duration;

            if (recipe.data.getBoolean("duration_is_total_cwu") &&
                    recipe.tickInputs.containsKey(CWURecipeCapability.CAP)) {
                int minimumCWUt = Math.max(recipe.tickInputs.get(CWURecipeCapability.CAP).stream()
                        .map(Content::getContent).mapToInt(CWURecipeCapability.CAP::of).sum(), 1);

                recipeTotalEUText.setComponent(
                        Component.translatable("gtceu.recipe.max_eu",
                                FormattingUtil.formatNumberReadable(euTotalDuration / minimumCWUt))
                                .withStyle(ChatFormatting.UNDERLINE));
                recipeTotalEUText.setHoverTooltips(Component.translatable("gtceu.recipe.eu.pure",
                        FormattingUtil.formatNumbers(euTotalDuration / minimumCWUt)));
            } else {
                recipeTotalEUText.setComponent(
                        Component.translatable("gtceu.recipe.total",
                                FormattingUtil.formatNumberReadable(euTotalDuration))
                                .withStyle(ChatFormatting.UNDERLINE));
                recipeTotalEUText.setHoverTooltips(Component.translatable("gtceu.recipe.eu.pure",
                        FormattingUtil.formatNumbers(euTotalDuration)));
            }
        }

        if (recipeVoltageText != null) {
            List<Component> texts = new ArrayList<>();

            if (euTotal > 1000) texts.add(Component.translatable("gtceu.recipe.eu.total",
                    FormattingUtil.formatNumbers(euTotal)));

            texts.add(Component.translatable("gtceu.recipe.eu.amp_notation",
                    FormattingUtil.formatNumber2Places(minAmperage), GTValues.VNF[minVoltageTier]));

            recipeVoltageText.setComponent(
                    Component.translatable("gtceu.recipe.eu", FormattingUtil.formatNumberReadable(euTotal))
                            .withStyle(ChatFormatting.UNDERLINE));
            recipeVoltageText.setHoverTooltips(texts);
        }
        detectAndSendChanges();
        updateScreen();
    }

    public static void setConsumedChance(Content content, ChanceLogic logic, List<Component> tooltips, int recipeTier,
                                         int chanceTier, ChanceBoostFunction function) {
        setConsumedChance(true, content, logic, tooltips, recipeTier, chanceTier, function);
    }

    public static void setConsumedChance(boolean isInput, Content content, ChanceLogic logic, List<Component> tooltips,
                                         int recipeTier,
                                         int chanceTier, ChanceBoostFunction function) {
        if (content.chance < ChanceLogic.getMaxChancedValue()) {
            int boostedChance = function.getBoostedChance(content, recipeTier, chanceTier);
            if (boostedChance == 0) {
                tooltips.add(Component
                        .translatable(isInput ? "gtceu.gui.content.chance_nc" : "gtceu.gui.content.chance_np"));
            } else {
                float baseChanceFloat = 100f * content.chance / content.maxChance;
                if (content.tierChanceBoost != 0) {
                    float boostedChanceFloat = 100f * boostedChance / content.maxChance;

                    if (logic != ChanceLogic.NONE && logic != ChanceLogic.OR) {
                        tooltips.add(Component.translatable("gtceu.gui.content.chance_base_logic",
                                FormattingUtil.formatNumber2Places(baseChanceFloat), logic.getTranslation())
                                .withStyle(ChatFormatting.YELLOW));
                    } else {
                        tooltips.add(
                                FormattingUtil.formatPercentage2Places("gtceu.gui.content.chance_base",
                                        baseChanceFloat));
                    }

                    String key = "gtceu.gui.content.chance_tier_boost_" +
                            ((content.tierChanceBoost > 0) ? "plus" : "minus");
                    tooltips.add(FormattingUtil.formatPercentage2Places(key,
                            Math.abs(100f * content.tierChanceBoost / content.maxChance)));

                    if (logic != ChanceLogic.NONE && logic != ChanceLogic.OR) {
                        tooltips.add(Component.translatable("gtceu.gui.content.chance_boosted_logic",
                                FormattingUtil.formatNumber2Places(boostedChanceFloat), logic.getTranslation())
                                .withStyle(ChatFormatting.YELLOW));
                    } else {
                        tooltips.add(
                                FormattingUtil.formatPercentage2Places("gtceu.gui.content.chance_boosted",
                                        boostedChanceFloat));
                    }
                } else {
                    if (logic != ChanceLogic.NONE && logic != ChanceLogic.OR) {
                        tooltips.add(Component.translatable("gtceu.gui.content.chance_no_boost_logic",
                                FormattingUtil.formatNumber2Places(baseChanceFloat), logic.getTranslation())
                                .withStyle(ChatFormatting.YELLOW));
                    } else {
                        tooltips.add(
                                FormattingUtil.formatPercentage2Places("gtceu.gui.content.chance_no_boost",
                                        baseChanceFloat));
                    }
                }
            }
        }
    }

    private void setTier(int tier) {
        this.tier = Mth.clamp(tier, minTier, GTValues.MAX);
    }

    private void setTierToMin() {
        setTier(minTier);
    }

    public void collectStorage(Table<IO, RecipeCapability<?>, Object> extraTable,
                               Table<IO, RecipeCapability<?>, List<Content>> extraContents, GTRecipe recipe) {
        for (var entry : recipe.inputs.entrySet()) {
            RecipeCapability<?> cap = entry.getKey();
            List<Content> contents = entry.getValue();

            extraContents.put(IO.IN, cap, contents);
        }
        for (var entry : recipe.tickInputs.entrySet()) {
            RecipeCapability<?> cap = entry.getKey();
            List<Content> contents = entry.getValue();

            if (extraContents.get(IO.IN, cap) == null) {
                extraContents.put(IO.IN, cap, contents);
            } else {
                ArrayList<Content> fullContents = new ArrayList<>(extraContents.get(IO.IN, cap));
                fullContents.addAll(contents);
                extraContents.put(IO.IN, cap, fullContents);
            }
        }
        if (extraContents.containsRow(IO.IN)) {
            Map<RecipeCapability<?>, List<Object>> inputCapabilities = new Object2ObjectLinkedOpenHashMap<>();
            for (var entry : extraContents.row(IO.IN).entrySet()) {
                RecipeCapability<?> cap = entry.getKey();
                inputCapabilities.put(cap, cap.createXEIContainerContents(entry.getValue(), recipe, IO.IN));
            }

            for (var entry : inputCapabilities.entrySet()) {
                while (entry.getValue().size() < recipe.recipeType.getMaxInputs(entry.getKey()))
                    entry.getValue().add(null);
                var container = entry.getKey().createXEIContainer(entry.getValue());
                if (container != null) {
                    extraTable.put(IO.IN, entry.getKey(), container);
                }
            }
        }

        for (var entry : recipe.outputs.entrySet()) {
            RecipeCapability<?> cap = entry.getKey();
            List<Content> contents = entry.getValue();

            extraContents.put(IO.OUT, cap, contents);
        }
        for (var entry : recipe.tickOutputs.entrySet()) {
            RecipeCapability<?> cap = entry.getKey();
            List<Content> contents = entry.getValue();

            if (extraContents.get(IO.OUT, cap) == null) {
                extraContents.put(IO.OUT, cap, contents);
            } else {
                ArrayList<Content> fullContents = new ArrayList<>(extraContents.get(IO.IN, cap));
                fullContents.addAll(contents);
                extraContents.put(IO.OUT, cap, fullContents);
            }
        }
        if (extraContents.containsRow(IO.OUT)) {
            Map<RecipeCapability<?>, List<Object>> outputCapabilities = new Object2ObjectLinkedOpenHashMap<>();
            for (var entry : extraContents.row(IO.OUT).entrySet()) {
                RecipeCapability<?> cap = entry.getKey();
                outputCapabilities.put(cap, cap.createXEIContainerContents(entry.getValue(), recipe, IO.OUT));
            }
            for (var entry : outputCapabilities.entrySet()) {
                while (entry.getValue().size() < recipe.recipeType.getMaxOutputs(entry.getKey()))
                    entry.getValue().add(null);
                var container = entry.getKey().createXEIContainer(entry.getValue());
                if (container != null) {
                    extraTable.put(IO.OUT, entry.getKey(), container);
                }
            }
        }
    }

    public void addSlots(Table<IO, RecipeCapability<?>, List<Content>> contentTable, WidgetGroup group,
                         GTRecipe recipe) {
        for (var capabilityEntry : contentTable.rowMap().entrySet()) {
            IO io = capabilityEntry.getKey();
            for (var contentsEntry : capabilityEntry.getValue().entrySet()) {
                RecipeCapability<?> cap = contentsEntry.getKey();
                int nonTickCount = (io == IO.IN ? recipe.getInputContents(cap) : recipe.getOutputContents(cap)).size();
                List<Content> contents = contentsEntry.getValue();
                // bind fluid out overlay
                var widgetClass = cap.getWidgetClass();
                if (widgetClass != null) {
                    WidgetUtils.widgetByIdForEach(group, "^%s_[0-9]+$".formatted(cap.slotName(io)), widgetClass,
                            widget -> {
                                var index = WidgetUtils.widgetIdIndex(widget);
                                if (index >= 0 && index < contents.size()) {
                                    boolean hideOC = recipe.getType() == GTRecipeTypes.MACERATOR_RECIPES &&
                                            tier < GTValues.HV;
                                    var content = contents.get(index);
                                    int boostedChance = hideOC ? 0 : recipe.getType().getChanceFunction()
                                            .getBoostedChance(content, minTier, tier);

                                    cap.applyWidgetInfo(widget, index, true, io, null, recipe.getType(), recipe,
                                            content,
                                            null, minTier, tier);
                                    widget.setOverlay(content.createOverlay(index >= nonTickCount, minTier, tier,
                                            !hideOC, hideOC ? (entry, recipeTier, chanceTier) -> boostedChance :
                                                    recipe.getType().getChanceFunction()));
                                }
                            });
                }
            }
        }
    }
}
