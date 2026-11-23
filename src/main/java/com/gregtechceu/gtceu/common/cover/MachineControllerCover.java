package com.gregtechceu.gtceu.common.cover;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.IUICover;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.IntInput;
import com.gregtechceu.gtceu.api.gui.widget.ToggleButton;
import com.gregtechceu.gtceu.api.machine.MachineCoverContainer;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.cover.data.ControllerMode;

import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MachineControllerCover extends CoverBehavior implements IUICover {

    private CustomItemStackHandler sideCoverSlot;
    private Button modeButton;

    @Persisted
    @Getter
    private boolean isInverted = false;

    @Persisted
    @Getter
    private int minRedstoneStrength = 1;

    @Persisted
    @DescSynced
    @Getter
    @Nullable
    private ControllerMode controllerMode = ControllerMode.MACHINE;

    @Getter
    @Accessors(fluent = true)
    @Persisted
    private boolean preventPowerFail = false;

    public MachineControllerCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
    }

    @Override
    public boolean canAttach() {
        return super.canAttach() && !getAllowedModes().isEmpty();
    }

    @Override
    public void onAttached(ItemStack itemStack, @Nullable ServerPlayer player) {
        super.onAttached(itemStack, player);

        var allowedModes = getAllowedModes();
        setControllerMode(allowedModes.isEmpty() ? null : allowedModes.get(0));
    }

    @Override
    public void onRemoved() {
        super.onRemoved();

        resetCurrentControllable();
    }

    @Override
    public boolean canConnectRedstone() {
        return true;
    }

    @Override
    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChanged(block, fromPos, isMoving);

        updateInput();
    }

    public void setControllerMode(@Nullable ControllerMode controllerMode) {
        resetCurrentControllable();

        this.controllerMode = controllerMode;
        updateAll();
    }

    public void setMinRedstoneStrength(int minRedstoneStrength) {
        this.minRedstoneStrength = minRedstoneStrength;
        updateAll();
    }

    public void setInverted(boolean inverted) {
        isInverted = inverted;
        updateAll();
    }

    private void updateAll() {
        updateInput();
        updateUI();
    }

    ///////////////////////////////////////////////////
    // *********** CONTROLLER LOGIC ***********//
    ///////////////////////////////////////////////////

    @Nullable
    private IControllable getControllable(@Nullable Direction side) {
        if (side == null) {
            return GTCapabilityHelper.getControllable(coverHolder.getLevel(), coverHolder.getPos(), null);
        }

        if (coverHolder.getCoverAtSide(side) instanceof IControllable cover) {
            return cover;
        } else {
            return null;
        }
    }

    private void updateInput() {
        if (controllerMode == null)
            return;

        IControllable controllable = getControllable(controllerMode.side);
        if (controllable != null) {
            controllable.setWorkingEnabled(shouldAllowWorking() && doOthersAllowWorking());
        }
    }

    private void resetCurrentControllable() {
        if (controllerMode == null)
            return;

        IControllable controllable = getControllable(controllerMode.side);
        if (controllable != null) {
            controllable.setWorkingEnabled(doOthersAllowWorking());
        }
    }

    private boolean shouldAllowWorking() {
        boolean shouldAllowWorking = getInputSignal() < minRedstoneStrength;

        return isInverted != shouldAllowWorking;
    }

    private boolean doOthersAllowWorking() {
        return coverHolder.getCovers().stream()
                .filter(cover -> this.attachedSide != cover.attachedSide)
                .filter(cover -> cover instanceof MachineControllerCover)
                .filter(cover -> ((MachineControllerCover) cover).controllerMode == this.controllerMode)
                .allMatch(cover -> ((MachineControllerCover) cover).shouldAllowWorking());
    }

    public List<ControllerMode> getAllowedModes() {
        return Arrays.stream(ControllerMode.values())
                .filter(mode -> mode.side != this.attachedSide)
                .filter(mode -> getControllable(mode.side) != null)
                .collect(Collectors.toList());
    }

    private int getInputSignal() {
        Level level = coverHolder.getLevel();
        BlockPos sourcePos = coverHolder.getPos().relative(attachedSide);

        return level.getSignal(sourcePos, attachedSide);
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////

    public UIElement createUIWidget() {
        if (controllerMode != null && getControllable(controllerMode.side) == null) {
            setControllerMode(null);
        }
        var group = new UIElement().layout(layout -> layout.minWidth(176).paddingAll(5).gapAll(3));

        group.addChild(new Label().setText(Component.translatable("cover.machine_controller.title")));

        var strengthRow = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(3));
        var strength = new IntInput(this::getMinRedstoneStrength, this::setMinRedstoneStrength);
        strength.setMin(1);
        strength.setMax(15);
        strength.layout(layout -> layout.flex(1).height(20));
        strengthRow.addChildren(strength, new ToggleButton(
                GuiTextures.INVERT_REDSTONE_BUTTON, this::isInverted, this::setInverted)
                .isMultiLang()
                .setTooltipText("cover.machine_controller.invert")
                .layout(layout -> layout.width(20).height(20)));
        group.addChild(strengthRow);

        var modeRow = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(3));
        modeButton = new Button().noText();
        modeButton.layout(layout -> layout.flex(1).height(20));
        var modeTexture = DynamicTexture.of(() -> new GuiTextureGroup(
                GuiTextures.VANILLA_BUTTON,
                new TextTexture(controllerMode != null ? controllerMode.localeName : ControllerMode.nullLocaleName)));
        modeButton.buttonStyle(style -> style.baseTexture(modeTexture).hoverTexture(modeTexture)
                .pressedTexture(modeTexture));
        modeButton.setOnServerClick(event -> selectNextMode());

        sideCoverSlot = new CustomItemStackHandler(1);
        var coverSlot = new ItemSlot().bind(new ItemHandlerSlot(sideCoverSlot, 0).setCanTake(player -> false)
                .setCanPlace(stack -> false));
        modeRow.addChildren(modeButton, coverSlot);
        group.addChild(modeRow);

        var powerRow = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(3)
                .alignItems(AlignItems.CENTER).justifyContent(AlignContent.SPACE_BETWEEN));
        powerRow.addChildren(
                new Label().setText(Component.translatable("cover.machine_controller.suspend_powerfail")),
                new ToggleButton(GuiTextures.BUTTON_POWER, this::preventPowerFail, data -> preventPowerFail = data));
        group.addChild(powerRow);

        updateUI();

        return group;
    }

    private void selectNextMode() {
        var allowedModes = getAllowedModes();

        setControllerMode(allowedModes.stream()
                .dropWhile(mode -> this.controllerMode != null && mode != this.controllerMode)
                .skip(1)
                .findFirst()
                .orElse(allowedModes.isEmpty() ? null : allowedModes.get(0)));

        updateAll();
    }

    private void updateUI() {
        updateCoverSlot();
    }

    private void updateCoverSlot() {
        if (sideCoverSlot == null) {
            return;
        }

        if (controllerMode == null) {
            sideCoverSlot.setStackInSlot(0, ItemStack.EMPTY);
            sideCoverSlot.onContentsChanged(0);
        } else {
            var side = controllerMode.side;
            if (side == null && coverHolder instanceof MachineCoverContainer coverContainer) {
                sideCoverSlot.setStackInSlot(0, coverContainer.getMachine().getDefinition().asStack());
            } else {
                var cover = coverHolder.getCoverAtSide(side);
                if (cover != null) {
                    sideCoverSlot.setStackInSlot(0, cover.getAttachItem().copy());
                } else {
                    sideCoverSlot.setStackInSlot(0, ItemStack.EMPTY);
                }
            }
            sideCoverSlot.onContentsChanged(0);
        }
    }
}
