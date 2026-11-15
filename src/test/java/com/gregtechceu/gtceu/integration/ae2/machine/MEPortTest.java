package com.gregtechceu.gtceu.integration.ae2.machine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeTransaction;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.machines.GTAEMachines;
import com.gregtechceu.gtceu.gametest.util.TestUtils;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.AmountEntryModel;
import com.gregtechceu.gtceu.integration.ae2.slot.PortAEInventory;
import com.gregtechceu.gtceu.integration.ae2.utils.KeyStorage;
import com.gregtechceu.gtceu.integration.ae2.utils.TopStacks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.gregtechceu.gtceu.gametest.util.TestUtils.getMetaMachine;

@PrefixGameTestTemplate(false)
@GameTestHolder(GTCEu.MOD_ID)
public class MEPortTest {

    private static final String BATCH = "MEPort";
    private static final String T = "patternbuffertest";

    private static GTRecipe RECIPE;

    @BeforeBatch(batch = BATCH)
    public static void prepare(ServerLevel level) {
        var type = TestUtils.createRecipeType("me_port", GTRecipeTypes.LARGE_CHEMICAL_RECIPES);
        RECIPE = type.recipeBuilder(GTCEu.id("me_port")).id(GTCEu.id("me_port"))
                .inputItems(new ItemStack(Items.PAPER)).outputItems(new ItemStack(Items.DIRT))
                .EUt(GTValues.V[GTValues.HV]).duration(1).buildRawRecipe();
    }

    private static Map<RecipeCapability<?>, List<Object>> both(Object item, Object fluid) {
        Map<RecipeCapability<?>, List<Object>> map = new HashMap<>();
        if (item != null) map.put(ItemRecipeCapability.CAP, new ArrayList<>(List.of(item)));
        if (fluid != null) map.put(FluidRecipeCapability.CAP, new ArrayList<>(List.of(fluid)));
        return map;
    }

    private static SizedIngredient items(Item item, int amount) {
        return SizedIngredient.create(new ItemStack(item, amount));
    }

    private static FluidIngredient fluid(Fluid fluid, int amount) {
        return FluidIngredient.of(fluid, amount);
    }

    private static GenericStack item(Item item, long amount) {
        return new GenericStack(AEItemKey.of(item), amount);
    }

    private static GenericStack fluidStack(Fluid fluid, long amount) {
        return new GenericStack(AEFluidKey.of(fluid), amount);
    }

    private static long amountAt(PortAEInventory inventory, int slot) {
        return inventory.stock.getAmount(slot);
    }

    private static MEPatternBufferPartMachine connect(GameTestHelper helper, MEBusPartMachine... parts) {
        var buffer = (MEPatternBufferPartMachine) getMetaMachine(helper.getBlockEntity(new BlockPos(2, 2, 1)));
        var all = EnumSet.allOf(Direction.class);
        buffer.getMainNode().setExposedOnSides(all);
        for (var part : parts) part.getMainNode().setExposedOnSides(all);
        return buffer;
    }

    @GameTest(template = T, batch = BATCH, setupTicks = 40, timeoutTicks = 100)
    public static void portInputSuppliesItemsAndFluidsFromOneHatch(GameTestHelper helper) {
        var part = (MEInputPortPartMachine) TestUtils.setMachine(helper, new BlockPos(2, 2, 0),
                GTAEMachines.INPUT_PORT_ME);
        var portSlots = part.getPortInventory();
        portSlots.stock.setStack(0, item(Items.PAPER, 10));
        portSlots.stock.setStack(1, fluidStack(Fluids.WATER, 4000));
        var handlerList = part.getRecipeHandlers().get(0);

        helper.assertTrue(
                handlerList.handleRecipe(IO.IN, RECIPE, both(items(Items.PAPER, 4), fluid(Fluids.WATER, 1000)), true)
                        .isEmpty(),
                "paper and water from the same hatch should be supplied");
        helper.assertTrue(
                !handlerList.handleRecipe(IO.IN, RECIPE, both(items(Items.PAPER, 11), fluid(Fluids.WATER, 1000)),
                        true).isEmpty(),
                "more paper than there is must fail");
        helper.assertTrue(
                !handlerList.handleRecipe(IO.IN, RECIPE, both(items(Items.PAPER, 1), fluid(Fluids.LAVA, 1000)),
                        true).isEmpty(),
                "a fluid that is not there must fail");
        helper.assertTrue(amountAt(portSlots, 0) == 10 && amountAt(portSlots, 1) == 4000,
                "simulating must not change stock");

        helper.assertTrue(
                handlerList.handleRecipe(IO.IN, RECIPE, both(items(Items.PAPER, 4), fluid(Fluids.WATER, 1000)),
                        false).isEmpty(),
                "real handling should succeed");
        helper.assertTrue(amountAt(portSlots, 0) == 6 && amountAt(portSlots, 1) == 3000,
                "stock after real handling: " + amountAt(portSlots, 0) + ", " + amountAt(portSlots, 1));

        try (var transaction = RecipeTransaction.begin()) {
            handlerList.handleRecipe(IO.IN, RECIPE, both(items(Items.PAPER, 2), fluid(Fluids.WATER, 500)), false);
            helper.assertTrue(amountAt(portSlots, 0) == 4 && amountAt(portSlots, 1) == 2500,
                    "taken inside the transaction");
        }

        helper.assertTrue(amountAt(portSlots, 0) == 6 && amountAt(portSlots, 1) == 3000,
                "rollback should restore the stock: " + amountAt(portSlots, 0) + ", " + amountAt(portSlots, 1));

        portSlots.config.setStack(2, item(Items.PAPER, 1_000_000));
        helper.assertTrue(portSlots.config.getAmount(2) == 1_000_000, "config amount was clamped");
        portSlots.stock.setStack(3, item(Items.PAPER, 5_000_000_000L));
        helper.assertTrue(amountAt(portSlots, 3) == 5_000_000_000L, "stock amount was clamped");

        helper.succeed();
    }

    @GameTest(template = T, batch = BATCH, setupTicks = 40, timeoutTicks = 200)
    public static void portInputSyncsItemsAndFluidsWithTheNetwork(GameTestHelper helper) {
        var part = (MEInputPortPartMachine) TestUtils.setMachine(helper, new BlockPos(2, 2, 0),
                GTAEMachines.INPUT_PORT_ME);
        var buffer = connect(helper, part);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(part.isOnline(), "The port did not come online");
            var network = buffer.getGrid().getStorageService().getInventory();
            var source = IActionSource.empty();
            network.insert(AEItemKey.of(Items.PAPER), 500, Actionable.MODULATE, source);
            network.insert(AEItemKey.of(Items.SUGAR), 500, Actionable.MODULATE, source);
            long paperBefore = network.extract(AEItemKey.of(Items.PAPER), Long.MAX_VALUE, Actionable.SIMULATE, source);
            helper.assertTrue(paperBefore >= 500, "The test network did not take the paper");

            var portSlots = part.getPortInventory();
            portSlots.config.setStack(0, item(Items.PAPER, 64));
            portSlots.config.setStack(1, item(Items.SUGAR, 20));
            part.syncME();
            helper.assertTrue(amountAt(portSlots, 0) == 64, "paper: " + amountAt(portSlots, 0));
            helper.assertTrue(amountAt(portSlots, 1) == 20, "sugar: " + amountAt(portSlots, 1));

            portSlots.config.setStack(0, item(Items.SUGAR, 10));
            part.syncME();
            part.syncME();
            helper.assertTrue(portSlots.stock.getStack(0) != null && portSlots.stock.getStack(0).what().equals(
                    AEItemKey.of(Items.SUGAR)) && amountAt(portSlots, 0) == 10,
                    "slot 0 should hold the new request: " + portSlots.stock.getStack(0));
            long paperAfter = network.extract(AEItemKey.of(Items.PAPER), Long.MAX_VALUE, Actionable.SIMULATE, source);
            helper.assertTrue(paperAfter == paperBefore, "paper not returned: " + paperAfter + " != " + paperBefore);

            long sugarBefore = network.extract(AEItemKey.of(Items.SUGAR), Long.MAX_VALUE, Actionable.SIMULATE, source);
            portSlots.config.setStack(0, fluidStack(Fluids.WATER, 100));
            part.syncME();
            part.syncME();
            helper.assertTrue(portSlots.stock.getStack(0) == null,
                    "the old item should be gone: " + portSlots.stock.getStack(0));
            long sugarAfter = network.extract(AEItemKey.of(Items.SUGAR), Long.MAX_VALUE, Actionable.SIMULATE, source);
            helper.assertTrue(sugarAfter == sugarBefore + 10, "the sugar should be back: " + sugarBefore + " -> " +
                    sugarAfter);

            helper.succeed();
        });
    }

    @GameTest(template = T, batch = BATCH, setupTicks = 40, timeoutTicks = 300)
    public static void portStockingAutoPullRanksItemsAndFluidsTogetherAndSplitsBetweenHatches(
                                                                                              GameTestHelper helper) {
        var controller = (WorkableMultiblockMachine) getMetaMachine(helper.getBlockEntity(new BlockPos(1, 2, 0)));
        var first = (MEStockingInputPortPartMachine) TestUtils.setMachine(helper, new BlockPos(2, 1, 0),
                GTAEMachines.STOCKING_INPUT_PORT_ME);
        var second = (MEStockingInputPortPartMachine) TestUtils.setMachine(helper, new BlockPos(2, 2, 0),
                GTAEMachines.STOCKING_INPUT_PORT_ME);
        TestUtils.formMultiblock(controller);
        var buffer = connect(helper, first, second);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(first.isOnline() && second.isOnline(), "The stocking hatches are not online");
            var network = buffer.getGrid().getStorageService().getInventory();
            var source = IActionSource.empty();
            for (int i = 0; i < 40; i++) {
                var tag = new CompoundTag();
                tag.putInt("n", i);
                network.insert(AEItemKey.of(Items.PAPER, tag), 100 + i * 7L, Actionable.MODULATE, source);
            }

            first.setAutoPull(true);
            second.setAutoPull(true);

            var ranked = new ArrayList<Double>();
            var perKey = new HashMap<AEKey, Double>();
            for (var entry : network.getAvailableStacks()) {
                var key = entry.getKey();
                if (!(key instanceof AEItemKey) && !(key instanceof AEFluidKey)) continue;
                if (entry.getLongValue() <= 0) continue;
                double units = (double) entry.getLongValue() / key.getType().getAmountPerUnit();
                perKey.put(key, units);
                ranked.add(units);
            }

            ranked.sort(Comparator.reverseOrder());

            var seen = new HashSet<AEKey>();
            var combined = new ArrayList<Double>();
            for (var hatch : List.of(first, second)) {
                var portSlots = hatch.getPortInventory();
                int configured = 0;
                var units = new ArrayList<Double>();
                for (int i = 0; i < PortAEInventory.BASE_SIZE; i++) {
                    var config = portSlots.config.getStack(i);
                    if (config == null) continue;
                    configured++;
                    helper.assertTrue(seen.add(config.what()), "Both hatches pull " + config.what());
                    units.add(perKey.getOrDefault(config.what(), -1.0));
                }

                helper.assertTrue(configured == PortAEInventory.BASE_SIZE,
                        "A hatch holds " + configured + " of " + PortAEInventory.BASE_SIZE + " slots");
                for (int i = 1; i < units.size(); i++) {
                    helper.assertTrue(units.get(i - 1) >= units.get(i), "Slots are not ordered by amount: " + units);
                }

                combined.addAll(units);
            }

            combined.sort(Comparator.reverseOrder());
            helper.assertTrue(combined.equals(ranked.subList(0, 32)),
                    "Pulled " + combined + " but the top 32 are " + ranked.subList(0, 32));

            helper.succeed();
        });
    }

    @GameTest(template = T, batch = BATCH, setupTicks = 40, timeoutTicks = 100)
    public static void portDataStickCarriesBothTypesAndFitsBusAndHatchKeys(GameTestHelper helper) {
        var source = (MEInputPortPartMachine) TestUtils.setMachine(helper, new BlockPos(2, 2, 0),
                GTAEMachines.INPUT_PORT_ME);
        var target = (MEInputPortPartMachine) TestUtils.setMachine(helper, new BlockPos(2, 1, 0),
                GTAEMachines.INPUT_PORT_ME);
        var player = helper.makeMockPlayer();
        var portSlots = source.getPortInventory();
        portSlots.config.setStack(0, item(Items.PAPER, 32));
        portSlots.config.setStack(1, fluidStack(Fluids.WATER, 2000));
        portSlots.config.setStack(5, item(Items.SUGAR, 64));
        source.setTicksPerCycle(77);

        var stick = new ItemStack(Items.PAPER);
        source.onDataStickShiftUse(player, stick);
        var tag = stick.getTag();
        helper.assertTrue(tag != null && tag.contains(MEInputPortPartMachine.TAG_PORT) &&
                tag.contains(MEInputPortPartMachine.TAG_BUS) &&
                tag.contains(MEInputPortPartMachine.TAG_HATCH),
                "The stick should carry the port, bus and hatch formats: " + tag);

        var busSlots = tag.getCompound(MEInputPortPartMachine.TAG_BUS).getCompound("ConfigStacks");
        var hatchSlots = tag.getCompound(MEInputPortPartMachine.TAG_HATCH).getCompound("ConfigStacks");
        helper.assertTrue(busSlots.getAllKeys().equals(Set.of("0", "5")),
                "bus part slots: " + busSlots.getAllKeys());
        helper.assertTrue(hatchSlots.getAllKeys().equals(Set.of("1")),
                "hatch part slots: " + hatchSlots.getAllKeys());
        for (var key : busSlots.getAllKeys()) {
            helper.assertTrue(GenericStack.readTag(busSlots.getCompound(key)).what() instanceof AEItemKey,
                    "a non-item in the bus format");
        }

        target.onDataStickUse(player, stick);
        var copy = target.getPortInventory();
        for (int i = 0; i < PortAEInventory.BASE_SIZE; i++) {
            helper.assertTrue(Objects.equals(portSlots.config.getStack(i), copy.config.getStack(i)),
                    "slot " + i + " differs after pasting: " + copy.config.getStack(i));
        }

        helper.assertTrue(target.getTicksPerCycle() == 77, "ticks per cycle not copied");

        var busStick = new ItemStack(Items.PAPER);
        var busTag = new CompoundTag();
        var busConfig = new CompoundTag();
        var stacks = new CompoundTag();
        stacks.put("3", GenericStack.writeTag(item(Items.COAL, 16)));
        busConfig.put("ConfigStacks", stacks);
        busTag.put(MEInputPortPartMachine.TAG_BUS, busConfig);
        busStick.setTag(busTag);
        target.onDataStickUse(player, busStick);
        helper.assertTrue(copy.config.getStack(3) != null && copy.config.getStack(0) == null &&
                copy.config.getStack(1) == null, "a bus stick should replace the port config");

        var other = new ItemStack(Items.PAPER);
        other.setTag(new CompoundTag());
        helper.assertTrue(target.onDataStickUse(player, other) == InteractionResult.PASS,
                "an unrelated stick must pass");

        helper.succeed();
    }

    @GameTest(template = T, batch = BATCH, setupTicks = 40, timeoutTicks = 100)
    public static void portOutputTakesItemsAndFluidsIntoOneBuffer(GameTestHelper helper)
                                                                                         throws ReflectiveOperationException {
        var part = (MEOutputPortPartMachine) TestUtils.setMachine(helper, new BlockPos(0, 1, 0),
                GTAEMachines.OUTPUT_PORT_ME);
        var field = MEOutputPortPartMachine.class.getDeclaredField("internalBuffer");
        field.setAccessible(true);
        var buffer = (KeyStorage) field.get(part);
        var handlerList = part.getRecipeHandlers().get(0);
        var dirt = AEItemKey.of(Items.DIRT);
        var water = AEFluidKey.of(Fluids.WATER);

        helper.assertTrue(
                handlerList.handleRecipe(IO.OUT, RECIPE, both(items(Items.DIRT, 16), fluid(Fluids.WATER, 500)), true)
                        .isEmpty(),
                "outputs should always fit");
        helper.assertTrue(buffer.isEmpty(), "simulating must not fill the buffer");

        helper.assertTrue(
                handlerList.handleRecipe(IO.OUT, RECIPE, both(items(Items.DIRT, 16), fluid(Fluids.WATER, 500)),
                        false).isEmpty(),
                "real output should succeed");
        helper.assertTrue(buffer.storage.getLong(dirt) == 16 && buffer.storage.getLong(water) == 500,
                "buffer: " + buffer.storage);

        try (var transaction = RecipeTransaction.begin()) {
            handlerList.handleRecipe(IO.OUT, RECIPE, both(items(Items.DIRT, 4), fluid(Fluids.WATER, 100)), false);
            helper.assertTrue(buffer.storage.getLong(dirt) == 20 && buffer.storage.getLong(water) == 600,
                    "added inside the transaction");
        }

        helper.assertTrue(buffer.storage.getLong(dirt) == 16 && buffer.storage.getLong(water) == 500,
                "rollback should remove what was added: " + buffer.storage);

        var saved = buffer.serializeNBT();
        var restored = new KeyStorage();
        restored.deserializeNBT(saved);
        helper.assertTrue(restored.storage.getLong(dirt) == 16 && restored.storage.getLong(water) == 500,
                "buffer not restored: " + restored.storage);

        helper.succeed();
    }

    @GameTest(template = T, batch = BATCH, setupTicks = 40, timeoutTicks = 100)
    public static void portInventorySurvivesSaving(GameTestHelper helper) {
        var part = (MEInputPortPartMachine) TestUtils.setMachine(helper, new BlockPos(2, 2, 0),
                GTAEMachines.INPUT_PORT_ME);
        var portSlots = part.getPortInventory();
        portSlots.config.setStack(0, item(Items.PAPER, 1_000_000));
        portSlots.config.setStack(7, fluidStack(Fluids.WATER, 123_456));
        portSlots.stock.setStack(0, item(Items.PAPER, 5_000_000_000L));
        portSlots.stock.setStack(7, fluidStack(Fluids.WATER, 777));

        CompoundTag tag = portSlots.serializeNBT();
        helper.assertTrue(tag.contains("config", Tag.TAG_LIST) && tag.contains("stock", Tag.TAG_LIST),
                "unexpected format: " + tag);
        var restored = new PortAEInventory(() -> {}, PortAEInventory.BASE_SIZE);
        restored.deserializeNBT(tag);
        for (int i = 0; i < PortAEInventory.BASE_SIZE; i++) {
            helper.assertTrue(Objects.equals(portSlots.config.getStack(i), restored.config.getStack(i)) &&
                    Objects.equals(portSlots.stock.getStack(i), restored.stock.getStack(i)),
                    "slot " + i + " differs after a save/load round trip");
        }

        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void mixedSelectionRanksFluidsByBucketsAndAppliesTheMinimumPerType(GameTestHelper helper) {
        GTCEu.LOGGER.info("[MEPort] fluid amount per unit = {}", AEKeyType.fluids().getAmountPerUnit());
        helper.assertTrue(AEKeyType.fluids().getAmountPerUnit() == 1000, "fluids are not counted in buckets");

        var counter = new KeyCounter();
        var paper = AEItemKey.of(Items.PAPER);
        var sugar = AEItemKey.of(Items.SUGAR);
        var coal = AEItemKey.of(Items.COAL);
        var water = AEFluidKey.of(Fluids.WATER);
        var lava = AEFluidKey.of(Fluids.LAVA);
        counter.add(paper, 500);
        counter.add(sugar, 5);
        counter.add(coal, 100);
        counter.add(water, 800_000);
        counter.add(lava, 1_500);

        var top = TopStacks.selectMixed(counter, 3,
                key -> key instanceof AEItemKey ? 10 : key instanceof AEFluidKey ? 2000 : Long.MAX_VALUE, null);
        var order = new ArrayList<AEKey>();
        while (!top.isEmpty()) order.add(0, top.poll().what());
        helper.assertTrue(order.equals(List.of(water, paper, coal)), "ranking: " + order);

        top = TopStacks.selectMixed(counter, 2,
                key -> key instanceof AEItemKey ? 10 : key instanceof AEFluidKey ? 2000 : Long.MAX_VALUE, null);
        order.clear();
        while (!top.isEmpty()) order.add(0, top.poll().what());
        helper.assertTrue(order.equals(List.of(water, paper)), "ranking with 2 slots: " + order);

        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void amountEntryFollowsTheInterfaceRules(GameTestHelper helper) {
        var model = new AmountEntryModel(Locale.ROOT);

        model.setKey(AEItemKey.of(Items.PAPER));
        helper.assertTrue(model.getAmountPerUnit() == 1, "items have no unit");
        helper.assertTrue(model.error("1") == null && model.error("64*4") == null && model.error("=2*3") == null,
                "valid item amounts rejected");
        helper.assertTrue(model.external("64*4").getAsLong() == 256 && model.external("=2*3").getAsLong() == 6,
                "expressions are not evaluated");
        helper.assertTrue(model.error("0") != null, "0 is below the minimum");
        helper.assertTrue(model.error("abc") != null, "text is not a number");
        helper.assertTrue(model.error("2147483648") != null, "above the maximum");
        helper.assertTrue(model.external("1.5").isEmpty(), "pieces must be whole numbers");
        helper.assertTrue(model.external(model.add("1", 10)).getAsLong() == 10, "1 + 10");
        helper.assertTrue(model.external(model.add("5", 10)).getAsLong() == 15, "5 + 10");
        helper.assertTrue(model.external(model.add("1", 16)).getAsLong() == 16, "1 + 16");
        helper.assertTrue(model.external(model.add("10", -1000)).getAsLong() == 1, "10 - 1000 stays at the minimum");
        helper.assertTrue(model.external(model.add("abc", 32)).getAsLong() == 32, "no number counts as 0");

        model.setKey(AEFluidKey.of(Fluids.WATER));
        helper.assertTrue(model.getAmountPerUnit() == 1000, "fluids are counted in buckets");
        helper.assertTrue(model.external("1").getAsLong() == 1000, "1 bucket");
        helper.assertTrue(model.error("1.5") == null && model.error("2*0.25") == null, "decimals rejected");
        helper.assertTrue(model.external("2*0.25").getAsLong() == 500, "half a bucket by expression");
        helper.assertTrue(model.external(model.format(1500)).orElse(-1) == 1500, "format/parse round trip: '" +
                model.format(1500) + "' -> " + model.external(model.format(1500)) + " error=" +
                model.error(model.format(1500)) + " locale=" + Locale.getDefault());
        helper.assertTrue(model.error("0") != null, "0 is below the minimum");
        helper.assertTrue(model.error("3000000") != null, "more than the maximum number of mB");
        helper.assertTrue(model.external(model.add("1", 100)).getAsLong() == 100_000, "1 B + 100 B");

        helper.succeed();
    }
}
