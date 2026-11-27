package com.gregtechceu.gtceu.api.pattern;

import com.gregtechceu.gtceu.api.block.IMachineBlock;
import com.gregtechceu.gtceu.api.data.RotationState;

import com.lowdragmc.lowdraglib2.utils.data.BlockInfo;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class MultiblockShapeInfo {

    private final BlockInfo[][][] blocks; // [z][y][x]

    public MultiblockShapeInfo(BlockInfo[][][] blocks) {
        this.blocks = blocks;
    }

    public BlockInfo[][][] getBlocks() {
        return blocks;
    }

    public static ShapeInfoBuilder builder() {
        return new ShapeInfoBuilder();
    }

    public static class ShapeInfoBuilder {

        private List<String[]> shape = new ArrayList<>();
        private Map<Character, BlockInfo> symbolMap = new LinkedHashMap<>();

        public ShapeInfoBuilder aisle(String... data) {
            this.shape.add(data);
            return this;
        }

        public ShapeInfoBuilder where(char symbol, BlockInfo value) {
            this.symbolMap.put(symbol, value);
            return this;
        }

        public ShapeInfoBuilder shallowCopy() {
            var builder = new ShapeInfoBuilder();
            builder.shape = new ArrayList<>(this.shape);
            builder.symbolMap = new HashMap<>(this.symbolMap);
            return builder;
        }

        public ShapeInfoBuilder where(char symbol, BlockState blockState) {
            return where(symbol, BlockInfo.fromBlockState(blockState));
        }

        public ShapeInfoBuilder where(char symbol, Supplier<? extends Block> block) {
            return where(symbol, block.get());
        }

        public ShapeInfoBuilder where(char symbol, Block block) {
            return where(symbol, block.defaultBlockState());
        }

        public ShapeInfoBuilder where(char symbol, Supplier<? extends IMachineBlock> machine, Direction facing) {
            return where(symbol, machine.get(), facing);
        }

        public ShapeInfoBuilder where(char symbol, IMachineBlock machine, Direction facing) {
            return where(symbol, machine.getRotationState() == RotationState.NONE ?
                    machine.self().defaultBlockState() :
                    machine.self().defaultBlockState().setValue(machine.getRotationState().property, facing));
        }

        private BlockInfo[][][] bake() {
            var infos = new BlockInfo[shape.get(0)[0].length()][shape.get(0).length][shape.size()];
            for (int z = 0; z < shape.size(); z++) {
                String[] aisleEntry = shape.get(z);
                for (int y = 0; y < shape.get(0).length; y++) {
                    String columnEntry = aisleEntry[y];
                    for (int x = 0; x < columnEntry.length(); x++) {
                        infos[x][y][z] = symbolMap.getOrDefault(columnEntry.charAt(x), BlockInfo.EMPTY);
                    }
                }
            }
            return infos;
        }

        public MultiblockShapeInfo build() {
            return new MultiblockShapeInfo(bake());
        }
    }
}
