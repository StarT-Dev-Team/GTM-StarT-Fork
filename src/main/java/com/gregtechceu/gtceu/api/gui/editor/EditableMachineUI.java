package com.gregtechceu.gtceu.api.gui.editor;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;

import net.minecraft.resources.ResourceLocation;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class EditableMachineUI implements IEditableUI<UIElement, MetaMachine> {

    @Getter
    final String groupName;
    @Getter
    final ResourceLocation uiPath;
    final Supplier<UIElement> creator;
    final BiConsumer<UIElement, MetaMachine> binder;

    public EditableMachineUI(String groupName, ResourceLocation uiPath, Supplier<UIElement> creator,
                             BiConsumer<UIElement, MetaMachine> binder) {
        this.groupName = groupName;
        this.uiPath = uiPath;
        this.creator = creator;
        this.binder = binder;
    }

    @Override
    public UIElement createDefault() {
        return creator.get();
    }

    @Override
    public void setupUI(UIElement template, MetaMachine machine) {
        binder.accept(template, machine);
    }

    @Nullable
    public UIElement createCustomUI() {
        var document = XmlUtils.loadXml(new ResourceLocation(uiPath.getNamespace(),
                "ui/machine/%s.xml".formatted(uiPath.getPath())));
        if (document == null) return null;
        return UI.of(document).rootElement;
    }
}
