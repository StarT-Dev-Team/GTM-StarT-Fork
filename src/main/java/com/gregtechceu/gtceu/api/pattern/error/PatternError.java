package com.gregtechceu.gtceu.api.pattern.error;

import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class PatternError {

    @Setter
    protected MultiblockState worldState;

    public Level getWorld() {
        return worldState.getWorld();
    }

    public BlockPos getPos() {
        return worldState.getPos();
    }

    public List<List<ItemStack>> getCandidates() {
        TraceabilityPredicate predicate = worldState.predicate;
        List<List<ItemStack>> candidates = new ArrayList<>();
        for (SimplePredicate common : predicate.common) {
            candidates.add(common.getCandidates());
        }
        for (SimplePredicate limited : predicate.limited) {
            candidates.add(limited.getCandidates());
        }
        return candidates;
    }

    protected MutableComponent formatCandidateItemStack(ItemStack itemStack) {
        return Component.empty().append(itemStack.getHoverName()).withStyle(itemStack.getRarity().getStyleModifier());
    }

    protected Component formatCandidate(List<ItemStack> candidate) {
        var firstCandidate = candidate.get(0);
        var candidateList = candidate.stream().skip(1)
                .map(this::formatCandidateItemStack)
                .reduce(
                        formatCandidateItemStack(firstCandidate),
                        (a, b) -> a.append(Component.literal("\n")).append(b),
                        (a, b) -> a.append(Component.literal("\n")).append(b));

        return Component.empty().append(firstCandidate.getHoverName())
                .withStyle(firstCandidate.getRarity().getStyleModifier())
                .withStyle(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, candidateList)));
    }

    public Component getErrorInfo() {
        var candidates = getCandidates();
        var builder = Component.literal("");
        var index = 0;
        for (var candidate : candidates) {
            if (candidate.isEmpty()) continue;
            if (index > 0) {
                builder.append(Component.literal(", "));
            }
            builder.append(formatCandidate(candidate));

            index++;
            if (index > 3 && candidates.size() > 3) {
                builder.append(Component.literal(", ..."));
                break;
            }
        }
        var position = Component.translatable("chat.coordinates", worldState.getPos().getX(),
                worldState.getPos().getY(), worldState.getPos().getZ());
        return Component.translatable("gtceu.multiblock.pattern.error", builder, position);
    }
}
