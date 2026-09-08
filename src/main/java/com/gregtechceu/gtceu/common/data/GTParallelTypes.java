package com.gregtechceu.gtceu.common.data;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.recipe.ParallelType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraftforge.fml.ModLoader;

public class GTParallelTypes {

    static {
        GTRegistries.PARALLEL_TYPES.unfreeze();
    }

    private GTParallelTypes() {}

    public final static ParallelType UNKNOWN = register("unknown");
    public final static ParallelType HATCH = register("hatch");
    public final static ParallelType BATCH = register("batch");
    public final static ParallelType SUBTICK = register("subtick");
    public final static ParallelType STEAM = register("steam");
    public final static ParallelType MULTI_SMELTER = register("multi_smelter");
    public final static ParallelType GENERATOR = register("generator");

    public static void init() {
        ModLoader.get().postEvent(new GTCEuAPI.RegisterEvent<>(GTRegistries.PARALLEL_TYPES, ParallelType.class));
        GTRegistries.PARALLEL_TYPES.freeze();
    }

    private static ParallelType register(String name) {
        return GTRegistries.PARALLEL_TYPES.register(name, new ParallelType(name));
    }
}
