package com.gregtechceu.gtceu.common.item.armor;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.item.armor.ArmorComponentItem;
import com.gregtechceu.gtceu.api.item.armor.ArmorUtils;
import com.gregtechceu.gtceu.api.item.armor.IArmorLogic;
import com.gregtechceu.gtceu.api.item.component.*;
import com.gregtechceu.gtceu.api.item.component.forge.IComponentCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.utils.GradientUtil;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;

import it.unimi.dsi.fastutil.ints.IntIntPair;
import it.unimi.dsi.fastutil.objects.AbstractObject2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PowerlessJetpackLogic implements IArmorLogic, IJetpack, IItemHUDProvider {

    // Map of FluidIngredient -> burn time
    public static final AbstractObject2IntMap<FluidIngredient> FUELS = new Object2IntOpenHashMap<>();
    public static final int DEFAULT_TANK_CAPACITY = 16000;

    private static final String TAG_BURN_TIMER = "burnTimer";

    private final int tankCapacity;
    private final JetpackStats stats;
    private final ResourceLocation texture;

    @OnlyIn(Dist.CLIENT)
    private ArmorUtils.ModularHUD HUD;

    public PowerlessJetpackLogic() {
        this(DEFAULT_TANK_CAPACITY, JetpackStats.DEFAULT, GTCEu.id("textures/armor/liquid_fuel_jetpack.png"));
    }

    public PowerlessJetpackLogic(int tankCapacity, JetpackStats stats, ResourceLocation texture) {
        this.tankCapacity = tankCapacity;
        this.stats = stats;
        this.texture = texture;
        if (GTCEu.isClientSide())
            HUD = new ArmorUtils.ModularHUD();
    }

    public static void registerFuel(FluidIngredient fuel, int burnTime) {
        FUELS.putIfAbsent(fuel, burnTime);
    }

    public static void registerFuel(GTRecipe recipe) {
        Content content = recipe.getInputContents(FluidRecipeCapability.CAP).get(0);
        registerFuel(FluidRecipeCapability.CAP.of(content.content), recipe.duration);
    }

    public static void reloadFuels(RecipeManager manager) {
        clearFuels();
        for (GTRecipe recipe : manager.getAllRecipesFor(GTRecipeTypes.COMBUSTION_GENERATOR_FUELS)) {
            registerFuel(recipe);
        }
    }

    public static void clearFuels() {
        FUELS.clear();
    }

    @Override
    public JetpackStats getJetpackStats() {
        return stats;
    }

    @Override
    public void onArmorTick(Level world, Player player, @NotNull ItemStack stack) {
        if (!FluidUtil.getFluidHandler(stack).isPresent()) return;
        tickJetpack(world, player, stack, null);
    }

    @Override
    public ArmorItem.Type getArmorType() {
        return ArmorItem.Type.CHESTPLATE;
    }

    @Override
    public int getArmorDisplay(Player player, @NotNull ItemStack armor, EquipmentSlot slot) {
        return 0;
    }

    @Override
    public void addToolComponents(@NotNull ArmorComponentItem item) {
        item.attachComponents(new Behaviour(tankCapacity));
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return texture;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void drawHUD(@NotNull ItemStack item, GuiGraphics guiGraphics) {
        IFluidHandler tank = FluidUtil.getFluidHandler(item).resolve().orElse(null);
        if (tank != null) {
            if (tank.getFluidInTank(0).getAmount() == 0) return;
            String formated = String.format("%.1f",
                    (tank.getFluidInTank(0).getAmount() * 100.0F / tank.getTankCapacity(0)));
            this.HUD.newString(Component.translatable("metaarmor.hud.fuel_lvl", formated + "%"));
            JetpackHelper.addStatusHUD(this.HUD, item.getTag());
        }
        this.HUD.draw(guiGraphics);
        this.HUD.reset();
    }

    @Override
    public int getEnergyPerUse() {
        return 1;
    }

    @Override
    public boolean canUseEnergy(ItemStack stack, int amount) {
        if (getBurnTimer(stack) > 0) return true;
        return findFuel(stack) != null;
    }

    @Override
    public void drainEnergy(ItemStack stack, int amount) {
        int burnTimer = getBurnTimer(stack);
        if (burnTimer <= 0) {
            FluidIngredient fuel = findFuel(stack);
            if (fuel == null) return;
            FluidUtil.getFluidHandler(stack)
                    .ifPresent(h -> h.drain(fuel.getAmount(), FluidAction.EXECUTE));
            burnTimer = FUELS.getInt(fuel);
        }
        stack.getOrCreateTag().putInt(TAG_BURN_TIMER, burnTimer - amount);
    }

    @Override
    public boolean hasEnergy(ItemStack stack) {
        return getBurnTimer(stack) > 0 || findFuel(stack) != null;
    }

    private static int getBurnTimer(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getInt(TAG_BURN_TIMER);
    }

    @Nullable
    private static FluidIngredient findFuel(ItemStack stack) {
        FluidStack fluid = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
        if (fluid.isEmpty()) return null;
        for (var fuel : FUELS.keySet()) {
            if (fuel.test(fluid) && fluid.getAmount() >= fuel.getAmount()) {
                return fuel;
            }
        }
        return null;
    }

    public static class Behaviour implements IDurabilityBar, IItemComponent, ISubItemHandler, IAddInformation,
                                  IInteractionItem, IComponentCapability {

        public final int maxCapacity;
        private final IntIntPair durabilityBarColors;

        public Behaviour(int internalCapacity) {
            this.maxCapacity = internalCapacity;
            this.durabilityBarColors = GradientUtil.getGradient(0xB7AF08, 10);
        }

        @Override
        public float getDurabilityForDisplay(@NotNull ItemStack itemStack) {
            return FluidUtil.getFluidContained(itemStack)
                    .map(stack -> (float) stack.getAmount() / maxCapacity)
                    .orElse(0f);
        }

        @Nullable
        @Override
        public IntIntPair getDurabilityColorsForDisplay(ItemStack itemStack) {
            return durabilityBarColors;
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(ItemStack itemStack, @NotNull Capability<T> cap) {
            return ForgeCapabilities.FLUID_HANDLER_ITEM.orEmpty(cap,
                    LazyOptional.of(() -> new FluidHandlerItemStack(itemStack, maxCapacity) {

                        @Override
                        public boolean canFillFluidType(FluidStack fluid) {
                            for (var ingredient : FUELS.keySet()) {
                                if (ingredient.test(fluid)) return true;
                            }
                            return false;
                        }
                    }));
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                                    TooltipFlag isAdvanced) {
            JetpackHelper.addTooltip(tooltipComponents, stack.getOrCreateTag());
        }

        @Override
        public void fillItemCategory(Item item, CreativeModeTab category, NonNullList<ItemStack> items) {
            ItemStack copy = item.getDefaultInstance();
            IFluidHandler fluidHandlerItem = FluidUtil.getFluidHandler(copy).resolve().orElse(null);
            if (fluidHandlerItem != null) {
                fluidHandlerItem.fill(GTMaterials.Diesel.getFluid(maxCapacity), IFluidHandler.FluidAction.SIMULATE);
                items.add(copy);
            } else {
                items.add(copy);
            }
        }
    }
}
