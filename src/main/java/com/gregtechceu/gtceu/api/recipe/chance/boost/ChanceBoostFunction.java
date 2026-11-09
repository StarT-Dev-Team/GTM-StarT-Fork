package com.gregtechceu.gtceu.api.recipe.chance.boost;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.recipe.content.Content;

import net.minecraft.util.Mth;

import org.jetbrains.annotations.NotNull;

/**
 * A function used to boost a {@link Content}'s chance
 */
@FunctionalInterface
public interface ChanceBoostFunction {

    /**
     * Chance boosting function based on the number of performed overclocks
     */
    ChanceBoostFunction OVERCLOCK = (entry, recipeTier, chanceTier) -> {
        int tierDiff = overclockTierDiff(recipeTier, chanceTier);

        if (tierDiff <= 0) return entry.chance; // equal or invalid tiers do not boost at all

        return Mth.clamp(entry.chance + (entry.tierChanceBoost * tierDiff), 0, entry.maxChance);
    };

    /**
     * Chance boosting function which performs no boosting
     */
    ChanceBoostFunction NONE = new ChanceBoostFunction() {

        @Override
        public int getBoostedChance(@NotNull Content entry, int recipeTier, int chanceTier) {
            return entry.chance;
        }

        @Override
        public int getTierDiff(int recipeTier, int chanceTier) {
            return 0;
        }
    };

    /**
     * @param entry      the amount to boost by
     * @param recipeTier the base tier of the recipe
     * @param chanceTier the tier the recipe is run at
     * @return the boosted chance
     */
    int getBoostedChance(@NotNull Content entry, int recipeTier, int chanceTier);

    /**
     * @param recipeTier the base tier of the recipe
     * @param chanceTier the tier the recipe is run at
     * @return the number of boosting tiers, 0 if the recipe is run at or below its own tier
     */
    default int getTierDiff(int recipeTier, int chanceTier) {
        return overclockTierDiff(recipeTier, chanceTier);
    }

    /**
     * @param chance the chance to return
     * @return the function
     */
    static ChanceBoostFunction fixed(int chance) {
        return new ChanceBoostFunction() {

            @Override
            public int getBoostedChance(@NotNull Content entry, int recipeTier, int chanceTier) {
                return chance;
            }

            @Override
            public int getTierDiff(int recipeTier, int chanceTier) {
                return 0;
            }
        };
    }

    static int overclockTierDiff(int recipeTier, int chanceTier) {
        int tierDiff = chanceTier - recipeTier;

        if (tierDiff <= 0) return 0;
        if (recipeTier == GTValues.ULV) tierDiff--;

        return tierDiff;
    }
}
