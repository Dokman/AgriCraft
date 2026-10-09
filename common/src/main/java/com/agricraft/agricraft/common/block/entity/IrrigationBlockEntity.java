package com.agricraft.agricraft.common.block.entity;

import com.agricraft.agricraft.api.config.IrrigationConfig;
import com.agricraft.agricraft.common.block.CropBlock;
import com.agricraft.agricraft.common.block.IrrigationBlock;
import com.agricraft.agricraft.common.block.IrrigationTankBlock;
import com.agricraft.agricraft.common.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class IrrigationBlockEntity extends BlockEntity implements IrrigationReservoir.Member {
    private int water;
    private int column;
    private float sprinklerAngle;
    private float previousSprinklerAngle;

    public void tickSprinklerAnimation() {
        previousSprinklerAngle = sprinklerAngle;
        if (getBlockState().getValue(IrrigationBlock.ACTIVE)) sprinklerAngle += 9;
        // Keep interpolation continuous across the end of a revolution.
        if (sprinklerAngle >= 360) {
            sprinklerAngle -= 360;
            previousSprinklerAngle -= 360;
        }
    }

    public float getSprinklerAngle(float partialTick) {
        return previousSprinklerAngle + (sprinklerAngle - previousSprinklerAngle) * partialTick;
    }
    private static final Map<Level, TankLevels> TANK_LEVELS = new WeakHashMap<>();

    private static class TankLevels {
        long tick = Long.MIN_VALUE;
        final Map<BlockPos, Integer> levels = new HashMap<>();
    }

    private int tankVisibleLevel(Level level, BlockPos pos) {
        TankLevels cache = TANK_LEVELS.computeIfAbsent(level, unused -> new TankLevels());
        if (cache.tick != level.getGameTime()) {
            cache.tick = level.getGameTime();
            cache.levels.clear();
        }
        Integer previous = cache.levels.get(pos);
        if (previous != null) return previous;
        Map<BlockPos, IrrigationBlockEntity> tanks = new HashMap<>();
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        tanks.put(pos, this);
        pending.add(pos);
        while (!pending.isEmpty()) {
            BlockPos current = pending.removeFirst();
            IrrigationBlockEntity tank = tanks.get(current);
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                if (!tanks.containsKey(neighbor) && level.hasChunkAt(neighbor)
                        && level.getBlockEntity(neighbor) instanceof IrrigationBlockEntity other
                        && other.block() instanceof IrrigationTankBlock) {
                    tanks.put(neighbor, other);
                    pending.add(neighbor);
                }
            }
        }
        IrrigationReservoir.balance(new ArrayList<>(tanks.values()));
        Map<Integer, long[]> layers = new HashMap<>();
        for (var entry : tanks.entrySet()) {
            long[] layer = layers.computeIfAbsent(entry.getKey().getY(), unused -> new long[2]);
            layer[0] += entry.getValue().water;
            layer[1] += entry.getValue().getCapacity();
        }
        for (BlockPos tank : tanks.keySet()) {
            long[] layer = layers.get(tank.getY());
            cache.levels.put(tank, IrrigationFlow.visibleLevel(layer[0], layer[1], 16));
        }
        return cache.levels.get(pos);
    }

    public IrrigationBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntityTypes.IRRIGATION.get(), pos, state); }
    private IrrigationBlock block() { return (IrrigationBlock) getBlockState().getBlock(); }
    public int getCapacity() { return block().kind == IrrigationBlock.Kind.TANK ? 16000 : block().isChannel() ? 500 : 0; }
    public int getWater() { return water; }
    public int getTankY() { return worldPosition.getY(); }

    public static void invalidateTankLevels(Level level) {
        if (!level.isClientSide) TANK_LEVELS.remove(level);
    }

    public boolean transferBucket(boolean filling) {
        if (level == null || level.isClientSide || !(block() instanceof IrrigationTankBlock)) return false;
        List<IrrigationBlockEntity> tanks = new ArrayList<>();
        ArrayDeque<IrrigationBlockEntity> pending = new ArrayDeque<>();
        HashSet<BlockPos> visited = new HashSet<>();
        pending.add(this);
        visited.add(worldPosition);
        while (!pending.isEmpty()) {
            IrrigationBlockEntity tank = pending.removeFirst();
            tanks.add(tank);
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = tank.worldPosition.relative(direction);
                if (visited.add(neighbor) && level.hasChunkAt(neighbor)
                        && level.getBlockEntity(neighbor) instanceof IrrigationBlockEntity other
                        && other.block() instanceof IrrigationTankBlock) pending.add(other);
            }
        }
        boolean transferred = IrrigationBuckets.transfer(tanks, filling);
        if (transferred) {
            IrrigationReservoir.balance(tanks);
            TANK_LEVELS.remove(level);
        }
        return transferred;
    }

    public void changeWater(int amount) {
        water = Math.max(0, Math.min(getCapacity(), water + amount));
        setChanged();
    }

    private boolean closed() {
        BlockState state = getBlockState();
        return state.getValue(IrrigationBlock.VALVE) && (state.getValue(IrrigationBlock.CLOSED) || level.hasNeighborSignal(worldPosition));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, IrrigationBlockEntity entity) {
        if (entity.block().kind == IrrigationBlock.Kind.SPRINKLER) {
            entity.sprinkle((ServerLevel) level);
            return;
        }
        // Refresh existing saves too: their old tank states had no connection information.
        if (entity.block() instanceof IrrigationTankBlock tank) state = tank.connections(state, level, pos);
        if (entity.block().kind == IrrigationBlock.Kind.TANK && level.getGameTime() % 20 == 0 && level.isRainingAt(pos.above())) entity.changeWater(25);
        boolean blocked = entity.closed();
        if (!blocked) {
            // Local transfers never traverse or load chunks. The normalized difference conserves water.
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!level.hasChunkAt(next) || !(level.getBlockEntity(next) instanceof IrrigationBlockEntity other)
                        || other.getCapacity() == 0 || other.closed()) continue;
                if (entity.block() instanceof IrrigationTankBlock && other.block() instanceof IrrigationTankBlock) continue;
                if (direction.getAxis().isVertical() && (entity.block().kind != IrrigationBlock.Kind.TANK || other.block().kind != IrrigationBlock.Kind.TANK)) continue;
                int amount = direction == Direction.UP ? 0 : IrrigationFlow.transfer(entity.water, entity.getCapacity(), other.water, other.getCapacity(), direction == Direction.DOWN);
                if (amount > 0) { entity.changeWater(-amount); other.changeWater(amount); }
            }
        }
        // Joined tanks share a surface height; channel levels still use four steps.
        int visible = entity.block() instanceof IrrigationTankBlock ? entity.tankVisibleLevel(level, pos)
                : IrrigationFlow.visibleLevel(entity.water, entity.getCapacity());
        BlockState visualState = state.setValue(IrrigationBlock.WATER, visible).setValue(IrrigationBlock.ACTIVE, blocked);
        if (entity.getBlockState() != visualState) level.setBlock(pos, visualState, 2);
    }

    private void sprinkle(ServerLevel level) {
        BlockPos above = worldPosition.above();
        boolean active = level.hasChunkAt(above)
                && level.getBlockEntity(above) instanceof IrrigationBlockEntity channel && channel.block().isChannel()
                && !channel.closed() && channel.water >= IrrigationConfig.waterPerTick;
        BlockState state = getBlockState();
        if (state.getValue(IrrigationBlock.ACTIVE) != active) level.setBlock(worldPosition, state.setValue(IrrigationBlock.ACTIVE, active), 2);
        if (!active) return;
        ((IrrigationBlockEntity) level.getBlockEntity(above)).changeWater(-IrrigationConfig.waterPerTick);
        int x = worldPosition.getX() - 3 + column % 7, z = worldPosition.getZ() - 3 + column / 7;
        column = (column + 1) % 49;
        setChanged();
        for (int y = worldPosition.getY() - 1; y >= Math.max(level.getMinBuildHeight(), worldPosition.getY() - 5); y--) {
            BlockPos target = new BlockPos(x, y, z);
            if (!level.hasChunkAt(target)) break;
            BlockState crop = level.getBlockState(target);
            if (crop.isAir()) continue;
            if (crop.getBlock() instanceof FarmBlock) {
                if (crop.getValue(FarmBlock.MOISTURE) < 7) level.setBlock(target, crop.setValue(FarmBlock.MOISTURE, 7), 2);
                break;
            }
            if (crop.getBlock() instanceof CropBlock || crop.getBlock() instanceof BushBlock || crop.getBlock() instanceof GrowingPlantBlock
                    || crop.getBlock() instanceof BonemealableBlock && crop.getCollisionShape(level, target).isEmpty()) {
                if (y > worldPosition.getY() - 5 && crop.isRandomlyTicking() && level.random.nextDouble() < IrrigationConfig.growthChance) crop.randomTick(level, target, level.random);
                continue;
            }
            break;
        }
    }

    @Override
    public void load(CompoundTag tag) { super.load(tag); water = Math.max(0, Math.min(getCapacity(), tag.getInt("water"))); column = Math.floorMod(tag.getInt("column"), 49); }
    @Override
    protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putInt("water", water); tag.putInt("column", column); }

    @Override
    public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
