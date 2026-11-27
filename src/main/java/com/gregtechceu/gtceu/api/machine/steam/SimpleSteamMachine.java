package com.gregtechceu.gtceu.api.machine.steam;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.widget.PredicatedImage;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IExhaustVentMachine;
import com.gregtechceu.gtceu.api.machine.feature.IUIMachine;
import com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerList;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.client.model.machine.MachineRenderState;
import com.gregtechceu.gtceu.common.recipe.condition.VentCondition;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fluids.FluidType;

import com.google.common.collect.Tables;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SimpleSteamMachine extends SteamWorkableMachine implements IExhaustVentMachine, IUIMachine {

    @Persisted
    public final NotifiableItemStackHandler importItems;
    @Persisted
    public final NotifiableItemStackHandler exportItems;
    @Getter
    @Setter
    @Persisted
    private boolean needsVenting;

    public SimpleSteamMachine(IMachineBlockEntity holder, boolean isHighPressure, Object... args) {
        super(holder, isHighPressure, args);
        this.importItems = createImportItemHandler(args);
        this.exportItems = createExportItemHandler(args);

        MachineRenderState renderState = getRenderState();
        if (renderState.hasProperty(GTMachineModelProperties.VENT_DIRECTION)) {
            // outputFacing will always be opposite the front facing on init
            setRenderState(renderState.setValue(GTMachineModelProperties.VENT_DIRECTION, RelativeDirection.BACK));
        }
    }

    //////////////////////////////////////
    // ***** Initialization *****//
    //////////////////////////////////////

    @Override
    protected NotifiableFluidTank createSteamTank(Object... args) {
        return new NotifiableFluidTank(this, 1, 16 * FluidType.BUCKET_VOLUME, IO.IN);
    }

    protected NotifiableItemStackHandler createImportItemHandler(@SuppressWarnings("unused") Object... args) {
        return new NotifiableItemStackHandler(this, getRecipeType().getMaxInputs(ItemRecipeCapability.CAP), IO.IN);
    }

    protected NotifiableItemStackHandler createExportItemHandler(@SuppressWarnings("unused") Object... args) {
        return new NotifiableItemStackHandler(this, getRecipeType().getMaxOutputs(ItemRecipeCapability.CAP), IO.OUT);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Simulate an EU machine via a SteamEnergyHandler
        this.addHandlerList(RecipeHandlerList.of(IO.IN, new SteamEnergyRecipeHandler(steamTank, getConversionRate())));
    }

    @Override
    public void onMachineRemoved() {
        clearInventory(importItems.storage);
        clearInventory(exportItems.storage);
    }

    //////////////////////////////////////
    // ****** Venting Logic ******//
    //////////////////////////////////////

    @Override
    public float getVentingDamage() {
        return isHighPressure() ? 12F : 6F;
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public @NotNull Direction getVentingDirection() {
        return getOutputFacing();
    }

    public void updateModelVentDirection() {
        MachineRenderState renderState = getRenderState();
        if (renderState.hasProperty(GTMachineModelProperties.VENT_DIRECTION)) {
            Direction upwardsDir = getUpwardsFacing();
            // the up facing is already rotated if extended facing is enabled for the machine
            if (getFrontFacing() == Direction.UP && !allowExtendedFacing()) {
                upwardsDir = upwardsDir.getOpposite();
            }
            var relative = RelativeDirection.findRelativeOf(getFrontFacing(), getVentingDirection(), upwardsDir);
            setRenderState(renderState.setValue(GTMachineModelProperties.VENT_DIRECTION, relative));
        }
    }

    @Override
    public void setOutputFacing(@NotNull Direction outputFacing) {
        var oldFacing = getOutputFacing();
        super.setOutputFacing(outputFacing);
        if (getOutputFacing() != oldFacing) {
            updateModelVentDirection();
        }
    }

    @Override
    public void setFrontFacing(Direction facing) {
        var oldFacing = getFrontFacing();
        super.setFrontFacing(facing);
        if (getFrontFacing() != oldFacing) {
            updateModelVentDirection();
        }
    }

    @Override
    public void setUpwardsFacing(@NotNull Direction upwardsFacing) {
        var oldFacing = getUpwardsFacing();
        super.setUpwardsFacing(upwardsFacing);
        if (getUpwardsFacing() != oldFacing) {
            updateModelVentDirection();
        }
    }

    @Override
    public void markVentingComplete() {
        this.needsVenting = false;
    }

    public double getConversionRate() {
        return isHighPressure() ? 2.0 : 1.0;
    }

    //////////////////////////////////////
    // ****** Recipe Logic ******//
    //////////////////////////////////////

    /**
     * Recipe Modifier for <b>Simple Steam Machines</b> - can be used as a valid {@link RecipeModifier}
     * <p>
     * Recipe is rejected if tier is greater than LV or if machine cannot vent.<br>
     * Duration is multiplied by {@code 2} if the machine is low pressure
     * </p>
     *
     * @param machine a {@link SimpleSteamMachine}
     * @param recipe  recipe
     * @return A {@link ModifierFunction} for the given Steam Machine
     */
    public static ModifierFunction recipeModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof SimpleSteamMachine steamMachine)) {
            return RecipeModifier.nullWrongType(SimpleSteamMachine.class, machine);
        }
        if (RecipeHelper.getRecipeEUtTier(recipe) > GTValues.LV || !steamMachine.checkVenting()) {
            return ModifierFunction.NULL;
        }

        var builder = ModifierFunction.builder().conditions(VentCondition.INSTANCE);
        if (!steamMachine.isHighPressure) builder.durationMultiplier(2);
        return builder.build();
    }

    @Override
    public void afterWorking() {
        super.afterWorking();
        needsVenting = true;
        checkVenting();
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////

    @Override
    public ModularUI createUI(Player entityPlayer) {
        var storages = Tables.newCustomTable(new EnumMap<>(IO.class), LinkedHashMap<RecipeCapability<?>, Object>::new);
        storages.put(IO.IN, ItemRecipeCapability.CAP, importItems.storage);
        storages.put(IO.OUT, ItemRecipeCapability.CAP, exportItems.storage);

        var group = getRecipeType().getRecipeUI().createUITemplate(recipeLogic::getProgressPercent,
                storages,
                new CompoundTag(),
                Collections.emptyList(),
                true,
                isHighPressure);
        var title = UITemplate.title(Component.translatable(getBlockState().getBlock().getDescriptionId()), 5);

        var indicatorLayer = new UIElement();
        indicatorLayer.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(0).right(0).top(0).bottom(0)
                .alignItems(AlignItems.CENTER).justifyContent(AlignContent.CENTER));
        indicatorLayer.setAllowHitTest(false);
        var indicator = new PredicatedImage(GuiTextures.INDICATOR_NO_STEAM.get(isHighPressure),
                recipeLogic::isWaiting);
        indicator.layout(layout -> layout.width(18).height(18));
        indicatorLayer.addChild(indicator);

        var recipeArea = new UIElement();
        recipeArea.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(0).right(0).top(32)
                .alignItems(AlignItems.CENTER).justifyContent(AlignContent.CENTER));
        recipeArea.addChildren(group, indicatorLayer);

        var machineArea = new UIElement();
        machineArea.layout(layout -> layout.widthPercent(100).height(84));
        machineArea.addChildren(title, recipeArea);

        var root = UITemplate.panel();
        root.style(style -> style.background(GuiTextures.BACKGROUND_STEAM.get(isHighPressure)));
        root.layout(layout -> layout.width(176).paddingAll(0).paddingBottom(5).gapAll(0));
        root.addChildren(machineArea, UITemplate.bindPlayerInventory(GuiTextures.SLOT_STEAM.get(isHighPressure)));
        return UITemplate.modularUI(root, entityPlayer);
    }
}
