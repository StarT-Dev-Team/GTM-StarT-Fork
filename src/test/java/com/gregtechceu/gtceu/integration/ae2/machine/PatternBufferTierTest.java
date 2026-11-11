package com.gregtechceu.gtceu.integration.ae2.machine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
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
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static com.gregtechceu.gtceu.gametest.util.TestUtils.getMetaMachine;

@PrefixGameTestTemplate(false)
@GameTestHolder(GTCEu.MOD_ID)
public class PatternBufferTierTest {

    private static final String BATCH = "PatternBufferTier";
    private static final String T = "patternbuffertest";

    private static final int ITERATIONS = 60;
    private static final int WARMUP = 10;
    private static final int RECIPES = 1500;

    private static GTRecipe PAPER_RECIPE;
    private static List<Item> ITEM_POOL;
    private static final Map<Integer, GTRecipeType> BENCH_TYPES = new HashMap<>();

    private static int topTier() {
        int top = GTValues.LuV;
        for (int tier = GTValues.LuV; tier < GTAEMachines.ME_PATTERN_BUFFERS.length; tier++) {
            if (GTAEMachines.ME_PATTERN_BUFFERS[tier] != null) top = tier;
        }

        return top;
    }

    @BeforeBatch(batch = BATCH)
    public static void prepare(ServerLevel level) {
        var type = TestUtils.createRecipeType("pb_tier", GTRecipeTypes.LARGE_CHEMICAL_RECIPES);
        PAPER_RECIPE = type.recipeBuilder(GTCEu.id("pb_tier")).id(GTCEu.id("pb_tier"))
                .inputItems(new ItemStack(Items.PAPER)).outputItems(new ItemStack(Items.DIRT))
                .EUt(GTValues.V[GTValues.HV]).duration(1).buildRawRecipe();

        if (!SlowTests.enabled()) return;
        int maxSlots = MEPatternBufferPartMachine.patternCountForTier(topTier());
        ITEM_POOL = BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> id.getNamespace().equals("minecraft")).sorted()
                .map(BuiltInRegistries.ITEM::get).filter(item -> item != Items.AIR).skip(50)
                .limit((long) maxSlots * 3).toList();
        for (int tier = GTValues.LuV; tier <= topTier(); tier++) {
            if (GTAEMachines.ME_PATTERN_BUFFERS[tier] == null) continue;
            int slots = MEPatternBufferPartMachine.patternCountForTier(tier);
            var benchType = TestUtils.createRecipeType("pb_tier_bench_" + tier,
                    GTRecipeTypes.LARGE_CHEMICAL_RECIPES);
            var random = new Random(1234 + tier);
            Set<List<Integer>> seen = new HashSet<>();
            int added = 0;
            for (int attempts = 0; added < RECIPES && attempts < RECIPES * 20; attempts++) {
                int a = random.nextInt(slots), b = random.nextInt(slots), c = random.nextInt(slots);
                if (a == b || b == c || a == c) continue;
                var items = new int[] { a * 3 + random.nextInt(3), b * 3 + random.nextInt(3),
                        c * 3 + random.nextInt(3) };

                Arrays.sort(items);
                if (!seen.add(List.of(items[0], items[1], items[2]))) continue;
                var id = GTCEu.id("pb_tier_bench_" + tier + "_" + added);
                var builder = benchType.recipeBuilder(id).id(id);
                for (int item : items) builder.inputItems(new ItemStack(ITEM_POOL.get(item), 64));
                if (benchType.getLookup().addRecipe(builder.outputItems(new ItemStack(Items.DIRT))
                        .EUt(GTValues.V[GTValues.HV]).duration(1).buildRawRecipe())) {
                    added++;
                }
            }

            BENCH_TYPES.put(tier, benchType);
        }
    }

    private static MEPatternBufferPartMachine placeBuffer(GameTestHelper helper, BlockPos pos, int tier) {
        return (MEPatternBufferPartMachine) TestUtils.setMachine(helper, pos, GTAEMachines.ME_PATTERN_BUFFERS[tier]);
    }

    private static void putPaper(GameTestHelper helper, MEPatternBufferPartMachine buffer, int slot) {
        var inputStacks = new GenericStack[] { new GenericStack(AEItemKey.of(Items.PAPER), 1) };

        var outputStacks = new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIRT), 1) };

        var stack = PatternDetailsHelper.encodeProcessingPattern(inputStacks, outputStacks, "pb_tier");
        buffer.getTerminalPatternInventory().setItemDirect(slot, stack);
        var details = PatternDetailsHelper.decodePattern(stack, helper.getLevel());
        helper.assertTrue(details != null, "Could not decode the pattern");
        var holders = new KeyCounter[] { new KeyCounter() };

        holders[0].add(AEItemKey.of(Items.PAPER), 1);
        buffer.getInternalInventory()[slot].pushPattern(details, holders);
    }

    @GameTest(template = T, batch = BATCH, setupTicks = 20, timeoutTicks = 100)
    public static void proxyFollowsTheSizeOfItsBufferEvenWhenFormed(GameTestHelper helper) {
        var controller = (WorkableMultiblockMachine) getMetaMachine(helper.getBlockEntity(new BlockPos(1, 2, 0)));
        var smallPos = new BlockPos(2, 2, 1);
        var bigPos = new BlockPos(8, 2, 1);
        var small = placeBuffer(helper, smallPos, GTValues.LuV);
        var big = placeBuffer(helper, bigPos, topTier());
        int bigSlots = big.getPatternCount();

        helper.assertTrue(small.getPatternCount() == 27 &&
                bigSlots == MEPatternBufferPartMachine.patternCountForTier(topTier()),
                "buffer sizes " +
                        small.getPatternCount() +
                        " and " + bigSlots);
        var proxy = (MEPatternBufferProxyPartMachine) TestUtils.setMachine(helper, new BlockPos(2, 2, 0),
                GTAEMachines.ME_PATTERN_BUFFER_PROXY);
        TestUtils.formMultiblock(controller);
        helper.assertTrue(controller.getParts().contains(proxy), "The proxy is not part of the multiblock");
        helper.assertTrue(proxy.getRecipeHandlers().isEmpty(), "An unlinked proxy has no handler lists");

        putPaper(helper, big, bigSlots - 1);
        helper.assertTrue(!controller.getParts().contains(big), "The big buffer became part of the structure");
        helper.assertTrue(!RecipeHelper.matchContents(controller, PAPER_RECIPE).isSuccess(),
                "Nothing is linked yet, the machine should have no paper: " +
                        RecipeHelper.matchContents(controller, PAPER_RECIPE));

        proxy.setBuffer(helper.absolutePos(smallPos));
        helper.assertTrue(proxy.getRecipeHandlers().size() == 27, "linked to the small buffer: " +
                proxy.getRecipeHandlers().size() + " lists");
        helper.assertTrue(!RecipeHelper.matchContents(controller, PAPER_RECIPE).isSuccess(),
                "The small buffer holds no paper");

        proxy.setBuffer(helper.absolutePos(bigPos));
        helper.assertTrue(proxy.getRecipeHandlers().size() == bigSlots, "linked to the big buffer: " +
                proxy.getRecipeHandlers().size() + " lists");
        helper.assertTrue(RecipeHelper.matchContents(controller, PAPER_RECIPE).isSuccess(),
                "The formed machine should see the paper of the big buffer's last slot through the proxy");

        proxy.setBuffer(helper.absolutePos(smallPos));
        helper.assertTrue(proxy.getRecipeHandlers().size() == 27, "linked to the small buffer again");
        helper.assertTrue(!RecipeHelper.matchContents(controller, PAPER_RECIPE).isSuccess(),
                "After going back to the small buffer the paper is out of reach again");

        helper.succeed();
    }

    private static void bench(GameTestHelper helper, int tier) {
        if (!SlowTests.enabled() || GTAEMachines.ME_PATTERN_BUFFERS[tier] == null) {
            helper.succeed();

            return;
        }

        var controller = (WorkableMultiblockMachine) getMetaMachine(helper.getBlockEntity(new BlockPos(1, 2, 0)));
        var buffer = placeBuffer(helper, new BlockPos(2, 2, 1), tier);
        TestUtils.formMultiblock(controller);
        controller.setRecipeType(BENCH_TYPES.get(tier));
        int slots = buffer.getPatternCount();

        var details = new IPatternDetails[slots];
        var holders = new KeyCounter[slots][];
        for (int s = 0; s < slots; s++) {
            var inputStacks = new GenericStack[3];
            for (int k = 0; k < 3; k++) inputStacks[k] = new GenericStack(AEItemKey.of(ITEM_POOL.get(s * 3 + k)), 1);
            var outputStacks = new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIRT), 1) };

            var stack = PatternDetailsHelper.encodeProcessingPattern(inputStacks, outputStacks, "pb_tier_bench");
            buffer.getTerminalPatternInventory().setItemDirect(s, stack);
            details[s] = PatternDetailsHelper.decodePattern(stack, helper.getLevel());
            var inputs = details[s].getInputs();
            holders[s] = new KeyCounter[inputs.length];
            for (int j = 0; j < inputs.length; j++) {
                holders[s][j] = new KeyCounter();
                var possible = inputs[j].getPossibleInputs()[0];
                holders[s][j].add(possible.what(), possible.amount() * inputs[j].getMultiplier());
            }
        }

        for (int s = 0; s < slots; s++) {
            buffer.getInternalInventory()[s].pushPattern(details[s], holders[s]);
        }

        var logic = controller.getRecipeLogic();
        var nanos = new long[ITERATIONS];
        for (int i = 0; i < ITERATIONS; i++) {
            final int iteration = i;
            helper.runAfterDelay(i + 1, () -> {
                int slot = iteration % slots;
                buffer.getInternalInventory()[slot].pushPattern(details[slot], holders[slot]);
                long start = System.nanoTime();
                logic.findAndHandleRecipe();
                nanos[iteration] = System.nanoTime() - start;
                if (logic.isActive()) helper.fail("A recipe started although no single slot can satisfy one");
                if (iteration == ITERATIONS - 1) {
                    helper.assertTrue(buffer.mergeInternalSlots().items().size() > slots,
                            "The slots are (almost) empty, the timing means nothing");
                    var timed = Arrays.copyOfRange(nanos, WARMUP, nanos.length);
                    Arrays.sort(timed);
                    double avg = Arrays.stream(timed).average().orElse(0) * 1e-6;
                    GTCEu.LOGGER.info(
                            "[PatternBufferTier] {} ({} slots): findAndHandleRecipe avg={}ms p95={}ms max={}ms",
                            GTValues.VN[tier], slots, String.format("%.1f", avg),
                            String.format("%.1f", timed[(int) (timed.length * 0.95)] * 1e-6),
                            String.format("%.1f", timed[timed.length - 1] * 1e-6));

                    helper.succeed();
                }
            });
        }
    }

    @GameTest(template = T, batch = BATCH, setupTicks = 40, timeoutTicks = ITERATIONS + 600)
    public static void benchLuV(GameTestHelper helper) {
        bench(helper, GTValues.LuV);
    }

    @GameTest(template = T, batch = BATCH, setupTicks = 40, timeoutTicks = ITERATIONS + 600)
    public static void benchZPM(GameTestHelper helper) {
        bench(helper, GTValues.ZPM);
    }

    @GameTest(template = T, batch = BATCH, setupTicks = 40, timeoutTicks = ITERATIONS + 600)
    public static void benchUV(GameTestHelper helper) {
        bench(helper, GTValues.UV);
    }

    @GameTest(template = T, batch = BATCH, setupTicks = 40, timeoutTicks = ITERATIONS + 600)
    public static void benchUHV(GameTestHelper helper) {
        bench(helper, GTValues.UHV);
    }

    @GameTest(template = T, batch = BATCH, setupTicks = 40, timeoutTicks = ITERATIONS + 600)
    public static void benchUEV(GameTestHelper helper) {
        bench(helper, GTValues.UEV);
    }
}
