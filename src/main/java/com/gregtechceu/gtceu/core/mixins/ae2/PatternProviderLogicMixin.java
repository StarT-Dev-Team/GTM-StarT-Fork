package com.gregtechceu.gtceu.core.mixins.ae2;

import com.gregtechceu.gtceu.integration.ae2.utils.AE2PatternCircuitHandler;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.KeyCounter;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PatternProviderLogic.class, remap = false)
public abstract class PatternProviderLogicMixin {

    @Shadow
    private PatternProviderLogicHost host;

    @Inject(method = "pushPattern", at = @At("HEAD"))
    private void gtceu$onPushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder,
                                     CallbackInfoReturnable<Boolean> cir) {
        AE2PatternCircuitHandler.handlePatternPush(this.host, patternDetails, inputHolder);
    }
}
