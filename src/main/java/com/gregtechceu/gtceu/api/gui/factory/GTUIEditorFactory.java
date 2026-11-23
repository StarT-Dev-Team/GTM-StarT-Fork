package com.gregtechceu.gtceu.api.gui.factory;

import com.lowdragmc.lowdraglib2.gui.editor.UIEditor;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;

import net.minecraft.server.level.ServerPlayer;

public class GTUIEditorFactory {

    public static final GTUIEditorFactory INSTANCE = new GTUIEditorFactory();

    private GTUIEditorFactory() {}

    public boolean openUI(ServerPlayer player) {
        return PlayerUIMenuType.openUI(player, UIEditor.WINDOW_ID);
    }
}
