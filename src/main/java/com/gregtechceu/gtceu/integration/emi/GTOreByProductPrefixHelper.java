package com.gregtechceu.gtceu.integration.emi;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.integration.xei.entry.item.ItemTagList;

import java.util.ArrayList;
import java.util.List;

public class GTOreByProductPrefixHelper {

    public static final List<TagPrefix> ORES = new ArrayList<>();

    public static void addOreByProductPrefix(TagPrefix orePrefix) {
        if (!ORES.contains(orePrefix)) {
            ORES.add(orePrefix);
        }
    }

    public static ItemTagList getOreByProductPrefix(Material material) {
        ItemTagList oreStacks = new ItemTagList();

        for (TagPrefix prefix : ORES) {
            // get all ores with the relevant oredicts instead of just the first unified ore
            oreStacks.add(ChemicalHelper.getTag(prefix, material), 1, null);
        }

        oreStacks.add(ChemicalHelper.getTag(TagPrefix.rawOre, material), 1, null);

        return oreStacks;
    }
}
