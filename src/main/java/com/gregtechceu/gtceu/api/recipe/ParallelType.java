package com.gregtechceu.gtceu.api.recipe;

import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import com.mojang.serialization.Codec;
import lombok.Getter;

public class ParallelType {

    public static final Codec<ParallelType> DIRECT_CODEC = GTRegistries.PARALLEL_TYPES.codec();

    @Getter
    private final String name;

    public ParallelType(String name) {
        this.name = name;
    }

    public MutableComponent format(int parallel) {
        var parallels = Component.literal(FormattingUtil.formatNumbers(parallel)).withStyle(ChatFormatting.DARK_PURPLE);
        return Component.translatable("gtceu.parallel_type." + name, parallels);
    }
}
