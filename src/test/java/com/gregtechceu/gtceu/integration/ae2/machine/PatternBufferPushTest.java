package com.gregtechceu.gtceu.integration.ae2.machine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.IRecipeHandlerTrait;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;
import com.gregtechceu.gtceu.gametest.util.TestUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;

import java.util.concurrent.atomic.AtomicInteger;

import static com.gregtechceu.gtceu.gametest.util.TestUtils.getMetaMachine;

@PrefixGameTestTemplate(false)
@GameTestHolder(GTCEu.MOD_ID)
public class PatternBufferPushTest {

    private static final String BATCH = "PatternBufferPush";

    private static final int PATTERNS = MEPatternBufferPartMachine.BASE_PATTERN_COUNT;
    private static final int INPUTS = 3;
    private static final int PUSHES_PER_SLOT = 100;

    private static MEPatternBufferPartMachine formAndGetBuffer(GameTestHelper helper) {
        var controller = (WorkableMultiblockMachine) getMetaMachine(helper.getBlockEntity(new BlockPos(1, 2, 0)));
        TestUtils.formMultiblock(controller);

        return (MEPatternBufferPartMachine) getMetaMachine(helper.getBlockEntity(new BlockPos(2, 2, 1)));
    }

    @GameTest(template = "patternbuffertest", batch = BATCH, setupTicks = 40, timeoutTicks = 400)
    public static void patternBufferPushTest(GameTestHelper helper) {
        var level = helper.getLevel();
        var buffer = formAndGetBuffer(helper);

        var details = new IPatternDetails[PATTERNS];
        var holders = new KeyCounter[PATTERNS][];
        for (int s = 0; s < PATTERNS; s++) {
            var inputStacks = new GenericStack[INPUTS];
            for (int k = 0; k < INPUTS; k++) {
                var tag = new CompoundTag();
                tag.putInt("n", s * INPUTS + k);
                inputStacks[k] = new GenericStack(AEItemKey.of(Items.PAPER, tag), 1);
            }

            var outputStacks = new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIRT), 1) };

            var stack = PatternDetailsHelper.encodeProcessingPattern(inputStacks, outputStacks, "pattern_buffer_push");
            buffer.getTerminalPatternInventory().setItemDirect(s, stack);

            details[s] = PatternDetailsHelper.decodePattern(stack, level);
            helper.assertTrue(details[s] != null, "Could not decode pattern " + s);
            var inputs = details[s].getInputs();
            holders[s] = new KeyCounter[inputs.length];
            for (int j = 0; j < inputs.length; j++) {
                holders[s][j] = new KeyCounter();
                var possible = inputs[j].getPossibleInputs()[0];
                holders[s][j].add(possible.what(), possible.amount() * inputs[j].getMultiplier());
            }
        }

        var slot0Handlers = buffer.getInternalRecipeHandler().getSlotHandlers().get(0)
                .getCapability(ItemRecipeCapability.CAP);
        var slot0Item = (IRecipeHandlerTrait<?>) slot0Handlers.get(slot0Handlers.size() - 1);
        var notified = new AtomicInteger();
        slot0Item.addChangedListener(notified::incrementAndGet);

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(buffer.pushPattern(details[0], holders[0]), "First push was rejected");
            helper.assertTrue(notified.get() > 0, "Slot item handler listener was not notified by a push");

            long start = System.nanoTime();
            int rejected = 0;
            int pushes = 1;
            for (int i = 0; i < PUSHES_PER_SLOT * PATTERNS - 1; i++) {
                int slot = (i + 1) % PATTERNS;
                if (buffer.pushPattern(details[slot], holders[slot])) pushes++;
                else rejected++;
            }

            long nanos = System.nanoTime() - start;
            GTCEu.LOGGER.info("[PatternBufferPush] {} pushes ({} rejected): total={}ms avg={}us/push",
                    pushes, rejected, String.format("%.2f", nanos * 1e-6),
                    String.format("%.3f", nanos * 1e-3 / Math.max(1, pushes)));
            helper.assertTrue(rejected == 0, rejected + " pushes were rejected");

            var merged = buffer.mergeInternalSlots();
            long total = merged.items().values().longStream().sum();
            helper.assertTrue(total == (long) pushes * INPUTS,
                    "Merged item total " + total + " != " + (long) pushes * INPUTS);
            helper.assertTrue(merged.items().size() == PATTERNS * INPUTS,
                    "Expected " + PATTERNS * INPUTS + " distinct stacks, got " + merged.items().size());

            var original = buffer.getInternalInventory()[0];
            var copy = buffer.new InternalSlot();
            copy.deserializeNBT(original.serializeNBT());
            long originalCount = original.getItems().stream().mapToLong(s -> s.getCount()).sum();
            long copyCount = copy.getItems().stream().mapToLong(s -> s.getCount()).sum();
            helper.assertTrue(originalCount > 0 && originalCount == copyCount,
                    "Slot NBT round trip changed the contents: " + originalCount + " -> " + copyCount);

            helper.succeed();
        });
    }

    @GameTest(template = "patternbuffertest", batch = BATCH, setupTicks = 40, timeoutTicks = 400)
    public static void patternBufferIdleTickTest(GameTestHelper helper) {
        var buffer = formAndGetBuffer(helper);

        helper.runAfterDelay(30, () -> {
            helper.assertTrue(autoIOSubscription(buffer) == null,
                    "The pattern buffer still has a permanent auto-IO tick subscription");
            helper.assertTrue(buffer.updateSubs == null, "The pattern sync tick is still subscribed while idle");

            buffer.markPatternSync();
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(buffer.updateSubs == null, "The pattern sync tick was not released after syncing");
                helper.assertTrue(autoIOSubscription(buffer) == null, "Auto-IO subscribed after a sync");

                helper.succeed();
            });
        });
    }

    @GameTest(template = "patternbuffertest", batch = BATCH, setupTicks = 40, timeoutTicks = 100)
    public static void patternBufferSlotLoadsLegacyNbtTest(GameTestHelper helper) {
        var buffer = formAndGetBuffer(helper);

        var tag = new CompoundTag();
        var items = new ListTag();
        var itemTag = new ItemStack(Items.PAPER).serializeNBT();
        itemTag.putLong("real", 5_000_000_000L); // more than an int, the reason for the "real" count
        items.add(itemTag);
        tag.put("inventory", items);
        var fluids = new ListTag();
        var fluidTag = new FluidStack(Fluids.WATER, 1)
                .writeToNBT(new CompoundTag());
        fluidTag.putLong("real", 12_345L);
        fluids.add(fluidTag);
        tag.put("fluidInventory", fluids);

        var slot = buffer.new InternalSlot();
        slot.deserializeNBT(tag);
        long itemTotal = slot.getItems().stream().mapToLong(s -> s.getCount()).sum();
        long fluidTotal = slot.getFluids().stream().mapToLong(s -> s.getAmount()).sum();
        helper.assertTrue(itemTotal == 5_000_000_000L, "Legacy item count loaded as " + itemTotal);
        helper.assertTrue(fluidTotal == 12_345L, "Legacy fluid amount loaded as " + fluidTotal);

        var saved = slot.serializeNBT();
        helper.assertTrue(saved.getList("inventory", 10).getCompound(0).getLong("real") == 5_000_000_000L &&
                saved.getList("fluidInventory", 10).getCompound(0).getLong("real") == 12_345L,
                "Re-saved slot lost its counts: " + saved);

        helper.succeed();
    }

    private static Object autoIOSubscription(MEPatternBufferPartMachine buffer) {
        try {
            var field = ItemBusPartMachine.class.getDeclaredField("autoIOSubs");
            field.setAccessible(true);

            return field.get(buffer);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
