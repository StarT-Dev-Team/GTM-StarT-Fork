package com.gregtechceu.gtceu.api.gui.widget.directional;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.scene.ISceneBlockRenderHook;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.utils.data.BlockPosFace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CombinedDirectionalConfigurator extends UIElement {

    public static final String SIDE_MESSAGE = "gtceu:directional_side";
    public static final String CLICK_MESSAGE = "gtceu:directional_click";

    protected final IDirectionalConfigHandler[] configHandlers;
    private final FancyMachineUIWidget machineUI;
    private final MetaMachine machine;

    protected final UIElement sceneHost = new UIElement();
    protected @Nullable Scene scene;

    protected @Nullable Direction selectedSide;

    public CombinedDirectionalConfigurator(FancyMachineUIWidget machineUI, IDirectionalConfigHandler[] configHandlers,
                                           MetaMachine machine) {
        this.machineUI = machineUI;
        this.configHandlers = configHandlers;
        this.machine = machine;

        layout(layout -> layout.minWidth(168).minHeight(80).flexGrow(1));
        style(style -> style.backgroundTexture(GuiTextures.BACKGROUND_INVERSE));

        sceneHost.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(4).top(4).right(4).bottom(4));
        addChild(sceneHost);

        if (LDLib2.isRemote()) {
            scene = createScene();
            sceneHost.addChild(scene);
        }

        for (var configHandler : configHandlers) {
            configHandler.addAdditionalUIElements(this);
        }

        addConfigWidgets();

        onMessage(SIDE_MESSAGE, tag -> applySide(GTUtil.DIRECTIONS[tag.getByte("side")]));
        onMessage(CLICK_MESSAGE, tag -> {
            var side = GTUtil.DIRECTIONS[tag.getByte("side")];
            for (var configHandler : configHandlers) {
                configHandler.handleClick(tag.getInt("button"), side);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private Scene createScene() {
        var pos = this.machine.getPos();

        var scene = new Scene();
        scene.createScene(this.machine.getLevel());
        scene.setTickWorld(false);
        scene.layout(layout -> layout.widthPercent(100).heightPercent(100));
        scene.setRenderedCore(List.of(pos), null);
        scene.setRenderSelect(false);
        scene.setOnSelected(this::onSideSelected);

        scene.getRenderer().addRenderedBlocks(
                List.of(pos.above(), pos.below(), pos.north(), pos.south(), pos.east(), pos.west()),
                new ISceneBlockRenderHook() {

                    @Override
                    @OnlyIn(Dist.CLIENT)
                    public void apply(RenderType layer) {
                        RenderSystem.enableBlend();
                        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
                    }
                });

        scene.setAfterWorldRender(this::renderOverlays);

        var player = Minecraft.getInstance().player;
        if (player != null) {
            var playerRotation = player.getRotationVector();
            scene.setCameraYawAndPitch(playerRotation.y - 90, playerRotation.x);
        }
        scene.style(style -> style.backgroundTexture(ColorPattern.BLACK.rectTexture()));

        scene.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 0 || selectedSide == null) return;
            var hover = scene.getLastHoverPosFace();
            if (hover != null && hover.pos().equals(machine.getPos()) && hover.facing() == selectedSide) {
                sendClick(event.button);
            }
        });
        return scene;
    }

    @OnlyIn(Dist.CLIENT)
    private void renderOverlays(Scene scene) {
        for (Direction face : GTUtil.DIRECTIONS) {
            for (var configHandler : configHandlers) {
                configHandler.renderOverlay(scene, new BlockPosFace(machine.getPos(), face));
            }
        }
    }

    private void addConfigWidgets() {
        var left = new UIElement().layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(6).bottom(6)
                .flexDirection(FlexDirection.COLUMN_REVERSE).gapAll(3));
        var right = new UIElement().layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).right(6).bottom(6)
                .flexDirection(FlexDirection.COLUMN_REVERSE).gapAll(3));

        for (var configHandler : configHandlers) {
            var widget = configHandler.getSideSelectorWidget(machineUI);
            if (widget == null)
                continue;

            switch (configHandler.getScreenSide()) {
                case LEFT -> left.addChild(widget);
                case RIGHT -> right.addChild(widget);
            }
        }

        addChildren(left, right);
    }

    protected void onSideSelected(BlockPos pos, Direction side) {
        if (!pos.equals(machine.getPos()))
            return;

        if (this.selectedSide == side) {
            sendClick(0);
            return;
        }

        applySide(side);
        var tag = new CompoundTag();
        tag.putByte("side", (byte) side.ordinal());
        sendMessage(SIDE_MESSAGE, tag);
    }

    protected void applySide(Direction side) {
        this.selectedSide = side;
        for (var configHandler : this.configHandlers) {
            configHandler.onSideSelected(machine.getPos(), side);
        }
    }

    private void sendClick(int button) {
        if (selectedSide == null) return;
        var tag = new CompoundTag();
        tag.putInt("button", button);
        tag.putByte("side", (byte) selectedSide.ordinal());
        sendMessage(CLICK_MESSAGE, tag);
    }
}
