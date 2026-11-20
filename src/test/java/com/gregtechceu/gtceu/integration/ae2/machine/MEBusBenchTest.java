package com.gregtechceu.gtceu.integration.ae2.machine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.machines.GTAEMachines;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;
import com.gregtechceu.gtceu.gametest.util.TestUtils;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemList;
import com.gregtechceu.gtceu.integration.ae2.utils.KeyStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@PrefixGameTestTemplate(false)
@GameTestHolder(GTCEu.MOD_ID)
public class MEBusBenchTest {

    private static final String BATCH = "MEBusBench";
    private static final int INPUT_CALLS = 20000;
    private static final int OUTPUT_CALLS = 4000;
    private static final List<Item> POOL = List.of(Items.PAPER, Items.SUGAR, Items.STICK, Items.COAL,
            Items.IRON_INGOT, Items.GOLD_INGOT, Items.WHITE_WOOL, Items.RED_WOOL, Items.OAK_PLANKS, Items.OAK_LOG,
            Items.COBBLESTONE, Items.DIRT, Items.SAND, Items.GRAVEL, Items.CLAY_BALL, Items.BRICK);

    private static GTRecipe RECIPE;

    @BeforeBatch(batch = BATCH)
    public static void prepare(ServerLevel level) {
        var type = TestUtils.createRecipeType("me_bus_bench", GTRecipeTypes.LARGE_CHEMICAL_RECIPES);

        RECIPE = type.recipeBuilder(GTCEu.id("me_bus_bench")).id(GTCEu.id("me_bus_bench"))
                .inputItems(new ItemStack(Items.PAPER)).outputItems(new ItemStack(Items.DIRT))
                .EUt(GTValues.V[GTValues.HV]).duration(1).buildRawRecipe();
    }

    private static Map<RecipeCapability<?>, List<Object>> contents(Ingredient ingredient) {
        Map<RecipeCapability<?>, List<Object>> map = new HashMap<>();

        map.put(ItemRecipeCapability.CAP, new ArrayList<>(List.of(ingredient)));

        return map;
    }

    private static double microsPerCall(long nanos, int calls) {
        return nanos * 1e-3 / calls;
    }

    @GameTest(template = "patternbuffertest", batch = BATCH, setupTicks = 20, timeoutTicks = 400)
    public static void meInputBusBench(GameTestHelper helper) {
        var bus = (ItemBusPartMachine) TestUtils.setMachine(helper, new BlockPos(2, 2, 0),
                GTAEMachines.ITEM_IMPORT_BUS_ME);
        var list = (ExportOnlyAEItemList) bus.getInventory();
        var handlerList = bus.getRecipeHandlers().get(0);
        var random = new Random(3);

        for (int slot = 0; slot < list.getInventory().length; slot++) {
            var item = POOL.get(slot % POOL.size());

            list.getInventory()[slot].setConfig(new GenericStack(AEItemKey.of(item), 64));
            list.getInventory()[slot].setStock(random.nextInt(5) == 0 ? null :
                    new GenericStack(AEItemKey.of(item), 1 + random.nextInt(200)));
        }

        list.getInventory()[15].setStock(new GenericStack(AEItemKey.of(POOL.get(15)), 100));

        var hit = SizedIngredient.create(new ItemStack(POOL.get(15), 1));
        var miss = SizedIngredient.create(new ItemStack(Items.DIAMOND, 1));

        for (int i = 0; i < 2000; i++) handlerList.handleRecipe(IO.IN, RECIPE, contents(hit), true);

        long start = System.nanoTime();

        for (int i = 0; i < INPUT_CALLS; i++) handlerList.handleRecipe(IO.IN, RECIPE, contents(hit), true);

        long hitNanos = System.nanoTime() - start;

        start = System.nanoTime();

        for (int i = 0; i < INPUT_CALLS; i++) handlerList.handleRecipe(IO.IN, RECIPE, contents(miss), true);

        long missNanos = System.nanoTime() - start;

        GTCEu.LOGGER.info("[MEBusBench] input bus, {} simulated matches: last-slot hit={}us/call miss={}us/call",
                INPUT_CALLS, String.format("%.2f", microsPerCall(hitNanos, INPUT_CALLS)),
                String.format("%.2f", microsPerCall(missNanos, INPUT_CALLS)));

        helper.succeed();
    }

    @GameTest(template = "patternbuffertest", batch = BATCH, setupTicks = 20, timeoutTicks = 400)
    public static void meOutputBusBench(GameTestHelper helper) throws ReflectiveOperationException {
        var bus = (MEOutputBusPartMachine) TestUtils.setMachine(helper, new BlockPos(0, 1, 0),
                GTAEMachines.ITEM_EXPORT_BUS_ME);
        var field = MEOutputBusPartMachine.class.getDeclaredField("internalBuffer");

        field.setAccessible(true);

        var buffer = (KeyStorage) field.get(bus);
        var handlerList = bus.getRecipeHandlers().get(0);
        var output = SizedIngredient.create(new ItemStack(Items.DIRT, 16));

        for (int i = 0; i < 500; i++) handlerList.handleRecipe(IO.OUT, RECIPE, contents(output), true);

        long start = System.nanoTime();

        for (int i = 0; i < OUTPUT_CALLS; i++) handlerList.handleRecipe(IO.OUT, RECIPE, contents(output), true);

        long simulateNanos = System.nanoTime() - start;

        start = System.nanoTime();

        for (int i = 0; i < OUTPUT_CALLS; i++) handlerList.handleRecipe(IO.OUT, RECIPE, contents(output), false);

        long realNanos = System.nanoTime() - start;

        GTCEu.LOGGER.info("[MEBusBench] output bus, {} calls: simulate={}us/call real={}us/call", OUTPUT_CALLS,
                String.format("%.2f", microsPerCall(simulateNanos, OUTPUT_CALLS)),
                String.format("%.2f", microsPerCall(realNanos, OUTPUT_CALLS)));

        long stored = buffer.storage.getLong(AEItemKey.of(Items.DIRT));

        helper.assertTrue(stored == 16L * OUTPUT_CALLS, "Buffer holds " + stored + " dirt, expected " +
                16L * OUTPUT_CALLS);
        helper.succeed();
    }
}
