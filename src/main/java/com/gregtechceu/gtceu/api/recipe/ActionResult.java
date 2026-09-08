package com.gregtechceu.gtceu.api.recipe;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;

import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

/**
 * @param isSuccess is action success
 * @param reason    if fail, fail reason
 */
public record ActionResult(boolean isSuccess, @Nullable RecipeFailureReason reason,
                           @Nullable RecipeCapability<?> capability,
                           @Nullable IO io) {

    private static final RecipeFailureReason NO_CONTENTS = new RecipeFailureReason(
            Component.translatable("gtceu.recipe_logic.no_contents"), true);

    private static final RecipeFailureReason NO_CAPABILITIES = new RecipeFailureReason(
            Component.translatable("gtceu.recipe_logic.no_contents"), true);

    public final static ActionResult SUCCESS = new ActionResult(true, null, null, null);
    public final static ActionResult FAIL_NO_REASON = new ActionResult(false, null, null, null);
    public final static ActionResult PASS_NO_CONTENTS = new ActionResult(true, NO_CONTENTS, null, null);
    public final static ActionResult FAIL_NO_CAPABILITIES = new ActionResult(false, NO_CAPABILITIES, null, null);

    public static ActionResult fail(@Nullable Component component, boolean preventCache,
                                    @Nullable RecipeCapability<?> capability, IO io) {
        return new ActionResult(false, new RecipeFailureReason(component, preventCache), capability, io);
    }

    public static ActionResult fail(@Nullable Component component, @Nullable RecipeCapability<?> capability, IO io) {
        return fail(component, true, capability, io);
    }

    public Component reasonComponent() {
        return reason == null ? Component.empty() : reason.component();
    }
}
