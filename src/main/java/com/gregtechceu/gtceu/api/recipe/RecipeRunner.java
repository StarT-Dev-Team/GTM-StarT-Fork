package com.gregtechceu.gtceu.api.recipe;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.feature.IVoidable;
import com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerGroup;
import com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerGroupColor;
import com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerList;
import com.gregtechceu.gtceu.api.recipe.chance.boost.ChanceBoostFunction;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;

import net.minecraft.network.chat.Component;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerGroupDistinctness.BUS_DISTINCT;
import static com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerGroupDistinctness.BYPASS_DISTINCT;
import static com.gregtechceu.gtceu.api.recipe.RecipeHelper.addToRecipeHandlerMap;

public class RecipeRunner {

    private final GTRecipe recipe;
    private final IO io;
    private final boolean isTick;
    private final Map<RecipeCapability<?>, Object2IntMap<?>> chanceCaches;
    private final Map<IO, List<RecipeHandlerList>> capabilityProxies;
    private final boolean simulated;
    private Map<RecipeCapability<?>, List<Object>> recipeContents;
    private final Map<RecipeCapability<?>, List<Object>> searchRecipeContents;
    private final Predicate<RecipeCapability<?>> outputVoid;

    public RecipeRunner(GTRecipe recipe, IO io, boolean isTick,
                        IRecipeCapabilityHolder holder, Map<RecipeCapability<?>, Object2IntMap<?>> chanceCaches,
                        boolean simulated) {
        this.recipe = recipe;
        this.io = io;
        this.isTick = isTick;
        this.chanceCaches = chanceCaches;
        this.capabilityProxies = holder.getCapabilitiesProxy();
        this.recipeContents = new Reference2ObjectOpenHashMap<>();
        this.searchRecipeContents = simulated ? recipeContents : new Reference2ObjectOpenHashMap<>();
        this.simulated = simulated;
        this.outputVoid = cap -> holder instanceof IVoidable voidable && voidable.canVoidRecipeOutputs(cap);
    }

    @NotNull
    public ActionResult handle(Map<RecipeCapability<?>, List<Content>> entries) {
        fillContentMatchList(entries);

        if (searchRecipeContents.isEmpty()) {
            return ActionResult.PASS_NO_CONTENTS;
        }

        return this.handleContents();
    }

    /**
     * Populates the content match list to know if conditions are satisfied.
     */
    private void fillContentMatchList(Map<RecipeCapability<?>, List<Content>> entries) {
        ChanceBoostFunction function = recipe.getType().getChanceFunction();
        int recipeTier = RecipeHelper.getPreOCRecipeEuTier(recipe);
        int chanceTier = recipeTier + recipe.getChanceOcLevel();
        for (var entry : entries.entrySet()) {
            RecipeCapability<?> cap = entry.getKey();
            if (!cap.doMatchInRecipe()) continue;
            if (simulated && io == IO.OUT && outputVoid.test(cap)) continue;

            ChanceLogic logic = recipe.getChanceLogicForCapability(cap, this.io, this.isTick);
            List<Content> chancedContents = new ArrayList<>();
            // skip if empty
            if (entry.getValue().isEmpty()) continue;
            // populate recipe content capability map
            var contentList = this.recipeContents.computeIfAbsent(cap, c -> new ArrayList<>());
            var searchContentList = this.searchRecipeContents.computeIfAbsent(cap, c -> new ArrayList<>());
            for (Content cont : entry.getValue()) {
                searchContentList.add(cont.content);

                // When simulating the recipe handling (used for recipe matching),
                // searchRecipeContents == recipeContents, so all contents, chanced and unchanced, must match
                if (simulated) continue;

                if (cont.chance >= cont.maxChance) {
                    contentList.add(cont.content);
                } else if (cont.chance > 0 || cont.tierChanceBoost > 0) {
                    chancedContents.add(cont);
                }
                // Do not add Non-Consumed ingredients; they'd just get dropped after the chance roll anyway
            }

            // add chanced contents to the recipe content map
            if (!chancedContents.isEmpty()) {
                var cache = this.chanceCaches.get(cap);
                chancedContents = logic.roll(cap, chancedContents, function, recipeTier, chanceTier, cache,
                        recipe.parallels);

                for (Content cont : chancedContents) {
                    contentList.add(cont.content);
                }
            }

            if (contentList.isEmpty()) recipeContents.remove(cap);
        }
    }

    private ActionResult handleContents() {
        if (recipeContents.isEmpty()) return ActionResult.SUCCESS;
        if (!capabilityProxies.containsKey(io)) {
            return ActionResult.fail(
                    Component.translatable("gtceu.recipe_logic.no_capabilities")
                            .append(Component.literal(": "))
                            .append(Component.translatable(io.tooltip)),
                    null, io);
        }

        List<RecipeHandlerList> handlers = capabilityProxies.getOrDefault(io, Collections.emptyList());
        // Only sort for non-tick outputs
        if (!isTick && io.support(IO.OUT)) {
            handlers.sort(RecipeHandlerList.COMPARATOR.reversed());
        }

        var handlerGroups = new HashMap<RecipeHandlerGroup, List<RecipeHandlerList>>();
        for (var handler : handlers) {
            addToRecipeHandlerMap(handler.getGroup(), handler, handlerGroups);
        }

        // Ordering is first undyed+bypass, then colored, then distinct

        var matched = false;

        var bypassHandlerList = handlerGroups.getOrDefault(BYPASS_DISTINCT, Collections.emptyList());
        var undyedHandlerList = handlerGroups.getOrDefault(RecipeHandlerGroupColor.UNDYED, Collections.emptyList());

        if (!bypassHandlerList.isEmpty() || !undyedHandlerList.isEmpty()) {
            matched = handleRecipeHandlerList(undyedHandlerList.stream(), bypassHandlerList.stream());
        }

        if (!matched) {
            for (var handlerListEntry : handlerGroups.entrySet()) {
                var key = handlerListEntry.getKey();
                if (!(key instanceof RecipeHandlerGroupColor) || key.equals(RecipeHandlerGroupColor.UNDYED)) continue;

                matched = handleRecipeHandlerList(handlerListEntry.getValue().stream(), bypassHandlerList.stream());
                if (matched) break;
            }
        }
        if (!matched) {
            for (var distinctHandler : handlerGroups.getOrDefault(BUS_DISTINCT, Collections.emptyList())) {
                matched = handleRecipeHandlerList(Stream.of(distinctHandler), undyedHandlerList.stream(),
                        bypassHandlerList.stream());
                if (matched) break;
            }
        }
        if (!matched) {
            return ActionResult.FAIL_NO_REASON;
        }
        if (simulated) {
            return ActionResult.SUCCESS;
        }

        for (var entry : recipeContents.entrySet()) {
            if (io == IO.OUT && outputVoid.test(entry.getKey())) {
                // void excess real output contents if it can be voided
                entry.getValue().clear();
            } else if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                // fail early if there are excess contents
                return ActionResult.fail(null, entry.getKey(), io);
            }
        }

        // if, post-voiding, we don't have stuff, pass instead of fail
        var containsStuff = false;
        for (var entry : recipeContents.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                containsStuff = true;
                break;
            }
        }
        if (!containsStuff) {
            return ActionResult.PASS_NO_CONTENTS;
        }

        return ActionResult.FAIL_NO_REASON;
    }

    @SafeVarargs
    private boolean handleRecipeHandlerList(Stream<RecipeHandlerList>... streams) {
        if (streams.length == 0) {
            return false;
        }
        var handlerList = Stream.of(streams).reduce(Stream::concat).get().toList();

        var copiedRecipeContents = searchRecipeContents;
        for (var handler : handlerList) {
            copiedRecipeContents = handler.handleRecipe(io, recipe, copiedRecipeContents, true);
            if (copiedRecipeContents.isEmpty()) {
                break;
            }
        }

        if (io == IO.OUT) {
            if (hasAnyNonVoidingContents(copiedRecipeContents)) return false;
        } else if (io == IO.IN) {
            if (!copiedRecipeContents.isEmpty()) return false;
        }

        if (!simulated) {
            // Start actually removing items
            for (var handler : handlerList) {
                recipeContents = handler.handleRecipe(io, recipe, recipeContents, false);
                if (recipeContents.isEmpty()) {
                    break;
                }
            }
        }

        return true;
    }

    private boolean hasAnyNonVoidingContents(Map<RecipeCapability<?>, List<Object>> contents) {
        for (var entry : contents.entrySet()) {
            if (outputVoid.test(entry.getKey())) continue;
            if (!(entry.getValue() == null || entry.getValue().isEmpty())) {
                return true;
            }
        }
        return false;
    }
}
