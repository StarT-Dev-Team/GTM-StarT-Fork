package com.gregtechceu.gtceu.integration.ae2.machine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.gametest.util.TestUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.IntFunction;

import static com.gregtechceu.gtceu.gametest.util.TestUtils.getMetaMachine;

@PrefixGameTestTemplate(false)
@GameTestHolder(GTCEu.MOD_ID)
public class PatternBufferStressTest {

    private static final String ITEM_BATCH = "PatternBufferItemStress";
    private static final String FLUID_BATCH = "PatternBufferFluidStress";
    private static final int PATTERNS = MEPatternBufferPartMachine.BASE_PATTERN_COUNT;
    private static final int INPUTS_PER_PATTERN = 3;
    private static final int ITEM_RECIPES = 1500;
    private static final int ITEM_ITERATIONS = 400;
    private static final int ITEM_WARMUP = 100;
    private static final int FLUID_RECIPES = 800;
    private static final int FLUID_ITERATIONS = 250;
    private static final int FLUID_WARMUP = 60;

    private static GTRecipeType ITEM_TYPE;
    private static GTRecipeType FLUID_TYPE;
    private static List<Item> ITEM_POOL;
    private static List<Fluid> FLUID_POOL;

    @BeforeBatch(batch = ITEM_BATCH)
    public static void prepareItems(ServerLevel level) {
        ITEM_TYPE = TestUtils.createRecipeType("pattern_buffer_stress", GTRecipeTypes.LARGE_CHEMICAL_RECIPES);

        createItemPool();

        var random = new Random(1234);
        Set<List<Integer>> seen = new HashSet<>();
        int added = 0;

        for (int attempts = 0; added < ITEM_RECIPES && attempts < ITEM_RECIPES * 20; attempts++) {
            int[] slots = { random.nextInt(PATTERNS), random.nextInt(PATTERNS), random.nextInt(PATTERNS) };

            if (slots[0] == slots[1] || slots[1] == slots[2] || slots[0] == slots[2]) continue;

            var items = new int[3];

            for (int i = 0; i < 3; i++) items[i] = slots[i] * INPUTS_PER_PATTERN + random.nextInt(INPUTS_PER_PATTERN);

            Arrays.sort(items);

            if (!seen.add(List.of(items[0], items[1], items[2]))) continue;

            var id = GTCEu.id("pattern_buffer_stress_" + added);
            var builder = ITEM_TYPE.recipeBuilder(id).id(id);

            for (int item : items) builder.inputItems(new ItemStack(ITEM_POOL.get(item), 64));

            var recipe = builder.outputItems(new ItemStack(Items.DIRT)).EUt(GTValues.V[GTValues.EV]).duration(1)
                    .buildRawRecipe();

            if (ITEM_TYPE.getLookup().addRecipe(recipe)) added++;
        }

        GTCEu.LOGGER.info("[PatternBufferStress] registered {} item stress recipes", added);
    }

    @BeforeBatch(batch = FLUID_BATCH)
    public static void prepareFluids(ServerLevel level) {
        FLUID_TYPE = TestUtils.createRecipeType("pattern_buffer_fluid_stress", GTRecipeTypes.LARGE_CHEMICAL_RECIPES);

        createItemPool();

        FLUID_POOL = BuiltInRegistries.FLUID.stream()
                .filter(fluid -> fluid.isSource(fluid.defaultFluidState()) && fluid != Fluids.EMPTY)
                .limit((long) PATTERNS * 2)
                .toList();

        var random = new Random(1234);
        Set<List<Integer>> seen = new HashSet<>();
        int added = 0;

        for (int attempts = 0; added < FLUID_RECIPES && attempts < FLUID_RECIPES * 20; attempts++) {
            int[] slots = { random.nextInt(PATTERNS), random.nextInt(PATTERNS), random.nextInt(PATTERNS) };

            if (slots[0] == slots[1] || slots[1] == slots[2] || slots[0] == slots[2]) continue;
            if (!seen.add(List.of(slots[0], slots[1], slots[2]))) continue;

            var id = GTCEu.id("pattern_buffer_fluid_stress_" + added);
            var builder = FLUID_TYPE.recipeBuilder(id).id(id);

            builder.inputItems(new ItemStack(ITEM_POOL.get(slots[0]), 64));
            builder.inputFluids(new FluidStack(FLUID_POOL.get(slots[1] * 2), 1000));
            builder.inputFluids(new FluidStack(FLUID_POOL.get(slots[2] * 2 + 1), 1000));

            var recipe = builder.outputItems(new ItemStack(Items.DIRT)).EUt(GTValues.V[GTValues.HV]).duration(1)
                    .buildRawRecipe();

            if (FLUID_TYPE.getLookup().addRecipe(recipe)) added++;
        }

        GTCEu.LOGGER.info("[PatternBufferStress] registered {} fluid stress recipes", added);
    }

    private static void createItemPool() {
        ITEM_POOL = BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> id.getNamespace().equals("minecraft"))
                .sorted()
                .map(BuiltInRegistries.ITEM::get)
                .filter(item -> item != Items.AIR)
                .skip(50)
                .limit((long) PATTERNS * INPUTS_PER_PATTERN)
                .toList();
    }

    @GameTest(template = "patternbuffertest",
              batch = ITEM_BATCH,
              setupTicks = 40,
              timeoutTicks = ITEM_ITERATIONS + 400)
    public static void patternBufferItemStress(GameTestHelper helper) {
        runStress(helper, "items", ITEM_TYPE, ITEM_ITERATIONS, ITEM_WARMUP, slot -> {
            var inputStacks = new GenericStack[INPUTS_PER_PATTERN];

            for (int i = 0; i < INPUTS_PER_PATTERN; i++) {
                inputStacks[i] = new GenericStack(AEItemKey.of(ITEM_POOL.get(slot * INPUTS_PER_PATTERN + i)), 1);
            }

            return inputStacks;
        });
    }

    @GameTest(template = "patternbuffertest",
              batch = FLUID_BATCH,
              setupTicks = 40,
              timeoutTicks = FLUID_ITERATIONS + 400)
    public static void patternBufferFluidStress(GameTestHelper helper) {
        runStress(helper, "fluids", FLUID_TYPE, FLUID_ITERATIONS, FLUID_WARMUP, slot -> new GenericStack[] {
                new GenericStack(AEItemKey.of(ITEM_POOL.get(slot)), 1),
                new GenericStack(AEFluidKey.of(FLUID_POOL.get(slot * 2)), 1000),
                new GenericStack(AEFluidKey.of(FLUID_POOL.get(slot * 2 + 1)), 1000) });
    }

    private static void runStress(GameTestHelper helper, String name, GTRecipeType type, int iterations, int warmup,
                                  IntFunction<GenericStack[]> patternInputs) {
        if (!SlowTests.enabled()) {
            helper.succeed();

            return;
        }

        var controller = (WorkableMultiblockMachine) getMetaMachine(helper.getBlockEntity(new BlockPos(1, 2, 0)));

        TestUtils.formMultiblock(controller);

        controller.setRecipeType(type);

        var buffer = (MEPatternBufferPartMachine) getMetaMachine(helper.getBlockEntity(new BlockPos(2, 2, 1)));
        var details = new IPatternDetails[PATTERNS];
        var holders = new KeyCounter[PATTERNS][];

        for (int slot = 0; slot < PATTERNS; slot++) {
            var outputStacks = new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIRT), 1) };
            var stack = PatternDetailsHelper.encodeProcessingPattern(patternInputs.apply(slot), outputStacks,
                    "pattern_buffer_stress_" + name);

            buffer.getTerminalPatternInventory().setItemDirect(slot, stack);

            details[slot] = PatternDetailsHelper.decodePattern(stack, helper.getLevel());

            helper.assertTrue(details[slot] != null, "Could not decode stress pattern " + slot);

            var inputs = details[slot].getInputs();

            holders[slot] = new KeyCounter[inputs.length];

            for (int i = 0; i < inputs.length; i++) {
                holders[slot][i] = new KeyCounter();

                var possible = inputs[i].getPossibleInputs()[0];

                holders[slot][i].add(possible.what(), possible.amount() * inputs[i].getMultiplier());
            }
        }

        var logic = controller.getRecipeLogic();
        var nanos = new long[iterations];
        int[] rejectedPushes = { 0 };

        for (int i = 0; i < iterations; i++) {
            final int iteration = i;

            helper.runAfterDelay(i + 1, () -> {
                int slot = iteration % PATTERNS;

                if (!buffer.pushPattern(details[slot], holders[slot])) rejectedPushes[0]++;

                long start = System.nanoTime();

                logic.findAndHandleRecipe();

                nanos[iteration] = System.nanoTime() - start;

                if (logic.isActive()) {
                    helper.fail("A stress recipe started, but none should be satisfiable by a single slot");
                }

                if (iteration == iterations - 1) {
                    report(name, nanos, warmup, rejectedPushes[0]);

                    helper.assertTrue(rejectedPushes[0] < iterations / 2,
                            "Most pushes were rejected (" + rejectedPushes[0] + "); is the buffer formed and online?");
                    helper.succeed();
                }
            });
        }
    }

    private static void report(String name, long[] nanos, int warmup, int rejectedPushes) {
        var timed = Arrays.copyOfRange(nanos, warmup, nanos.length);

        Arrays.sort(timed);

        double toMs = 1e-6;
        double average = Arrays.stream(timed).average().orElse(0) * toMs;

        GTCEu.LOGGER.info("[PatternBufferStress] {}: findAndHandleRecipe over {} iterations ({} warm-up skipped, " +
                "{} pushes rejected): avg={}ms p50={}ms p95={}ms max={}ms (tick budget is 50ms)",
                name, timed.length, warmup, rejectedPushes, String.format("%.3f", average),
                String.format("%.3f", timed[timed.length / 2] * toMs),
                String.format("%.3f", timed[(int) (timed.length * 0.95)] * toMs),
                String.format("%.3f", timed[timed.length - 1] * toMs));
    }
}
