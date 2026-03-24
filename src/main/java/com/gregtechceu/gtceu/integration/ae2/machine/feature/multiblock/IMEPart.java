package com.gregtechceu.gtceu.integration.ae2.machine.feature.multiblock;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;

public interface IMEPart extends IMultiPart {

    int getTicksPerCycle();

    void setTicksPerCycle(int newSize);
}
