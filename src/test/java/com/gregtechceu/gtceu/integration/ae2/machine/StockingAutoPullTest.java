package com.gregtechceu.gtceu.integration.ae2.machine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.common.data.machines.GTAEMachines;
import com.gregtechceu.gtceu.gametest.util.TestUtils;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemList;
import com.gregtechceu.gtceu.integration.ae2.utils.TopStacks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import it.unimi.dsi.fastutil.objects.Object2LongMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static com.gregtechceu.gtceu.gametest.util.TestUtils.getMetaMachine;

@PrefixGameTestTemplate(false)
@GameTestHolder(GTCEu.MOD_ID)
public class StockingAutoPullTest {

    private static final int ITEMS = 40;

    private static List<AEKey> configOf(MEStockingBusPartMachine bus) {
        var keys = new ArrayList<AEKey>();

        for (var slot : ((ExportOnlyAEItemList) bus.getSlotList()).getInventory()) {
            if (slot.getConfig() != null) keys.add(slot.getConfig().what());
        }

        return keys;
    }

    private static PriorityQueue<GenericStack> reference(Iterable<Object2LongMap.Entry<AEKey>> counter,
                                                         Class<? extends AEKey> keyType, int limit, long minStackSize,
                                                         Predicate<GenericStack> autoPullTest) {
        PriorityQueue<GenericStack> topStacks = new PriorityQueue<>(Comparator.comparingLong(GenericStack::amount));

        for (Object2LongMap.Entry<AEKey> entry : counter) {
            long amount = entry.getLongValue();

            AEKey key = entry.getKey();

            if (amount <= 0) continue;
            if (!keyType.isInstance(key)) continue;

            var genericStack = new GenericStack(key, amount);

            if (autoPullTest != null && !autoPullTest.test(genericStack)) continue;

            if (amount >= minStackSize) {
                if (topStacks.size() < limit) {
                    topStacks.offer(genericStack);
                } else if (amount > topStacks.peek().amount()) {
                    topStacks.poll();
                    topStacks.offer(genericStack);
                }
            }
        }

        return topStacks;
    }

    private static List<String> drain(PriorityQueue<GenericStack> queue) {
        var stacks = new ArrayList<String>();

        while (!queue.isEmpty()) {
            var stack = queue.poll();

            stacks.add(stack.what() + "x" + stack.amount());
        }

        return stacks;
    }

    @GameTest(template = "patternbuffertest", batch = "StockingAutoPull", setupTicks = 40, timeoutTicks = 300)
    public static void stockingBusesNeverPullTheSameItems(GameTestHelper helper) {
        var controller = (WorkableMultiblockMachine) getMetaMachine(helper.getBlockEntity(new BlockPos(1, 2, 0)));
        var buffer = (MEPatternBufferPartMachine) getMetaMachine(helper.getBlockEntity(new BlockPos(2, 2, 1)));
        var positions = new BlockPos[] { new BlockPos(2, 1, 0), new BlockPos(2, 2, 0) };
        var buses = new MEStockingBusPartMachine[positions.length];

        for (int i = 0; i < positions.length; i++) {
            buses[i] = (MEStockingBusPartMachine) TestUtils.setMachine(helper, positions[i],
                    GTAEMachines.STOCKING_IMPORT_BUS_ME);
        }

        TestUtils.formMultiblock(controller);

        var sides = EnumSet.allOf(Direction.class);

        buffer.getMainNode().setExposedOnSides(sides);

        for (var bus : buses) bus.getMainNode().setExposedOnSides(sides);

        for (var bus : buses) {
            helper.assertTrue(controller.getParts().contains(bus), "A stocking bus is not part of the multiblock");
        }

        helper.runAfterDelay(40, () -> {
            for (var bus : buses) helper.assertTrue(bus.isOnline(), "A stocking bus is not online yet");

            var grid = buffer.getGrid();

            helper.assertTrue(grid != null, "No grid");

            var inventory = grid.getStorageService().getInventory();

            for (int i = 0; i < ITEMS; i++) {
                var tag = new CompoundTag();

                tag.putInt("n", i);

                long inserted = inventory.insert(AEItemKey.of(Items.PAPER, tag), 100 + i * 7L, Actionable.MODULATE,
                        IActionSource.empty());

                helper.assertTrue(inserted > 0, "The test network did not accept item " + i);
            }

            for (var bus : buses) bus.setAutoPull(true);

            var amounts = new HashMap<AEKey, Long>();

            for (var entry : inventory.getAvailableStacks()) {
                if (entry.getKey() instanceof AEItemKey && entry.getLongValue() > 0) {
                    amounts.put(entry.getKey(), entry.getLongValue());
                }
            }

            helper.assertTrue(amounts.size() >= 32, "Too few item types in the test network: " + amounts.size());

            var seen = new HashSet<AEKey>();
            long previousMin = Long.MAX_VALUE;

            for (int b = 0; b < buses.length; b++) {
                var config = configOf(buses[b]);

                helper.assertTrue(config.size() == 16, "Bus " + b + " holds " + config.size() + " items, expected 16");

                long max = 0;
                long min = Long.MAX_VALUE;

                for (var key : config) {
                    helper.assertTrue(seen.add(key), "Item " + key + " is configured in more than one stocking bus");

                    long amount = amounts.getOrDefault(key, 0L);

                    max = Math.max(max, amount);
                    min = Math.min(min, amount);
                }

                helper.assertTrue(max <= previousMin,
                        "Bus " + b + " holds an item bigger than the previous bus' smallest");

                previousMin = min;
            }

            for (var entry : amounts.entrySet()) {
                if (!seen.contains(entry.getKey())) {
                    helper.assertTrue(entry.getValue() <= previousMin,
                            "Unassigned item " + entry.getKey() + " (" + entry.getValue() + ") is bigger than the " +
                                    "smallest assigned one (" + previousMin + ")");
                }
            }

            var before = new ArrayList<List<AEKey>>();

            for (var bus : buses) before.add(configOf(bus));
            for (var bus : buses) bus.setAutoPull(false);
            for (var bus : buses) bus.setAutoPull(true);

            for (int b = 0; b < buses.length; b++) {
                helper.assertTrue(configOf(buses[b]).equals(before.get(b)),
                        "Bus " + b + " changed on the second round");
            }

            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void topStacksMatchesPreviousSelection(GameTestHelper helper) {
        var random = new Random(11);
        List<Item> pool = List.of(Items.PAPER, Items.SUGAR, Items.STICK, Items.COAL, Items.DIRT, Items.SAND);

        for (int trial = 0; trial < 400; trial++) {
            var counter = new KeyCounter();
            int entries = random.nextInt(80);

            for (int i = 0; i < entries; i++) {
                var tag = new CompoundTag();

                tag.putInt("n", random.nextInt(60));

                AEKey key = random.nextInt(4) == 0 ? AEFluidKey.of(Fluids.WATER, tag) :
                        AEItemKey.of(pool.get(random.nextInt(pool.size())), tag);

                counter.add(key, random.nextInt(40) - 5);
            }

            int limit = 1 + random.nextInt(16);
            long minStackSize = random.nextInt(10);
            int modulus = 1 + random.nextInt(4);
            Predicate<GenericStack> autoPullTest = random.nextInt(5) == 0 ? null :
                    stack -> Math.floorMod(stack.what().hashCode() + (int) stack.amount(), modulus) != 0;
            Class<? extends AEKey> keyType = random.nextBoolean() ? AEItemKey.class : AEFluidKey.class;
            var expected = drain(reference(counter, keyType, limit, minStackSize, autoPullTest));
            var actual = drain(TopStacks.select(counter, keyType, limit, minStackSize, autoPullTest));

            helper.assertTrue(expected.equals(actual),
                    "Trial " + trial + ": expected " + expected + " but got " + actual);
        }

        var network = new KeyCounter();

        for (int i = 0; i < 150_000; i++) {
            var tag = new CompoundTag();

            tag.putInt("n", i);

            network.add(AEItemKey.of(Items.PAPER, tag), 1 + random.nextInt(1_000_000));
        }

        var calls = new AtomicInteger();
        Predicate<GenericStack> counting = stack -> {
            calls.incrementAndGet();

            return stack.amount() % 7 != 0;
        };

        long start = System.nanoTime();
        var expected = drain(reference(network, AEItemKey.class, 16, 1, counting));
        long referenceNanos = System.nanoTime() - start;
        int referenceCalls = calls.getAndSet(0);

        start = System.nanoTime();

        var actual = drain(TopStacks.select(network, AEItemKey.class, 16, 1, counting));
        long selectNanos = System.nanoTime() - start;

        helper.assertTrue(expected.equals(actual), "Big network: results differ");

        GTCEu.LOGGER.info("[TopStacks] 150000 entries: previous={}ms ({} tests) now={}ms ({} tests)",
                String.format("%.1f", referenceNanos * 1e-6), referenceCalls,
                String.format("%.1f", selectNanos * 1e-6), calls.get());

        helper.succeed();
    }
}
