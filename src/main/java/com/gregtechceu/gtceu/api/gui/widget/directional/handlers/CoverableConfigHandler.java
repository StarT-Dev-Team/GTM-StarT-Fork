package com.gregtechceu.gtceu.api.gui.widget.directional.handlers;

import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.IUICover;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.widget.CoverConfigurator;
import com.gregtechceu.gtceu.api.gui.widget.PredicatedButton;
import com.gregtechceu.gtceu.api.gui.widget.directional.IDirectionalConfigHandler;
import com.gregtechceu.gtceu.api.item.IComponentItem;
import com.gregtechceu.gtceu.api.item.component.IItemComponent;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.item.CoverPlaceBehavior;

import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.MCSprites;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import dev.vfyjxf.taffy.style.FlexDirection;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CoverableConfigHandler implements IDirectionalConfigHandler {

    private static final String TOGGLE_TAB_MESSAGE = "gtceu:cover_config_toggle";
    private static final IGuiTexture CONFIG_BTN_TEXTURE = IGuiTexture.group(GuiTextures.VANILLA_BUTTON,
            GuiTextures.IO_CONFIG_COVER_SETTINGS);

    private final ICoverable machine;
    private final CustomItemStackHandler handler;
    private @Nullable Direction side;

    private ConfiguratorPanel panel;
    private @Nullable ConfiguratorPanel.FloatingTab coverConfigurator;

    private FancyMachineUIWidget machineUI;
    private ItemSlot slotWidget;
    private @Nullable CoverBehavior coverBehavior;

    public CoverableConfigHandler(ICoverable machine) {
        this.machine = machine;
        this.handler = createItemStackHandler();
    }

    private CustomItemStackHandler createItemStackHandler() {
        var handler = new CustomItemStackHandler(1) {

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };

        handler.setFilter(itemStack -> {
            if (itemStack.isEmpty()) return true;
            if (this.side == null) return false;
            return CoverPlaceBehavior.isCoverBehaviorItem(itemStack, () -> false,
                    coverDef -> ICoverable.canPlaceCover(coverDef, this.machine));
        });

        return handler;
    }

    @Override
    public UIElement getSideSelectorWidget(FancyMachineUIWidget machineUI) {
        var group = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(1));
        this.machineUI = machineUI;
        this.panel = machineUI.getConfiguratorPanel();

        var button = new PredicatedButton(
                () -> side != null && coverBehavior != null && machine.getCoverAtSide(side) instanceof IUICover);
        button.layout(layout -> layout.width(18).height(18));
        button.buttonStyle(style -> style
                .baseTexture(IGuiTexture.group(GuiTextures.TOGGLE_BUTTON_BACK.copy().setSprite(0, 0, 18, 18),
                        GuiTextures.IO_CONFIG_COVER_SETTINGS))
                .hoverTexture(IGuiTexture.group(
                        GuiTextures.TOGGLE_BUTTON_BACK.copy().setColor(0xffdddddd).setSprite(0, 0, 18, 18),
                        GuiTextures.IO_CONFIG_COVER_SETTINGS))
                .pressedTexture(IGuiTexture.group(GuiTextures.TOGGLE_BUTTON_BACK.copy().setSprite(0, 18, 18, 18),
                        GuiTextures.IO_CONFIG_COVER_SETTINGS)));
        button.setOnClick(event -> {
            toggleConfigTab();
            group.sendMessage(TOGGLE_TAB_MESSAGE);
            button.setActive(true); // .setActive(coverConfigurator != null);
        });
        group.onMessage(TOGGLE_TAB_MESSAGE, tag -> {
            toggleConfigTab();
            button.setActive(true); // .setActive(coverConfigurator != null);
        });

        var slotHandler = new ItemHandlerSlot(handler, 0).setCanPlace(stack -> side != null &&
                CoverPlaceBehavior.isCoverBehaviorItem(stack, () -> false,
                        def -> def.createCoverBehavior(machine, side).canAttach()));
        slotHandler.addChangeListener(this::coverItemChanged);
        slotWidget = new ItemSlot().bind(slotHandler);
        slotWidget.style(style -> style.backgroundTexture(
                IGuiTexture.group(GuiTextures.SLOT, GuiTextures.IO_CONFIG_COVER_SLOT_OVERLAY)));

        group.addChildren(button, slotWidget);

        checkCoverBehaviour();

        return group;
    }

    private void coverItemChanged() {
        closeConfigTab();

        if (!(machineUI.getPlayer() instanceof ServerPlayer serverPlayer) || side == null)
            return;

        var item = handler.getStackInSlot(0);
        if (machine.getCoverAtSide(side) != null) {
            machine.removeCover(false, side, serverPlayer);
        }

        if (!item.isEmpty() && machine.getCoverAtSide(side) == null) {
            if (item.getItem() instanceof IComponentItem componentItem) {
                for (IItemComponent component : componentItem.getComponents()) {
                    if (component instanceof CoverPlaceBehavior placeBehavior) {
                        machine.placeCoverOnSide(side, item, placeBehavior.coverDefinition(), serverPlayer);
                        break;
                    }
                }
            }
        }

        checkCoverBehaviour();
    }

    @Override
    public void onSideSelected(BlockPos pos, Direction side) {
        this.side = side;
        checkCoverBehaviour();
        closeConfigTab();
    }

    private void updateWidgetVisibility() {
        if (slotWidget == null) return;
        slotWidget.setDisplay(this.side != null);
        slotWidget.setActive(this.side != null);
    }

    public void checkCoverBehaviour() {
        if (side == null)
            return;

        var coverBehaviour = machine.getCoverAtSide(side);
        if (coverBehaviour != this.coverBehavior) {
            this.coverBehavior = coverBehaviour;

            var attachItem = coverBehaviour == null ? ItemStack.EMPTY : coverBehaviour.getAttachItem();
            handler.setStackInSlot(0, attachItem);
            handler.onContentsChanged(0);
        }

        updateWidgetVisibility();
    }

    private void toggleConfigTab() {
        if (this.coverConfigurator == null)
            openConfigTab();
        else
            closeConfigTab();
    }

    private void openConfigTab() {
        if (side == null || panel == null) return;

        var configurator = new CoverConfigurator(this.machine, this.side, this.coverBehavior) {

            @Override
            public Component getTitle() {
                if (side == null || !(coverable.getCoverAtSide(side) instanceof IUICover iuiCover))
                    return Component.empty();
                return iuiCover.getCoverUITitle();
            }

            @Override
            public IGuiTexture getIcon() {
                return IGuiTexture.group(MCSprites.RECT_BORDER, GuiTextures.CLOSE_ICON);
            }

            @Override
            public UIElement createConfigurator() {
                var group = new UIElement().layout(layout -> layout.minWidth(120).minHeight(80));

                if (side == null || !(coverable.getCoverAtSide(side) instanceof IUICover iuiCover))
                    return group;

                var coverUI = iuiCover.createUIWidget();
                coverUI.layout(layout -> layout.marginLeft(-1));
                group.addChild(coverUI);
                return group;
            }
        };

        var tab = this.panel.createFloatingTab(configurator);
        this.coverConfigurator = tab;
        this.panel.addChild(tab);
        this.panel.expandTab(tab);

        tab.onClose(() -> this.coverConfigurator = null);
    }

    private void closeConfigTab() {
        if (this.coverConfigurator != null && this.panel != null) {
            this.panel.collapseTab();
        }
    }

    @Override
    public ScreenSide getScreenSide() {
        return ScreenSide.RIGHT;
    }
}
