package com.gregtechceu.gtceu.api.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record RecipeFailureReason(@Nullable Component component, boolean preventCache) {

    public Component component() {
        if (component == null) return Component.empty();
        return component;
    }
}
