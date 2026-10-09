package com.agricraft.agricraft.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import com.agricraft.agricraft.common.block.entity.IrrigationBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Connected reservoirs have only their exterior walls, including the collision shape. */
public class IrrigationTankBlock extends IrrigationBlock {
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    public IrrigationTankBlock() { super(Kind.TANK); }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
        super.onPlace(state, level, pos, previous, moving);
        if (previous.getBlock() != this) IrrigationBlockEntity.invalidateTankLevels(level);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (next.getBlock() != this) IrrigationBlockEntity.invalidateTankLevels(level);
        super.onRemove(state, level, pos, next, moving);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DOWN);
    }

    private boolean isTank(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof IrrigationTankBlock;
    }

    @Override
    public BlockState connections(BlockState state, LevelReader level, BlockPos pos) {
        // A channel carries water, but does not remove the tank's entire wall.
        return state.setValue(NORTH, isTank(level, pos.north())).setValue(EAST, isTank(level, pos.east()))
                .setValue(SOUTH, isTank(level, pos.south())).setValue(WEST, isTank(level, pos.west()))
                .setValue(DOWN, isTank(level, pos.below()));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = state.getValue(DOWN) ? Shapes.empty() : box(0, 0, 0, 16, 2, 16);
        if (!state.getValue(NORTH)) shape = Shapes.or(shape, box(0, 0, 0, 16, 16, 2));
        if (!state.getValue(EAST)) shape = Shapes.or(shape, box(14, 0, 0, 16, 16, 16));
        if (!state.getValue(SOUTH)) shape = Shapes.or(shape, box(0, 0, 14, 16, 16, 16));
        if (!state.getValue(WEST)) shape = Shapes.or(shape, box(0, 0, 0, 2, 16, 16));
        return shape;
    }
}
