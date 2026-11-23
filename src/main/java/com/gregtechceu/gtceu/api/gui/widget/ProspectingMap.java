package com.gregtechceu.gtceu.api.gui.widget;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.misc.PacketProspecting;
import com.gregtechceu.gtceu.api.gui.misc.ProspectorMode;
import com.gregtechceu.gtceu.api.gui.texture.ProspectingTexture;
import com.gregtechceu.gtceu.api.item.IComponentItem;
import com.gregtechceu.gtceu.common.item.ProspectorScannerBehavior;
import com.gregtechceu.gtceu.integration.map.WaypointManager;
import com.gregtechceu.gtceu.integration.map.cache.client.GTClientCache;
import com.gregtechceu.gtceu.integration.map.cache.server.ServerCache;
import com.gregtechceu.gtceu.integration.map.layer.builtin.OreRenderLayer;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SearchComponent;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;

import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;

public class ProspectingMap extends UIElement {

    private static final String SCAN_MESSAGE = "scan";

    private final Player player;
    private final int chunkRadius;
    private final ProspectorMode mode;
    private final int scanTick;
    @Getter
    private boolean darkMode = false;
    private final ScrollerView itemList = new ScrollerView();
    private final UIElement mapView = new MapView();
    @Nullable
    private ProspectingTexture texture;
    private final int playerChunkX;
    private final int playerChunkZ;
    private int chunkIndex = 0;
    private int tickCount = 0;
    private final Queue<PacketProspecting> packetQueue = new LinkedBlockingQueue<>();
    private final Set<Object> items = new CopyOnWriteArraySet<>();
    private final Map<String, UIElement> selectedMap = new ConcurrentHashMap<>();
    @Nullable
    private UIElement selectedRow;

    public ProspectingMap(Player player, int chunkRadius, @NotNull ProspectorMode mode, int scanTick) {
        this.player = player;
        this.chunkRadius = chunkRadius;
        this.mode = mode;
        this.scanTick = scanTick;
        this.playerChunkX = player.chunkPosition().x;
        this.playerChunkZ = player.chunkPosition().z;
        int imageSize = (chunkRadius * 2 - 1) * 16;
        layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(2).widthPercent(100).heightPercent(100));

        var mapFrame = new UIElement();
        mapFrame.layout(layout -> layout.width(imageSize + 8).heightPercent(100).alignItems(AlignItems.CENTER)
                .paddingAll(4));
        mapFrame.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND_INVERSE));
        mapView.layout(layout -> layout.width(imageSize).height(imageSize));
        mapView.addEventListener(UIEvents.HOVER_TOOLTIPS, this::onHoverTooltips);
        mapView.addEventListener(UIEvents.MOUSE_DOWN, this::onMapClicked);
        mapFrame.addChild(mapView);

        var side = new UIElement();
        side.layout(layout -> layout.flex(1).heightPercent(100).paddingAll(4).gapAll(2));
        side.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND_INVERSE));
        var search = new SearchComponent<>(new SearchComponent.ISearchUI<Object>() {

            @Override
            public String resultText(Object value) {
                return I18n.get(mode.getDescriptionId(value));
            }

            @Override
            public void onResultSelected(@Nullable Object value) {
                if (value != null) selectResult(value);
            }

            @Override
            public void search(String word, IResultHandler<Object> find) {
                ProspectingMap.this.search(word, find);
            }
        });
        search.layout(layout -> layout.widthPercent(100).height(18));
        itemList.layout(layout -> layout.widthPercent(100).flex(1));
        side.addChildren(search, itemList);

        addChildren(mapFrame, side);

        if (player.level().isClientSide) {
            texture = new ProspectingTexture(playerChunkX, playerChunkZ, player.getBlockX(), player.getBlockZ(),
                    player.getVisualRotationYInDegrees(), mode, chunkRadius, darkMode);
            addEventListener(UIEvents.TICK, event -> drainPackets());
        }
        onMessage(SCAN_MESSAGE, this::receiveScan);
        addNewItem("[all]", "all resources", IGuiTexture.EMPTY, -1);
    }

    public void setDarkMode(boolean darkMode) {
        if (this.darkMode != darkMode) {
            this.darkMode = darkMode;
            if (texture != null) {
                texture.setDarkMode(darkMode);
            }
        }
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (player.level().isClientSide) return;
        tickCount++;
        var gridSize = chunkRadius * 2 - 1;
        if (tickCount % scanTick == 0 && chunkIndex < gridSize * gridSize) {
            int row = chunkIndex / gridSize;
            int column = chunkIndex % gridSize;
            int ox = column - chunkRadius + 1;
            int oz = row - chunkRadius + 1;

            var level = player.level();
            var chunk = level.getChunk(playerChunkX + ox, playerChunkZ + oz);
            if (mode == ProspectorMode.ORE) {
                ServerCache.instance.prospectAllInChunk(level.dimension(), chunk.getPos(), (ServerPlayer) player);
            }
            var packet = new PacketProspecting(playerChunkX + ox, playerChunkZ + oz, mode);
            mode.scan(packet.data, chunk);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            packet.writePacketData(buffer);
            var bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            var tag = new CompoundTag();
            tag.putByteArray("data", bytes);
            sendMessage(SCAN_MESSAGE, tag);
            chunkIndex++;
        }
        var held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (held.getItem() instanceof IComponentItem componentItem) {
            for (var component : componentItem.getComponents()) {
                if (component instanceof ProspectorScannerBehavior prospector) {
                    if (!player.isCreative() && !prospector.drainEnergy(held, false)) {
                        player.closeContainer();
                    }
                }
            }
        }
    }

    private void receiveScan(CompoundTag tag) {
        if (texture == null) return;
        var buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(tag.getByteArray("data")));
        var packet = PacketProspecting.readPacketData(mode, buffer);
        packetQueue.add(packet);
        if (mode == ProspectorMode.FLUID && packet.data[0][0].length > 0) {
            GTClientCache.instance.addFluid(player.level().dimension(), packet.chunkX, packet.chunkZ,
                    (ProspectorMode.FluidInfo) packet.data[0][0][0]);
        }
    }

    private void drainPackets() {
        if (texture == null) return;
        int max = 10;
        while (max-- > 0 && !packetQueue.isEmpty()) {
            var packet = packetQueue.poll();
            texture.updateTexture(packet);
            addOresToList(packet.data);
        }
    }

    private void addOresToList(Object[][][] data) {
        var newItems = new HashSet<>();
        for (int x = 0; x < mode.cellSize; x++) {
            for (int z = 0; z < mode.cellSize; z++) {
                for (var item : data[x][z]) {
                    newItems.add(item);
                    addNewItem(mode.getUniqueID(item), mode.getDescriptionId(item), mode.getItemIcon(item),
                            mode.getItemColor(item));
                }
            }
        }
        items.addAll(newItems);
    }

    private void addNewItem(String uniqueID, String renderingName, IGuiTexture icon, int color) {
        if (selectedMap.containsKey(uniqueID)) return;
        var row = new UIElement();
        row.layout(layout -> layout.widthPercent(100).height(15).flexDirection(FlexDirection.ROW));
        var iconElement = new UIElement();
        iconElement.layout(layout -> layout.width(15).height(15));
        iconElement.style(style -> style.backgroundTexture(icon));
        var name = new Label().setText(Component.translatable(renderingName));
        name.layout(layout -> layout.flex(1).height(15));
        name.textStyle(style -> style.textWrap(TextWrap.HIDE).textAlignHorizontal(Horizontal.LEFT));
        row.addChildren(iconElement, name);
        row.addEventListener(UIEvents.MOUSE_DOWN, event -> selectEntry(uniqueID));
        itemList.addScrollViewChild(row);
        selectedMap.put(uniqueID, row);
    }

    private void selectEntry(String uniqueID) {
        if (texture != null) {
            texture.setSelected(uniqueID);
        }
        if (selectedRow != null) {
            selectedRow.style(style -> style.overlay(IGuiTexture.EMPTY));
        }
        selectedRow = selectedMap.get(uniqueID);
        if (selectedRow != null) {
            selectedRow.style(style -> style.overlay(new ColorBorderTexture(1, -1)));
        }
    }

    private void selectResult(Object item) {
        selectEntry(mode.getUniqueID(item));
    }

    private void search(String s, IResultHandler<Object> consumer) {
        var added = new HashSet<String>();
        var query = s.toLowerCase(Locale.ROOT);
        for (var item : this.items) {
            if (Thread.currentThread().isInterrupted()) return;
            var id = mode.getUniqueID(item);
            if (added.add(id)) {
                var localized = I18n.get(mode.getDescriptionId(item));
                if (item.toString().toLowerCase(Locale.ROOT).contains(query) ||
                        localized.toLowerCase(Locale.ROOT).contains(query)) {
                    consumer.acceptResult(item);
                }
            }
        }
    }

    private int chunkCell(float local) {
        return (int) local / 16;
    }

    private void onHoverTooltips(UIEvent event) {
        if (texture == null) return;
        int cX = chunkCell(event.x - mapView.getContentX());
        int cZ = chunkCell(event.y - mapView.getContentY());
        var gridSize = chunkRadius * 2 - 1;
        if (cX < 0 || cZ < 0 || cX >= gridSize || cZ >= gridSize) return;
        var tooltips = new ArrayList<Component>();
        tooltips.add(Component.translatable(mode.unlocalizedName));
        var cells = new ArrayList<Object[]>();
        for (int i = 0; i < mode.cellSize; i++) {
            for (int j = 0; j < mode.cellSize; j++) {
                if (texture.data[cX * mode.cellSize + i][cZ * mode.cellSize + j] != null) {
                    cells.add(texture.data[cX * mode.cellSize + i][cZ * mode.cellSize + j]);
                }
            }
        }
        mode.appendTooltips(cells, tooltips, texture.getSelected());
        event.hoverTooltips = new HoverTooltips(tooltips, null, null, null);
    }

    private void onMapClicked(UIEvent event) {
        if (texture == null || !player.level().isClientSide) return;
        var clickedItem = getClickedVein(event.x - mapView.getContentX(), event.y - mapView.getContentY());
        if (clickedItem == null || !WaypointManager.isActive()) return;
        MutableComponent veinName = Component.literal(clickedItem.name());
        veinName.setStyle(veinName.getStyle().withColor(clickedItem.color));
        WaypointManager.setWaypoint(new ChunkPos(clickedItem.position).toString(),
                clickedItem.name,
                clickedItem.color,
                player.level().dimension(),
                clickedItem.position.getX(), clickedItem.position.getY(), clickedItem.position.getZ());
        player.displayClientMessage(Component.translatable("behavior.prospector.added_waypoint", veinName), false);
    }

    @Nullable
    private WaypointItem getClickedVein(float localX, float localY) {
        if (texture == null) return null;
        int cX = (int) localX / 16;
        int cZ = (int) localY / 16;
        int offsetX = Math.abs((int) localX % 16);
        int offsetZ = Math.abs((int) localY % 16);
        int xDiff = cX - (chunkRadius - 1);
        int zDiff = cZ - (chunkRadius - 1);

        int xPos = ((player.chunkPosition().x + xDiff) << 4) + offsetX;
        int zPos = ((player.chunkPosition().z + zDiff) << 4) + offsetZ;

        var blockPos = new BlockPos(xPos, player.level().getHeight(Heightmap.Types.WORLD_SURFACE, xPos, zPos), zPos);
        if (cX < 0 || cZ < 0 || cX >= chunkRadius * 2 - 1 || cZ >= chunkRadius * 2 - 1) {
            return null;
        }

        if (!texture.getSelected().equals(ProspectingTexture.SELECTED_ALL)) {
            for (var item : items) {
                if (!texture.getSelected().equals(mode.getUniqueID(item))) continue;
                var name = Component.translatable(mode.getDescriptionId(item)).getString();
                var color = mode.getItemColor(item);
                return new WaypointItem(blockPos, name, color);
            }
        }

        var hoveredItem = texture.data[cX * mode.cellSize + (offsetX * mode.cellSize / 16)][cZ * mode.cellSize +
                (offsetZ * mode.cellSize / 16)];
        if (hoveredItem != null && hoveredItem.length != 0) {
            var name = Component.translatable(mode.getDescriptionId(hoveredItem[0])).getString();
            var color = mode.getItemColor(hoveredItem[0]);
            return new WaypointItem(blockPos, name, color);
        }

        var vein = GTClientCache.instance.getNearbyVeins(player.level().dimension(), blockPos, 32);
        if (!vein.isEmpty()) {
            vein.sort((o1, o2) -> (int) (o1.center().distToCenterSqr(xPos, o1.center().getY(), zPos) -
                    o2.center().distToCenterSqr(xPos, o2.center().getY(), zPos)));
            var name = OreRenderLayer.getName(vein.get(0)).getString();
            var materials = vein.get(0).definition().veinGenerator().getAllMaterials();
            var mostCommonItem = materials.get(materials.size() - 1);
            var color = mostCommonItem.getMaterialRGB();
            return new WaypointItem(blockPos, name, color);
        }

        return new WaypointItem(blockPos, "Depleted Vein", 0x990000);
    }

    private record WaypointItem(BlockPos position, String name, int color) {}

    private class MapView extends UIElement {

        @Override
        public void drawBackgroundAdditional(GUIContext guiContext) {
            super.drawBackgroundAdditional(guiContext);
            if (texture == null) return;
            var graphics = guiContext.graphics;
            int x = (int) getContentX();
            int y = (int) getContentY();
            texture.draw(graphics, x, y);
            int cX = (int) (guiContext.mouseX - x) / 16;
            int cZ = (int) (guiContext.mouseY - y) / 16;
            var gridSize = chunkRadius * 2 - 1;
            if (isHover() && guiContext.mouseX >= x && guiContext.mouseY >= y && cX < gridSize && cZ < gridSize) {
                DrawerHelper.drawSolidRect(graphics, cX * 16 + x, cZ * 16 + y, 16, 16, 0x4B6C6C6C);
            }
        }
    }
}
