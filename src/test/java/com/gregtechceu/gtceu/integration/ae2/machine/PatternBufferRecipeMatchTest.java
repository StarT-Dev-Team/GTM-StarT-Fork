package com.gregtechceu.gtceu.integration.ae2.machine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.RecipeRunner;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.machines.GTAEMachines;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.gregtechceu.gtceu.gametest.util.TestUtils;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEFluidList;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemList;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static com.gregtechceu.gtceu.gametest.util.TestUtils.getMetaMachine;

@PrefixGameTestTemplate(false)
@GameTestHolder(GTCEu.MOD_ID)
public class PatternBufferRecipeMatchTest {

    private static final String BATCH = "PatternBufferRecipeMatch";
    private static final String T = "patternbuffertest";

    private static final Map<String, GTRecipeType> SKIP_TYPES = new HashMap<>();
    private static final Map<String, GTRecipe> SKIP_RECIPES = new HashMap<>();
    private static final Consumer<MEPatternBufferPartMachine> NO_SETUP = buffer -> {};
    private static final Consumer<ItemBusPartMachine> NO_BUS = bus -> {};

    private static GTRecipe ITEM_RECIPE;
    private static GTRecipe FLUID_RECIPE;
    private static GTRecipe PROXY_RECIPE;

    private record Input(AEKey key, long amount) {}

    private record Pattern(IPatternDetails details, KeyCounter[] holders) {}

    @BeforeBatch(batch = BATCH)
    public static void prepare(ServerLevel level) {
        register("items", builder -> builder.inputItems(new ItemStack(Items.PAPER, 4))
                .inputItems(new ItemStack(Items.SUGAR, 2)));
        register("item_fluid", builder -> builder.inputItems(new ItemStack(Items.PAPER))
                .inputFluids(new FluidStack(Fluids.WATER, 1000)));
        register("tag", builder -> builder.inputItems(ItemTags.WOOL, 2));
        register("circuit", builder -> builder.circuitMeta(3).inputItems(new ItemStack(Items.PAPER)));
        register("shared_item", builder -> builder.inputItems(new ItemStack(Items.PAPER))
                .inputItems(new ItemStack(Items.SUGAR)));
        register("shared_fluid", builder -> builder.inputItems(new ItemStack(Items.PAPER))
                .inputFluids(new FluidStack(Fluids.WATER, 1000)));
        register("missing", builder -> builder.inputItems(new ItemStack(Items.PAPER))
                .inputItems(new ItemStack(Items.SUGAR)));
        register("bus_item", builder -> builder.inputItems(new ItemStack(Items.PAPER))
                .inputItems(new ItemStack(Items.SUGAR)));

        var type = TestUtils.createRecipeType("pb_match", GTRecipeTypes.LARGE_CHEMICAL_RECIPES);

        ITEM_RECIPE = type.recipeBuilder(GTCEu.id("pb_match_item")).id(GTCEu.id("pb_match_item"))
                .inputItems(new ItemStack(Items.PAPER)).inputItems(new ItemStack(Items.SUGAR))
                .outputItems(new ItemStack(Items.DIRT)).EUt(GTValues.V[GTValues.HV]).duration(1).buildRawRecipe();
        FLUID_RECIPE = type.recipeBuilder(GTCEu.id("pb_match_fluid")).id(GTCEu.id("pb_match_fluid"))
                .inputItems(new ItemStack(Items.PAPER)).inputFluids(new FluidStack(Fluids.WATER, 1000))
                .outputItems(new ItemStack(Items.DIRT)).EUt(GTValues.V[GTValues.HV]).duration(1).buildRawRecipe();
        PROXY_RECIPE = type.recipeBuilder(GTCEu.id("pb_match_proxy")).id(GTCEu.id("pb_match_proxy"))
                .inputItems(new ItemStack(Items.PAPER)).outputItems(new ItemStack(Items.DIRT))
                .EUt(GTValues.V[GTValues.HV]).duration(1).buildRawRecipe();
    }

    private static void register(String name, UnaryOperator<GTRecipeBuilder> inputs) {
        var type = TestUtils.createRecipeType("pb_skip_" + name, GTRecipeTypes.LARGE_CHEMICAL_RECIPES);
        var id = GTCEu.id("pb_skip_" + name);
        var recipe = inputs.apply(type.recipeBuilder(id).id(id)).outputItems(new ItemStack(Items.DIRT))
                .EUt(GTValues.V[GTValues.HV]).duration(1).buildRawRecipe();

        type.getLookup().addRecipe(recipe);

        SKIP_TYPES.put(name, type);
        SKIP_RECIPES.put(name, recipe);
    }

    private static Pattern encode(GameTestHelper helper, MEPatternBufferPartMachine buffer, String name,
                                  GenericStack... inputStacks) {
        var outputStacks = new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIRT), 1) };
        var stack = PatternDetailsHelper.encodeProcessingPattern(inputStacks, outputStacks, name);

        buffer.getTerminalPatternInventory().setItemDirect(0, stack);

        var details = PatternDetailsHelper.decodePattern(stack, helper.getLevel());

        helper.assertTrue(details != null, "Could not decode pattern");

        var holders = new KeyCounter[details.getInputs().length];

        for (int i = 0; i < holders.length; i++) {
            holders[i] = new KeyCounter();

            var possible = details.getInputs()[i].getPossibleInputs()[0];

            holders[i].add(possible.what(), possible.amount() * details.getInputs()[i].getMultiplier());
        }

        return new Pattern(details, holders);
    }

    private static Input item(Item item, long amount) {
        return new Input(AEItemKey.of(item), amount);
    }

    private static void runSkipCase(GameTestHelper helper, String name, boolean expectStart, Input[] inputs,
                                    Consumer<MEPatternBufferPartMachine> setup, Consumer<ItemBusPartMachine> busSetup) {
        var controller = (WorkableMultiblockMachine) getMetaMachine(helper.getBlockEntity(new BlockPos(1, 2, 0)));

        TestUtils.formMultiblock(controller);

        controller.setRecipeType(SKIP_TYPES.get(name));

        var buffer = (MEPatternBufferPartMachine) getMetaMachine(helper.getBlockEntity(new BlockPos(2, 2, 1)));
        var bus = (ItemBusPartMachine) getMetaMachine(helper.getBlockEntity(new BlockPos(2, 1, 0)));
        var inputStacks = new GenericStack[inputs.length];

        for (int i = 0; i < inputs.length; i++) {
            inputStacks[i] = new GenericStack(inputs[i].key(), inputs[i].amount());
        }

        var pattern = encode(helper, buffer, "pb_skip_" + name, inputStacks);

        helper.runAfterDelay(2, () -> {
            setup.accept(buffer);
            busSetup.accept(bus);

            helper.assertTrue(buffer.pushPattern(pattern.details(), pattern.holders()), "Push was rejected");

            var result = RecipeHelper.matchContents(controller, SKIP_RECIPES.get(name));

            helper.assertTrue(result.isSuccess() == expectStart, "Case '" + name + "': match " +
                    (result.isSuccess() ? "succeeded" : "failed") + ", expected " +
                    (expectStart ? "success" : "failure") + " | " + diagnose(controller));
            helper.succeed();
        });
    }

    private static String diagnose(WorkableMultiblockMachine controller) {
        try {
            var logic = controller.getRecipeLogic();
            var field = RecipeLogic.class.getDeclaredField("failureReasonMap");

            field.setAccessible(true);

            return "status=" + logic.getStatus() + " formed=" + controller.isFormed() + " enabled=" +
                    controller.isWorkingEnabled() + " failures=" + field.get(logic) + " lookup=" +
                    controller.getRecipeType().getLookup().find(controller, recipe -> true) + " lastFailed=" +
                    (logic.lastFailedMatches == null ? "null" : logic.lastFailedMatches.size());
        } catch (ReflectiveOperationException e) {
            return e.toString();
        }
    }

    private static void runPartCase(GameTestHelper helper, MachineDefinition part, GTRecipe recipe, boolean supply,
                                    Consumer<Object> stock) {
        var controller = (WorkableMultiblockMachine) getMetaMachine(helper.getBlockEntity(new BlockPos(1, 2, 0)));
        var buffer = (MEPatternBufferPartMachine) getMetaMachine(helper.getBlockEntity(new BlockPos(2, 2, 1)));
        var pattern = encode(helper, buffer, "pb_match", new GenericStack(AEItemKey.of(Items.PAPER), 1));

        helper.runAfterDelay(2, () -> {
            var machine = TestUtils.setMachine(helper, new BlockPos(2, 2, 0), part);

            TestUtils.formMultiblock(controller);

            helper.assertTrue(controller.getParts().contains(machine), part.getId() + " is not part of the multiblock");
            helper.assertTrue(buffer.pushPattern(pattern.details(), pattern.holders()), "Push was rejected");

            if (supply) stock.accept(machine);

            var result = RecipeHelper.matchContents(controller, recipe);

            helper.assertTrue(result.isSuccess() == supply, part.getId() + ": match " +
                    (result.isSuccess() ? "succeeded" : "failed") + ", expected " +
                    (supply ? "success (the ME part supplies the rest)" : "failure"));
            helper.succeed();
        });
    }

    private static Consumer<Object> stockItems() {
        return machine -> ((ExportOnlyAEItemList) ((ItemBusPartMachine) machine).getInventory()).getInventory()[0]
                .setStock(new GenericStack(AEItemKey.of(Items.SUGAR), 100));
    }

    private static Consumer<Object> stockFluids() {
        return machine -> ((ExportOnlyAEFluidList) ((FluidHatchPartMachine) machine).tank).getInventory()[0]
                .setStock(new GenericStack(AEFluidKey.of(Fluids.WATER), 4000));
    }

    private static Map<RecipeCapability<?>, List<Object>> contents(RecipeCapability<?> capability,
                                                                   Object ingredient) {
        Map<RecipeCapability<?>, List<Object>> map = new HashMap<>();

        map.put(capability, new ArrayList<>(List.of(ingredient)));

        return map;
    }

    @GameTest(template = T, batch = BATCH, setupTicks = 40, timeoutTicks = 200)
    public static void skipItemsOnly(GameTestHelper helper) {
        runSkipCase(helper, "items", true, new Input[] { item(Items.PAPER, 4), item(Items.SUGAR, 2) }, NO_SETUP,
                NO_BUS);
    }

    @GameTest(template = T, batch = BATCH, setupTicks = 40, timeoutTicks = 200)
    public static void skipItemAndFluid(GameTestHelper helper) {
        runSkipCase(helper, "item_fluid", true,
                new Input[] { item(Items.PAPER, 1), new Input(AEFluidKey.of(Fluids.WATER), 1000) }, NO_SETUP,
                NO_BUS);
    }

    @GameTest(template = T, batch = BATCH, setupTicks = 40, timeoutTicks = 200)
    public static void skipTagIngredient(GameTestHelper helper) {
        runSkipCase(helper, "tag", true, new Input[] { item(Items.WHITE_WOOL, 2) }, NO_SETUP, NO_BUS);
    }

    @GameTest(template = T, batch = BATCH, setupTicks = 40, timeoutTicks = 200)
    public static void skipCircuit(GameTestHelper helper) {
        runSkipCase(helper, "circuit", true, new Input[] { item(Items.PAPER, 1) },
                buffer -> buffer.getCircuitInventory().setStackInSlot(0, IntCircuitBehaviour.stack(3)), NO_BUS);
    }

    @GameTest(template = T, batch = BATCH, setupTicks = 40, timeoutTicks = 200)
    public static void skipSharedInventory(GameTestHelper helper) {
        runSkipCase(helper, "shared_item", true, new Input[] { item(Items.PAPER, 1) },
                buffer -> buffer.getShareInventory().storage.setStackInSlot(0, new ItemStack(Items.SUGAR)), NO_BUS);
    }

    @GameTest(template = T, batch = BATCH, setupTicks = 40, timeoutTicks = 200)
    public static void skipSharedTank(GameTestHelper helper) {
        runSkipCase(helper, "shared_fluid", true, new Input[] { item(Items.PAPER, 1) },
                buffer -> buffer.getShareTank().getStorages()[0].setFluid(new FluidStack(Fluids.WATER, 4000)),
                NO_BUS);
    }

    @GameTest(template = T, batch = BATCH, setupTicks = 40, timeoutTicks = 200)
    public static void skipMissingInputStillFails(GameTestHelper helper) {
        runSkipCase(helper, "missing", false, new Input[] { item(Items.PAPER, 1) }, NO_SETUP, NO_BUS);
    }

    @GameTest(template = T, batch = BATCH, setupTicks = 40, timeoutTicks = 200)
    public static void skipBusSuppliesMissingInput(GameTestHelper helper) {
        runSkipCase(helper, "bus_item", true, new Input[] { item(Items.PAPER, 1) }, NO_SETUP,
                bus -> bus.getInventory().setStackInSlot(0, new ItemStack(Items.SUGAR)));
    }

    @GameTest(template = T, batch = BATCH, setupTicks = 40, timeoutTicks = 100)
    public static void meInputBusSuppliesMissingItem(GameTestHelper helper) {
        runPartCase(helper, GTAEMachines.ITEM_IMPORT_BUS_ME, ITEM_RECIPE, true, stockItems());
    }

    @GameTest(template = T, batch = BATCH, setupTicks = 40, timeoutTicks = 100)
    public static void meInputBusWithoutStockStillFails(GameTestHelper helper) {
        runPartCase(helper, GTAEMachines.ITEM_IMPORT_BUS_ME, ITEM_RECIPE, false, stockItems());
    }

    @GameTest(template = T, batch = BATCH, setupTicks = 40, timeoutTicks = 100)
    public static void meInputHatchSuppliesMissingFluid(GameTestHelper helper) {
        runPartCase(helper, GTAEMachines.FLUID_IMPORT_HATCH_ME, FLUID_RECIPE, true, stockFluids());
    }

    @GameTest(template = T, batch = BATCH, setupTicks = 40, timeoutTicks = 100)
    public static void stockingPartsAlwaysCountAsSuppliers(GameTestHelper helper) {
        var bus = (ItemBusPartMachine) TestUtils.setMachine(helper, new BlockPos(2, 2, 0),
                GTAEMachines.STOCKING_IMPORT_BUS_ME);

        helper.assertTrue(RecipeRunner.mayContribute(bus.getRecipeHandlers(), ItemRecipeCapability.CAP),
                "An empty ME stocking input bus must still count as able to supply items");

        var hatch = (FluidHatchPartMachine) TestUtils.setMachine(helper, new BlockPos(2, 1, 0),
                GTAEMachines.STOCKING_IMPORT_HATCH_ME);

        helper.assertTrue(RecipeRunner.mayContribute(hatch.getRecipeHandlers(), FluidRecipeCapability.CAP),
                "An empty ME stocking input hatch must still count as able to supply fluids");

        var meBus = (ItemBusPartMachine) TestUtils.setMachine(helper, new BlockPos(2, 0, 0),
                GTAEMachines.ITEM_IMPORT_BUS_ME);

        helper.assertTrue(RecipeRunner.mayContribute(meBus.getRecipeHandlers(), ItemRecipeCapability.CAP),
                "An empty ME input bus must still count as able to supply items");
        helper.succeed();
    }

    @GameTest(template = T, batch = BATCH, setupTicks = 40, timeoutTicks = 100)
    public static void plainPartsOnlyCountWhenFilled(GameTestHelper helper) {
        var bus = (ItemBusPartMachine) getMetaMachine(helper.getBlockEntity(new BlockPos(2, 1, 0)));

        bus.getInventory().storage.setStackInSlot(0, ItemStack.EMPTY);

        helper.assertTrue(!RecipeRunner.mayContribute(bus.getRecipeHandlers(), ItemRecipeCapability.CAP),
                "An empty plain bus must not count as able to supply items");

        bus.getInventory().storage.setStackInSlot(0, new ItemStack(Items.SUGAR));

        helper.assertTrue(RecipeRunner.mayContribute(bus.getRecipeHandlers(), ItemRecipeCapability.CAP),
                "A filled plain bus must count as able to supply items");
        helper.succeed();
    }

    @GameTest(template = T, batch = BATCH, setupTicks = 40, timeoutTicks = 100)
    public static void proxyMirrorsBufferSlot(GameTestHelper helper) {
        var controller = (WorkableMultiblockMachine) getMetaMachine(helper.getBlockEntity(new BlockPos(1, 2, 0)));
        var buffer = (MEPatternBufferPartMachine) getMetaMachine(helper.getBlockEntity(new BlockPos(2, 2, 1)));
        var pattern = encode(helper, buffer, "pb_match_proxy", new GenericStack(AEItemKey.of(Items.PAPER), 1),
                new GenericStack(AEFluidKey.of(Fluids.WATER), 1000));

        helper.runAfterDelay(2, () -> {
            var proxy = (MEPatternBufferProxyPartMachine) TestUtils.setMachine(helper, new BlockPos(2, 2, 0),
                    GTAEMachines.ME_PATTERN_BUFFER_PROXY);

            proxy.setBuffer(helper.absolutePos(new BlockPos(2, 2, 1)));

            TestUtils.formMultiblock(controller);

            helper.assertTrue(proxy.getBuffer() == buffer, "Proxy did not resolve its buffer");
            helper.assertTrue(buffer.pushPattern(pattern.details(), pattern.holders()), "Push was rejected");

            var slot0 = proxy.getRecipeHandlers().get(0);
            var slot1 = proxy.getRecipeHandlers().get(1);
            var paper = SizedIngredient.create(new ItemStack(Items.PAPER));
            var sugar = SizedIngredient.create(new ItemStack(Items.SUGAR));
            var water = FluidIngredient.of(Fluids.WATER, 1000);
            var lava = FluidIngredient.of(Fluids.LAVA, 1000);

            helper.assertTrue(!slot0.cannotSupplyItems(List.of(paper), new HashMap<>()), "slot 0 should supply paper");
            helper.assertTrue(slot0.cannotSupplyItems(List.of(sugar), new HashMap<>()), "slot 0 must not supply sugar");
            helper.assertTrue(!slot0.cannotSupplyFluids(List.of(water), new HashMap<>()), "slot 0 should supply water");
            helper.assertTrue(slot0.cannotSupplyFluids(List.of(lava), new HashMap<>()), "slot 0 must not supply lava");
            helper.assertTrue(slot1.cannotSupplyItems(List.of(paper), new HashMap<>()),
                    "empty slot 1 must not supply paper");

            var itemCapability = ItemRecipeCapability.CAP;
            var fluidCapability = FluidRecipeCapability.CAP;

            helper.assertTrue(
                    slot0.handleRecipe(IO.IN, PROXY_RECIPE, contents(itemCapability, paper), true).isEmpty(),
                    "proxy should satisfy paper from slot 0");
            helper.assertTrue(
                    !slot0.handleRecipe(IO.IN, PROXY_RECIPE, contents(itemCapability, sugar), true).isEmpty(),
                    "proxy must not satisfy sugar");
            helper.assertTrue(
                    slot0.handleRecipe(IO.IN, PROXY_RECIPE, contents(fluidCapability, water), true).isEmpty(),
                    "proxy should satisfy water from slot 0");
            helper.assertTrue(
                    !slot1.handleRecipe(IO.IN, PROXY_RECIPE, contents(itemCapability, paper), true).isEmpty(),
                    "empty slot 1 must not satisfy paper");

            long before = buffer.mergeInternalSlots().items().values().longStream().sum();

            helper.assertTrue(
                    slot0.handleRecipe(IO.IN, PROXY_RECIPE, contents(itemCapability, paper), false).isEmpty(),
                    "proxy should consume paper from slot 0");

            long after = buffer.mergeInternalSlots().items().values().longStream().sum();

            helper.assertTrue(before == 1 && after == 0,
                    "Buffer slot not drained through the proxy: " + before + " -> " + after);
            helper.succeed();
        });
    }
}
