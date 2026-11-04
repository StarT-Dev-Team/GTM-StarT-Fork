package com.gregtechceu.gtceu.api.gui.widget;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.preview.*;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMufflerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.client.model.machine.MachineRenderState;
import com.gregtechceu.gtceu.common.data.GTParticleTypes;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.config.MultiblockPreviewConfig;
import com.gregtechceu.gtceu.integration.xei.handlers.item.CycleItemStackHandler;

import com.lowdragmc.lowdraglib.core.mixins.accessor.ParticleEngineAccessor;
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.Position;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.emi.screen.RecipeScreen;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import me.shedaniel.rei.impl.client.gui.screen.AbstractDisplayViewingScreen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class PatternPreviewWidget extends WidgetGroup implements PreviewSceneWidget.Host, VariantSelectorUI.Host {

    public static final int WIDGET_WIDTH = 214;
    public static final int WIDGET_HEIGHT = 206;

    private static final int COL_RIGHT = WIDGET_WIDTH - 22;
    private static final int COL_MIDDLE = WIDGET_WIDTH - 42;
    private static final int COL_LEFT = WIDGET_WIDTH - 62;
    private static final int ROW_PAGE = 30;
    private static final int ROW_GRID = 50;
    private static final int ROW_ORIENT = 70;
    private static final int ROW_TURNTABLE_SHIFTED = 90;

    public static int getPage(MultiblockMachineDefinition definition) {
        return PatternLoader.getPage(definition);
    }

    public static List<MultiblockMachineDefinition> getMultiblockPreviews() {
        return MultiblockPreviewConfig.getOrderedMultiblocks();
    }

    @Nullable
    public static MBPattern getPattern(MultiblockMachineDefinition definition, int page) {
        MBPattern[] patterns = PatternLoader.getCached(definition);
        if (patterns == null || patterns.length == 0) {
            getPatternWidget(definition); // populates the cache
            patterns = PatternLoader.getCached(definition);
        }
        if (patterns == null || patterns.length == 0) return null;
        if (page < 0 || page >= patterns.length) page = 0;
        return patterns[page];
    }

    public static PatternPreviewWidget getPatternWidget(MultiblockMachineDefinition controllerDefinition) {
        PreviewLevel.getOrCreate();
        return new PatternPreviewWidget(controllerDefinition);
    }

    public final MultiblockMachineDefinition controllerDefinition;
    public final MBPattern[] patterns;
    private final PreviewState state;

    private final PatternSceneWidget sceneWidget;
    private final DraggableScrollableWidgetGroup partsBar;
    private final VariantSelectorUI variantSelector;
    private SlotWidget[] slotWidgets;

    private PreviewButton pageButton;
    private PreviewButton layerButton;
    private PreviewButton gridButton;
    private PreviewButton graduationButton;
    private PreviewButton modeButton;
    private PreviewButton turntableButton;
    private PreviewButton rotateButton;
    private PreviewButton flipButton;
    private PreviewButton resetButton;

    private int simulationTick = 0;
    private long lastSimulationTickTime = 0L;

    @Nullable
    private BlockPos selectedPos = null;
    private boolean isLoaded;

    protected PatternPreviewWidget(MultiblockMachineDefinition controllerDefinition) {
        super(0, 0, WIDGET_WIDTH, WIDGET_HEIGHT);
        setClientSideWidget();
        this.controllerDefinition = controllerDefinition;
        this.state = new PreviewState(controllerDefinition);
        this.variantSelector = new VariantSelectorUI(this, WIDGET_WIDTH, WIDGET_HEIGHT);

        this.sceneWidget = buildScene();
        this.partsBar = buildPartsBar();
        addWidget(new ImageWidget(3, 3, WIDGET_WIDTH - 26, 10,
                new TextTexture(controllerDefinition.getDescriptionId(), -1)
                        .setType(TextTexture.TextType.ROLL)
                        .setWidth(WIDGET_WIDTH - 26)
                        .setDropShadow(true)));

        this.patterns = PatternLoader.loadPatterns(controllerDefinition,
                positions -> sceneWidget.setRenderedCore(positions, null));

        buildControlButtons();

        addWidget(variantSelector.getListGroup());
        addWidget(variantSelector.getFlyoutGroup());

        setPage(getPage(this.controllerDefinition));
    }

    private PatternSceneWidget buildScene() {
        PreviewSceneWidget scene = new PreviewSceneWidget(3, 3, WIDGET_WIDTH - 6, WIDGET_HEIGHT - 32,
                PreviewLevel.get(), this);
        addWidget(scene);
        scene.setOnSelected(this::onPosSelected);
        scene.setRenderFacing(false);
        scene.setBeforeBatchEnd(this::renderGrid);
        scene.setTurntableActive(state.turntable);
        if (scene.getDummyWorld() != null && scene.getParticleManager() != null) {
            scene.getDummyWorld().setParticleManager(scene.getParticleManager());
        }
        return scene;
    }

    private DraggableScrollableWidgetGroup buildPartsBar() {
        DraggableScrollableWidgetGroup bar = new DraggableScrollableWidgetGroup(3, WIDGET_HEIGHT - 28,
                WIDGET_WIDTH - 8,
                25)
                .setXScrollBarHeight(4)
                .setXBarStyle(GuiTextures.SLIDER_BACKGROUND, GuiTextures.BUTTON)
                .setScrollable(true)
                .setDraggable(true);
        bar.setScrollWheelDirection(DraggableScrollableWidgetGroup.ScrollWheelDirection.HORIZONTAL);
        bar.setScrollYOffset(0);
        addWidget(bar);

        if (ConfigHolder.INSTANCE.client.useVBO) {
            if (!RenderSystem.isOnRenderThread()) {
                RenderSystem.recordRenderCall(sceneWidget::useCacheBuffer);
            } else {
                sceneWidget.useCacheBuffer();
            }
        }
        return bar;
    }

    private void buildControlButtons() {
        boolean canRotate = canRotatePitch() || canRotateRoll();
        boolean canFlip = controllerDefinition.isAllowFlip();

        addWidget(pageButton = new PreviewButton(COL_MIDDLE, ROW_PAGE, () -> "P:" + state.index,
                cd -> updatePage(cd.button == 1))
                .onRightClick(() -> updatePage(true))
                .onWheel(delta -> stepPage(delta > 0 ? 1 : -1))
                .tooltip("gtceu.multiblock.preview.page"));
        pageButton.setVisible(patterns.length > 1);

        addWidget(layerButton = new PreviewButton(COL_RIGHT, ROW_PAGE,
                () -> state.layer >= 0 ? "L:" + state.layer : "ALL",
                cd -> updateLayer(cd.button == 1))
                .onRightClick(() -> updateLayer(true))
                .onWheel(delta -> stepLayer(delta > 0 ? 1 : -1))
                .tooltip("gtceu.multiblock.preview.layer"));

        addWidget(graduationButton = new PreviewButton(COL_MIDDLE, ROW_GRID,
                () -> state.graduationStep() == 0 ? "OFF" : String.valueOf(state.graduationStep()),
                cd -> state.cycleGraduation(cd.button == 1))
                .onRightClick(() -> state.cycleGraduation(true))
                .onWheel(delta -> state.cycleGraduation(delta < 0))
                .tooltip("gtceu.multiblock.preview.measurement_grid_graduation"));
        graduationButton.setVisible(state.showGrid);

        addWidget(gridButton = new PreviewButton(COL_RIGHT, ROW_GRID, () -> state.showGrid ? "ON" : "OFF",
                cd -> toggleGrid())
                .tooltip("gtceu.multiblock.preview.measurement_grid"));

        if (canFlip) {
            addWidget(flipButton = new PreviewButton(COL_RIGHT, ROW_ORIENT, () -> state.flipped ? "MIR" : "OFF",
                    cd -> flipStructure())
                    .tooltip("gtceu.multiblock.preview.flip"));
        }

        if (canRotate) {
            addWidget(rotateButton = new PreviewButton(canFlip ? COL_MIDDLE : COL_RIGHT, ROW_ORIENT, () -> {
                if (isRollMode()) {
                    return "R:" + state.upwardsFacing.getName().substring(0, 1).toUpperCase();
                } else {
                    return "P:" + state.frontFacing.getName().substring(0, 1).toUpperCase();
                }
            }, cd -> rotateStructure(cd.button != 1))
                    .onRightClick(() -> rotateStructure(false))
                    .onWheel(delta -> rotateStructure(delta > 0))
                    .dynamicTooltip(this::rotateTooltip));
        }

        if (canRotate || canFlip) {
            addWidget(resetButton = new PreviewButton(canRotate && canFlip ? COL_LEFT : COL_MIDDLE, ROW_ORIENT,
                    () -> "RST", cd -> resetOrientation())
                    .tooltip("gtceu.multiblock.preview.reset"));
            resetButton.setVisible(!state.isDefaultOrientation(controllerDefinition));
        }

        int turntableY = (canRotate || canFlip) ? ROW_TURNTABLE_SHIFTED : ROW_ORIENT;
        addWidget(modeButton = new PreviewButton(COL_MIDDLE, turntableY, () -> state.mode.label,
                cd -> cycleMode(cd.button == 1))
                .onRightClick(() -> cycleMode(true))
                .onWheel(delta -> cycleMode(delta < 0))
                .dynamicTooltip(this::modeTooltip));

        addWidget(turntableButton = new PreviewButton(COL_RIGHT, turntableY, () -> state.turntable ? "ON" : "OFF",
                cd -> toggleTurntable())
                .tooltip("gtceu.multiblock.preview.turntable"));
    }

    private List<Component> modeTooltip() {
        List<Component> list = new ArrayList<>();
        list.add(Component.translatable("gtceu.multiblock.preview.mode")
                .append(": ")
                .append(Component.translatable(state.mode.tooltipKey).withStyle(ChatFormatting.GOLD)));
        list.add(Component.translatable("gtceu.multiblock.preview.mode.desc")
                .withStyle(ChatFormatting.GRAY));
        return list;
    }

    private List<Component> rotateTooltip() {
        List<Component> list = new ArrayList<>();
        if (canRotatePitch() && canRotateRoll()) {
            if (Screen.hasShiftDown()) {
                list.add(Component.translatable("gtceu.multiblock.preview.roll"));
                list.add(Component.translatable("gtceu.multiblock.preview.rotate_switch_pitch")
                        .withStyle(ChatFormatting.GRAY));
            } else {
                list.add(Component.translatable("gtceu.multiblock.preview.pitch"));
                list.add(Component.translatable("gtceu.multiblock.preview.rotate_switch_roll")
                        .withStyle(ChatFormatting.GRAY));
            }
        } else if (canRotateRoll()) {
            list.add(Component.translatable("gtceu.multiblock.preview.roll"));
        } else {
            list.add(Component.translatable("gtceu.multiblock.preview.pitch"));
        }
        return list;
    }

    public int getIndex() {
        return state.index;
    }

    public void toggleTurntable() {
        setTurntableActive(!state.turntable);
    }

    public void setTurntableActive(boolean active) {
        state.setTurntable(active);
        sceneWidget.setTurntableActive(active);
    }

    public void toggleGrid() {
        state.toggleGrid();
        graduationButton.setVisible(state.showGrid);
    }

    private void renderGrid(MultiBufferSource bufferSource, float partialTicks) {
        MBPattern pattern = currentPattern();
        if (!state.showGrid || pattern == null) return;
        GridRenderer.render(pattern, bufferSource, state.graduationStep());
    }

    public boolean canRotatePitch() {
        return this.controllerDefinition.getRotationState() == RotationState.ALL ||
                this.controllerDefinition.getRotationState() == RotationState.Y_AXIS;
    }

    public boolean canRotateRoll() {
        return this.controllerDefinition.isAllowExtendedFacing();
    }

    public boolean isRollMode() {
        if (!canRotateRoll()) return false;
        if (!canRotatePitch()) return true;
        return Screen.hasShiftDown();
    }

    public void rotateStructure(boolean clockwise) {
        if (isRollMode()) {
            state.upwardsFacing = clockwise ? state.upwardsFacing.getClockWise() :
                    state.upwardsFacing.getCounterClockWise();
        } else {
            if (!canRotatePitch()) return;
            Direction horizontal = state.baseHorizontalFacing;
            Direction[] cycle = { horizontal, Direction.UP, horizontal.getOpposite(), Direction.DOWN };
            int currentIndex = 0;
            for (int i = 0; i < cycle.length; i++) {
                if (cycle[i] == state.frontFacing) {
                    currentIndex = i;
                    break;
                }
            }
            state.frontFacing = cycle[(currentIndex + (clockwise ? 1 : cycle.length - 1)) % cycle.length];
        }
        onOrientationChanged();
    }

    public void flipStructure() {
        if (!this.controllerDefinition.isAllowFlip()) return;
        state.flipped = !state.flipped;
        onOrientationChanged();
    }

    public void resetOrientation() {
        state.resetOrientation(controllerDefinition);
        onOrientationChanged();
    }

    public boolean isDefaultOrientation() {
        return state.isDefaultOrientation(controllerDefinition);
    }

    private void onOrientationChanged() {
        MBPattern pattern = currentPattern();
        if (pattern == null) return;
        pattern.applyTransformation(state.frontFacing, state.upwardsFacing, state.flipped);

        state.layer = Math.min(state.layer, pattern.maxLayer());
        sceneWidget.setCenterSmooth(focalCenter(pattern));

        clearBlockSelection();
        refreshPattern(pattern);
    }

    private void updatePage(boolean reverse) {
        if (patterns.length == 0) return;
        int count = patterns.length;
        setPage((state.index + (reverse ? count - 1 : 1)) % count);
    }

    public void stepPage(int delta) {
        if (patterns.length == 0) return;
        int target = Mth.clamp(state.index + delta, 0, patterns.length - 1);
        if (target != state.index) {
            setPage(target);
        }
    }

    public void setPage(int index) {
        if (index >= patterns.length || index < 0) return;
        revertActiveSimulation(currentPattern());
        clearBlockSelection();
        state.index = index;
        PatternLoader.setPage(controllerDefinition, index);
        state.layer = -1;
        MBPattern pattern = patterns[index];
        pattern.applyTransformation(state.frontFacing, state.upwardsFacing, state.flipped);
        sceneWidget.setCenter(focalCenter(pattern), false);
        refreshPattern(pattern);
    }

    private void updateLayer(boolean reverse) {
        MBPattern pattern = currentPattern();
        if (pattern == null) return;
        int maxLayer = pattern.maxLayer();
        if (reverse) {
            setLayer(state.layer > -1 ? state.layer - 1 : maxLayer);
        } else {
            setLayer(state.layer < maxLayer ? state.layer + 1 : -1);
        }
    }

    @Override
    public void stepLayer(int delta) {
        setLayer(state.layer + delta);
    }

    public void setLayer(int targetLayer) {
        MBPattern pattern = currentPattern();
        if (pattern == null) return;
        int newLayer = Mth.clamp(targetLayer, -1, pattern.maxLayer());
        if (state.layer == newLayer) return;

        state.layer = newLayer;
        if (selectedPos != null && newLayer != -1 && newLayer + pattern.minY != selectedPos.getY()) {
            clearBlockSelection();
        }
        setupScene(pattern);
    }

    public void cycleMode(boolean reverse) {
        state.cycleMode(reverse);
        applyMode(currentPattern());
    }

    private void applyMode(@Nullable MBPattern pattern) {
        if (pattern == null) return;
        PreviewMode mode = state.mode;
        if (mode == PreviewMode.UNFORMED) {
            revertActiveSimulation(pattern);
            if (pattern.controllerBase.isFormed()) {
                pattern.invalidateStructure();
            }
            pattern.setRotorHoldersState(false, false);
        } else if (mode == PreviewMode.FORMED) {
            revertActiveSimulation(pattern);
            if (!pattern.controllerBase.isFormed()) {
                pattern.formStructure();
            }
            pattern.setRotorHoldersState(true, false);
        } else if (mode == PreviewMode.ACTIVE) {
            if (!pattern.controllerBase.isFormed()) {
                pattern.formStructure();
            }
            applyActiveSimulation(pattern);
            pattern.setRotorHoldersState(true, true);
        }
        sceneWidget.needCompileCache();
        setupScene(pattern);
    }

    private void applyActiveSimulation(MBPattern pattern) {
        pattern.setCoilsActive(true);
        if (pattern.controllerBase instanceof IRecipeLogicMachine rlm) {
            rlm.getRecipeLogic().setStatus(RecipeLogic.Status.WORKING);
        }
        MachineRenderState renderState = pattern.controllerBase.self().getRenderState();
        if (renderState.hasProperty(GTMachineModelProperties.RECIPE_LOGIC_STATUS)) {
            pattern.controllerBase.self().setRenderState(
                    renderState.setValue(GTMachineModelProperties.RECIPE_LOGIC_STATUS, RecipeLogic.Status.WORKING));
        }
    }

    private void revertActiveSimulation(@Nullable MBPattern pattern) {
        if (pattern != null) {
            pattern.setCoilsActive(false);
            if (pattern.controllerBase instanceof IRecipeLogicMachine rlm) {
                rlm.getRecipeLogic().setStatus(RecipeLogic.Status.IDLE);
            }
            MachineRenderState renderState = pattern.controllerBase.self().getRenderState();
            if (renderState.hasProperty(GTMachineModelProperties.RECIPE_LOGIC_STATUS)) {
                pattern.controllerBase.self().setRenderState(
                        renderState.setValue(GTMachineModelProperties.RECIPE_LOGIC_STATUS, RecipeLogic.Status.IDLE));
            }
            pattern.setRotorHoldersState(state.mode != PreviewMode.UNFORMED, false);
        }
        var particleManager = sceneWidget.getParticleManager();
        if (particleManager != null) {
            particleManager.clearAllParticles();
        }
    }

    private static Vector3f focalCenter(MBPattern pattern) {
        return new Vector3f((pattern.xMin + pattern.xMax) / 2.0f,
                (pattern.yMin + pattern.yMax) / 2.0f,
                (pattern.zMin + pattern.zMax) / 2.0f);
    }

    private void refreshPattern(MBPattern pattern) {
        applyMode(pattern);
        setupParts(pattern);
        if (resetButton != null) {
            resetButton.setVisible(!isDefaultOrientation());
        }
        variantSelector.closeFlyout();
        updateVariantUI();
    }

    private void setupScene(MBPattern pattern) {
        Stream<BlockPos> stream = pattern.blockMap.keySet().stream()
                .filter(pos -> state.layer == -1 || state.layer + pattern.minY == pos.getY());
        if (state.mode != PreviewMode.UNFORMED && pattern.controllerBase.isFormed()) {
            LongSet modelDisabled = pattern.controllerBase.getMultiblockState().getMatchContext().getOrDefault(
                    "renderMask",
                    LongSets.EMPTY_SET);
            if (!modelDisabled.isEmpty()) {
                stream = stream.filter(pos -> !modelDisabled.contains(pos.asLong()));
            }
        }
        float maxDim = Math.max(Math.max(pattern.xMax - pattern.xMin, pattern.yMax - pattern.yMin),
                pattern.zMax - pattern.zMin);
        float fullZoom = (float) (3.5 * Math.sqrt(Math.max(maxDim, 1)));
        sceneWidget.setFocalCenter(focalCenter(pattern));
        sceneWidget.setCustomZoom(fullZoom);
        sceneWidget.setRenderedCore(stream.toList(), null);
    }

    public void setupParts(MBPattern pattern) {
        if (slotWidgets != null) {
            for (SlotWidget slotWidget : slotWidgets) {
                partsBar.removeWidget(slotWidget);
            }
        }
        slotWidgets = new SlotWidget[pattern.parts.size()];
        var itemHandler = new CycleItemStackHandler(pattern.parts);
        int xOffset = 0;
        for (int i = 0; i < slotWidgets.length; i++) {
            int padding = slotPadding(itemHandler.getStackInSlot(i).getCount());
            slotWidgets[i] = new PatternPreviewSlotWidget(itemHandler, i, (4 + xOffset + padding), 0, false, false)
                    .setBackgroundTexture(ColorPattern.T_GRAY.rectTexture())
                    .setIngredientIO(IngredientIO.INPUT);
            xOffset += 18 + (2 * padding);
            partsBar.addWidget(slotWidgets[i]);
        }
    }

    private static int slotPadding(int count) {
        if (count / 100_000 >= 1) return 10;
        if (count / 10_000 >= 1) return 7;
        if (count / 1_000 >= 1) return 4;
        return 1;
    }

    private void onPosSelected(BlockPos pos, Direction facing) {
        MBPattern pattern = currentPattern();
        if (pattern == null || pattern.predicateMap == null) return;
        if (pos.equals(this.selectedPos)) {
            clearBlockSelection();
            return;
        }
        this.selectedPos = pos;
        variantSelector.resetScroll();
        variantSelector.closeFlyout();
        updateVariantUI();
    }

    @Override
    public void clearBlockSelection() {
        this.selectedPos = null;
        sceneWidget.clearSelection();
        variantSelector.clear();
    }

    public void updateVariantUI() {
        variantSelector.update();
    }

    @Override
    public boolean hasSelection() {
        return selectedPos != null;
    }

    @Override
    public boolean isOverVariantUi(double mouseX, double mouseY) {
        return variantSelector.isMouseOverAny(mouseX, mouseY);
    }

    @Nullable
    @Override
    public MBPattern currentPattern() {
        if (state.index < 0 || state.index >= patterns.length) return null;
        return patterns[state.index];
    }

    @Nullable
    @Override
    public BlockPos selectedPos() {
        return selectedPos;
    }

    @Override
    public Position widgetPosition() {
        return getPosition();
    }

    @Override
    public void selectVariant(PartAbility ability, BlockState chosenState) {
        MBPattern pattern = currentPattern();
        if (pattern == null) return;
        pattern.activeAbilityChoices.put(ability, chosenState);

        sceneWidget.needCompileCache();
        pattern.applyTransformation(state.frontFacing, state.upwardsFacing, state.flipped);
        refreshPattern(pattern);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (sceneWidget.isTurntableActive() != state.turntable) {
            sceneWidget.setTurntableActive(state.turntable);
        }

        if (state.mode == PreviewMode.ACTIVE) {
            updateActiveSimulation();
        }

        if (!isLoaded) {
            Screen screen = Minecraft.getInstance().screen;
            if ((GTCEu.Mods.isEMILoaded() && screen instanceof RecipeScreen) ||
                    (GTCEu.Mods.isREILoaded() && screen instanceof AbstractDisplayViewingScreen)) {
                setPage(getPage(this.controllerDefinition));
                isLoaded = true;
            }
        }
    }

    private void updateActiveSimulation() {
        simulationTick++;
        if (simulationTick % 5 == 0) {
            emitMufflerParticles();
        }
    }

    private void emitMufflerParticles() {
        MBPattern pattern = currentPattern();
        if (pattern == null) return;
        List<IMufflerMachine> mufflers = pattern.getMufflers();
        if (mufflers.isEmpty()) return;

        var particleManager = sceneWidget.getParticleManager();
        if (particleManager == null) return;

        for (IMufflerMachine muffler : mufflers) {
            var pos = muffler.self().getPos();
            var facing = muffler.self().getFrontFacing();
            if (facing == null) facing = Direction.UP;

            var center = pos.getCenter();
            var offset = 0.75f;
            double xPos = center.x + facing.getStepX() * offset + (GTValues.RNG.nextFloat() - 0.5f) * 0.35f;
            double yPos = center.y + facing.getStepY() * offset + (GTValues.RNG.nextFloat() - 0.5f) * 0.35f;
            double zPos = center.z + facing.getStepZ() * offset + (GTValues.RNG.nextFloat() - 0.5f) * 0.35f;

            double ySpd = facing.getStepY() + (GTValues.RNG.nextFloat() - 0.15f) * 0.5f;
            double xSpd = facing.getStepX() + (GTValues.RNG.nextFloat() - 0.5f) * 0.5f;
            double zSpd = facing.getStepZ() + (GTValues.RNG.nextFloat() - 0.5f) * 0.5f;

            Particle particle = ((ParticleEngineAccessor) Minecraft.getInstance().particleEngine)
                    .invokeMakeParticle(GTParticleTypes.MUFFLER_PARTICLE.get(), xPos, yPos, zPos, xSpd, ySpd, zSpd);
            if (particle != null) {
                particleManager.addParticle(particle);
            }
        }
    }

    @Override
    public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
        for (PreviewButton button : new PreviewButton[] { pageButton, layerButton, graduationButton,
                rotateButton, modeButton }) {
            if (button != null && button.isVisible() && button.isMouseOverElement(mouseX, mouseY) &&
                    button.mouseWheelMove(mouseX, mouseY, wheelDelta)) {
                return true;
            }
        }
        if (variantSelector.mouseWheelMove(mouseX, mouseY, wheelDelta)) {
            return true;
        }
        return super.mouseWheelMove(mouseX, mouseY, wheelDelta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (variantSelector.isFlyoutOpen() && variantSelector.getFlyoutGroup().isVisible() &&
                !variantSelector.isMouseOverAny(mouseX, mouseY)) {
            variantSelector.closeFlyout();
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.enableBlend();
        if (state.mode == PreviewMode.ACTIVE) {
            long now = System.currentTimeMillis();
            if (now - lastSimulationTickTime >= 50L) {
                lastSimulationTickTime = now;
                updateActiveSimulation();
            }
        }
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
    }
}
