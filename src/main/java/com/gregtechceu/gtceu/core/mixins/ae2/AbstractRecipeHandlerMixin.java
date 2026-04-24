package com.gregtechceu.gtceu.core.mixins.ae2;

import com.gregtechceu.gtceu.integration.emi.multipage.MultiblockInfoEmiRecipe;
import com.gregtechceu.gtceu.integration.emi.recipe.GTEmiRecipe;

import dev.emi.emi.api.recipe.EmiRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "appeng.integration.modules.emi.AbstractRecipeHandler", remap = false)
public class AbstractRecipeHandlerMixin {

    @Inject(method = "supportsRecipe", at = @At("HEAD"), cancellable = true)
    public void injectSupportsRecipe(EmiRecipe recipe, CallbackInfoReturnable<Boolean> cir) {
        if (recipe instanceof GTEmiRecipe || recipe instanceof MultiblockInfoEmiRecipe) {
            cir.setReturnValue(false);
        }
    }
}
