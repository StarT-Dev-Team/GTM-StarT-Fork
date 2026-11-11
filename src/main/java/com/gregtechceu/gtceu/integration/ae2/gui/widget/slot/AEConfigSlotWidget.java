package com.gregtechceu.gtceu.integration.ae2.gui.widget.slot;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.gui.misc.IGhostFluidTarget;
import com.gregtechceu.gtceu.api.gui.misc.IGhostItemTarget;
import com.gregtechceu.gtceu.api.gui.widget.TankWidget;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.AEAmountDialog;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.ConfigWidget;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEFluidSlot;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAESlot;
import com.gregtechceu.gtceu.integration.ae2.slot.IConfigurableSlot;
import com.gregtechceu.gtceu.integration.ae2.utils.AEUtil;
import com.gregtechceu.gtceu.utils.GTMath;

import com.lowdragmc.lowdraglib.gui.ingredient.IRecipeIngredientSlot;
import com.lowdragmc.lowdraglib.gui.ingredient.Target;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.jei.JEIPlugin;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.emi.api.forge.ForgeEmiStack;
import dev.emi.emi.api.stack.EmiStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.gregtechceu.gtceu.api.gui.widget.PhantomFluidWidget.drainFrom;
import static com.lowdragmc.lowdraglib.gui.util.DrawerHelper.drawGradientRect;

public class AEConfigSlotWidget extends Widget
                                implements IGhostItemTarget, IGhostFluidTarget, IRecipeIngredientSlot {

    protected static final int REMOVE_ID = 1000;
    protected static final int UPDATE_ID = 1001;
    protected static final int AMOUNT_CHANGE_ID = 1002;
    protected static final int PICK_UP_ID = 1003;

    private static final byte KIND_ITEM = 0;
    private static final byte KIND_FLUID = 1;

    protected final ConfigWidget parentWidget;
    protected final int index;

    public AEConfigSlotWidget(int x, int y, ConfigWidget parentWidget, int index) {
        super(new Position(x, y), new Size(18, 18 * 2));

        this.parentWidget = parentWidget;
        this.index = index;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void drawInForeground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInForeground(graphics, mouseX, mouseY, partialTicks);

        IConfigurableSlot slot = this.parentWidget.getDisplay(this.index);

        if (slot.getConfig() == null) {
            if (mouseOverConfig(mouseX, mouseY)) {
                List<Component> hoverStringList = new ArrayList<>();

                hoverStringList.add(Component.translatable("gtceu.gui.config_slot"));

                if (this.parentWidget.isAutoPull()) {
                    hoverStringList.add(Component.translatable("gtceu.gui.config_slot.auto_pull_managed"));
                } else {
                    if (!this.parentWidget.isStocking()) {
                        hoverStringList.add(Component.translatable("gtceu.gui.config_slot.set"));
                        hoverStringList.add(Component.translatable("gtceu.gui.config_slot.scroll"));
                    } else {
                        hoverStringList.add(Component.translatable("gtceu.gui.config_slot.set_only"));
                    }

                    hoverStringList.add(Component.translatable("gtceu.gui.config_slot.remove"));
                }

                graphics.renderTooltip(Minecraft.getInstance().font, hoverStringList, Optional.empty(), mouseX, mouseY);
            }
        } else {
            GenericStack stack = null;

            if (mouseOverConfig(mouseX, mouseY)) {
                stack = slot.getConfig();
            } else if (mouseOverStock(mouseX, mouseY)) {
                stack = slot.getStock();
            }

            if (stack != null) {
                graphics.renderTooltip(Minecraft.getInstance().font, GenericStack.wrapInItemStack(stack), mouseX,
                        mouseY);
            }
        }
    }

    protected boolean mouseOverConfig(double mouseX, double mouseY) {
        Position position = getPosition();
        return isMouseOver(position.x, position.y, 18, 18, mouseX, mouseY);
    }

    protected boolean mouseOverStock(double mouseX, double mouseY) {
        Position position = getPosition();
        return isMouseOver(position.x, position.y + 18, 18, 18, mouseX, mouseY);
    }

    @OnlyIn(Dist.CLIENT)
    public static void drawSelectionOverlay(GuiGraphics graphics, int x, int y, int width, int height) {
        RenderSystem.disableDepthTest();
        RenderSystem.colorMask(true, true, true, false);
        drawGradientRect(graphics, x, y, width, height, -2130706433, -2130706433);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
    }

    protected boolean isStackValidForSlot(GenericStack stack) {
        if (stack == null || stack.amount() < 0) return true;
        if (!this.parentWidget.acceptsKey(stack.what())) return false;
        if (!this.parentWidget.isStocking()) return true;

        return !this.parentWidget.hasStackInConfig(stack);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseOverConfig(mouseX, mouseY)) {
            if (this.parentWidget.isAutoPull()) return false;

            if (button == 1) {
                writeClientAction(REMOVE_ID, buf -> {});
            } else if (button == 0) {
                ItemStack held = this.gui.getModularUIContainer().getCarried();

                if (!held.isEmpty()) {
                    FluidStack contained = getContainedFluid(held);

                    if (!contained.isEmpty()) {
                        acceptFluid(contained);
                    } else {
                        acceptItem(held);
                    }
                } else {
                    GenericStack config = this.parentWidget.getDisplay(this.index).getConfig();

                    if (config != null) {
                        new AEAmountDialog(this.gui.mainGroup, config.what(), config.amount(), this::sendAmount);
                    }
                }
            }

            return true;
        }

        if (mouseOverStock(mouseX, mouseY) && button == 0) {
            if (this.parentWidget.isStocking()) return false;

            if (this.parentWidget.getDisplay(this.index).getStock() != null) {
                writeClientAction(PICK_UP_ID, buf -> buf.writeBoolean(isShiftDown()));
            }

            return true;
        }

        return false;
    }

    private FluidStack getContainedFluid(ItemStack held) {
        if (!this.parentWidget.acceptsFluids()) return FluidStack.EMPTY;
        if (this.parentWidget.acceptsItems() && isShiftDown()) return FluidStack.EMPTY;

        return FluidUtil.getFluidContained(held).orElse(FluidStack.EMPTY);
    }

    @Override
    public void handleClientAction(int id, FriendlyByteBuf buffer) {
        super.handleClientAction(id, buffer);

        if (this.parentWidget.isAutoPull()) return;

        IConfigurableSlot slot = this.parentWidget.getConfig(this.index);

        if (id == REMOVE_ID) {
            slot.setConfig(null);

            writeUpdateInfo(REMOVE_ID, buf -> {});
        }

        if (id == UPDATE_ID) {
            byte kind = buffer.readByte();

            GenericStack stack;

            if (kind == KIND_FLUID) {
                FluidStack fluid = FluidStack.readFromPacket(buffer);

                stack = AEUtil.fromFluidStack(fluid);

                if (!isStackValidForSlot(stack)) return;

                slot.setConfig(stack);

                if (!fluid.isEmpty()) {
                    writeUpdateInfo(UPDATE_ID, buf -> {
                        buf.writeByte(KIND_FLUID);

                        fluid.writeToPacket(buf);
                    });
                }
            } else {
                ItemStack item = buffer.readItem();

                stack = GenericStack.fromItemStack(item);

                if (!isStackValidForSlot(stack)) return;

                slot.setConfig(stack);

                if (!item.isEmpty()) {
                    writeUpdateInfo(UPDATE_ID, buf -> {
                        buf.writeByte(KIND_ITEM);
                        buf.writeItem(item);
                    });
                }
            }
        }

        if (id == AMOUNT_CHANGE_ID) {
            long amount = buffer.readVarLong();

            if (slot.getConfig() != null && amount >= 1 && amount <= Integer.MAX_VALUE) {
                slot.setConfig(new GenericStack(slot.getConfig().what(), amount));

                writeUpdateInfo(AMOUNT_CHANGE_ID, buf -> buf.writeVarLong(amount));
            }
        }

        if (id == PICK_UP_ID) {
            boolean shift = buffer.readBoolean();
            GenericStack stock = slot.getStock();

            if (stock == null) return;

            if (stock.what() instanceof AEItemKey key) {
                if (!this.gui.getModularUIContainer().getCarried().isEmpty()) return;

                pickUpItem(slot, key, stock);

                writeUpdateInfo(PICK_UP_ID, buf -> buf.writeByte(KIND_ITEM));
            } else if (stock.what() instanceof AEFluidKey) {
                int clickResult = tryClickContainer(shift);

                if (clickResult >= 0) {
                    writeUpdateInfo(PICK_UP_ID, buf -> {
                        buf.writeByte(KIND_FLUID);
                        buf.writeVarInt(clickResult);
                    });
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void readUpdateInfo(int id, FriendlyByteBuf buffer) {
        super.readUpdateInfo(id, buffer);

        IConfigurableSlot slot = this.parentWidget.getDisplay(this.index);

        if (id == REMOVE_ID) {
            slot.setConfig(null);
        }

        if (id == UPDATE_ID) {
            if (buffer.readByte() == KIND_FLUID) {
                FluidStack fluid = FluidStack.readFromPacket(buffer);

                slot.setConfig(new GenericStack(AEFluidKey.of(fluid.getFluid(), fluid.getTag()), fluid.getAmount()));
            } else {
                ItemStack item = buffer.readItem();

                slot.setConfig(new GenericStack(AEItemKey.of(item.getItem(), item.getTag()), item.getCount()));
            }
        }

        if (id == AMOUNT_CHANGE_ID) {
            long amount = buffer.readVarLong();

            if (slot.getConfig() != null) {
                slot.setConfig(new GenericStack(slot.getConfig().what(), amount));
            }
        }

        if (id == PICK_UP_ID) {
            byte kind = buffer.readByte();
            GenericStack stock = slot.getStock();

            if (stock == null) return;

            if (kind == KIND_ITEM && stock.what() instanceof AEItemKey key) {
                pickUpItem(slot, key, stock);
            } else if (kind == KIND_FLUID && stock.what() instanceof AEFluidKey key) {
                ItemStack carried = this.gui.getModularUIContainer().getCarried();

                carried.setCount(buffer.readVarInt());

                this.gui.getModularUIContainer().setCarried(carried);

                int taken = GTMath.saturatedCast(stock.amount());
                GenericStack remaining = ExportOnlyAESlot.copy(stock, Math.max(0, stock.amount() - taken));

                slot.setStock(remaining.amount() == 0 ? null : remaining);
            }
        }
    }

    private void pickUpItem(IConfigurableSlot slot, AEItemKey key, GenericStack stock) {
        ItemStack stack = key.toStack(Math.min(GTMath.saturatedCast(stock.amount()), key.getItem().getMaxStackSize()));

        this.gui.getModularUIContainer().setCarried(stack);

        GenericStack remaining = ExportOnlyAESlot.copy(stock, Math.max(0, stock.amount() - stack.getCount()));

        slot.setStock(remaining.amount() == 0 ? null : remaining);
    }

    private int tryClickContainer(boolean isShiftKeyDown) {
        ExportOnlyAEFluidSlot fluidTank = this.parentWidget
                .getConfig(this.index) instanceof ExportOnlyAEFluidSlot fluid ? fluid : null;

        if (fluidTank == null) return -1;

        Player player = this.gui.entityPlayer;
        ItemStack currentStack = this.gui.getModularUIContainer().getCarried();
        var handler = FluidUtil.getFluidHandler(currentStack).resolve().orElse(null);

        if (handler == null) return -1;

        int maxAttempts = isShiftKeyDown ? currentStack.getCount() : 1;

        if (fluidTank.getFluidAmount() > 0) {
            boolean performedFill = false;
            FluidStack initialFluid = fluidTank.getFluid();

            for (int i = 0; i < maxAttempts; i++) {
                FluidActionResult result = FluidUtil.tryFillContainer(currentStack, fluidTank, Integer.MAX_VALUE, null,
                        false);

                if (!result.isSuccess()) break;

                ItemStack remainingStack = FluidUtil
                        .tryFillContainer(currentStack, fluidTank, Integer.MAX_VALUE, null, true).getResult();

                currentStack.shrink(1);

                performedFill = true;

                if (!remainingStack.isEmpty() && !player.addItem(remainingStack)) {
                    Block.popResource(player.level(), player.getOnPos(), remainingStack);

                    break;
                }
            }

            if (performedFill) {
                SoundEvent soundevent = initialFluid.getFluid().getFluidType().getSound(initialFluid,
                        SoundActions.BUCKET_FILL);

                if (soundevent != null) {
                    player.level().playSound(null, player.position().x, player.position().y + 0.5, player.position().z,
                            soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
                }

                this.gui.getModularUIContainer().setCarried(currentStack);

                return currentStack.getCount();
            }
        }

        return -1;
    }

    @OnlyIn(Dist.CLIENT)
    private void sendAmount(long amount) {
        writeClientAction(AMOUNT_CHANGE_ID, buf -> buf.writeVarLong(amount));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Rect2i getRectangleBox() {
        Rect2i rectangle = toRectangleBox();

        rectangle.setHeight(rectangle.getHeight() / 2);

        return rectangle;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void acceptItem(ItemStack itemStack) {
        if (!this.parentWidget.acceptsItems()) return;

        writeClientAction(UPDATE_ID, buf -> {
            buf.writeByte(KIND_ITEM);
            buf.writeItem(itemStack);
        });
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void acceptFluid(FluidStack fluidStack) {
        if (!this.parentWidget.acceptsFluids()) return;

        if (fluidStack.getRawFluid() != Fluids.EMPTY && fluidStack.getAmount() <= 0L) {
            fluidStack.setAmount(1000);
        }

        if (!fluidStack.isEmpty()) {
            writeClientAction(UPDATE_ID, buf -> {
                buf.writeByte(KIND_FLUID);
                fluidStack.writeToPacket(buf);
            });
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Object convertIngredient(Object ingredient) {
        return ingredient;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public List<Target> getPhantomTargets(Object ingredient) {
        List<Target> targets = new ArrayList<>();
        Object itemIngredient = IGhostItemTarget.super.convertIngredient(ingredient);

        if (this.parentWidget.acceptsItems() && itemIngredient instanceof ItemStack stack && !stack.isEmpty()) {
            targets.add(createTarget(stack, true));
        }

        Object fluidIngredient = IGhostFluidTarget.super.convertIngredient(ingredient);
        FluidStack fluid = fluidIngredient instanceof FluidStack fluidStack ? fluidStack : drainFrom(fluidIngredient);

        if (this.parentWidget.acceptsFluids() && fluid != null && fluid.getRawFluid() != Fluids.EMPTY) {
            targets.add(createTarget(fluid, false));
        }

        return targets;
    }

    @OnlyIn(Dist.CLIENT)
    private Target createTarget(Object ingredient, boolean item) {
        final Rect2i area = getRectangleBox();

        return new Target() {

            @NotNull
            @Override
            public Rect2i getArea() {
                return area;
            }

            @Override
            public void accept(@NotNull Object ignored) {
                if (item) acceptItem((ItemStack) ingredient);
                else acceptFluid((FluidStack) ingredient);
            }
        };
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
        if (this.parentWidget.isStocking()) return false;

        IConfigurableSlot slot = this.parentWidget.getDisplay(this.index);

        if (slot.getConfig() == null || wheelDelta == 0 || !getRectangleBox().contains((int) mouseX, (int) mouseY)) {
            return false;
        }

        long current = slot.getConfig().amount();
        long amount;

        if (isCtrlDown()) {
            amount = wheelDelta > 0 ? current * 2L : current / 2L;
        } else {
            amount = wheelDelta > 0 ? current + 1L : current - 1L;
        }

        if (amount > 0 && amount < Integer.MAX_VALUE + 1L) {
            writeClientAction(AMOUNT_CHANGE_ID, buf -> buf.writeVarLong(amount));

            return true;
        }

        return false;
    }

    @Nullable
    private Object getXEIStack(@Nullable GenericStack stack, Position position) {
        if (stack == null) return null;

        if (stack.what() instanceof AEItemKey key) {
            ItemStack itemStack = key.toStack(GTMath.saturatedCast(stack.amount()));

            if (itemStack.isEmpty()) return null;

            return switch (GTCEu.Mods.loadedXEI()) {
                case JEI -> JEIPlugin.getItemIngredient(itemStack, position.x, position.y, 18, 18);
                case REI -> EntryStacks.of(itemStack);
                case EMI -> EmiStack.of(itemStack);
                default -> itemStack;
            };
        }

        FluidStack fluidStack = AEUtil.toFluidStack(stack);

        if (fluidStack.isEmpty()) return null;

        return switch (GTCEu.Mods.loadedXEI()) {
            case JEI -> TankWidget.JEICallWrapper.getJEIFluidClickable(fluidStack, position, new Size(18, 18));
            case REI -> EntryStacks.of(TankWidget.REICallWrapper.toREIStack(fluidStack));
            case EMI -> ForgeEmiStack.of(fluidStack);
            default -> fluidStack;
        };
    }

    @Override
    public List<Object> getXEIIngredients() {
        IConfigurableSlot slot = this.parentWidget.getDisplay(this.index);
        List<Object> ingredients = new ArrayList<>();
        Position position = getPosition();
        Object config = getXEIStack(slot.getConfig(), position);
        Object stock = getXEIStack(slot.getStock(), new Position(position.x, position.y + 18));

        if (config != null) ingredients.add(config);
        if (stock != null) ingredients.add(stock);

        return ingredients;
    }

    @Nullable
    @Override
    public Object getXEIIngredientOverMouse(double mouseX, double mouseY) {
        IConfigurableSlot slot = this.parentWidget.getDisplay(this.index);
        Position position = getPosition();

        if (mouseOverConfig(mouseX, mouseY)) {
            return getXEIStack(slot.getConfig(), position);
        }

        if (mouseOverStock(mouseX, mouseY)) {
            return getXEIStack(slot.getStock(), new Position(position.x, position.y + 18));
        }

        return null;
    }
}
