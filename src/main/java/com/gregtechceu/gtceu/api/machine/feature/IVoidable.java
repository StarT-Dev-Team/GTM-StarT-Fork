package com.gregtechceu.gtceu.api.machine.feature;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.texture.UVSpriteTexture;
import com.gregtechceu.gtceu.api.gui.widget.EnumSelector;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.FancySelectorConfigurator;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Predicate;

public interface IVoidable extends IMachineFeature {

    default boolean canVoidRecipeOutputs(RecipeCapability<?> capability) {
        return getVoidingMode().canVoid(capability) ||
                self().getDefinition().getRecipeOutputLimits().getOrDefault(capability, -1) == 0;
    }

    default Reference2IntMap<RecipeCapability<?>> getOutputLimits() {
        return self().getDefinition().getRecipeOutputLimits();
    }

    default void setVoidingMode(VoidingMode mode) {}

    default VoidingMode getVoidingMode() {
        return VoidingMode.VOID_NONE;
    }

    static void attachConfigurators(ConfiguratorPanel configuratorPanel, IVoidable controller) {
        configuratorPanel
                .attachConfigurators(new FancySelectorConfigurator<>(VoidingMode.VALUES, controller.getVoidingMode(),
                        controller::setVoidingMode)
                        .setTooltip(m -> List.of(Component.translatable("gtceu.gui.multiblock.voiding_mode"),
                                Component.translatable(m.localeName))));
    }

    enum VoidingMode implements StringRepresentable, EnumSelector.SelectableEnum {

        VOID_NONE("gtceu.gui.no_voiding", cap -> false),
        VOID_ITEMS("gtceu.gui.item_voiding", cap -> cap == ItemRecipeCapability.CAP),
        VOID_FLUIDS("gtceu.gui.fluid_voiding", cap -> cap == FluidRecipeCapability.CAP),
        VOID_ITEMS_FLUIDS("gtceu.gui.all_voiding",
                cap -> cap == ItemRecipeCapability.CAP || cap == FluidRecipeCapability.CAP);

        public static final VoidingMode[] VALUES = values();

        private final String localeName;
        @Getter
        private final IGuiTexture icon;

        @Override
        public IGuiTexture getIcon() {
            return icon;
        }

        private final Predicate<RecipeCapability<?>> canVoid;

        VoidingMode(String name, Predicate<RecipeCapability<?>> canVoid) {
            this.localeName = name;
            this.canVoid = canVoid;
            this.icon = UVSpriteTexture.of(GuiTextures.BUTTON_VOID_MULTIBLOCK, 0, ordinal() * 0.25f, 1, 0.25f);
        }

        public boolean canVoid(RecipeCapability<?> capability) {
            return canVoid.test(capability);
        }

        @NotNull
        @Override
        public String getSerializedName() {
            return localeName;
        }

        @Override
        public @NotNull String getTooltip() {
            return localeName;
        }
    }
}
