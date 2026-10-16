package com.gregtechceu.gtceu.core.mixins.ae2;

import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetails.IInput;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.pattern.AEProcessingPattern;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = AEProcessingPattern.class, remap = false)
public abstract class AEProcessingPatternMixin {

    @Shadow
    private GenericStack[] sparseInputs;

    @Inject(method = "getInputs", at = @At("RETURN"), cancellable = true)
    private void gtceu$filterCircuitInputs(CallbackInfoReturnable<IInput[]> cir) {
        IInput[] original = cir.getReturnValue();
        if (original == null || original.length == 0) return;
        List<IInput> filtered = new ArrayList<>(original.length);
        for (IInput input : original) {
            if (input == null || input.getPossibleInputs() == null || hasCircuit(input.getPossibleInputs()))
                continue;

            filtered.add(input);
        }

        if (filtered.size() < original.length) cir.setReturnValue(filtered.toArray(new IInput[0]));
    }

    @Inject(method = "pushInputsToExternalInventory", at = @At("HEAD"), cancellable = true)
    private void gtceu$pushInputsSkippingCircuits(KeyCounter[] inputHolder,
                                                  IPatternDetails.PatternInputSink inputSink,
                                                  CallbackInfo ci) {
        if (this.sparseInputs == null) return;

        if (!hasCircuit(this.sparseInputs)) return;

        ci.cancel();

        var allInputs = new KeyCounter();
        for (var counter : inputHolder) {
            allInputs.addAll(counter);
        }

        for (var sparseInput : this.sparseInputs) {

            if (sparseInput == null || isCircuit(sparseInput)) continue;

            var key = sparseInput.what();
            var amount = sparseInput.amount();
            long available = allInputs.get(key);

            if (available < amount) {
                throw new RuntimeException("Expected at least %d of %s when pushing pattern, but only %d available"
                        .formatted(amount, key, available));
            }

            inputSink.pushInput(key, amount);
            allInputs.remove(key, amount);
        }

        for (var entry : allInputs) {
            inputSink.pushInput(entry.getKey(), entry.getLongValue());
        }
    }

    private boolean hasCircuit(@NotNull GenericStack[] stacks) {
        for (GenericStack stack : stacks) {
            if (isCircuit(stack)) return true;
        }
        return false;
    }

    private boolean isCircuit(GenericStack stack) {
        return stack != null && stack.what() instanceof AEItemKey itemKey &&
                IntCircuitBehaviour.isIntegratedCircuit(itemKey.toStack());
    }
}
