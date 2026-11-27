package com.gregtechceu.gtceu.integration.ae2.machine.feature.multiblock;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton;
import com.gregtechceu.gtceu.api.gui.texture.UVSpriteTexture;

import net.minecraft.network.chat.Component;

import appeng.api.stacks.GenericStack;

import java.util.List;
import java.util.function.Predicate;

public interface IAutoPullPart extends IMEPart {

    boolean isAutoPull();

    void setAutoPull(boolean autoPull);

    void setAutoPullTest(Predicate<GenericStack> test);

    @Override
    default void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        configuratorPanel.attachConfigurators(new IFancyConfiguratorButton.Toggle(
                UVSpriteTexture.of(GuiTextures.BUTTON_AUTO_PULL, 0, 0, 1, 0.5f),
                UVSpriteTexture.of(GuiTextures.BUTTON_AUTO_PULL, 0, 0.5f, 1, 0.5f),
                this::isAutoPull,
                (event, pressed) -> setAutoPull(pressed))
                .setTooltipsSupplier(pressed -> List.of(Component.translatable("gtceu.gui.me_bus.auto_pull_button"))));
    }
}
