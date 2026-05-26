package com.gregtechceu.gtceu.data.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

import static com.gregtechceu.gtceu.data.lang.LangHandler.*;

public class ModifierLang {

    protected static void init(RegistrateLangProvider provider) {
        provider.add("gtceu.modifier.oc_perfect.name", "%sPerfect Overclock:");
        provider.add("gtceu.modifier.oc_perfect.description",
                "  §7Overclocks now 4x the speed for each voltage instead of the usual 2x.");

        provider.add("gtceu.modifier.oc_perfect_subtick.name", "%sPerfect Overclock Subtick:");
        multiLang(provider, "gtceu.modifier.oc_perfect_subtick.description",
                "  §7Overclocks now 4x the speed for each voltage instead of the usual 2x.",
                "  §7If the recipe time goes under 1 tick additional overclocks",
                "  §7are converted into parallels, respecting perfect overclock mechanics.");

        provider.add("gtceu.modifier.oc_non_perfect_subtick.name", "%sSubtick:");
        provider.add("gtceu.modifier.oc_non_perfect_subtick.description",
                "  §7If the recipe time goes under 1 tick additional overclocks are converted into parallels.");

        provider.add("gtceu.modifier.parallel_hatch.name", "%sParallel Hatch:");
        multiLang(provider, "gtceu.modifier.parallel_hatch.description",
                "  §7Enables the use of parallel hatches.",
                "  §7Parallel hatches allow a machine to run multiples of the same recipe at once.",
                "  §7However, for each speed overclock your recipe duration will increase by 2x.");

        provider.add("gtceu.modifier.batch_mode.name", "%sBatch Mode:");
        multiLang(provider, "gtceu.modifier.batch_mode.description",
                "  §7Enables the use of Batch Mode.",
                "  §7If the recipe duration goes below 5 seconds Batch Mode will activate.",
                "  §7When Batch Mode is active recipe durations will not go below 5 seconds.",
                "  §7Instead the time is converted into parallel recipes, thus boosting performance.");

        provider.add("gtceu.modifier.cracker_oc.name", "%sCracker Overclock:");
        provider.add("gtceu.modifier.cracker_oc.description",
                "  §7Every coil after §6Cupronickel§7 reduces energy usage by §f10%%§7.");

        provider.add("gtceu.modifier.ebf_oc.name", "%sElectric Blast Furnace Overclock:");
        multiLang(provider, "gtceu.modifier.ebf_oc.description",
                "  §7For every §f900K§7 above the recipe temperature, a multiplicative §f95%%§7 energy multiplier is applied pre-overclocking.",
                "  §7For every §f1800K§7 above the recipe temperature, one overclock becomes §f100%% efficient§7 (perfect overclock).",
                "  §7For every voltage tier above §bMV§7, temperature is increased by §f100K§7.");

        provider.add("gtceu.modifier.pyrolyse_oven_oc.name", "%sPyrolyse Oven Overclock:");
        multiLang(provider, "gtceu.modifier.pyrolyse_oven_oc.description",
                "  §6Cupronickel §7coils are §f25%%§7 slower.",
                "  §7Every coil after §bKanthal§7 increases speed by §f50%%§7.");

        provider.add("gtceu.modifier.multi_smellter_parallel.name", "%sMulti Smelter Parallel:");
        multiLang(provider, "gtceu.modifier.multi_smellter_parallel.description",
                "  §7Runs recipes in parallel based on the coils used.",
                "  §7Higher tiers of coils increase speed and reduce energy use.");

        provider.add("gtceu.modifier.chemical_reactor_oc.name", "%sChemical Overclock:");
        multiLang(provider, "gtceu.modifier.chemical_reactor_oc.description",
                "  §7Starting after §6Kanthal§7, every coil increases speed by §f25%%§7. §6Cupronickel §7coils are §f25%%§7 slower.",
                "  §7Starting after §6Cupronickel§7, energy usage is also decreased by §f5%%§7.");

        provider.add("gtceu.modifier.consume_eu_to_start.name", "%sConsume EU to start:");
        provider.add("gtceu.modifier.consume_eu_to_start.description",
                "  §7Recipes require a certain amount of EU to start.");

        provider.add("gtceu.modifier.fusion_overclock.name", "%sFusion Overclock:");
        multiLang(provider, "gtceu.modifier.fusion_overclock.description",
                "  §7Unlike most machines Fusion Reactors do not overclock by upgrading the energy hatches on them.",
                "  §7To overclock Fusion Reactors you instead have to build the next tier.",
                "  §7Each overclock doubles the energy cost while halving the duration. §b[2:2]",
                "  §7Example Reactors and their Tiers:",
                "    §fMK1 §7= §dLuV",
                "    §fMK2 §7= §cZPM",
                "    §fMK3 §7= §3UV");
    }
}
