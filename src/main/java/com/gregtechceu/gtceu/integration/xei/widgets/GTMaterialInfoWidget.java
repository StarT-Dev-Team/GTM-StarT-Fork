package com.gregtechceu.gtceu.integration.xei.widgets;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKey;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.gui.widget.TankWidget;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import com.gregtechceu.gtceu.common.data.GTMaterialItems;
import com.gregtechceu.gtceu.integration.emi.GTOreByProductPrefixHelper;
import com.gregtechceu.gtceu.integration.xei.entry.fluid.FluidEntryList;
import com.gregtechceu.gtceu.integration.xei.entry.fluid.FluidStackList;
import com.gregtechceu.gtceu.integration.xei.entry.item.ItemEntryList;
import com.gregtechceu.gtceu.integration.xei.entry.item.ItemStackList;
import com.gregtechceu.gtceu.integration.xei.entry.item.ItemTagList;
import com.gregtechceu.gtceu.integration.xei.handlers.fluid.CycleFluidEntryHandler;
import com.gregtechceu.gtceu.integration.xei.handlers.item.CycleItemEntryHandler;

import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.*;
import java.util.regex.Pattern;

import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;

public class GTMaterialInfoWidget extends WidgetGroup {

    private static final int SLOT = 18, PAD = 3, PER_ROW = 10;

    public enum Section {
        RAW,
        MATERIALS,
        PARTS
    }

    private static final Map<Section, List<List<TagPrefix>>> SLOTS = new EnumMap<>(Section.class);

    static {
        SLOTS.put(Section.RAW, slots(
                slot(rawOreBlock),
                slot(crushed, crushedPurified, crushedRefined),
                slot(dustImpure, dustPure),
                slot(dust, dustSmall, dustTiny)));

        SLOTS.put(Section.MATERIALS, slots(
                slot(block),
                slot(ingot),
                slot(ingotHot),
                slot(nugget),
                slot(gem, gemChipped, gemFlawed, gemFlawless, gemExquisite)));

        SLOTS.put(Section.PARTS, slots(
                slot(plate, plateDouble, plateDense),
                slot(foil),
                slot(round),
                slot(rod, rodLong),
                slot(bolt),
                slot(screw),
                slot(ring),
                slot(spring, springSmall),
                slot(wireFine),
                slot(gear, gearSmall),
                slot(rotor),
                slot(lens),
                slot(turbineBlade),
                slot(frameGt),
                slot(wireGtSingle, wireGtDouble, wireGtQuadruple, wireGtOctal, wireGtHex),
                slot(cableGtSingle, cableGtDouble, cableGtQuadruple, cableGtOctal, cableGtHex),
                slot(pipeTinyFluid, pipeSmallFluid, pipeNormalFluid, pipeLargeFluid, pipeHugeFluid,
                        pipeQuadrupleFluid, pipeNonupleFluid),
                slot(pipeSmallItem, pipeNormalItem, pipeLargeItem, pipeHugeItem),
                slot(pipeSmallRestrictive, pipeNormalRestrictive, pipeLargeRestrictive, pipeHugeRestrictive),
                slot(toolHeadDrill, toolHeadChainsaw, toolHeadBuzzSaw, toolHeadScrewdriver, toolHeadWrench,
                        toolHeadWireCutter)));
    }

    private static List<TagPrefix> slot(TagPrefix... prefixes) {
        return new ArrayList<>(Arrays.asList(prefixes));
    }

    @SafeVarargs
    private static List<List<TagPrefix>> slots(List<TagPrefix>... slots) {
        return new ArrayList<>(Arrays.asList(slots));
    }

    public static List<TagPrefix> getSlot(Section section, TagPrefix member) {
        for (List<TagPrefix> slot : SLOTS.get(section)) {
            if (slot.contains(member)) return slot;
        }

        return null;
    }

    public static void addSlot(Section section, TagPrefix... prefixes) {
        SLOTS.get(section).add(slot(prefixes));
    }

    public static void addSlotAfter(Section section, TagPrefix anchor, TagPrefix... prefixes) {
        List<List<TagPrefix>> slots = SLOTS.get(section);
        List<TagPrefix> at = getSlot(section, anchor);

        slots.add(at == null ? slots.size() : slots.indexOf(at) + 1, slot(prefixes));
    }

    public static void removeSlot(Section section, TagPrefix member) {
        SLOTS.get(section).removeIf(slot -> slot.contains(member));
    }

    public static void addToSlot(Section section, TagPrefix member, TagPrefix... added) {
        List<TagPrefix> slot = getSlot(section, member);

        if (slot != null) slot.addAll(Arrays.asList(added));
    }

    public static void removeFromSlot(Section section, TagPrefix member) {
        List<TagPrefix> slot = getSlot(section, member);

        if (slot == null) return;

        slot.remove(member);

        if (slot.isEmpty()) SLOTS.get(section).remove(slot);
    }

    public static final String MATERIAL_CONTENT_GROUP_ID = "materialContentGroup";
    public static final Pattern MATERIAL_CONTENT_GROUP_ID_REGEX = Pattern.compile("^materialContentGroup$");

    private final Material material;

    public GTMaterialInfoWidget(Material material) {
        super(0, 0, PER_ROW * 18 + PAD * 2, 166);

        this.material = material;

        setClientSideWidget();
        setMaterial();
    }

    public void setMaterial() {
        this.widgets.clear();

        WidgetGroup group = new WidgetGroup();

        group.setId(MATERIAL_CONTENT_GROUP_ID);

        getWidgetsById(MATERIAL_CONTENT_GROUP_ID_REGEX).forEach(this::removeWidget);

        addWidget(group);

        int y = addWrappedText(group, material.getLocalizedName().getString(), PAD, -1, true);

        String formula = material.getChemicalFormula();
        if (formula != null && !formula.isEmpty()) {
            y = addWrappedText(group, formula, y, 0xFF404040, false);
        }

        y += PAD;

        List<ItemEntryList> rawEntries = new ArrayList<>();

        if (material.hasProperty(PropertyKey.ORE)) {
            rawEntries.add(GTOreByProductPrefixHelper.getOreByProductPrefix(material));
        }

        rawEntries.addAll(fromSlots(SLOTS.get(Section.RAW)));

        List<ItemEntryList> topRow = new ArrayList<>(rawEntries);
        List<ItemEntryList> materials = fromSlots(SLOTS.get(Section.MATERIALS));

        if (!topRow.isEmpty() && !materials.isEmpty()) topRow.add(null);

        topRow.addAll(materials);

        y = addItemRow(group, topRow, y);

        if (material.hasProperty(PropertyKey.FLUID)) {
            List<FluidEntryList> rawFluids = new ArrayList<>();

            for (FluidStorageKey key : FluidStorageKey.allKeys()) {
                Fluid fluid = material.getFluid(key);

                if (fluid != null) rawFluids.add(FluidStackList.of(new FluidStack(fluid, 1000)));
            }

            if (!rawFluids.isEmpty()) y = addFluidRow(group, rawFluids, y);
        }

        y = addItemRow(group, fromSlots(SLOTS.get(Section.PARTS)), y);
        y = addTools(group, y);

        setSize(getSize().width, y);
    }

    private List<ItemEntryList> fromSlots(List<List<TagPrefix>> slots) {
        List<ItemEntryList> list = new ArrayList<>();

        for (List<TagPrefix> slot : slots) {
            if (slot.isEmpty()) continue;

            if (slot.get(0).hasItemTable()) {
                ItemStackList stacks = new ItemStackList();

                for (TagPrefix p : slot) {
                    ItemStack s = ChemicalHelper.get(p, material);

                    if (!s.isEmpty()) stacks.add(s);
                }

                if (!stacks.isEmpty()) list.add(stacks);
            } else {
                ItemTagList tags = new ItemTagList();

                for (TagPrefix p : slot) {
                    var tag = ChemicalHelper.getTag(p, material);

                    if (tag != null && (p.doGenerateItem(material) || p.doGenerateBlock(material))) {
                        tags.add(tag, 1, null);
                    }
                }

                if (!tags.isEmpty()) list.add(tags);
            }
        }

        return list;
    }

    private int addTools(WidgetGroup group, int y) {
        if (!material.hasProperty(PropertyKey.TOOL)) return y;

        List<ItemEntryList> plain = new ArrayList<>();
        Map<String, List<GTToolType>> electric = new TreeMap<>();

        GTToolType.getTypes().values().stream()
                .sorted(Comparator.comparing(type -> type.name))
                .forEach(type -> {
                    if (type.electricTier < 0) {
                        ItemStack stack = toolStack(type);

                        if (!stack.isEmpty()) plain.add(ItemStackList.of(stack));
                    } else {
                        electric.computeIfAbsent(toolFamily(type), k -> new ArrayList<>()).add(type);
                    }
                });

        List<ItemEntryList> powered = new ArrayList<>();

        for (List<GTToolType> family : electric.values()) {
            family.sort(Comparator.comparingInt(type -> type.electricTier));

            ItemStackList stacks = new ItemStackList();

            for (GTToolType type : family) {
                ItemStack stack = toolStack(type);

                if (!stack.isEmpty()) stacks.add(stack);
            }

            if (!stacks.isEmpty()) powered.add(stacks);
        }

        y = addItemRow(group, plain, y);

        return addItemRow(group, powered, y);
    }

    private static String toolFamily(GTToolType type) {
        return type.name.replaceFirst("^" + GTValues.VN[type.electricTier].toLowerCase() + "_", "");
    }

    private ItemStack toolStack(GTToolType type) {
        var entry = GTMaterialItems.TOOL_ITEMS.get(material, type);

        return entry == null ? ItemStack.EMPTY : entry.asStack();
    }

    private int addItemRow(WidgetGroup group, List<ItemEntryList> row, int y) {
        if (row.isEmpty()) return y;

        CycleItemEntryHandler handler = new CycleItemEntryHandler(row);

        for (int i = 0; i < row.size(); i++) {
            if (row.get(i) == null) continue;

            int x = PAD + (i % PER_ROW) * SLOT, yy = y + (i / PER_ROW) * SLOT;

            group.addWidget(new SlotWidget(handler, i, x, yy)
                    .setCanTakeItems(false)
                    .setCanPutItems(false)
                    .setIngredientIO(IngredientIO.INPUT));
        }

        return y + ((row.size() + PER_ROW - 1) / PER_ROW) * SLOT + PAD;
    }

    private int addFluidRow(WidgetGroup group, List<FluidEntryList> row, int y) {
        CycleFluidEntryHandler handler = new CycleFluidEntryHandler(row);

        for (int i = 0; i < row.size(); i++) {
            group.addWidget(
                    new TankWidget(new CustomFluidTank(handler.getFluidInTank(i)), PAD + i * SLOT, y, false, false)
                            .setIngredientIO(IngredientIO.INPUT)
                            .setBackground(GuiTextures.FLUID_SLOT)
                            .setShowAmount(false));
        }

        return y + SLOT + PAD;
    }

    private int addWrappedText(WidgetGroup group, String text, int y, int color, boolean shadow) {
        Font font = Minecraft.getInstance().font;

        for (FormattedText line : font.getSplitter().splitLines(text, getSize().width - PAD * 2, Style.EMPTY)) {
            group.addWidget(new LabelWidget(PAD, y, line.getString()).setTextColor(color).setDropShadow(shadow));

            y += font.lineHeight + 1;
        }

        return y;
    }
}
