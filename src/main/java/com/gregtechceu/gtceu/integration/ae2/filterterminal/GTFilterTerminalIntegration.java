package com.gregtechceu.gtceu.integration.ae2.filterterminal;

import com.gregtechceu.gtceu.integration.ae2.machine.MEInputBusPartMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.MEInputHatchPartMachine;

import appeng.api.filterterminal.FilterTerminalTargetRegistry;

public final class GTFilterTerminalIntegration {

    private GTFilterTerminalIntegration() {}

    public static void init() {
        FilterTerminalTargetRegistry.register(MEInputBusPartMachine.class, MEBusPartMachineTargetProvider.INSTANCE);
        FilterTerminalTargetRegistry.register(MEInputHatchPartMachine.class, MEHatchPartMachineTargetProvider.INSTANCE);
    }
}
