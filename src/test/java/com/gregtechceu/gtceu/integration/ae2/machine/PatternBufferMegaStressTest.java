package com.gregtechceu.gtceu.integration.ae2.machine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.machines.GTAEMachines;
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

import java.util.*;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static com.gregtechceu.gtceu.gametest.util.TestUtils.getMetaMachine;

@PrefixGameTestTemplate(false)
@GameTestHolder(GTCEu.MOD_ID)
public class PatternBufferMegaStressTest {

    private static final String BATCH = "PatternBufferMegaStress";

    private static final int PATTERNS = MEPatternBufferPartMachine.BASE_PATTERN_COUNT;
    private static final int MAX_PATTERN_INPUTS = 6;
    private static final int ITERATIONS = 30;
    private static final int WARMUP = 8;
    private static final int WATER_EVERY = 9;
    private static final int BIND_DELAY = 2;
    private static final int FIRST_ITERATION_DELAY = 6;

    private record Config(int patternInputs, long pushAmount, int recipes, int recipeAmount) {}

    private static final Config[] CONFIGS = {
            new Config(2, 1, 500, 64),
            new Config(3, 16, 1000, 64),
            new Config(4, 64, 1500, 128),
            new Config(5, 1_000, 1500, 1_000),
            new Config(6, 100_000, 2000, 5_000),
            new Config(3, 1_500_000_000L, 2500, 64),
    };

    private static final int INSTANCES = CONFIGS.length;

    private static final GTRecipeType[] RECIPE_TYPES = new GTRecipeType[INSTANCES];
    private static List<Item> ITEM_POOL;
    private static final AtomicReferenceArray<BlockPos> BUFFER_POSITIONS = new AtomicReferenceArray<>(INSTANCES);
    private static final long[][] TIMINGS = new long[INSTANCES][];
    private static final Object REPORT_LOCK = new Object();

    @BeforeBatch(batch = BATCH)
    public static void prepare(ServerLevel level) {
        for (int i = 0; i < INSTANCES; i++) {
            BUFFER_POSITIONS.set(i, null);
            TIMINGS[i] = null;
        }

        ITEM_POOL = BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> id.getNamespace().equals("minecraft"))
                .sorted()
                .map(BuiltInRegistries.ITEM::get)
                .filter(item -> item != Items.AIR)
                .skip(50)
                .limit((long) PATTERNS * MAX_PATTERN_INPUTS)
                .toList();

        int total = 0;
        for (int i = 0; i < INSTANCES; i++) {
            RECIPE_TYPES[i] = TestUtils.createRecipeType("pattern_buffer_mega_" + i,
                    GTRecipeTypes.LARGE_CHEMICAL_RECIPES);
            total += registerRecipes(i, CONFIGS[i]);
        }

        GTCEu.LOGGER.info("[PatternBufferMegaStress] registered {} stress recipes over {} recipe types", total,
                INSTANCES);
    }

    private static int registerRecipes(int instance, Config config) {
        var random = new Random(1234L + instance);
        Set<List<Integer>> seen = new HashSet<>();
        int added = 0;
        for (int attempts = 0; added < config.recipes && attempts < config.recipes * 20; attempts++) {
            int[] slots = { random.nextInt(PATTERNS), random.nextInt(PATTERNS), random.nextInt(PATTERNS) };

            if (slots[0] == slots[1] || slots[1] == slots[2] || slots[0] == slots[2]) continue;
            var items = new int[3];
            for (int i = 0; i < 3; i++) items[i] = slots[i] * MAX_PATTERN_INPUTS + random.nextInt(config.patternInputs);
            Arrays.sort(items);
            if (!seen.add(List.of(items[0], items[1], items[2]))) continue;

            var id = GTCEu.id("pattern_buffer_mega_" + instance + "_" + added);
            var builder = RECIPE_TYPES[instance].recipeBuilder(id).id(id);
            for (int item : items) builder.inputItems(new ItemStack(ITEM_POOL.get(item), config.recipeAmount));
            var recipe = builder.inputFluids(new FluidStack(Fluids.WATER, 1000))
                    .outputItems(new ItemStack(Items.DIRT))
                    .EUt(GTValues.V[GTValues.EV])
                    .duration(1)
                    .buildRawRecipe();
            if (RECIPE_TYPES[instance].getLookup().addRecipe(recipe)) added++;
        }

        return added;
    }

    @GameTest(template = "patternbuffertest",
              batch = BATCH,
              setupTicks = 40,
              timeoutTicks = ITERATIONS + 600,
              required = false)
    public static void megaStress0(GameTestHelper helper) {
        run(helper, 0);
    }

    @GameTest(template = "patternbuffertest",
              batch = BATCH,
              setupTicks = 40,
              timeoutTicks = ITERATIONS + 600,
              required = false)
    public static void megaStress1(GameTestHelper helper) {
        run(helper, 1);
    }

    @GameTest(template = "patternbuffertest",
              batch = BATCH,
              setupTicks = 40,
              timeoutTicks = ITERATIONS + 600,
              required = false)
    public static void megaStress2(GameTestHelper helper) {
        run(helper, 2);
    }

    @GameTest(template = "patternbuffertest",
              batch = BATCH,
              setupTicks = 40,
              timeoutTicks = ITERATIONS + 600,
              required = false)
    public static void megaStress3(GameTestHelper helper) {
        run(helper, 3);
    }

    @GameTest(template = "patternbuffertest",
              batch = BATCH,
              setupTicks = 40,
              timeoutTicks = ITERATIONS + 600,
              required = false)
    public static void megaStress4(GameTestHelper helper) {
        run(helper, 4);
    }

    @GameTest(template = "patternbuffertest",
              batch = BATCH,
              setupTicks = 40,
              timeoutTicks = ITERATIONS + 600,
              required = false)
    public static void megaStress5(GameTestHelper helper) {
        run(helper, 5);
    }

    private static void run(GameTestHelper helper, int instance) {
        if (!SlowTests.enabled()) {
            helper.succeed();

            return;
        }

        var config = CONFIGS[instance];
        var level = helper.getLevel();
        var controllerPos = new BlockPos(1, 2, 0);
        var bufferPos = new BlockPos(2, 2, 1);
        var inputBusPos = new BlockPos(2, 2, 0);

        var controller = (WorkableMultiblockMachine) getMetaMachine(helper.getBlockEntity(controllerPos));
        TestUtils.formMultiblock(controller);
        controller.setRecipeType(RECIPE_TYPES[instance]);
        var buffer = (MEPatternBufferPartMachine) getMetaMachine(helper.getBlockEntity(bufferPos));

        var details = new IPatternDetails[PATTERNS];
        var holders = new KeyCounter[PATTERNS][];
        for (int s = 0; s < PATTERNS; s++) {
            boolean withWater = s % WATER_EVERY == 0;
            var inputStacks = new GenericStack[config.patternInputs + (withWater ? 1 : 0)];
            for (int k = 0; k < config.patternInputs; k++) {
                inputStacks[k] = new GenericStack(AEItemKey.of(ITEM_POOL.get(s * MAX_PATTERN_INPUTS + k)), 1);
            }

            if (withWater) inputStacks[config.patternInputs] = new GenericStack(AEFluidKey.of(Fluids.WATER), 1);
            var outputStacks = new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIRT), 1) };

            var stack = PatternDetailsHelper.encodeProcessingPattern(inputStacks, outputStacks,
                    "pattern_buffer_mega_stress");
            buffer.getTerminalPatternInventory().setItemDirect(s, stack);

            details[s] = PatternDetailsHelper.decodePattern(stack, level);
            helper.assertTrue(details[s] != null, "Could not decode stress pattern " + s);

            var inputs = details[s].getInputs();
            holders[s] = new KeyCounter[inputs.length];
            for (int j = 0; j < inputs.length; j++) {
                holders[s][j] = new KeyCounter();
                var possible = inputs[j].getPossibleInputs()[0];
                holders[s][j].add(possible.what(),
                        possible.amount() * inputs[j].getMultiplier() * config.pushAmount);
            }
        }

        BUFFER_POSITIONS.set(instance, helper.absolutePos(bufferPos));

        helper.runAfterDelay(BIND_DELAY, () -> {
            var target = BUFFER_POSITIONS.get((instance + 1) % INSTANCES);
            if (target == null) {
                helper.fail("Instance " + (instance + 1) % INSTANCES + " did not register its buffer in time");

                return;
            }

            var proxy = (MEPatternBufferProxyPartMachine) TestUtils.setMachine(helper, inputBusPos,
                    GTAEMachines.ME_PATTERN_BUFFER_PROXY);
            proxy.setBuffer(target);
            TestUtils.formMultiblock(controller);
            helper.assertTrue(proxy.getBuffer() != null, "Proxy did not resolve its buffer");
            helper.assertTrue(controller.getParts().contains(proxy), "Proxy is not part of the formed multiblock");
        });

        var logic = controller.getRecipeLogic();
        var nanos = new long[ITERATIONS];
        int[] rejectedPushes = { 0 };

        for (int i = 0; i < ITERATIONS; i++) {
            final int iteration = i;
            helper.runAfterDelay(FIRST_ITERATION_DELAY + i, () -> {
                int slot = iteration % PATTERNS;
                if (!buffer.pushPattern(details[slot], holders[slot])) rejectedPushes[0]++;

                long start = System.nanoTime();
                logic.findAndHandleRecipe();
                nanos[iteration] = System.nanoTime() - start;
                if (iteration % 5 == 0) {
                    GTCEu.LOGGER.info("[PatternBufferMegaStress] instance {} iteration {}/{}: {}ms", instance,
                            iteration, ITERATIONS, ms(nanos[iteration]));
                }

                if (logic.isActive()) {
                    helper.fail("A stress recipe started, but none should be satisfiable by a single slot");
                }

                if (iteration == ITERATIONS - 1) {
                    finish(helper, instance, nanos, rejectedPushes[0]);
                }
            });
        }
    }

    private static void finish(GameTestHelper helper, int instance, long[] nanos, int rejectedPushes) {
        var timed = Arrays.copyOfRange(nanos, WARMUP, nanos.length);
        Arrays.sort(timed);
        var config = CONFIGS[instance];
        GTCEu.LOGGER.info(
                "[PatternBufferMegaStress] instance {} (inputs={} push={} recipes={} amount={}): {} timed calls, " +
                        "{} pushes rejected: avg={}ms p50={}ms p95={}ms max={}ms",
                instance, config.patternInputs, config.pushAmount, config.recipes, config.recipeAmount, timed.length,
                rejectedPushes, ms(average(timed)), ms(timed[timed.length / 2]),
                ms(timed[(int) (timed.length * 0.95)]), ms(timed[timed.length - 1]));

        synchronized (REPORT_LOCK) {
            TIMINGS[instance] = timed;
            if (Arrays.stream(TIMINGS).allMatch(Objects::nonNull)) {
                var all = Arrays.stream(TIMINGS).flatMapToLong(Arrays::stream).sorted().toArray();
                GTCEu.LOGGER.info(
                        "[PatternBufferMegaStress] ALL {} instances, {} timed calls: avg={}ms p50={}ms p95={}ms " +
                                "max={}ms; server average tick={}ms (budget 50ms)",
                        INSTANCES, all.length, ms(average(all)), ms(all[all.length / 2]),
                        ms(all[(int) (all.length * 0.95)]), ms(all[all.length - 1]),
                        String.format(Locale.ROOT, "%.1f", helper.getLevel().getServer().getAverageTickTime()));
            }
        }

        helper.assertTrue(rejectedPushes < ITERATIONS / 2,
                "Most pushes were rejected (" + rejectedPushes + "); is the buffer formed and online?");

        helper.succeed();
    }

    private static double average(long[] values) {
        return Arrays.stream(values).average().orElse(0);
    }

    private static String ms(double nanos) {
        return String.format(Locale.ROOT, "%.3f", nanos * 1e-6);
    }
}
