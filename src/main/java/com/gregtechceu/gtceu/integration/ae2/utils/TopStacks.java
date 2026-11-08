package com.gregtechceu.gtceu.integration.ae2.utils;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import it.unimi.dsi.fastutil.objects.Object2LongMap;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

public final class TopStacks {

    public static PriorityQueue<GenericStack> select(Iterable<? extends Object2LongMap.Entry<AEKey>> counter,
                                                     Class<? extends AEKey> keyType, int limit, long minStackSize,
                                                     Predicate<GenericStack> autoPullTest) {
        return selectMixed(counter, limit, key -> keyType.isInstance(key) ? minStackSize : Long.MAX_VALUE,
                autoPullTest);
    }

    public static PriorityQueue<GenericStack> selectMixed(Iterable<? extends Object2LongMap.Entry<AEKey>> counter,
                                                          int limit, ToLongFunction<AEKey> minStackSize,
                                                          Predicate<GenericStack> autoPullTest) {
        PriorityQueue<GenericStack> topStacks = new PriorityQueue<>(Comparator.comparingDouble(TopStacks::getUnits));

        for (var entry : counter) {
            long amount = entry.getLongValue();
            AEKey key = entry.getKey();

            if (amount <= 0) continue;
            if (amount < minStackSize.applyAsLong(key)) continue;

            boolean isFull = topStacks.size() >= limit;

            if (isFull && (double) amount / key.getType().getAmountPerUnit() <= getUnits(topStacks.peek())) continue;

            var genericStack = new GenericStack(key, amount);

            if (autoPullTest != null && !autoPullTest.test(genericStack)) continue;

            if (isFull) topStacks.poll();

            topStacks.offer(genericStack);
        }

        return topStacks;
    }

    private static double getUnits(GenericStack stack) {
        return (double) stack.amount() / stack.what().getType().getAmountPerUnit();
    }
}
