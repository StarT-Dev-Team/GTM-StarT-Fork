package com.gregtechceu.gtceu.client;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.HazardProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.fluids.FluidConstants;
import com.gregtechceu.gtceu.api.fluids.FluidState;
import com.gregtechceu.gtceu.api.fluids.GTFluid;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.common.data.GTFluids;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.common.fluid.potion.PotionFluidHelper;
import com.gregtechceu.gtceu.data.lang.LangHandler;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.gregtechceu.gtceu.utils.input.SyncedKeyMappings;
import com.gregtechceu.gtceu.utils.input.TooltipScrollHandler;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MilkBucketItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.material.EmptyFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class TooltipsHandler {

    private static final String ITEM_PREFIX = "item." + GTCEu.MOD_ID;
    private static final String BLOCK_PREFIX = "block." + GTCEu.MOD_ID;

    public static void appendTooltips(ItemStack stack, TooltipFlag flag, List<Component> tooltips) {
        // Formula
        var materialEntry = ChemicalHelper.getMaterialEntry(stack.getItem());
        if (!materialEntry.isEmpty()) {
            var formula = materialEntry.material().getChemicalFormula();
            if (formula != null && !formula.isEmpty()) {
                tooltips.add(1, Component.literal(formula).withStyle(ChatFormatting.YELLOW));
            }
        }
        if (stack.getItem() instanceof BucketItem bucket) {
            var fluid = bucket.getFluid();
            if (!(fluid instanceof EmptyFluid)) {
                appendFluidTooltips(new FluidStack(fluid, FluidType.BUCKET_VOLUME), tooltips::add, flag);
            }
        } else if (stack.getItem() instanceof MilkBucketItem) {
            appendFluidTooltips(GTMaterials.Milk.getFluid(FluidType.BUCKET_VOLUME), tooltips::add, flag);
        }

        // Block/Item custom tooltips
        String translationKey = stack.getDescriptionId();
        if (translationKey.startsWith(ITEM_PREFIX) || translationKey.startsWith(BLOCK_PREFIX)) {
            String tooltipKey = translationKey + ".tooltip";
            if (Language.getInstance().has(tooltipKey)) {
                tooltips.add(1, Component.translatable(tooltipKey));
            } else {
                List<MutableComponent> multiLang = LangHandler.getMultiLang(tooltipKey);
                if (multiLang != null && !multiLang.isEmpty()) {
                    tooltips.addAll(1, multiLang);
                }
            }
        }

        Material material = HazardProperty.getValidHazardMaterial(stack);
        if (material.isNull()) {
            return;
        }
        GTUtil.appendHazardTooltips(material, tooltips);
    }

    public static void appendFluidTooltips(FluidStack fluidStack, Consumer<Component> tooltips, TooltipFlag flag) {
        Fluid fluid = fluidStack.getFluid();
        int amount = fluidStack.getAmount();
        FluidType fluidType = fluid.getFluidType();

        if (fluidType == GTFluids.POTION.getType()) {
            if (fluid.is(FluidTags.WATER)) {
                return;
            }
            PotionFluidHelper.addPotionTooltip(fluidStack, tooltips);
            return;
        }

        var material = ChemicalHelper.getMaterial(fluid);
        if (!material.isNull()) {
            var formula = material.getChemicalFormula();
            if (formula != null && !formula.isEmpty()) {
                tooltips.accept(Component.literal(formula).withStyle(ChatFormatting.YELLOW));
            }

            if (material.hasProperty(PropertyKey.INGOT)) {
                if (GTUtil.isShiftDown() && amount >= GTValues.L) {
                    long ingots = amount / GTValues.L;
                    long remainder = amount % GTValues.L;
                    String fluidAmount = String.format(" %,d mB = %,d * %d mB", amount, ingots, GTValues.L);
                    if (remainder != 0) {
                        fluidAmount += String.format(" + %d mB", remainder);
                    }
                    tooltips.accept(Component.translatable("gtceu.gui.fluid_amount").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(fluidAmount)));
                }
            }
        }

        if (fluid instanceof GTFluid attributedFluid) {
            FluidState state = attributedFluid.getState();
            switch (state) {
                case LIQUID -> tooltips.accept(Component.translatable("gtceu.fluid.state_liquid"));
                case GAS -> tooltips.accept(Component.translatable("gtceu.fluid.state_gas"));
                case PLASMA -> tooltips.accept(Component.translatable("gtceu.fluid.state_plasma"));
            }
            attributedFluid.getAttributes().forEach(a -> a.appendFluidTooltips(tooltips));
        } else {
            String key = "gtceu.fluid.state_" + (fluidType.isLighterThanAir() ? "gas" : "liquid");
            tooltips.accept(Component.translatable(key));
        }

        tooltips.accept(Component.translatable("gtceu.fluid.temperature", fluidType.getTemperature()));
        if (fluidType.getTemperature() < FluidConstants.CRYOGENIC_FLUID_THRESHOLD) {
            tooltips.accept(Component.translatable("gtceu.fluid.temperature.cryogenic"));
        }
    }

    public static BiConsumer<ItemStack, List<Component>> makePaginatedTooltipHandler(MachineDefinition machineDefinition,
                                                                                     List<List<Component>> paginatedTooltips) {
        List<String> shiftTooltips = new ArrayList<>();
        List<String> shiftTooltipDescriptions = new ArrayList<>();

        Component showCapabilities = Component.translatable("gtceu.tooltip.show_capabilities");
        Component showCapabilitiesShift = Component.translatable("gtceu.tooltip.show_capabilities_shift");
        Component breakerLine = Component.translatable("gtceu.universal.tooltip.breaker");

        for (RecipeModifier modifier : machineDefinition.getRecipeModifier().modifiers()) {
            String modifierId = modifier.getId();
            if (!GTRecipeModifiers.ignoreModifiers.contains(modifierId) && !modifierId.contains("lambda") &&
                    !modifierId.contains("proxy")) {
                shiftTooltips.add("gtceu.modifier.%s.name".formatted(modifierId));
                shiftTooltipDescriptions.add("gtceu.modifier.%s.description".formatted(modifierId));
            }
        }

        boolean isShiftToolsEmpty = shiftTooltips.isEmpty();
        boolean isPaginatedTooltipsEmpty = paginatedTooltips.isEmpty();
        int maxModifierPages = shiftTooltips.size();
        int maxPaginatedPages = paginatedTooltips.size();

        return (itemStack, components) -> {
            TooltipScrollHandler.setCurrentTooltipMachine(machineDefinition);
            boolean isShiftDown = GTUtil.isShiftDown();

            if (!isShiftToolsEmpty && isShiftDown) {
                var currentModifierPage = Math.floorMod(TooltipScrollHandler.getCurrentTooltipModifier(),
                        maxModifierPages);

                components.add(breakerLine);
                components.add(showCapabilitiesShift);
                for (int i = 0; i < maxModifierPages; i++) {
                    if (i == currentModifierPage) {
                        components.add(Component.translatable(shiftTooltips.get(i), "[x] "));
                        components.addAll(LangHandler.getSingleOrMultiLang(shiftTooltipDescriptions.get(i)));
                    } else {
                        components.add(Component.translatable(shiftTooltips.get(i), "[ ] "));
                    }
                }

                var keyPrev = Component.literal("[")
                        .append(SyncedKeyMappings.TOOLTIP_PREV.getCombinedDisplayName(KeyModifier.SHIFT))
                        .append("]");
                var keyNext = Component.literal("[")
                        .append(SyncedKeyMappings.TOOLTIP_NEXT.getCombinedDisplayName(KeyModifier.SHIFT))
                        .append("]");

                components.add(Component.translatable("gtceu.tooltip.capabilities_info",
                        keyPrev.withStyle(ChatFormatting.LIGHT_PURPLE),
                        keyNext.withStyle(ChatFormatting.LIGHT_PURPLE))
                        .withStyle(ChatFormatting.GRAY));
                return;
            }

            if (!isPaginatedTooltipsEmpty) {
                var currentPage = Math.floorMod(TooltipScrollHandler.getCurrentTooltipPage(), maxPaginatedPages);

                components.add(breakerLine);

                if (maxPaginatedPages > 1) {
                    components.addAll(paginatedTooltips.get(currentPage));

                    var keyPrev = Component.literal("[")
                            .append(SyncedKeyMappings.TOOLTIP_PREV.getCombinedDisplayName(TooltipScrollHandler
                                    .getRequiredModifierForPageScroll(SyncedKeyMappings.TOOLTIP_PREV)))
                            .append("]");
                    var keyNext = Component.literal("[")
                            .append(SyncedKeyMappings.TOOLTIP_NEXT.getCombinedDisplayName(TooltipScrollHandler
                                    .getRequiredModifierForPageScroll(SyncedKeyMappings.TOOLTIP_NEXT)))
                            .append("]");

                    components.add(Component.translatable("gtceu.tooltip.paginated_info",
                            keyPrev.withStyle(ChatFormatting.LIGHT_PURPLE),
                            currentPage + 1, maxPaginatedPages,
                            keyNext.withStyle(ChatFormatting.LIGHT_PURPLE))
                            .withStyle(ChatFormatting.GRAY));
                } else {
                    components.addAll(paginatedTooltips.get(0));
                }
            }

            if (!isShiftToolsEmpty) {
                components.add(breakerLine);
                components.add(showCapabilities);
            }
        };
    }
}
