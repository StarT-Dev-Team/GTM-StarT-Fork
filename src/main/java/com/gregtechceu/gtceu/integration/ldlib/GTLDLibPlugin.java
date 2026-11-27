package com.gregtechceu.gtceu.integration.ldlib;

import com.gregtechceu.gtceu.common.data.GTSyncedFieldAccessors;

import com.lowdragmc.lowdraglib2.plugin.ILDLibPlugin;
import com.lowdragmc.lowdraglib2.plugin.LDLibPlugin;

@LDLibPlugin
public class GTLDLibPlugin implements ILDLibPlugin {

    @Override
    public void onLoad() {
        GTSyncedFieldAccessors.init();
    }
}
