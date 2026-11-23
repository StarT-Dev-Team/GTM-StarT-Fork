package com.gregtechceu.gtceu.api.gui.widget.directional.handlers;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.widget.ToggleButton;
import com.gregtechceu.gtceu.api.gui.widget.directional.IDirectionalConfigHandler;
import com.gregtechceu.gtceu.api.machine.feature.IAutoOutputFluid;
import com.gregtechceu.gtceu.api.machine.feature.IAutoOutputItem;
import com.gregtechceu.gtceu.data.lang.LangHandler;

import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.utils.data.BlockPosFace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class AutoOutputItemConfigHandler implements IDirectionalConfigHandler {

    private static final IGuiTexture TEXTURE_OFF = IGuiTexture.group(
            GuiTextures.VANILLA_BUTTON,
            GuiTextures.IO_CONFIG_ITEM_MODES_BUTTON.copy().setSprite(0, 0, 36, 36));
    private static final IGuiTexture TEXTURE_OUTPUT = IGuiTexture.group(
            GuiTextures.VANILLA_BUTTON,
            GuiTextures.IO_CONFIG_ITEM_MODES_BUTTON.copy().setSprite(0, 36, 36, 36));
    private static final IGuiTexture TEXTURE_AUTO = IGuiTexture.group(
            GuiTextures.VANILLA_BUTTON,
            GuiTextures.IO_CONFIG_ITEM_MODES_BUTTON.copy().setSprite(0, 72, 36, 36));

    private final IAutoOutputItem machine;
    private @Nullable Direction side;

    public AutoOutputItemConfigHandler(IAutoOutputItem machine) {
        this.machine = machine;
    }

    @Override
    public UIElement getSideSelectorWidget(FancyMachineUIWidget machineUI) {
        var group = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(1));

        var ioModeButton = new Button().noText();
        ioModeButton.layout(layout -> layout.width(18).height(18));
        var texture = DynamicTexture.of(this::currentTexture);
        ioModeButton.buttonStyle(style -> style.baseTexture(texture).hoverTexture(texture).pressedTexture(texture));
        ioModeButton.addEventListener(UIEvents.HOVER_TOOLTIPS,
                event -> event.hoverTooltips = new HoverTooltips(currentTooltips(), null, null, null));
        ioModeButton.setOnServerClick(event -> onIOModePressed());
        group.addChild(ioModeButton);

        group.addChild(new ToggleButton(
                GuiTextures.BUTTON_ITEM_OUTPUT,
                machine::isAllowInputFromOutputSideItems, machine::setAllowInputFromOutputSideItems)
                .setShouldUseBaseBackground().setTooltipText("gtceu.gui.item_auto_output.allow_input"));

        return group;
    }

    private IGuiTexture currentTexture() {
        if (side == null || machine.getOutputFacingItems() != side) {
            return TEXTURE_OFF;
        }
        return machine.isAutoOutputItems() ? TEXTURE_AUTO : TEXTURE_OUTPUT;
    }

    private List<Component> currentTooltips() {
        if (side == null) {
            return new ArrayList<>(LangHandler.getMultiLang("gtceu.gui.item_auto_output.unselected"));
        } else if (machine.getOutputFacingItems() == side) {
            return List.of(Component.translatable(machine.isAutoOutputItems() ?
                    "gtceu.gui.item_auto_output.enabled" : "gtceu.gui.item_auto_output.disabled"));
        }
        return new ArrayList<>(LangHandler.getMultiLang("gtceu.gui.item_auto_output.other_direction"));
    }

    private void onIOModePressed() {
        if (this.side == null)
            return;

        if (machine.getOutputFacingItems() == this.side) {
            machine.setAutoOutputItems(!machine.isAutoOutputItems());
        } else {
            machine.setAutoOutputItems(false);
            machine.setOutputFacingItems(this.side);
        }
    }

    @Override
    public void onSideSelected(BlockPos pos, Direction side) {
        this.side = side;
    }

    @Override
    public ScreenSide getScreenSide() {
        return ScreenSide.LEFT;
    }

    @Override
    public void handleClick(int button, Direction direction) {
        if (!canHandleClick(button) || !machine.hasAutoOutputItem() || side == null)
            return;

        if (machine.getOutputFacingItems() != side) {
            machine.setOutputFacingItems(side);
            machine.setAutoOutputItems(false);
        } else {
            machine.setAutoOutputItems(!machine.isAutoOutputItems());
        }
    }

    private boolean canHandleClick(int button) {
        if (button == 0)
            return true;

        return !(machine instanceof IAutoOutputFluid) && button == 1;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderOverlay(Scene scene, BlockPosFace blockPosFace) {
        if (machine.getOutputFacingItems() != blockPosFace.facing())
            return;

        scene.drawFacingBorder(new PoseStack(), blockPosFace,
                machine.isAutoOutputItems() ? 0xffff6e0f : 0x8fff6e0f, 1);
    }

    @Override
    public void addAdditionalUIElements(UIElement parent) {
        var text = new Label().setText(Component.translatable("gtceu.gui.auto_output.name"));
        text.textStyle(style -> style.textColor(0xffff6e0f).textShadow(false));
        text.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(4).top(4));
        text.addEventListener(UIEvents.TICK, event -> text
                .setDisplay(machine.isAutoOutputItems() && machine.getOutputFacingItems() != null));
        parent.addChild(text);
    }
}
