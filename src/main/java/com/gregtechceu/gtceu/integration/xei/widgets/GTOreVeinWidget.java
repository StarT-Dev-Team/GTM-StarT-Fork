package com.gregtechceu.gtceu.integration.xei.widgets;

import com.gregtechceu.gtceu.api.data.DimensionMarker;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidDefinition;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockore.BedrockOreDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.client.ClientProxy;
import com.gregtechceu.gtceu.config.ConfigHolder;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import com.lowdragmc.lowdraglib2.utils.LocalizationUtils;

import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import dev.vfyjxf.taffy.style.TaffyPosition;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Getter
public class GTOreVeinWidget extends UIElement {

    private final String name;
    private final int weight;
    private final String range;
    private final Set<ResourceKey<Level>> dimensionFilter;
    public final static int width = 120;

    private static <T extends UIElement> T place(T element, int x, int y) {
        element.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(x).top(y));
        return element;
    }

    private void setSize(int height) {
        layout(layout -> layout.width(width).height(height));
    }

    private void addLabel(int x, int y, String text) {
        addChild(GTRecipeWidget.createLabel(x, y, text));
    }

    private void addTitle(String key) {
        var title = new UIElement();
        title.style(style -> style.backgroundTexture(new TextTexture(key).setType(TextTexture.TextType.LEFT_ROLL)
                .setWidth(width - 10)));
        title.layout(layout -> layout.width(width - 10).height(16));
        addChild(place(title, 5, 0));
    }

    private ItemSlot createOreSlot(List<ItemStack> stacks, int index, int x, int chance) {
        var oreSlot = new ItemSlot().setItem(stacks.get(index));
        oreSlot.xeiRecipeIngredient(IngredientIO.OUTPUT).xeiRecipeSlot(IngredientIO.OUTPUT, 1);
        oreSlot.style(style -> style.appendTooltips(Component.nullToEmpty(
                LocalizationUtils.format("gtceu.jei.ore_vein_diagram.chance", chance))));
        return place(oreSlot, x, 18);
    }

    public GTOreVeinWidget(GTOreDefinition oreDefinition) {
        setSize(160);
        this.name = getOreName(oreDefinition);
        this.weight = oreDefinition.weight();
        this.dimensionFilter = oreDefinition.dimensionFilter();
        this.range = range(oreDefinition);
        setupBaseGui(oreDefinition);
        setupText(oreDefinition);
    }

    public GTOreVeinWidget(BedrockFluidDefinition fluid) {
        setSize(140);
        this.name = getFluidName(fluid);
        this.weight = fluid.getWeight();
        this.dimensionFilter = fluid.getDimensionFilter();
        this.range = "NULL";
        setupBaseGui(fluid);
        setupText(fluid);
    }

    public GTOreVeinWidget(BedrockOreDefinition bedrockOre) {
        setSize(140);
        this.name = getBedrockOreName(bedrockOre);
        this.weight = bedrockOre.weight();
        this.dimensionFilter = bedrockOre.dimensionFilter();
        this.range = "NULL";
        setupBaseGui(bedrockOre);
        setupText(bedrockOre);
    }

    @SuppressWarnings("all")
    private String range(GTOreDefinition oreDefinition) {
        HeightProvider height = oreDefinition.range().height;
        int minHeight = 0, maxHeight = 0;
        if (height instanceof UniformHeight uniform) {
            minHeight = uniform.minInclusive.resolveY(null);
            maxHeight = uniform.maxInclusive.resolveY(null);
        }
        return String.format("%d - %d", minHeight, maxHeight);
    }

    private void setupBaseGui(GTOreDefinition oreDefinition) {
        NonNullList<ItemStack> containedOresAsItemStacks = NonNullList.create();
        List<Integer> chances = oreDefinition.veinGenerator().getAllChances();
        containedOresAsItemStacks.addAll(getRawMaterialList(oreDefinition));
        int n = containedOresAsItemStacks.size();
        int x = (width - 18 * n) / 2;
        for (int i = 0; i < n; i++) {
            addChild(createOreSlot(containedOresAsItemStacks, i, x, chances.get(i)));
            x += 18;
        }
    }

    private void setupBaseGui(BedrockFluidDefinition fluid) {
        Fluid storedFluid = fluid.getStoredFluid().get();
        var fluidSlot = new FluidSlot().setFluid(new FluidStack(storedFluid, 1000));
        fluidSlot.slotStyle(style -> style.fillDirection(FillDirection.ALWAYS_FULL));
        fluidSlot.xeiRecipeIngredient(IngredientIO.OUTPUT).xeiRecipeSlot(IngredientIO.OUTPUT, 1);
        addChild(place(fluidSlot, 51, 18));
    }

    private void setupBaseGui(BedrockOreDefinition bedrockOreDefinition) {
        NonNullList<ItemStack> containedOresAsItemStacks = NonNullList.create();
        IntList chances = bedrockOreDefinition.getAllChances();
        containedOresAsItemStacks.addAll(getRawMaterialList(bedrockOreDefinition));
        int n = containedOresAsItemStacks.size();
        int x = (width - 18 * n) / 2;
        for (int i = 0; i < n; i++) {
            addChild(createOreSlot(containedOresAsItemStacks, i, x, chances.getInt(i)));
            x += 18;
        }
    }

    private void setupText(GTOreDefinition ignored) {
        addTitle("gtceu.jei.ore_vein." + name);
        addLabel(5, 40, LocalizationUtils.format("gtceu.jei.ore_vein_diagram.spawn_range"));
        addLabel(5, 50, range);

        addLabel(5, 60, LocalizationUtils.format("gtceu.jei.ore_vein_diagram.weight", weight));
        addLabel(5, 70, LocalizationUtils.format("gtceu.jei.ore_vein_diagram.dimensions"));
        setupDimensionMarker(80);
    }

    private void setupText(BedrockFluidDefinition ignored) {
        addTitle("gtceu.jei.bedrock_fluid." + name);
        addLabel(5, 40, LocalizationUtils.format("gtceu.jei.ore_vein_diagram.weight", weight));
        addLabel(5, 50, LocalizationUtils.format("gtceu.jei.ore_vein_diagram.dimensions"));
        setupDimensionMarker(60);
    }

    private void setupText(BedrockOreDefinition ignored) {
        addTitle("gtceu.jei.bedrock_ore." + name);
        addLabel(5, 40, LocalizationUtils.format("gtceu.jei.ore_vein_diagram.weight", weight));
        addLabel(5, 50, LocalizationUtils.format("gtceu.jei.ore_vein_diagram.dimensions"));
        setupDimensionMarker(60);
    }

    private void setupDimensionMarker(int yPosition) {
        if (this.dimensionFilter != null) {
            int interval = 2;
            int rowSlots = (width - 10 + interval) / (16 + interval);

            DimensionMarker[] dimMarkers = dimensionFilter.stream()
                    .map(ResourceKey::location)
                    .map(loc -> GTRegistries.DIMENSION_MARKERS.getOrDefault(loc,
                            new DimensionMarker(DimensionMarker.MAX_TIER, () -> Blocks.BARRIER, loc.toString())))
                    .sorted(Comparator.comparingInt(DimensionMarker::getTier))
                    .toArray(DimensionMarker[]::new);
            for (int i = 0; i < dimMarkers.length; i++) {
                var dimMarker = dimMarkers[i];
                var icon = dimMarker.getIcon();
                int row = Math.floorDiv(i, rowSlots);
                var dimSlot = new ItemSlot().setItem(icon);
                dimSlot.xeiRecipeIngredient(IngredientIO.CATALYST).xeiRecipeSlot(IngredientIO.CATALYST, 1);
                dimSlot.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
                if (ConfigHolder.INSTANCE.compat.showDimensionTier) {
                    var overlay = new TextTexture("T" + (dimMarker.tier >= DimensionMarker.MAX_TIER ? "?" :
                            dimMarker.tier)).scale(0.75F).transform(-3F, 5F);
                    dimSlot.style(style -> style.overlay(overlay));
                }
                addChild(place(dimSlot, 5 + (16 + interval) * (i - row * rowSlots), yPosition + 18 * row));
            }
        } else {
            addLabel(5, yPosition, "Any");
        }
    }

    public static List<ItemStack> getContainedOresAndBlocks(GTOreDefinition oreDefinition) {
        return oreDefinition.veinGenerator().getAllEntries().stream()
                .flatMap(entry -> entry.map(state -> Stream.of(state.getBlock().asItem().getDefaultInstance()),
                        material -> {
                            Set<ItemStack> ores = new HashSet<>();
                            ores.add(ChemicalHelper.get(TagPrefix.rawOre, material));
                            for (TagPrefix prefix : TagPrefix.ORES.keySet()) {
                                ores.add(ChemicalHelper.get(prefix, material));
                            }
                            return ores.stream();
                        }))
                .toList();
    }

    public static List<ItemStack> getRawMaterialList(GTOreDefinition oreDefinition) {
        return oreDefinition.veinGenerator().getAllEntries().stream()
                .map(entry -> entry.map(state -> state.getBlock().asItem().getDefaultInstance(),
                        material -> ChemicalHelper.get(TagPrefix.rawOre, material)))
                .toList();
    }

    public static List<ItemStack> getRawMaterialList(BedrockOreDefinition bedrockOreDefinition) {
        return bedrockOreDefinition.materials().stream()
                .map(entry -> ChemicalHelper.get(TagPrefix.rawOre, entry.material()))
                .toList();
    }

    public static String getOreName(GTOreDefinition oreDefinition) {
        ResourceLocation id = ClientProxy.CLIENT_ORE_VEINS.inverse().get(oreDefinition);
        return id.getPath();
    }

    public static String getFluidName(BedrockFluidDefinition fluid) {
        ResourceLocation id = ClientProxy.CLIENT_FLUID_VEINS.inverse().get(fluid);
        return id.getPath();
    }

    public static String getBedrockOreName(BedrockOreDefinition oreDefinition) {
        ResourceLocation id = ClientProxy.CLIENT_BEDROCK_ORE_VEINS.inverse().get(oreDefinition);
        return id.getPath();
    }
}
