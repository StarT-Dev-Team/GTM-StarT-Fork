package com.gregtechceu.gtceu.api.machine.feature;

import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.fancy.*;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CombinedDirectionalFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.MachineModeFancyConfigurator;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.utils.data.BlockInfo;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import dev.vfyjxf.taffy.style.TaffyPosition;

import java.util.ArrayList;
import java.util.List;

public interface IFancyUIMachine extends IUIMachine, IFancyUIProvider {

    @Override
    default ModularUI createUI(Player entityPlayer) {
        return UITemplate.modularUI(new FancyMachineUIWidget(this, 176, 166), entityPlayer);
    }

    /**
     * We should not override this method in general, and use {@link IFancyUIMachine#createUIWidget()} instead,
     */
    @Override
    default UIElement createMainPage(FancyMachineUIWidget widget) {
        var editableUI = self().getDefinition().getEditableUI();
        if (editableUI != null) {
            var template = editableUI.createCustomUI();
            if (template == null) {
                template = editableUI.createDefault();
            }
            editableUI.setupUI(template, self());
            return template;
        }
        return createUIWidget();
    }

    /**
     * Create the core widget of this machine.
     */
    default UIElement createUIWidget() {
        var group = new UIElement();
        group.layout(l -> l.width(100).height(100));
        if (isRemote()) {
            var image = new UIElement();
            image.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left((100 - 48) / 2f).top(60).width(48)
                    .height(16));
            image.style(s -> s.background(GuiTextures.SCENE));
            var world = new TrackedDummyWorld();
            world.addBlock(BlockPos.ZERO, BlockInfo.fromBlockState(self().getBlockState()));
            var scene = new Scene();
            scene.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(0).top(0).width(100).height(100));
            scene.useOrtho(true);
            scene.setOrthoRange(0.5f);
            scene.setScalable(false);
            scene.setDraggable(false);
            scene.setRenderFacing(false);
            scene.setRenderSelect(false);
            scene.setTickWorld(false);
            scene.createScene(world);
            scene.setRenderedCore(List.of(BlockPos.ZERO));
            enableSceneAutoRotate(scene);

            group.addChildren(image, scene);
        }
        return group;
    }

    private static void enableSceneAutoRotate(Scene scene) {
        var updateRotation = new Runnable() {

            private float yaw = Float.NaN;

            @Override
            public void run() {
                var mui = scene.getModularUI();
                var player = Minecraft.getInstance().player;
                if (mui == null || player == null) return;
                if (Float.isNaN(yaw)) {
                    var playerRotation = player.getRotationVector();
                    yaw = playerRotation.y - 90;
                }
                scene.setCameraYawAndPitch(yaw + mui.getTickCounter() * 2f, scene.getRotationPitch());
            }
        };

        scene.addEventListener(UIEvents.MUI_CHANGED, e -> updateRotation.run());
        scene.addEventListener(UIEvents.TICK, e -> updateRotation.run());
    }

    @Override
    default IGuiTexture getTabIcon() {
        return new ItemStackTexture(self().getDefinition().getItem());
    }

    @Override
    default void attachSideTabs(TabsWidget sideTabs) {
        sideTabs.setMainTab(this);

        if (this instanceof IRecipeLogicMachine rLMachine && rLMachine.getRecipeTypes().length > 1) {
            sideTabs.attachSubTab(new MachineModeFancyConfigurator(rLMachine));
        }
        var directionalConfigurator = CombinedDirectionalFancyConfigurator.of(self(), self());
        if (directionalConfigurator != null)
            sideTabs.attachSubTab(directionalConfigurator);
    }

    @Override
    default void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        if (this instanceof IControllable controllable) {
            configuratorPanel.attachConfigurators(new IFancyConfiguratorButton.Toggle(
                    GuiTextures.BUTTON_POWER.copy().setSprite(0, 0, 18, 18),
                    GuiTextures.BUTTON_POWER.copy().setSprite(0, 18, 18, 18),
                    controllable::isWorkingEnabled, (event, pressed) -> controllable.setWorkingEnabled(pressed))
                    .setTooltipsSupplier(pressed -> List.of(
                            Component.translatable(
                                    pressed ? "behaviour.soft_hammer.enabled" : "behaviour.soft_hammer.disabled"))));
        }
        if (this instanceof MetaMachine machine) {
            for (var direction : Direction.values()) {
                if (machine.getCoverContainer().hasCover(direction)) {
                    var configurator = machine.getCoverContainer().getCoverAtSide(direction).getConfigurator();
                    if (configurator != null)
                        configuratorPanel.attachConfigurators(configurator);
                }
            }
        }
    }

    @Override
    default void attachAutoPushConfigurators(ConfiguratorPanel configuratorPanel) {
        if (this instanceof MetaMachine machine) {
            for (var direction : Direction.values()) {
                if (machine.getCoverContainer().hasCover(direction)) {
                    var configurator = machine.getCoverContainer().getCoverAtSide(direction).getConfigurator();
                    if (configurator != null)
                        configuratorPanel.attachConfigurators(configurator);
                }
            }
        }
    }

    @Override
    default void attachTooltips(TooltipsPanel tooltipsPanel) {
        tooltipsPanel.attachTooltips(self());
        self().getTraits().stream().filter(IFancyTooltip.class::isInstance).map(IFancyTooltip.class::cast)
                .forEach(tooltipsPanel::attachTooltips);
    }

    @Override
    default List<Component> getTabTooltips() {
        var list = new ArrayList<Component>();
        list.add(Component.translatable(self().getDefinition().getDescriptionId()));
        return list;
    }

    @Override
    default Component getTitle() {
        return Component.translatable(self().getDefinition().getDescriptionId());
    }
}
