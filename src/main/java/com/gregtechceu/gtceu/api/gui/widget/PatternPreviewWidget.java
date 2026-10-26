package com.gregtechceu.gtceu.api.gui.widget;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.block.property.GTBlockStateProperties;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.xei.handlers.item.CycleItemStackHandler;

import com.lowdragmc.lowdraglib.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib.client.utils.RenderUtils;
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.BlockPosFace;
import com.lowdragmc.lowdraglib.utils.ItemStackKey;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.emi.emi.screen.RecipeScreen;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import me.shedaniel.rei.impl.client.gui.screen.AbstractDisplayViewingScreen;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class PatternPreviewWidget extends WidgetGroup {

    private boolean isLoaded;
    private static TrackedDummyWorld LEVEL;
    private static final int REGION_SIZE = 512;
    private static int LAST_OFFSET_INDEX = 0;
    private static final Map<MultiblockMachineDefinition, MBPattern[]> CACHE = new HashMap<>();
    private final PatternSceneWidget sceneWidget;
    private final DraggableScrollableWidgetGroup scrollableWidgetGroup;
    public final MultiblockMachineDefinition controllerDefinition;
    public final MBPattern[] patterns;
    private final List<SimplePredicate> predicates;
    private int index;
    public int layer;
    public ButtonWidget pageButton;
    public ButtonWidget layerButton;
    private SlotWidget[] slotWidgets;
    private SlotWidget[] candidates;

    @Getter
    private Direction frontFacing;
    @Getter
    private Direction baseHorizontalFacing;
    @Getter
    private Direction upwardsFacing;
    @Getter
    private boolean isFlipped;

    public ButtonWidget rotateButton;
    public ButtonWidget pitchButton;
    public ButtonWidget yawButton;
    public ButtonWidget rollButton;
    public ButtonWidget flipButton;
    public ButtonWidget resetButton;

    private static boolean defaultShowMeasurementGrid = true;
    public boolean showMeasurementGrid = defaultShowMeasurementGrid;

    private static boolean defaultTurntableActive = false;
    public boolean isTurntableActive = defaultTurntableActive;

    private static int defaultGraduationStep = 0;
    public int graduationStep = defaultGraduationStep;

    public void toggleTurntable() {
        setTurntableActive(!this.isTurntableActive);
    }

    public void setTurntableActive(boolean active) {
        this.isTurntableActive = active;
        defaultTurntableActive = this.isTurntableActive;
        if (this.sceneWidget != null) {
            this.sceneWidget.setTurntableActive(active);
        }
    }

    public void toggleGrid() {
        this.showMeasurementGrid = !this.showMeasurementGrid;
        defaultShowMeasurementGrid = this.showMeasurementGrid;
    }

    public void cycleGraduation() {
        if (this.graduationStep == 0) {
            this.graduationStep = 3;
        } else if (this.graduationStep == 3) {
            this.graduationStep = 5;
        } else if (this.graduationStep == 5) {
            this.graduationStep = 10;
        } else {
            this.graduationStep = 0;
        }
        defaultGraduationStep = this.graduationStep;
    }

    private void renderGrid(MultiBufferSource bufferSource, float partialTicks) {
        if (!this.showMeasurementGrid) return;
        if (this.patterns == null || this.index < 0 || this.index >= this.patterns.length) return;
        GridRenderer.render(this.patterns[this.index], bufferSource, this.graduationStep);
    }

    public static final int WIDGET_WIDTH = 214;
    public static final int WIDGET_HEIGHT = 206;

    protected PatternPreviewWidget(MultiblockMachineDefinition controllerDefinition) {
        super(0, 0, WIDGET_WIDTH, WIDGET_HEIGHT);
        setClientSideWidget();
        this.controllerDefinition = controllerDefinition;
        predicates = new ArrayList<>();
        layer = -1;
        this.frontFacing = controllerDefinition.getRotationState().defaultDirection;
        this.baseHorizontalFacing = this.frontFacing.getAxis() == Direction.Axis.Y ? Direction.NORTH : this.frontFacing;
        this.upwardsFacing = Direction.NORTH;
        this.isFlipped = false;

        addWidget(sceneWidget = new PatternSceneWidget(3, 3, WIDGET_WIDTH - 6, WIDGET_HEIGHT - 32, LEVEL) {

            @Override
            public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
                if (isMouseOverElement(mouseX, mouseY)) {
                    if (Screen.hasShiftDown() || Screen.hasControlDown()) {
                        int delta = wheelDelta > 0 ? 1 : -1;
                        stepLayer(delta);
                        return true;
                    }
                }
                return super.mouseWheelMove(mouseX, mouseY, wheelDelta);
            }

            @Override
            public void renderBlockOverLay(WorldSceneRenderer renderer) {
                PoseStack poseStack = new PoseStack();
                hoverPosFace = null;
                hoverItem = null;
                if (isMouseOverElement(currentMouseX, currentMouseY)) {
                    BlockHitResult hit = renderer.getLastTraceResult();
                    if (hit != null) {
                        if (core.contains(hit.getBlockPos())) {
                            hoverPosFace = new BlockPosFace(hit.getBlockPos(), hit.getDirection());
                        } else if (!useOrtho) {
                            Vector3f hitPos = hit.getLocation().toVector3f();
                            Level world = renderer.world;
                            Vec3 eyePos = new Vec3(renderer.getEyePos());
                            hitPos.mul(2); // Double view range to ensure pos can be seen.
                            Vec3 endPos = new Vec3((hitPos.x - eyePos.x), (hitPos.y - eyePos.y), (hitPos.z - eyePos.z));
                            double min = Float.MAX_VALUE;
                            for (BlockPos pos : core) {
                                BlockState blockState = world.getBlockState(pos);
                                if (blockState.getBlock() == Blocks.AIR) {
                                    continue;
                                }
                                hit = world.clipWithInteractionOverride(eyePos, endPos, pos,
                                        blockState.getShape(world, pos), blockState);
                                if (hit != null && hit.getType() != HitResult.Type.MISS) {
                                    double dist = eyePos.distanceToSqr(hit.getLocation());
                                    if (dist < min) {
                                        min = dist;
                                        hoverPosFace = new BlockPosFace(hit.getBlockPos(), hit.getDirection());
                                    }
                                }
                            }
                        }
                    }
                }
                if (hoverPosFace != null) {
                    var state = getDummyWorld().getBlockState(hoverPosFace.pos);
                    hoverItem = state.getBlock().getCloneItemStack(getDummyWorld(), hoverPosFace.pos, state);
                }
                BlockPosFace tmp = dragging ? clickPosFace : hoverPosFace;
                if (selectedPosFace != null || tmp != null) {
                    if (selectedPosFace != null && renderFacing) {
                        drawFacingBorder(poseStack, selectedPosFace, 0xff00ff00);
                    }
                    if (tmp != null && !tmp.equals(selectedPosFace) && renderFacing) {
                        drawFacingBorder(poseStack, tmp, 0xffffffff);
                    }
                }
                if (selectedPosFace != null && renderSelect) {
                    RenderUtils.renderBlockOverLay(poseStack, selectedPosFace.pos, 0.6f, 0, 0, 1.03f);
                }

                if (this.afterWorldRender != null) {
                    this.afterWorldRender.accept(this);
                }
            }
        });
        sceneWidget.setOnSelected(this::onPosSelected);
        sceneWidget.setRenderFacing(false);
        sceneWidget.setBeforeBatchEnd(this::renderGrid);
        sceneWidget.setTurntableActive(this.isTurntableActive);

        scrollableWidgetGroup = new DraggableScrollableWidgetGroup(3, WIDGET_HEIGHT - 28, WIDGET_WIDTH - 6, 25)
                .setXScrollBarHeight(4)
                .setXBarStyle(GuiTextures.SLIDER_BACKGROUND, GuiTextures.BUTTON)
                .setScrollable(true)
                .setDraggable(true);
        scrollableWidgetGroup.setScrollWheelDirection(DraggableScrollableWidgetGroup.ScrollWheelDirection.HORIZONTAL);
        scrollableWidgetGroup.setScrollYOffset(0);
        addWidget(scrollableWidgetGroup);

        if (ConfigHolder.INSTANCE.client.useVBO) {
            if (!RenderSystem.isOnRenderThread()) {
                RenderSystem.recordRenderCall(sceneWidget::useCacheBuffer);
            } else {
                sceneWidget.useCacheBuffer();
            }
        }

        addWidget(new ImageWidget(3, 3, WIDGET_WIDTH - 26, 10,
                new TextTexture(controllerDefinition.getDescriptionId(), -1)
                        .setType(TextTexture.TextType.ROLL)
                        .setWidth(WIDGET_WIDTH - 26)
                        .setDropShadow(true)));

        this.patterns = CACHE.computeIfAbsent(controllerDefinition, definition -> {
            HashSet<ItemStackKey> drops = new HashSet<>();
            drops.add(new ItemStackKey(this.controllerDefinition.asStack()));
            return controllerDefinition.getMatchingShapes().stream()
                    .map(it -> initializePattern(it, drops))
                    .filter(Objects::nonNull)
                    .toArray(MBPattern[]::new);
        });

        addWidget(pageButton = new ButtonWidget(WIDGET_WIDTH - 22, 30, 18, 18, new GuiTextureGroup(
                ColorPattern.T_GRAY.rectTexture(),
                new TextTexture("1").setSupplier(() -> "P:" + index)),
                cd -> updatePage(cd.button == 1)) {

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 1 && isMouseOverElement(mouseX, mouseY)) {
                    updatePage(true);
                    playButtonClickSound();
                    return true;
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
                if (isMouseOverElement(mouseX, mouseY)) {
                    int delta = wheelDelta > 0 ? 1 : -1;
                    stepPage(delta);
                    return true;
                }
                return super.mouseWheelMove(mouseX, mouseY, wheelDelta);
            }
        }.setHoverBorderTexture(1, -1));

        addWidget(layerButton = new ButtonWidget(WIDGET_WIDTH - 22, 50, 18, 18, new GuiTextureGroup(
                ColorPattern.T_GRAY.rectTexture(),
                new TextTexture("1").setSupplier(() -> layer >= 0 ? "L:" + layer : "ALL")),
                cd -> updateLayer(cd.button == 1)) {

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 1 && isMouseOverElement(mouseX, mouseY)) {
                    updateLayer(true);
                    playButtonClickSound();
                    return true;
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
                if (isMouseOverElement(mouseX, mouseY)) {
                    int delta = wheelDelta > 0 ? 1 : -1;
                    stepLayer(delta);
                    return true;
                }
                return super.mouseWheelMove(mouseX, mouseY, wheelDelta);
            }
        }.setHoverBorderTexture(1, -1));

        addWidget(new ButtonWidget(WIDGET_WIDTH - 22, 70, 18, 18, new GuiTextureGroup(
                ColorPattern.T_GRAY.rectTexture(),
                new TextTexture("1").setSupplier(() -> showMeasurementGrid ? "ON" : "OFF")),
                cd -> toggleGrid())
                .setHoverBorderTexture(1, -1)
                .setHoverTooltips(Component.translatable("gtceu.multiblock.preview.measurement_grid")));

        addWidget(new ButtonWidget(WIDGET_WIDTH - 22, 90, 18, 18, new GuiTextureGroup(
                ColorPattern.T_GRAY.rectTexture(),
                new TextTexture("1").setSupplier(() -> graduationStep == 0 ? "OFF" : String.valueOf(graduationStep))),
                cd -> cycleGraduation())
                .setHoverBorderTexture(1, -1)
                .setHoverTooltips(Component.translatable("gtceu.multiblock.preview.measurement_grid_graduation")));

        addWidget(new ButtonWidget(WIDGET_WIDTH - 22, 110, 18, 18, new GuiTextureGroup(
                ColorPattern.T_GRAY.rectTexture(),
                new TextTexture("1").setSupplier(() -> isTurntableActive ? "ON" : "OFF")),
                cd -> toggleTurntable())
                .setHoverBorderTexture(1, -1)
                .setHoverTooltips(Component.translatable("gtceu.multiblock.preview.turntable")));

        if (canRotatePitch() || canRotateRoll()) {
            rotateButton = new ButtonWidget(WIDGET_WIDTH - 22, 130, 18, 18, new GuiTextureGroup(
                    ColorPattern.T_GRAY.rectTexture(),
                    new TextTexture("1").setSupplier(() -> {
                        if (isRollMode()) {
                            return "R:" + upwardsFacing.getName().substring(0, 1).toUpperCase();
                        } else {
                            return "P:" + frontFacing.getName().substring(0, 1).toUpperCase();
                        }
                    })),
                    cd -> rotateStructure(cd.button != 1)) {

                @Override
                public boolean mouseClicked(double mouseX, double mouseY, int button) {
                    if (button == 1 && isMouseOverElement(mouseX, mouseY)) {
                        rotateStructure(false);
                        playButtonClickSound();
                        return true;
                    }
                    return super.mouseClicked(mouseX, mouseY, button);
                }

                @Override
                public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
                    if (isMouseOverElement(mouseX, mouseY)) {
                        rotateStructure(wheelDelta > 0);
                        return true;
                    }
                    return super.mouseWheelMove(mouseX, mouseY, wheelDelta);
                }

                @Override
                public List<Component> getTooltipTexts() {
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

                @Override
                protected void drawTooltipTexts(int mouseX, int mouseY) {
                    List<Component> list = this.getTooltipTexts();
                    this.tooltipTexts.clear();
                    this.tooltipTexts.addAll(list);
                    if (!list.isEmpty() && isMouseOverElement(mouseX, mouseY) &&
                            getHoverElement(mouseX, mouseY) == this &&
                            gui != null && gui.getModularUIGui() != null) {
                        gui.getModularUIGui().setHoverTooltip(list, ItemStack.EMPTY, null, null);
                    }
                }
            };
            rotateButton.setHoverBorderTexture(1, -1);
            rotateButton.setHoverTooltips(Component.translatable("gtceu.multiblock.preview.pitch"));
            addWidget(rotateButton);
            pitchButton = rotateButton;
            yawButton = rotateButton;
            rollButton = rotateButton;
        }

        if (this.controllerDefinition.isAllowFlip()) {
            flipButton = new ButtonWidget(WIDGET_WIDTH - 22, (canRotatePitch() || canRotateRoll()) ? 150 : 130, 18, 18,
                    new GuiTextureGroup(
                            ColorPattern.T_GRAY.rectTexture(),
                            new TextTexture("1").setSupplier(() -> isFlipped ? "MIR" : "OFF")),
                    cd -> flipStructure());
            flipButton.setHoverBorderTexture(1, -1);
            flipButton.setHoverTooltips(Component.translatable("gtceu.multiblock.preview.flip"));
            addWidget(flipButton);
        }

        if (canRotatePitch() || canRotateRoll() || this.controllerDefinition.isAllowFlip()) {
            resetButton = new ButtonWidget(WIDGET_WIDTH - 22, (canRotatePitch() || canRotateRoll()) ? 170 : 150, 18, 18,
                    new GuiTextureGroup(
                            ColorPattern.T_GRAY.rectTexture(),
                            new TextTexture("RST")),
                    cd -> resetOrientation());
            resetButton.setHoverBorderTexture(1, -1);
            resetButton.setHoverTooltips(Component.translatable("gtceu.multiblock.preview.reset"));
            addWidget(resetButton);
        }

        setPage(0);
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
            if (!canRotateRoll()) return;
            this.upwardsFacing = clockwise ? this.upwardsFacing.getClockWise() :
                    this.upwardsFacing.getCounterClockWise();
            this.onOrientationChanged();
        } else {
            if (!canRotatePitch()) return;
            Direction horizontal = (this.baseHorizontalFacing != null) ? this.baseHorizontalFacing : Direction.NORTH;
            Direction[] cycle = new Direction[] { horizontal, Direction.UP, horizontal.getOpposite(), Direction.DOWN };
            int currentIndex = 0;
            for (int i = 0; i < cycle.length; i++) {
                if (cycle[i] == this.frontFacing) {
                    currentIndex = i;
                    break;
                }
            }
            int nextIndex = clockwise ? (currentIndex + 1) % 4 : (currentIndex + 3) % 4;
            this.frontFacing = cycle[nextIndex];
            this.onOrientationChanged();
        }
    }

    public void flipStructure() {
        if (!this.controllerDefinition.isAllowFlip()) return;
        this.isFlipped = !this.isFlipped;
        this.onOrientationChanged();
    }

    public void resetOrientation() {
        this.frontFacing = controllerDefinition.getRotationState().defaultDirection;
        this.baseHorizontalFacing = this.frontFacing.getAxis() == Direction.Axis.Y ? Direction.NORTH : this.frontFacing;
        this.upwardsFacing = Direction.NORTH;
        this.isFlipped = false;
        this.onOrientationChanged();
    }

    private void onOrientationChanged() {
        if (patterns == null || index < 0 || index >= patterns.length) return;
        MBPattern pattern = patterns[index];
        pattern.applyTransformation(this.frontFacing, this.upwardsFacing, this.isFlipped);

        int maxLayer = Math.max(-1, pattern.maxY - pattern.minY);
        if (this.layer > maxLayer) this.layer = maxLayer;

        Vector3f focalCenter = new Vector3f(
                (pattern.xMin + pattern.xMax) / 2.0f,
                (pattern.yMin + pattern.yMax) / 2.0f,
                (pattern.zMin + pattern.zMax) / 2.0f);
        sceneWidget.setCenterSmooth(focalCenter);

        if (candidates != null) {
            for (SlotWidget candidate : candidates) {
                removeWidget(candidate);
            }
            candidates = null;
        }

        setupScene(pattern);
    }

    private void updatePage(boolean reverse) {
        if (patterns == null || patterns.length == 0) return;
        if (reverse) {
            setPage(index - 1 < 0 ? patterns.length - 1 : index - 1);
        } else {
            setPage(index + 1 >= patterns.length ? 0 : index + 1);
        }
    }

    public void stepPage(int delta) {
        if (patterns == null || patterns.length == 0) return;
        int target = Mth.clamp(this.index + delta, 0, patterns.length - 1);
        if (target != this.index) {
            setPage(target);
        }
    }

    private void updateLayer(boolean reverse) {
        if (patterns == null || index < 0 || index >= patterns.length) return;
        MBPattern pattern = patterns[index];
        int maxLayer = Math.max(-1, pattern.maxY - pattern.minY);
        if (reverse) {
            setLayer((layer > -1) ? layer - 1 : maxLayer);
        } else {
            setLayer((layer < maxLayer) ? layer + 1 : -1);
        }
    }

    public void stepLayer(int delta) {
        setLayer(this.layer + delta);
    }

    public void setLayer(int targetLayer) {
        if (patterns == null || index < 0 || index >= patterns.length) return;
        MBPattern pattern = patterns[index];
        int newLayer = Mth.clamp(targetLayer, -1, Math.max(-1, pattern.maxY - pattern.minY));
        if (this.layer == newLayer) return;

        this.layer = newLayer;
        if (this.layer == -1 && !pattern.controllerBase.isFormed()) {
            onFormedSwitch(true);
        } else if (pattern.controllerBase.isFormed()) {
            onFormedSwitch(false);
        }
        setupScene(pattern);
    }

    private void setupScene(MBPattern pattern) {
        Stream<BlockPos> stream = pattern.blockMap.keySet().stream()
                .filter(pos -> layer == -1 || layer + pattern.minY == pos.getY());
        if (pattern.controllerBase.isFormed()) {
            LongSet modelDisabled = pattern.controllerBase.getMultiblockState().getMatchContext().getOrDefault(
                    "renderMask",
                    LongSets.EMPTY_SET);
            if (!modelDisabled.isEmpty()) {
                stream = stream.filter(pos -> !modelDisabled.contains(pos.asLong()));
            }
        }
        Vector3f focalCenter = new Vector3f((pattern.xMin + pattern.xMax) / 2.0f,
                (pattern.yMin + pattern.yMax) / 2.0f,
                (pattern.zMin + pattern.zMax) / 2.0f);
        float maxDim = Math.max(Math.max(pattern.xMax - pattern.xMin, pattern.yMax - pattern.minY),
                pattern.zMax - pattern.zMin);
        float fullZoom = (float) (3.5 * Math.sqrt(Math.max(maxDim, 1)));
        sceneWidget.setFocalCenter(focalCenter);
        sceneWidget.setCustomZoom(fullZoom);
        sceneWidget.setRenderedCore(stream.toList(), null);
    }

    public static PatternPreviewWidget getPatternWidget(MultiblockMachineDefinition controllerDefinition) {
        if (LEVEL == null) {
            if (Minecraft.getInstance().level == null) {
                GTCEu.LOGGER.error("Try to init pattern previews before level load");
                throw new IllegalStateException();
            }
            LEVEL = new TrackedDummyWorld();
        }
        return new PatternPreviewWidget(controllerDefinition);
    }

    public void setPage(int index) {
        if (this.patterns == null || index >= patterns.length || index < 0) return;
        this.index = index;
        this.layer = -1;
        MBPattern pattern = patterns[index];
        pattern.applyTransformation(this.frontFacing, this.upwardsFacing, this.isFlipped);
        Vector3f focalCenter = new Vector3f((pattern.xMin + pattern.xMax) / 2.0f,
                (pattern.yMin + pattern.yMax) / 2.0f,
                (pattern.zMin + pattern.zMax) / 2.0f);
        sceneWidget.setCenter(focalCenter, false);
        setupScene(pattern);
        if (slotWidgets != null) {
            for (SlotWidget slotWidget : slotWidgets) {
                scrollableWidgetGroup.removeWidget(slotWidget);
            }
        }
        slotWidgets = new SlotWidget[pattern.parts.size()];
        var itemHandler = new CycleItemStackHandler(pattern.parts);
        int xOffset = 0;
        for (int i = 0; i < slotWidgets.length; i++) {
            int padding = 1;
            if (itemHandler.getStackInSlot(i).getCount() / 100_000 >= 1) {
                padding = 10;
            } else if (itemHandler.getStackInSlot(i).getCount() / 10_000 >= 1) {
                padding = 7;
            } else if (itemHandler.getStackInSlot(i).getCount() / 1_000 >= 1) {
                padding = 4;
            }

            slotWidgets[i] = new PatternPreviewSlotWidget(itemHandler, i, (4 + xOffset + padding), 0, false, false)
                    .setBackgroundTexture(ColorPattern.T_GRAY.rectTexture())
                    .setIngredientIO(IngredientIO.INPUT);
            xOffset += 18 + (2 * padding);
            scrollableWidgetGroup.addWidget(slotWidgets[i]);
        }
    }

    private void onFormedSwitch(boolean isFormed) {
        MBPattern pattern = patterns[index];
        IMultiController controllerBase = pattern.controllerBase;
        if (isFormed) {
            this.layer = -1;
            loadControllerFormed(pattern.blockMap.keySet(), controllerBase);
        } else {
            sceneWidget.setRenderedCore(pattern.blockMap.keySet(), null);
            controllerBase.onStructureInvalid();
        }
    }

    private void onPosSelected(BlockPos pos, Direction facing) {
        if (index >= patterns.length || index < 0 || patterns[index].predicateMap == null) return;
        TraceabilityPredicate predicate = patterns[index].predicateMap.get(pos);
        if (predicate != null) {
            predicates.clear();
            predicates.addAll(predicate.common);
            predicates.addAll(predicate.limited);
            predicates.removeIf(p -> p == null || p.candidates == null); // why it happens?
            if (candidates != null) {
                for (SlotWidget candidate : candidates) {
                    removeWidget(candidate);
                }
            }
            List<List<ItemStack>> candidateStacks = new ArrayList<>();
            List<List<Component>> predicateTips = new ArrayList<>();
            for (SimplePredicate simplePredicate : predicates) {
                List<ItemStack> itemStacks = simplePredicate.getCandidates();
                if (!itemStacks.isEmpty()) {
                    candidateStacks.add(itemStacks);
                    predicateTips.add(simplePredicate.getToolTips(predicate));
                }
            }
            candidates = new SlotWidget[candidateStacks.size()];
            CycleItemStackHandler itemHandler = new CycleItemStackHandler(candidateStacks);
            int maxCol = Math.max(1, (WIDGET_HEIGHT - 60) / 18);
            for (int i = 0; i < candidateStacks.size(); i++) {
                int finalI = i;
                candidates[i] = new SlotWidget(itemHandler, i, 3 + (i / maxCol) * 18, 14 + (i % maxCol) * 18, false,
                        false)
                        .setIngredientIO(IngredientIO.INPUT)
                        .setBackgroundTexture(new ColorRectTexture(0x4fffffff))
                        .setOnAddedTooltips((slot, list) -> list.addAll(predicateTips.get(finalI)));
                addWidget(candidates[i]);
            }
        }
    }

    /**
     * Finds the next section of the dummy preview level to place a multiblock at in a spiral pattern.
     * <p>
     * This results in positions that are considerably closer to the world origin than
     * the one it replaces, which did {@code prevPos.offset(500, 0, 500)},
     * which results in absurdly high offsets for the later multiblocks.
     * </p>
     * The regions being closer to {@code (0,0)} means that Z-fighting should be less likely,
     * since floating point inaccuracies won't be as large of a factor.
     *
     * @return the area to place the current multiblock at
     */
    public static BlockPos locateNextRegion() {
        int currentIndex = LAST_OFFSET_INDEX++;

        // Origin coordinates scaled back to the offset value, from global
        int x = 0, z = 0;
        if (currentIndex > 0) {
            int v = (int) (Mth.sqrt(currentIndex + 0.25f) - 0.5f);
            int nextV = v + 1;
            int spiralBaseIndex = v * nextV;
            // this is 1 or -1 depending on if v is odd or even
            int flipFlop = (v & 1) * 2 - 1;

            int offset = flipFlop * nextV / 2;
            x += offset;
            z += offset;

            int cornerIndex = spiralBaseIndex + nextV;
            if (currentIndex < cornerIndex) {
                x -= flipFlop * (currentIndex - spiralBaseIndex + 1);
            } else {
                x -= flipFlop * nextV;
                z -= flipFlop * (currentIndex - cornerIndex + 1);
            }
        }
        return new BlockPos(x * REGION_SIZE, 50, z * REGION_SIZE);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (sceneWidget != null && sceneWidget.isTurntableActive() != this.isTurntableActive) {
            sceneWidget.setTurntableActive(this.isTurntableActive);
        }
        // I can only think of this way
        if (!isLoaded && GTCEu.Mods.isEMILoaded() && Minecraft.getInstance().screen instanceof RecipeScreen) {
            setPage(0);
            isLoaded = true;
        } else if (!isLoaded && GTCEu.Mods.isREILoaded() &&
                Minecraft.getInstance().screen instanceof AbstractDisplayViewingScreen) {
                    setPage(0);
                    isLoaded = true;
                }
    }

    @Override
    public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
        if (pageButton != null && pageButton.isMouseOverElement(mouseX, mouseY)) {
            int delta = wheelDelta > 0 ? 1 : -1;
            stepPage(delta);
            return true;
        }
        if (layerButton != null && layerButton.isMouseOverElement(mouseX, mouseY)) {
            int delta = wheelDelta > 0 ? 1 : -1;
            stepLayer(delta);
            return true;
        }
        if (rotateButton != null && rotateButton.isMouseOverElement(mouseX, mouseY)) {
            rotateStructure(wheelDelta > 0);
            return true;
        }
        return super.mouseWheelMove(mouseX, mouseY, wheelDelta);
    }

    @Override
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.enableBlend();
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
    }

    private MBPattern initializePattern(MultiblockShapeInfo shapeInfo, HashSet<ItemStackKey> blockDrops) {
        IMultiController controllerBase = null;
        BlockPos controllerPatternPos = BlockPos.ZERO;
        BlockPos multiPos = locateNextRegion();

        BlockInfo[][][] blocks = shapeInfo.getBlocks();
        for (int x = 0; x < blocks.length; x++) {
            BlockInfo[][] aisle = blocks[x];
            for (int y = 0; y < aisle.length; y++) {
                BlockInfo[] column = aisle[y];
                for (int z = 0; z < column.length; z++) {
                    BlockPos pos = multiPos.offset(x, y, z);
                    if (column[z].getBlockEntity(pos) instanceof IMachineBlockEntity holder &&
                            holder.getMetaMachine() instanceof IMultiController controller) {
                        holder.getSelf().setLevel(LEVEL);
                        controllerBase = controller;
                        controllerPatternPos = new BlockPos(x, y, z);
                    }
                }
            }
        }

        if (controllerBase == null) return null;

        MBPattern pattern = new MBPattern(shapeInfo, multiPos, controllerPatternPos, controllerBase);
        pattern.applyTransformation(controllerDefinition.getRotationState().defaultDirection, Direction.NORTH, false);

        LEVEL.setInnerBlockEntity(controllerBase.self().holder.getSelf());

        Map<ItemStackKey, PartInfo> parts = gatherBlockDrops(pattern.blockMap);
        blockDrops.addAll(parts.keySet());

        pattern.parts = parts.values().stream().sorted((one, two) -> {
            if (one.isController) return -1;
            if (two.isController) return +1;
            if (one.isTile && !two.isTile) return -1;
            if (two.isTile && !one.isTile) return +1;
            if (one.blockId != two.blockId) return two.blockId - one.blockId;
            return two.amount - one.amount;
        }).map(PartInfo::getItemStack).filter(list -> !list.isEmpty()).collect(Collectors.toList());

        loadControllerFormed(pattern.blockMap.keySet(), controllerBase);
        Map<BlockPos, TraceabilityPredicate> predicates = controllerBase.getMultiblockState().getMatchContext()
                .get("predicates");
        pattern.predicateMap = predicates != null ? predicates : new HashMap<>();

        return pattern;
    }

    private void loadControllerFormed(Collection<BlockPos> positions, IMultiController controllerBase) {
        BlockPattern pattern = controllerBase.getPattern();
        if (pattern != null && pattern.checkPatternAt(controllerBase.getMultiblockState(), true)) {
            controllerBase.onStructureFormed();
        }
        if (controllerBase.isFormed()) {
            LongSet modelDisabled = controllerBase.getMultiblockState().getMatchContext().getOrDefault("renderMask",
                    LongSets.EMPTY_SET);
            if (!modelDisabled.isEmpty()) {
                positions = new HashSet<>(positions);
                positions.removeIf(pos -> modelDisabled.contains(pos.asLong()));
            }
            sceneWidget.setRenderedCore(positions, null);
        } else {
            GTCEu.LOGGER.warn("Pattern formed checking failed: {}", controllerBase.self().getDefinition());
        }
    }

    private Map<ItemStackKey, PartInfo> gatherBlockDrops(Map<BlockPos, BlockInfo> blocks) {
        Map<ItemStackKey, PartInfo> partsMap = new Object2ObjectOpenHashMap<>();
        for (Map.Entry<BlockPos, BlockInfo> entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState blockState = PatternPreviewWidget.LEVEL.getBlockState(pos);
            ItemStack itemStack = blockState.getBlock().getCloneItemStack(PatternPreviewWidget.LEVEL, pos, blockState);

            if (itemStack.isEmpty() && !blockState.getFluidState().isEmpty()) {
                Fluid fluid = blockState.getFluidState().getType();
                itemStack = fluid.getBucket().getDefaultInstance();
            }

            ItemStackKey itemStackKey = new ItemStackKey(itemStack);
            partsMap.computeIfAbsent(itemStackKey, key -> new PartInfo(key, entry.getValue())).amount++;
        }
        return partsMap;
    }

    private static class PartInfo {

        final ItemStackKey itemStackKey;
        boolean isController = false;
        boolean isTile = false;
        final int blockId;
        int amount = 0;

        PartInfo(final ItemStackKey itemStackKey, final BlockInfo blockInfo) {
            this.itemStackKey = itemStackKey;
            this.blockId = Block.getId(blockInfo.getBlockState());
            this.isTile = blockInfo.hasBlockEntity();

            if (blockInfo.getBlockState().getBlock() instanceof MetaMachineBlock block) {
                if (block.definition instanceof MultiblockMachineDefinition)
                    this.isController = true;
            }
        }

        public List<ItemStack> getItemStack() {
            return Arrays.stream(itemStackKey.getItemStack())
                    .map(stack -> stack.copyWithCount(amount))
                    .filter(item -> !item.isEmpty())
                    .toList();
        }
    }

    public static class MBPattern {

        public final MultiblockShapeInfo shapeInfo;
        public final BlockPos multiPos;
        public final BlockPos controllerPatternPos;
        @NotNull
        public List<List<ItemStack>> parts;
        @NotNull
        public Map<BlockPos, TraceabilityPredicate> predicateMap;
        @NotNull
        public Map<BlockPos, BlockInfo> blockMap;
        @NotNull
        public final IMultiController controllerBase;

        public int maxY, minY;
        public int xMin, xMax, yMin, yMax, zMin, zMax;

        public Direction currentFront;
        public Direction currentUp;
        public boolean currentFlipped;

        public MBPattern(@NotNull MultiblockShapeInfo shapeInfo, @NotNull BlockPos multiPos,
                         @NotNull BlockPos controllerPatternPos, @NotNull IMultiController controllerBase) {
            this.shapeInfo = shapeInfo;
            this.multiPos = multiPos;
            this.controllerPatternPos = controllerPatternPos;
            this.controllerBase = controllerBase;
            this.parts = new ArrayList<>();
            this.predicateMap = new HashMap<>();
            this.blockMap = new HashMap<>();
        }

        public static Direction.Axis getMirrorAxis(Direction front, Direction up) {
            if (up == Direction.NORTH || up == Direction.SOUTH) {
                if (front.getAxis() == Direction.Axis.Z || front.getAxis() == Direction.Axis.Y) {
                    return Direction.Axis.X;
                } else {
                    return Direction.Axis.Z;
                }
            } else {
                return Direction.Axis.Y;
            }
        }

        public void applyTransformation(Direction front, Direction up, boolean isFlipped) {
            if (LEVEL != null) {
                for (BlockPos pos : this.blockMap.keySet()) {
                    LEVEL.removeBlock(pos);
                }
            }

            Direction.Axis mirrorAxis = getMirrorAxis(front, up);
            Map<BlockPos, BlockInfo> newBlockMap = new HashMap<>();
            BlockInfo[][][] blocks = shapeInfo.getBlocks();
            for (int x = 0; x < blocks.length; x++) {
                BlockInfo[][] aisle = blocks[x];
                for (int y = 0; y < aisle.length; y++) {
                    BlockInfo[] column = aisle[y];
                    for (int z = 0; z < column.length; z++) {
                        BlockInfo info = column[z];
                        if (info == null) continue;
                        boolean isController = (x == controllerPatternPos.getX() &&
                                y == controllerPatternPos.getY() &&
                                z == controllerPatternPos.getZ());
                        BlockPos relP = new BlockPos(x - controllerPatternPos.getX(),
                                y - controllerPatternPos.getY(),
                                z - controllerPatternPos.getZ());

                        BlockPos o1 = rotateByFront(relP, front);
                        BlockPos o2 = rotateByFrontAxis(o1, front, getRollRotation(up));
                        BlockPos o3 = isFlipped ? mirror(o2, mirrorAxis) : o2;

                        BlockPos worldPos = multiPos.offset(controllerPatternPos).offset(o3);
                        BlockState transformedState = rotateBlockState(info.getBlockState(), front, up, isFlipped,
                                mirrorAxis, isController);
                        newBlockMap.put(worldPos, BlockInfo.fromBlockState(transformedState));
                    }
                }
            }

            this.blockMap = newBlockMap;
            this.currentFront = front;
            this.currentUp = up;
            this.currentFlipped = isFlipped;

            BlockPos controllerPos = multiPos.offset(controllerPatternPos);
            BlockInfo cInfo = this.blockMap.get(controllerPos);
            BlockState cState = cInfo != null ? cInfo.getBlockState() : null;

            if (this.controllerBase != null && cState != null) {
                this.controllerBase.self().holder.getSelf().setBlockState(cState);
            }

            if (LEVEL != null) {
                LEVEL.addBlocks(this.blockMap);
                if (this.controllerBase != null) {
                    LEVEL.setInnerBlockEntity(this.controllerBase.self().holder.getSelf());
                }
            }

            recalculateBounds();

            if (this.controllerBase != null) {
                Direction initialFacing = this.controllerBase.self().getDefinition()
                        .getRotationState().defaultDirection;
                Direction controllerFacing = rotateFacing(initialFacing, front, up, isFlipped, mirrorAxis);
                if (controllerFacing == null) controllerFacing = front;

                Direction initialTop = Direction.UP;
                Direction targetTop = rotateFacing(initialTop, front, up, isFlipped, mirrorAxis);
                Direction controllerUpwards = getUpwardsFacing(controllerFacing, targetTop);

                if (this.controllerBase.self().isFacingValid(controllerFacing)) {
                    this.controllerBase.self().setFrontFacing(controllerFacing);
                }

                if (this.controllerBase.self().allowExtendedFacing()) {
                    this.controllerBase.self().setUpwardsFacing(controllerUpwards);
                }

                if (this.controllerBase instanceof MultiblockControllerMachine mcm) {
                    mcm.setFlipped(isFlipped);
                }

                if (cState != null) {
                    this.controllerBase.self().holder.getSelf().setBlockState(cState);
                }

                BlockPattern pattern = this.controllerBase.getPattern();
                if (pattern != null) {
                    this.controllerBase.getMultiblockState().clean();

                    if (pattern.checkPatternAt(this.controllerBase.getMultiblockState(),
                            controllerPos, controllerFacing, controllerUpwards, isFlipped, true) ||
                            pattern.checkPatternAt(this.controllerBase.getMultiblockState(), true)) {
                        this.controllerBase.onStructureFormed();
                    } else {
                        this.controllerBase.onStructureInvalid();
                    }

                    Map<BlockPos, TraceabilityPredicate> predicates = this.controllerBase.getMultiblockState()
                            .getMatchContext().get("predicates");
                    this.predicateMap = (predicates != null) ? predicates : new HashMap<>();
                }
            }
        }

        public void recalculateBounds() {
            int minXVal = Integer.MAX_VALUE, maxXVal = Integer.MIN_VALUE;
            int minYVal = Integer.MAX_VALUE, maxYVal = Integer.MIN_VALUE;
            int minZVal = Integer.MAX_VALUE, maxZVal = Integer.MIN_VALUE;
            for (BlockPos pos : blockMap.keySet()) {
                minXVal = Math.min(minXVal, pos.getX());
                maxXVal = Math.max(maxXVal, pos.getX());
                minYVal = Math.min(minYVal, pos.getY());
                maxYVal = Math.max(maxYVal, pos.getY());
                minZVal = Math.min(minZVal, pos.getZ());
                maxZVal = Math.max(maxZVal, pos.getZ());
            }

            if (minXVal == Integer.MAX_VALUE) {
                minXVal = maxXVal = minYVal = maxYVal = minZVal = maxZVal = 0;
            }

            minY = minYVal;
            maxY = maxYVal;
            xMin = minXVal;
            xMax = maxXVal + 1;
            yMin = minYVal;
            yMax = maxYVal + 1;
            zMin = minZVal;
            zMax = maxZVal + 1;
        }
    }

    public static BlockPos rotateByFront(BlockPos pos, Direction front) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        return switch (front) {
            case NORTH -> new BlockPos(x, y, z);
            case SOUTH -> new BlockPos(x, -y, -z);
            case EAST -> new BlockPos(-z, y, x);
            case WEST -> new BlockPos(z, y, -x);
            case UP -> new BlockPos(x, -z, y);
            case DOWN -> new BlockPos(x, z, -y);
        };
    }

    public static Rotation getRollRotation(Direction up) {
        return switch (up) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public static BlockPos rotateByFrontAxis(BlockPos pos, Direction frontFacing, Rotation rotation) {
        if (rotation == Rotation.NONE) return pos;

        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        if (rotation == Rotation.CLOCKWISE_180) {
            return switch (frontFacing.getAxis()) {
                case X -> new BlockPos(x, -y, -z);
                case Y -> new BlockPos(-x, y, -z);
                case Z -> new BlockPos(-x, -y, z);
            };
        }

        int step = frontFacing.getAxisDirection().getStep();
        boolean clockwise90 = rotation == Rotation.CLOCKWISE_90;

        return switch (frontFacing.getAxis()) {
            case Z -> {
                int newX = clockwise90 ? -step * y : step * y;
                int newY = clockwise90 ? step * x : -step * x;
                yield new BlockPos(newX, newY, z);
            }
            case X -> {
                int newZ = clockwise90 ? step * y : -step * y;
                int newY = clockwise90 ? -step * z : step * z;
                yield new BlockPos(x, newY, newZ);
            }
            case Y -> {
                int newX = clockwise90 ? -step * z : step * z;
                int newZ = clockwise90 ? step * x : -step * x;
                yield new BlockPos(newX, y, newZ);
            }
        };
    }

    public static BlockPos mirror(BlockPos pos, Direction.Axis axis) {
        return switch (axis) {
            case X -> new BlockPos(-pos.getX(), pos.getY(), pos.getZ());
            case Y -> new BlockPos(pos.getX(), -pos.getY(), pos.getZ());
            case Z -> new BlockPos(pos.getX(), pos.getY(), -pos.getZ());
        };
    }

    private static Direction getDirectionFromNormal(int x, int y, int z) {
        if (x > 0) return Direction.EAST;
        if (x < 0) return Direction.WEST;
        if (y > 0) return Direction.UP;
        if (y < 0) return Direction.DOWN;
        if (z > 0) return Direction.SOUTH;
        if (z < 0) return Direction.NORTH;
        return null;
    }

    public static Direction rotateFacing(Direction facing, Direction front, Direction up, boolean isFlipped,
                                         Direction.Axis mirrorAxis) {
        if (facing == null) return null;

        BlockPos normal = new BlockPos(facing.getStepX(), facing.getStepY(), facing.getStepZ());

        BlockPos o1 = rotateByFront(normal, front);
        BlockPos o2 = rotateByFrontAxis(o1, front, getRollRotation(up));
        BlockPos o3 = isFlipped ? mirror(o2, mirrorAxis) : o2;

        Direction rotated = getDirectionFromNormal(o3.getX(), o3.getY(), o3.getZ());

        return (rotated != null) ? rotated : facing;
    }

    public static Direction getTopDirection(Direction facing, Direction upwardsFacing) {
        if (facing == null) facing = Direction.NORTH;

        if (upwardsFacing == null) upwardsFacing = Direction.NORTH;

        if (facing.getAxis() == Direction.Axis.Y) {
            return upwardsFacing.getAxis().isHorizontal() ? upwardsFacing : Direction.NORTH;
        }

        return switch (upwardsFacing) {
            case NORTH -> Direction.UP;
            case SOUTH -> Direction.DOWN;
            case EAST -> facing.getClockWise();
            case WEST -> facing.getCounterClockWise();
            default -> Direction.UP;
        };
    }

    public static Direction getUpwardsFacing(Direction facing, Direction top) {
        if (facing == null) facing = Direction.NORTH;

        if (top == null) top = Direction.UP;

        if (facing.getAxis() == Direction.Axis.Y) {
            return top.getAxis().isHorizontal() ? top : Direction.NORTH;
        }

        if (top == Direction.UP) {
            return Direction.NORTH;
        } else if (top == Direction.DOWN) {
            return Direction.SOUTH;
        } else if (top == facing.getClockWise()) {
            return Direction.EAST;
        } else if (top == facing.getCounterClockWise()) {
            return Direction.WEST;
        }

        return Direction.NORTH;
    }

    public static BlockState rotateBlockState(BlockState state, Direction front, Direction up, boolean isFlipped,
                                              Direction.Axis mirrorAxis, boolean isController) {
        if (state.getBlock() instanceof MetaMachineBlock machine) {
            var rotationState = machine.definition.getRotationState();
            Direction initialFacing = null;

            if (rotationState != RotationState.NONE && rotationState.property != null &&
                    state.hasProperty(rotationState.property)) {
                initialFacing = state.getValue(rotationState.property);
            }

            if (initialFacing == null) initialFacing = Direction.NORTH;

            Direction newFacing = rotateFacing(initialFacing, front, up, isFlipped, mirrorAxis);
            if (newFacing != null && rotationState != RotationState.NONE && rotationState.property != null &&
                    state.hasProperty(rotationState.property) && rotationState.test(newFacing)) {
                state = state.setValue(rotationState.property, newFacing);
            }

            if (machine.definition.isAllowExtendedFacing() &&
                    state.hasProperty(GTBlockStateProperties.UPWARDS_FACING)) {
                Direction initialUpwards = state.getValue(GTBlockStateProperties.UPWARDS_FACING);
                Direction initialTop = getTopDirection(initialFacing, initialUpwards);
                Direction newTop = rotateFacing(initialTop, front, up, isFlipped, mirrorAxis);

                Direction effectiveFacing = (rotationState != RotationState.NONE && rotationState.property != null &&
                        state.hasProperty(rotationState.property)) ? state.getValue(rotationState.property) :
                                (newFacing != null ? newFacing : front);

                Direction newUpwards = getUpwardsFacing(effectiveFacing, newTop);
                state = state.setValue(GTBlockStateProperties.UPWARDS_FACING, newUpwards);
            }
            return state;
        }

        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction facing = state.getValue(BlockStateProperties.FACING);
            Direction newFacing = rotateFacing(facing, front, up, isFlipped, mirrorAxis);

            if (newFacing != null) {
                state = state.setValue(BlockStateProperties.FACING, newFacing);
            }
        } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            Direction newFacing = rotateFacing(facing, front, up, isFlipped, mirrorAxis);

            if (newFacing != null && newFacing.getAxis().isHorizontal()) {
                state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, newFacing);
            }
        }

        return state;
    }
}
