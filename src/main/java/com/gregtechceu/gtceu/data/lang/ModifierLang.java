package com.gregtechceu.gtceu.data.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

import static com.gregtechceu.gtceu.data.lang.LangHandler.*;

public class ModifierLang {

    protected static void init(RegistrateLangProvider provider) {
        provider.add("gtceu.modifier.oc_perfect.name", "%sPerfect Overclock:");
        provider.add("gtceu.modifier.oc_perfect.description",
                "  §7It makes the recipe faster without increasing total energy use.");

        provider.add("gtceu.modifier.oc_perfect_subtick.name", "%sPerfect Overclock Subtick:");
        multiLang(provider, "gtceu.modifier.oc_perfect_subtick.description",
                "  §7It makes the recipe faster without increasing total energy use.",
                "  §7If the recipe time goes under 1 tick,",
                "  §7it turns into parallel outputs with no extra energy cost.");

        provider.add("gtceu.modifier.oc_non_perfect_subtick.name", "%sSubtick:");
        provider.add("gtceu.modifier.oc_non_perfect_subtick.description",
                "  §7If the recipe time goes under 1 tick, it turns into parallel outputs.");

        provider.add("gtceu.modifier.parallel_hatch.name", "%sParallel Hatch:");
        multiLang(provider, "gtceu.modifier.parallel_hatch.description",
                "  §7Enables the use of parallel hatches.",
                "  §7Which run the recipe X amounts of time.");

        provider.add("gtceu.modifier.batch_mode.name", "%sBatch Mode:");
        multiLang(provider, "gtceu.modifier.batch_mode.description",
                "  §7Enables the use of Batch Mode.",
                "  §7The configured batch mode duration will be divided by recipe time.",
                "  §7Then it will use that to get the actual parallel to multiply to the current amount.");

        provider.add("gtceu.modifier.cracker_oc.name", "%sCracker Overclock:");
        provider.add("gtceu.modifier.cracker_oc.description",
                "  §7Every coil after §6Cupronickel§7 reduces energy usage by §f10%%§7.");

        provider.add("gtceu.modifier.ebf_oc.name", "%sElectric Blast Furnace Overclock:");
        multiLang(provider, "gtceu.modifier.ebf_oc.description",
                "  §7For every §f900K§7 above the recipe temperature, a multiplicative §f95%%§7 energy multiplier is applied pre-overclocking.",
                "  §7For every §f1800K§7 above the recipe temperature, one overclock becomes §f100%% efficient§7 (perfect overclock).",
                "  §7For every voltage tier above §bMV§7, temperature is increased by §f100K§7.");

        provider.add("gtceu.modifier.pyrolize_oven_oc.name", "%sPyrolize Oven Overclock:");
        multiLang(provider, "gtceu.modifier.pyrolize_oven_oc.description",
                "  §6Cupronickel §7coils are §f25%%§7 slower.",
                "  §7Every coil after §bKanthal§7 increases speed by §f50%%§7.");

        provider.add("gtceu.modifier.multi_smellter_parallel.name", "%sMulti Smelter Parallel:");
        multiLang(provider, "gtceu.modifier.multi_smellter_parallel.description",
                "  §7Max parallels: §f32x§7 coil tier.",
                "  §7Higher tiers increase speed and reduce energy use.");

        provider.add("gtceu.modifier.chemical_reactor_oc.name", "%sChemical Overclock:");
        multiLang(provider, "gtceu.modifier.chemical_reactor_oc.description",
                "  §7Starting at §6Cupronickel, §7every coil after increases speed by §f25%%.",
                "  §7Energy usage is also decreased by §f5%% §7per tier.");

        provider.add("gtceu.modifier.consume_eu_to_start.name", "%sConsume EU to start:");
        provider.add("gtceu.modifier.consume_eu_to_start.description", "  §7Needs X EU to start the recipe.");

        provider.add("gtceu.modifier.fusion_overclock.name", "%sFusion Overclock:");
        multiLang(provider, "gtceu.modifier.fusion_overclock.description",
                "  §7Unlike most machines Fusion Reactors can not overclock by increasing the energy hatches on them.",
                "  §7Instead the way to overclock Fusion Reactors is to use the next tier of reactor.",
                "  §7Each overclock is special as it only doubles the energy cost while halfing the duration. §b[2:2]",
                "  §7Example Reactors and their Tiers:",
                "    §fMK1 §7= §dLuV",
                "    §fMK2 §7= §cZPM",
                "    §fMK3 §7= §3UV");
    }
}
