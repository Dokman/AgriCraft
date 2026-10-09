package com.agricraft.agricraft.common.block;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/** Uses real block states and collision shapes without launching a game client. */
public class IrrigationTankBlockTest {
    private static boolean contains(VoxelShape shape, double x, double y, double z) {
        return shape.toAabbs().stream().anyMatch(box -> box.contains(x, y, z));
    }

    public static void main(String[] args) throws ReflectiveOperationException {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // Vanilla bootstrap freezes block registration. Reopen only this isolated test JVM's
        // registry so the real mod blocks can be constructed, then register and freeze them.
        var frozen = MappedRegistry.class.getDeclaredField("frozen");
        frozen.setAccessible(true);
        frozen.setBoolean(BuiltInRegistries.BLOCK, false);
        var holders = MappedRegistry.class.getDeclaredField("unregisteredIntrusiveHolders");
        holders.setAccessible(true);
        holders.set(BuiltInRegistries.BLOCK, new IdentityHashMap<>());
        IrrigationTankBlock tank = new IrrigationTankBlock();
        IrrigationBlock channel = new IrrigationBlock(IrrigationBlock.Kind.CHANNEL);
        IrrigationBlock sprinkler = new IrrigationBlock(IrrigationBlock.Kind.SPRINKLER);
        Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation("agricraft", "test_tank"), tank);
        Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation("agricraft", "test_channel"), channel);
        Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation("agricraft", "test_sprinkler"), sprinkler);
        BuiltInRegistries.BLOCK.freeze();
        // Minecraft initializes state caches after registration; reproduce that here for
        // these blocks added after bootstrap, including the legacy solidity predicate.
        for (IrrigationBlock block : new IrrigationBlock[]{tank, channel, sprinkler}) {
            block.getStateDefinition().getPossibleStates().forEach(BlockState::initCache);
        }
        // BucketItem.emptyContents destroys non-solid blocks when this predicate is true.
        // Check every joined/stacked geometry, including a completely empty interior shape.
        for (BlockState state : tank.getStateDefinition().getPossibleStates()) {
            assert !state.canBeReplaced(Fluids.WATER) : "A water bucket must never replace a tank: " + state;
            assert !state.canBeReplaced(Fluids.LAVA) : "Lava must never replace a tank";
            assert state.blocksMotion() : "Flowing world fluids must not destroy hollow tank members";
        }
        assert !channel.defaultBlockState().canBeReplaced(Fluids.WATER);
        assert !sprinkler.defaultBlockState().canBeReplaced(Fluids.WATER);
        assert channel.defaultBlockState().blocksMotion();
        assert sprinkler.defaultBlockState().blocksMotion();
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        LevelReader world = (LevelReader) Proxy.newProxyInstance(LevelReader.class.getClassLoader(), new Class[]{LevelReader.class}, (proxy, method, arguments) -> {
            if (method.getName().equals("getBlockState")) return blocks.getOrDefault((BlockPos) arguments[0], Blocks.AIR.defaultBlockState());
            throw new UnsupportedOperationException(method.getName());
        });
        BlockPos origin = BlockPos.ZERO;
        blocks.put(origin, tank.defaultBlockState());
        blocks.put(origin.east(), tank.defaultBlockState());
        BlockState connected = tank.connections(tank.defaultBlockState(), world, origin);
        assert connected.getValue(IrrigationBlock.EAST);
        assert !connected.getValue(IrrigationBlock.NORTH);
        VoxelShape shape = tank.getShape(connected, world, origin, CollisionContext.empty());
        assert !contains(shape, .99, .5, .5) : "The shared east wall must disappear";
        assert contains(shape, .01, .5, .5) : "The exterior west wall must remain";
        assert contains(shape, .5, .05, .5) : "The bottom must remain";

        // The middle tank in the reported 3x3 arrangement must have no internal walls.
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) blocks.put(new BlockPos(x, 0, z), tank.defaultBlockState());
        connected = tank.connections(tank.defaultBlockState(), world, origin);
        shape = tank.getShape(connected, world, origin, CollisionContext.empty());
        assert !contains(shape, .01, .5, .5) && !contains(shape, .99, .5, .5);
        assert !contains(shape, .5, .5, .01) && !contains(shape, .5, .5, .99);
        assert contains(shape, .5, .05, .5);

        // Removing an adjacent tank restores its wall, including for previously saved states.
        blocks.remove(origin.east());
        BlockState repaired = tank.connections(connected.setValue(IrrigationBlock.WATER, 3), world, origin);
        assert !repaired.getValue(IrrigationBlock.EAST);
        assert repaired.getValue(IrrigationBlock.WATER) == 3;
        assert contains(tank.getShape(repaired, world, origin, CollisionContext.empty()), .99, .5, .5);

        // Channels do not count as tanks, and vertical joins remove the intermediate floor.
        blocks.put(origin.east(), channel.defaultBlockState());
        assert !tank.connections(connected, world, origin).getValue(IrrigationBlock.EAST);
        blocks.put(origin.below(), tank.defaultBlockState());
        connected = tank.connections(repaired, world, origin);
        assert connected.getValue(IrrigationTankBlock.DOWN);
        assert !contains(tank.getShape(connected, world, origin, CollisionContext.empty()), .5, .05, .5);
        for (int mask = 0; mask < 16; mask++) {
            boolean north = (mask & 1) != 0, east = (mask & 2) != 0, south = (mask & 4) != 0, west = (mask & 8) != 0;
            BlockState channelState = channel.defaultBlockState().setValue(IrrigationBlock.NORTH, north)
                    .setValue(IrrigationBlock.EAST, east).setValue(IrrigationBlock.SOUTH, south).setValue(IrrigationBlock.WEST, west);
            shape = channel.getShape(channelState, world, origin, CollisionContext.empty());
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                boolean floor = (x >= 5 && x < 11 && z >= 5 && z < 11) || (north && x >= 5 && x < 11 && z < 5)
                        || (east && x >= 11 && z >= 5 && z < 11) || (south && x >= 5 && x < 11 && z >= 11)
                        || (west && x < 5 && z >= 5 && z < 11);
                boolean opening = (x >= 6 && x < 10 && z >= 6 && z < 10) || (north && x >= 6 && x < 10 && z < 6)
                        || (east && x >= 10 && z >= 6 && z < 10) || (south && x >= 6 && x < 10 && z >= 10)
                        || (west && x < 6 && z >= 6 && z < 10);
                assert contains(shape, (x+.5)/16, 8.0/16, (z+.5)/16) == (floor && !opening) : "Channel collision must leave the connected water path open";
            }
        }
        shape = sprinkler.getShape(sprinkler.defaultBlockState(), world, origin, CollisionContext.empty());
        assert contains(shape, .5, 20.5/16, .5) : "The sprinkler attachment must reach the channel floor at y=21";
        // Water jets must start outside the sprinkler itself at every rotation angle.
        // Vanilla falling-water particles have a 0.01-block collision box.
        for (int angle = 0; angle < 360; angle++) {
            double x = .5 + .32 * Math.cos(Math.toRadians(angle));
            double z = .5 + .32 * Math.sin(Math.toRadians(angle));
            var drop = new net.minecraft.world.phys.AABB(x - .005, .30, z - .005,
                    x + .005, .31, z + .005);
            assert shape.toAabbs().stream().noneMatch(box -> box.intersects(drop))
                    : "The sprinkler must not obstruct its own water jet at " + angle;
        }
        System.out.println("Connected tanks: adjacency, 3x3 interior walls, removal, channels and stacked floors passed.");
    }
}
