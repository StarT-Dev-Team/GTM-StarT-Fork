package com.gregtechceu.gtceu.integration.kjs.builders;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.gui.SteamTexture;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI;
import com.gregtechceu.gtceu.api.registry.registrate.BuilderBase;
import com.gregtechceu.gtceu.api.sound.SoundEntry;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class GTRecipeTypeBuilder extends BuilderBase<GTRecipeType> {

    public transient String name, category;
    public transient final Object2IntMap<RecipeCapability<?>> maxInputs;
    public transient final Object2IntMap<RecipeCapability<?>> maxOutputs;
    private SpriteTexture progressBarTexture;
    private FillDirection progressBarDirection;
    private SteamTexture steamProgressBarTexture;
    private FillDirection steamMoveType;
    private transient final Byte2ObjectMap<IGuiTexture> slotOverlays;
    @Nullable
    protected SoundEntry sound;
    protected boolean hasResearchSlot;
    protected int maxTooltips;
    protected int minWidth;

    private GTRecipeType smallRecipeMap;
    private Supplier<ItemStack> iconSupplier;
    @Nullable
    protected BiConsumer<GTRecipe, UIElement> uiBuilder;
    private boolean layered;

    public GTRecipeTypeBuilder(ResourceLocation i) {
        super(i);
        name = i.getPath();
        maxInputs = new Object2IntOpenHashMap<>();
        maxOutputs = new Object2IntOpenHashMap<>();
        progressBarTexture = null;
        progressBarDirection = FillDirection.LEFT_TO_RIGHT;
        steamProgressBarTexture = null;
        steamMoveType = FillDirection.LEFT_TO_RIGHT;
        slotOverlays = new Byte2ObjectArrayMap<>();
        this.sound = null;
        this.hasResearchSlot = false;
        this.maxTooltips = 4;
        this.minWidth = GTRecipeTypeUI.DEFAULT_MIN_WIDTH;
        this.smallRecipeMap = null;
        this.iconSupplier = null;
        this.uiBuilder = null;
    }

    public GTRecipeTypeBuilder category(String category) {
        this.category = category;
        return this;
    }

    public GTRecipeTypeBuilder setMaxIOSize(int maxInputs, int maxOutputs, int maxFluidInputs, int maxFluidOutputs) {
        return setMaxSize(IO.IN, ItemRecipeCapability.CAP, maxInputs)
                .setMaxSize(IO.IN, FluidRecipeCapability.CAP, maxFluidInputs)
                .setMaxSize(IO.OUT, ItemRecipeCapability.CAP, maxOutputs)
                .setMaxSize(IO.OUT, FluidRecipeCapability.CAP, maxFluidOutputs);
    }

    public GTRecipeTypeBuilder setEUIO(IO io) {
        if (io.support(IO.IN)) {
            setMaxSize(IO.IN, EURecipeCapability.CAP, 1);
        }
        if (io.support(IO.OUT)) {
            setMaxSize(IO.OUT, EURecipeCapability.CAP, 1);
        }
        return this;
    }

    public GTRecipeTypeBuilder setMaxSize(IO io, RecipeCapability<?> cap, int max) {
        if (io == IO.IN || io == IO.BOTH) {
            maxInputs.put(cap, max);
        }
        if (io == IO.OUT || io == IO.BOTH) {
            maxOutputs.put(cap, max);
        }
        return this;
    }

    public GTRecipeTypeBuilder setSlotOverlay(boolean isOutput, boolean isFluid, IGuiTexture slotOverlay) {
        return this.setSlotOverlay(isOutput, isFluid, false, slotOverlay).setSlotOverlay(isOutput, isFluid, true,
                slotOverlay);
    }

    public GTRecipeTypeBuilder setSlotOverlay(boolean isOutput, boolean isFluid, boolean isLast,
                                              IGuiTexture slotOverlay) {
        this.slotOverlays.put((byte) ((isOutput ? 2 : 0) + (isFluid ? 1 : 0) + (isLast ? 4 : 0)), slotOverlay);
        return this;
    }

    public GTRecipeTypeBuilder setProgressBar(SpriteTexture progressBar, FillDirection moveType) {
        this.progressBarTexture = progressBar;
        this.progressBarDirection = moveType;
        return this;
    }

    public GTRecipeTypeBuilder setSteamProgressBar(SteamTexture progressBar, FillDirection moveType) {
        this.steamProgressBarTexture = progressBar;
        this.steamMoveType = moveType;
        return this;
    }

    public GTRecipeTypeBuilder setSound(SoundEntry sound) {
        this.sound = sound;
        return this;
    }

    public GTRecipeTypeBuilder setHasResearchSlot(boolean hasResearchSlot) {
        this.hasResearchSlot = hasResearchSlot;
        return this;
    }

    public GTRecipeTypeBuilder setMaxTooltips(int maxTooltips) {
        this.maxTooltips = maxTooltips;
        return this;
    }

    public GTRecipeTypeBuilder setMinWidth(int minWidth) {
        this.minWidth = minWidth;

        return this;
    }

    public GTRecipeTypeBuilder setSmallRecipeMap(GTRecipeType smallRecipeMap) {
        this.smallRecipeMap = smallRecipeMap;
        return this;
    }

    public GTRecipeTypeBuilder setIconSupplier(Supplier<ItemStack> iconSupplier) {
        this.iconSupplier = iconSupplier;
        return this;
    }

    public GTRecipeTypeBuilder setUiBuilder(BiConsumer<GTRecipe, UIElement> uiBuilder) {
        this.uiBuilder = uiBuilder;
        return this;
    }

    public GTRecipeTypeBuilder setLayered() {
        this.layered = true;
        return this;
    }

    @Override
    public GTRecipeType register() {
        var type = GTRecipeTypes.register(name, category);
        if (layered) type.setLayered();
        type.maxInputs.putAll(maxInputs);
        type.maxOutputs.putAll(maxOutputs);
        type.getRecipeUI().getSlotOverlays().putAll(slotOverlays);
        if (progressBarTexture != null) type.getRecipeUI().setProgressBar(progressBarTexture, progressBarDirection);
        type.getRecipeUI().setSteamProgressBarTexture(steamProgressBarTexture);
        type.getRecipeUI().setSteamMoveType(steamMoveType);
        type.setSound(sound);
        type.setHasResearchSlot(hasResearchSlot);
        type.setMaxTooltips(maxTooltips);
        type.setMinWidth(minWidth);
        type.setSmallRecipeMap(smallRecipeMap);
        type.setIconSupplier(iconSupplier);
        if (uiBuilder != null) type.setUiBuilder(uiBuilder);
        return value = type;
    }
}
