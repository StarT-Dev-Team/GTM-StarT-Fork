package com.gregtechceu.gtceu.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.ICentralMonitor;
import com.gregtechceu.gtceu.api.capability.IMonitorComponent;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.widget.IntInput;
import com.gregtechceu.gtceu.api.gui.widget.MenuSlots;
import com.gregtechceu.gtceu.api.item.IComponentItem;
import com.gregtechceu.gtceu.api.item.component.IItemComponent;
import com.gregtechceu.gtceu.api.item.component.IMonitorModuleItem;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IDataInfoProvider;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockDisplayText;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;
import com.gregtechceu.gtceu.api.pattern.*;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.item.PortableScannerBehavior;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.monitor.MonitorGroup;
import com.gregtechceu.gtceu.common.machine.trait.CentralMonitorLogic;
import com.gregtechceu.gtceu.common.network.GTNetwork;
import com.gregtechceu.gtceu.common.network.packets.SCPacketMonitorGroupNBTChange;
import com.gregtechceu.gtceu.data.lang.LangHandler;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.VanillaSpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.RequireRerender;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;

import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CentralMonitorMachine extends WorkableElectricMultiblockMachine
                                   implements IMonitorComponent, IDataInfoProvider, IMachineLife, ICentralMonitor {

    @Persisted
    @DescSynced
    @Getter
    private int leftDist = 0, rightDist = 0, upDist = 0, downDist = 0;
    @Persisted
    @DescSynced
    @Getter
    @RequireRerender
    private final List<MonitorGroup> monitorGroups = new ArrayList<>();
    private final Set<IMonitorComponent> selectedComponents = new HashSet<>();
    private final List<IMonitorComponent> selectedTargets = new ArrayList<>();

    private MultiblockState patternFindingState;

    private static TraceabilityPredicate MULTI_PREDICATE = null;

    public CentralMonitorMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    public static TraceabilityPredicate getMultiPredicate() {
        if (MULTI_PREDICATE == null) {
            MULTI_PREDICATE = Predicates.abilities(PartAbility.INPUT_ENERGY)
                    .setMinGlobalLimited(1).setMaxGlobalLimited(2).setPreviewCount(1)
                    .or(Predicates.abilities(PartAbility.DATA_ACCESS).setPreviewCount(1)
                            .or(Predicates.machines(GTMachines.BATTERY_BUFFER_4).setPreviewCount(0))
                            .or(Predicates.machines(GTMachines.BATTERY_BUFFER_16).setPreviewCount(0))
                            .setMaxGlobalLimited(4))
                    .or(Predicates.machines(GTMachines.HULL))
                    .or(Predicates.machines(GTMachines.MONITOR))
                    .or(Predicates.machines(GTMachines.ADVANCED_MONITOR))
                    .or(Predicates.blocks(GTBlocks.CASING_ALUMINIUM_FROSTPROOF.get()));
        }
        return MULTI_PREDICATE;
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        this.clearPatternFindingState();
    }

    @Override
    public CentralMonitorLogic getRecipeLogic() {
        return (CentralMonitorLogic) super.getRecipeLogic();
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new CentralMonitorLogic(this);
    }

    public @Nullable EnergyContainerList getFormedEnergyContainer() {
        return this.energyContainer;
    }

    public void tick() {
        Level level = getLevel();
        if (level == null) {
            return;
        }

        for (MonitorGroup group : monitorGroups) {
            ItemStack stack = group.getItemStackHandler().getStackInSlot(0);
            if (stack.isEmpty() || !(stack.getItem() instanceof IComponentItem componentItem)) {
                continue;
            }

            for (IItemComponent component : componentItem.getComponents()) {
                if (!(component instanceof IMonitorModuleItem module)) {
                    continue;
                }
                module.tick(stack, this, group);
                GTNetwork.sendToAllPlayersTrackingChunk(level.getChunkAt(getPos()),
                        new SCPacketMonitorGroupNBTChange(stack, group, this));
            }
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        this.clearPatternFindingState();
    }

    protected void clearPatternFindingState() {
        if (this.patternFindingState != null)
            this.patternFindingState.clean();
        this.patternFindingState = null;
    }

    protected MultiblockState getPatternFindingState() {
        if (this.patternFindingState == null) {
            this.patternFindingState = new MultiblockState(getLevel(), getPos());
            this.patternFindingState.clean();
        }
        return this.patternFindingState;
    }

    public boolean isValidMonitorBlock(Level level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) return false;

        MultiblockState state = getPatternFindingState();
        if (!state.update(pos, getMultiPredicate())) {
            return false;
        }
        state.io = IO.BOTH;

        return Stream.concat(state.predicate.common.stream(), state.predicate.limited.stream())
                .anyMatch(predicate -> predicate.test(state));
    }

    public void updateStructureDimensions() {
        Level level = getLevel();
        if (level == null) return;

        Direction front = getFrontFacing();
        Direction spin = getUpwardsFacing();

        Direction left = RelativeDirection.LEFT.getRelative(front, spin, false);
        Direction right = RelativeDirection.RIGHT.getRelative(front, spin, false);
        Direction up = RelativeDirection.UP.getRelative(front, spin, false);
        Direction down = RelativeDirection.DOWN.getRelative(front, spin, false);
        BlockPos.MutableBlockPos posLeft = getPos().mutable().move(left);
        BlockPos.MutableBlockPos posRight = getPos().mutable().move(right);
        BlockPos.MutableBlockPos posUp = getPos().mutable().move(up);
        BlockPos.MutableBlockPos posDown = getPos().mutable().move(down);
        this.leftDist = 0;
        this.rightDist = 0;
        this.upDist = 0;
        this.downDist = 0;

        while (isValidMonitorBlock(level, posLeft)) {
            posLeft.move(left);
            leftDist++;
        }
        while (isValidMonitorBlock(level, posRight)) {
            posRight.move(right);
            rightDist++;
        }
        while (isValidMonitorBlockRow(level, posUp, leftDist, rightDist, left, right)) {
            posUp.move(up);
            upDist++;
        }
        while (isValidMonitorBlockRow(level, posDown, leftDist, rightDist, left, right)) {
            posDown.move(down);
            downDist++;
        }
    }

    private boolean isValidMonitorBlockRow(Level level, BlockPos pos, int leftDist, int rightDist, Direction left,
                                           Direction right) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        mutable.move(left, leftDist);
        for (int i = 0; i < leftDist + rightDist; i++) {
            if (!isValidMonitorBlock(level, mutable)) return false;
            mutable.move(right);
        }
        return isValidMonitorBlock(level, mutable);
    }

    @Override
    public BlockPattern getPattern() {
        updateStructureDimensions();
        if (leftDist + rightDist < 1 || upDist + downDist < 1) {
            leftDist = 3;
            rightDist = 0;
            upDist = 1;
            downDist = 1;
        }

        StringBuilder[] pattern = new StringBuilder[upDist + downDist + 1];
        for (int i = 0; i < upDist + downDist + 1; i++) {
            pattern[i] = new StringBuilder(leftDist + rightDist + 1);
            for (int j = 0; j < leftDist + rightDist + 1; j++) {
                if (i == downDist && j == rightDist)
                    pattern[i].append('C'); // controller
                else
                    pattern[i].append('B'); // any valid block
            }
        }

        String[] aisle = new String[upDist + downDist + 1];
        for (int i = 0; i < upDist + downDist + 1; i++) {
            aisle[i] = pattern[i].toString();
        }

        return FactoryBlockPattern.start()
                .aisle(aisle)
                .where('B', getMultiPredicate())
                .where('C', Predicates.controller(Predicates.blocks(this.getDefinition().get())))
                .build();
    }

    public BlockPos toRelative(BlockPos pos) {
        Direction front = getFrontFacing();
        Direction spin = getUpwardsFacing();
        boolean flipped = isFlipped();
        Direction right = RelativeDirection.RIGHT.getRelative(front, spin, flipped);
        Direction up = RelativeDirection.UP.getRelative(front, spin, flipped);

        BlockPos tmp = getPos().mutable().move(right, rightDist).move(up, upDist);

        return new BlockPos(Math.abs(tmp.get(right.getAxis()) - pos.get(right.getAxis())),
                Math.abs(tmp.get(up.getAxis()) - pos.get(up.getAxis())),
                0);
    }

    @Nullable
    public IMonitorComponent getComponent(int row, int col) {
        Level level = getLevel();
        if (level == null) return null;

        Direction front = getFrontFacing();
        Direction spin = getUpwardsFacing();
        boolean flipped = isFlipped();

        Direction left = RelativeDirection.LEFT.getRelative(front, spin, flipped);
        Direction up = RelativeDirection.UP.getRelative(front, spin, flipped);

        col = leftDist + rightDist - col;
        BlockPos pos = getPos().relative(left, leftDist - col).relative(up, upDist - row);

        return GTCapabilityHelper.getMonitorComponent(level, pos, null);
    }

    public boolean isMonitor(int row, int col) {
        IMonitorComponent component = this.getComponent(row, col);
        if (component == null) return false;
        return component.isMonitor();
    }

    private IGuiTexture getComponentTexture(int row, int col) {
        if (row < 0 || col < 0 || row > downDist + upDist + 1 || col > leftDist + rightDist + 1)
            return GuiTextures.BLANK_TRANSPARENT;
        IMonitorComponent component = getComponent(row, col);
        if (component == null) return GuiTextures.BLANK_TRANSPARENT;
        return component.getComponentIcon();
    }

    private boolean isInAnyGroup(IMonitorComponent component) {
        return monitorGroups.stream().anyMatch(group -> group.contains(component.getPos()));
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        MultiblockDisplayText.builder(textList, isFormed())
                .addPatternErrorLine(getMultiblockState().error)
                .addWorkingStatusLine();
        getDefinition().getAdditionalDisplay().accept(this, textList);
    }

    private static void onBothSides(UIElement element, Consumer<UIEvent> handler) {
        element.addEventListener(UIEvents.MOUSE_DOWN, handler::accept);
        element.addServerEventListener(UIEvents.MOUSE_DOWN, handler::accept);
    }

    private static void setButtonTexture(Button button, IGuiTexture texture) {
        button.buttonStyle(style -> style.baseTexture(texture).hoverTexture(texture).pressedTexture(texture));
    }

    private static Button textButton(String key, int width, int height) {
        var button = new Button();
        button.setText(Component.translatable(key));
        button.layout(layout -> layout.width(width).height(height));
        return button;
    }

    @Override
    public UIElement createUIWidget() {
        updateStructureDimensions();
        selectedComponents.clear();
        var builder = super.createUIWidget();

        var main = new UIElement();
        main.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(0).top(0));

        var componentSelection = UITemplate.displayScreen(IGuiTexture.EMPTY);
        componentSelection.scrollerStyle(style -> style.verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.AUTO));
        componentSelection.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(6).top(6).width(112)
                .height(113));
        var selectionContent = new UIElement();
        selectionContent.layout(layout -> layout.width(2 + 16 * (leftDist + rightDist + 1) + 2)
                .height(30 + 16 * (downDist + upDist + 1) + 4));
        componentSelection.addScrollViewChild(selectionContent);
        main.addChild(componentSelection);

        var options = new UIElement();
        options.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(122).top(28).width(62)
                .height(32));
        var groupConfig = new UIElement();
        groupConfig.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(6).top(6).width(178)
                .height(113));
        groupConfig.setDisplay(false);

        var infoWidget = new Button();
        infoWidget.noText();
        infoWidget.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(166).top(6).width(18)
                .height(18));
        setButtonTexture(infoWidget, GuiTextures.INFO_ICON);
        var infoTooltips = LangHandler.getSingleOrMultiLang("gtceu.central_monitor.info_tooltip")
                .toArray(Component[]::new);
        infoWidget.style(style -> style.tooltips(infoTooltips));
        builder.addChild(infoWidget);

        List<MonitorGroup> configGroup = new ArrayList<>();
        configGroup.add(null);

        Consumer<MonitorGroup> openGroupConfig = group -> {
            configGroup.set(0, group);
            if (group == null) {
                MenuSlots.clearChildren(groupConfig);
                main.setDisplay(true);
                groupConfig.setDisplay(false);
                return;
            }
            MenuSlots.clearChildren(groupConfig);
            var editing = new Label();
            editing.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(0).top(5));
            editing.setText(Component.translatable("gtceu.central_monitor.gui.currently_editing", group.getName()));
            groupConfig.addChild(editing);
            for (int i = 0; i < 8; i++) {
                var placeholder = new ItemSlot().bind(group.getPlaceholderSlotsHandler(), i);
                int left = 140 + 18 * (i / 4);
                int top = 14 + 18 * (i % 4);
                placeholder.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(left).top(top));
                var tooltips = LangHandler.getMultiLang("gtceu.gui.computer_monitor_cover.slot_tooltip", i + 1)
                        .toArray(Component[]::new);
                placeholder.style(style -> style.tooltips(tooltips));
                groupConfig.addChild(placeholder);
            }
            var itemUI = new UIElement();
            itemUI.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(22).top(14).width(114)
                    .height(99));
            var lastItem = new ItemStack[] { ItemStack.EMPTY };
            var itemHandlerSlot = new ItemHandlerSlot(group.getItemStackHandler(), 0);
            Runnable changeListener = () -> {
                var current = itemHandlerSlot.getItem();
                if (lastItem[0].is(current.getItem())) return;
                lastItem[0] = current.copy();
                MenuSlots.clearChildren(itemUI);
                if (current.getItem() instanceof IComponentItem item) {
                    for (IItemComponent component : item.getComponents()) {
                        if (component instanceof IMonitorModuleItem module) {
                            itemUI.addChild(module.createUIWidget(current, this, group));
                        }
                    }
                }
            };
            itemHandlerSlot.addChangeListener(changeListener);
            var slot = new ItemSlot().bind(itemHandlerSlot);
            slot.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(0).top(14));
            changeListener.run();
            groupConfig.addChildren(itemUI, slot);
            main.setDisplay(false);
            groupConfig.setDisplay(true);
        };
        builder.addChild(groupConfig);

        var groupListView = UITemplate.displayScreen(IGuiTexture.EMPTY);
        groupListView.scrollerStyle(style -> style.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO));
        groupListView.verticalScroller(bar -> bar.layout(layout -> layout.width(4).marginLeft(1)));
        groupListView.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(122).top(80).width(62)
                .height(39));
        var groupList = new UIElement();
        groupList.layout(layout -> layout.width(56).minHeight(39));
        groupListView.addScrollViewChild(groupList);

        List<List<Consumer<Iterator<IMonitorComponent>>>> imageButtons = new ArrayList<>();
        Map<BlockPos, Runnable> rightClickCallbacks = new HashMap<>();
        int[] dataSlot = new int[2];
        dataSlot[0] = 1;
        dataSlot[1] = 9;
        var dataSlotInput = new IntInput(() -> dataSlot[0], n -> dataSlot[0] = Mth.clamp(n, 1, dataSlot[1]));
        dataSlotInput.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(122).top(62).width(62));
        dataSlotInput.setDisplay(false);
        builder.addChild(dataSlotInput);

        Consumer<MonitorGroup> addGroupToList = group -> {
            int top = groupList.getChildren().size() / 2 * 15 + 3;
            var label = new Button();
            label.setText(group.getName(), false);
            label.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(16).top(top - 1).width(40)
                    .height(12));
            onBothSides(label, click -> {
                group.getMonitorPositions().forEach(pos -> {
                    BlockPos rel = toRelative(pos);
                    if (imageButtons.size() - 1 < rel.getY()) return;
                    if (imageButtons.get(rel.getY()).size() - 1 < rel.getX()) return;
                    imageButtons.get(rel.getY()).get(rel.getX()).accept(null);
                });
                if (group.getTargetRaw() != null) {
                    rightClickCallbacks.getOrDefault(group.getTargetRaw(), () -> {}).run();
                }
            });
            groupList.addChild(label);

            var configButton = new Button();
            configButton.noText();
            configButton.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(0).top(top - 2).width(14)
                    .height(14));
            setButtonTexture(configButton, GuiTextures.IO_CONFIG_COVER_SETTINGS);
            onBothSides(configButton, click -> {
                if (configGroup.get(0) == null) {
                    openGroupConfig.accept(group);
                } else {
                    openGroupConfig.accept(null);
                }
            });
            groupList.addChild(configButton);
        };

        monitorGroups.forEach(addGroupToList);
        builder.addChild(groupListView);
        main.addChild(options);

        var removeFromGroupButton = textButton("gtceu.central_monitor.gui.remove_from_group", 62, 15);
        removeFromGroupButton.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(0).top(0));
        removeFromGroupButton.setDisplay(false);
        var setTargetButton = textButton("gtceu.central_monitor.gui.set_target", 62, 15);
        setTargetButton.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(0).top(17));
        setTargetButton.setDisplay(false);
        var createGroupButton = textButton("gtceu.central_monitor.gui.create_group", 62, 15);
        createGroupButton.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(0).top(0));
        createGroupButton.setDisplay(false);

        onBothSides(createGroupButton, click -> {
            MonitorGroup group = new MonitorGroup(
                    Component.translatable("gtceu.gui.central_monitor.group_default_name", monitorGroups.size() + 1)
                            .getString());
            for (IMonitorComponent component : selectedComponents) {
                if (isInAnyGroup(component)) return;
                group.add(component.getPos());
            }
            monitorGroups.add(group);
            addGroupToList.accept(group);

            createGroupButton.setDisplay(false);
            removeFromGroupButton.setDisplay(true);
            Iterator<IMonitorComponent> it = selectedComponents.iterator();
            while (it.hasNext()) {
                IMonitorComponent c = it.next();
                BlockPos rel = toRelative(c.getPos());
                imageButtons.get(rel.getY()).get(rel.getX()).accept(it);
            }
            if (!selectedTargets.isEmpty()) {
                rightClickCallbacks.getOrDefault(selectedTargets.get(0).getPos(), () -> {}).run();
            }
        });
        onBothSides(setTargetButton, click -> {
            MonitorGroup group = null;
            for (MonitorGroup group2 : monitorGroups) {
                for (IMonitorComponent component : selectedComponents) {
                    if (group2.contains(component.getPos())) {
                        group = group2;
                        break;
                    }
                }
                if (group != null) break;
            }
            if (group == null) return;
            if (selectedTargets.isEmpty()) group.setTarget(null);
            else {
                group.setTarget(selectedTargets.get(0).getPos());
                group.setDataSlot(dataSlot[0] - 1);
            }
        });
        onBothSides(removeFromGroupButton, click -> {
            for (MonitorGroup group : monitorGroups) {
                for (IMonitorComponent component : selectedComponents) group.remove(component.getPos());
            }
            Iterator<MonitorGroup> itg = monitorGroups.iterator();
            while (itg.hasNext()) {
                MonitorGroup group = itg.next();
                if (group.isEmpty()) {
                    clearInventory(group.getItemStackHandler());
                    clearInventory(group.getPlaceholderSlotsHandler());
                    itg.remove();
                }
            }
            groupList.clearAllChildren();
            monitorGroups.forEach(addGroupToList);

            removeFromGroupButton.setDisplay(false);
            createGroupButton.setDisplay(true);
            Iterator<IMonitorComponent> it = selectedComponents.iterator();
            while (it.hasNext()) {
                IMonitorComponent c = it.next();
                BlockPos rel = toRelative(c.getPos());
                if (imageButtons.size() - 1 < rel.getY()) continue;
                if (imageButtons.get(rel.getY()).size() - 1 < rel.getX()) continue;
                imageButtons.get(rel.getY()).get(rel.getX()).accept(it);
            }
            if (!selectedTargets.isEmpty()) {
                rightClickCallbacks.getOrDefault(selectedTargets.get(0).getPos(), () -> {}).run();
            }
        });
        options.addChildren(removeFromGroupButton, createGroupButton, setTargetButton);

        int startX = 2;
        int startY = 30;
        for (int row = 0; row <= downDist + upDist; row++) {
            imageButtons.add(new ArrayList<>());
            for (int col = 0; col <= leftDist + rightDist; col++) {
                IGuiTexture texture = getComponentTexture(row, col);
                var border = new ColorBorderTexture(2, 0xFFFFFF);
                IMonitorComponent component = getComponent(row, col);
                if (component == null) {
                    GTUtil.getLast(imageButtons).add(it -> {});
                    continue;
                }
                var img = new Button();
                img.noText();
                int left = startX + 16 * col;
                int top = startY + 16 * row;
                img.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(left).top(top).width(16)
                        .height(16));
                Consumer<IGuiTexture> applyTexture = tex -> setButtonTexture(img, new GuiTextureGroup(tex, border));
                applyTexture.accept(texture);
                Consumer<Iterator<IMonitorComponent>> callback = it -> {
                    if (!component.isMonitor()) return;
                    if (selectedComponents.contains(component)) {
                        if (it == null) {
                            selectedComponents.remove(component);
                        } else {
                            it.remove();
                        }

                        if (!selectedTargets.isEmpty() && selectedTargets.get(0) == component) {
                            applyTexture.accept(new GuiTextureGroup(new ColorRectTexture(Color.BLUE), texture));
                        } else {
                            applyTexture.accept(texture);
                        }

                        createGroupButton.setDisplay(selectedComponents.stream().noneMatch(this::isInAnyGroup));
                        removeFromGroupButton.setDisplay(selectedComponents.stream().allMatch(this::isInAnyGroup));
                        setTargetButton.setDisplay(removeFromGroupButton.isDisplayed());

                        if (selectedComponents.isEmpty()) {
                            createGroupButton.setDisplay(false);
                            removeFromGroupButton.setDisplay(false);
                            setTargetButton.setDisplay(false);
                        }
                    } else {
                        boolean inAnyGroup = isInAnyGroup(component);
                        if (selectedComponents.isEmpty() && !inAnyGroup) createGroupButton.setDisplay(true);
                        if (inAnyGroup) createGroupButton.setDisplay(false);
                        if (selectedComponents.isEmpty() && inAnyGroup) {
                            removeFromGroupButton.setDisplay(true);
                            setTargetButton.setDisplay(true);
                        }
                        if (!inAnyGroup) {
                            removeFromGroupButton.setDisplay(false);
                            setTargetButton.setDisplay(false);
                        }
                        selectedComponents.add(component);
                        var rect = new ColorRectTexture(
                                (selectedTargets.isEmpty() || selectedTargets.get(0) != component) ? Color.RED :
                                        Color.PINK);
                        applyTexture.accept(new GuiTextureGroup(rect, texture));
                    }
                    updateGroupTooltip(img, component);
                };
                Runnable rightClickCallback = () -> {
                    if (!selectedTargets.isEmpty()) {
                        if (selectedTargets.get(0).getPos() == component.getPos()) {
                            selectedTargets.clear();
                            if (selectedComponents.contains(component)) {
                                applyTexture.accept(new GuiTextureGroup(new ColorRectTexture(Color.RED), texture));
                            } else {
                                applyTexture.accept(texture);
                            }
                            dataSlotInput.setDisplay(false);
                            return;
                        } else {
                            try {
                                rightClickCallbacks.get(selectedTargets.get(0).getPos()).run();
                            } catch (StackOverflowError e) {
                                GTCEu.LOGGER.error(
                                        "Stack overflow when right-clicking monitor component {} at {} (selectedTarget is {} at {})",
                                        component, component.getPos(), selectedTargets.get(0),
                                        selectedTargets.get(0).getPos());
                            }
                        }
                    }
                    selectedTargets.add(component);
                    ColorRectTexture rect;
                    if (selectedComponents.contains(component)) {
                        rect = new ColorRectTexture(Color.PINK);
                    } else {
                        rect = new ColorRectTexture(Color.BLUE);
                    }
                    applyTexture.accept(new GuiTextureGroup(rect, texture));
                    if (component.getDataItems() != null) {
                        IItemHandler dataItems = component.getDataItems();
                        MonitorGroup selectedGroup = null;
                        for (MonitorGroup group : monitorGroups) {
                            for (IMonitorComponent c : selectedComponents) {
                                if (group.contains(c.getPos())) {
                                    if (selectedGroup == null || selectedGroup == group) {
                                        selectedGroup = group;
                                    } else {
                                        selectedGroup = null;
                                        break;
                                    }
                                }
                            }
                        }
                        if (selectedGroup != null) {
                            dataSlot[0] = selectedGroup.getDataSlot() + 1;
                        }
                        dataSlot[1] = dataItems.getSlots();
                        dataSlotInput.setDisplay(true);
                    }
                };
                updateGroupTooltip(img, component);
                onBothSides(img, click -> {
                    if (click.button == 0) callback.accept(null);
                    else if (click.button == 1) rightClickCallback.run();
                });
                selectionContent.addChild(img);
                GTUtil.getLast(imageButtons).add(callback);
                rightClickCallbacks.put(component.getPos(), rightClickCallback);
            }
        }
        builder.addChild(main);
        return builder;
    }

    private void updateGroupTooltip(Button img, IMonitorComponent component) {
        Component name = Component.translatable("gtceu.gui.central_monitor.none");
        for (MonitorGroup group : monitorGroups) {
            if (group.contains(component.getPos())) {
                name = Component.literal(group.getName());
            }
        }
        var tooltip = Component.translatable("gtceu.gui.central_monitor.group", name);
        img.style(style -> style.tooltips(tooltip));
    }

    @Override
    public IGuiTexture getComponentIcon() {
        return VanillaSpriteTexture.of(GTCEu.id("block/multiblock/network_switch/overlay_front_active"));
    }

    @Override
    public @NotNull List<Component> getDebugInfo(Player player, int logLevel,
                                                 PortableScannerBehavior.DisplayMode mode) {
        return List.of(Component.translatable("gtceu.central_monitor.size", leftDist, rightDist, upDist, downDist));
    }

    @Override
    public @NotNull List<Component> getDataInfo(PortableScannerBehavior.DisplayMode mode) {
        return List.of(Component.translatable("gtceu.central_monitor.size", leftDist, rightDist, upDist, downDist));
    }

    @Override
    public void onMachineRemoved() {
        for (MonitorGroup group : monitorGroups) {
            clearInventory(group.getItemStackHandler());
            clearInventory(group.getPlaceholderSlotsHandler());
        }
    }
}
