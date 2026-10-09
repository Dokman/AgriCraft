package com.agricraft.agricraft.common.block;

import com.agricraft.agricraft.common.block.entity.IrrigationBlockEntity;
import com.agricraft.agricraft.common.block.entity.IrrigationBuckets;
import com.agricraft.agricraft.common.registry.ModBlockEntityTypes;
import com.agricraft.agricraft.common.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Ordinary baked block models deliberately keep irrigation out of custom shader render paths. */
public class IrrigationBlock extends Block implements EntityBlock {
    public enum Kind { TANK, CHANNEL, HOLLOW_CHANNEL, SPRINKLER }
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty VALVE = BooleanProperty.create("valve");
    public static final BooleanProperty CLOSED = BooleanProperty.create("closed");
    public static final IntegerProperty WATER = IntegerProperty.create("water", 0, 16);
    public final Kind kind;

    public IrrigationBlock(Kind kind) {
        // FlowingFluid checks blocksMotion, independently of bucket replaceability.
        // Mark these structures solid for fluid logic while retaining their hollow shapes.
        super(Properties.of().mapColor(MapColor.WOOD).strength(2, 3).sound(SoundType.WOOD).noOcclusion().forceSolidOn());
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false)
                .setValue(SOUTH, false).setValue(WEST, false).setValue(ACTIVE, false)
                .setValue(VALVE, false).setValue(CLOSED, false).setValue(WATER, 0));
    }

    public boolean isChannel() { return kind == Kind.CHANNEL || kind == Kind.HOLLOW_CHANNEL; }

    @Override
    public boolean canBeReplaced(BlockState state, Fluid fluid) {
        // The vanilla implementation treats non-solid shapes as disposable. Joined tanks
        // can have no walls or floor, but still own a block entity and stored water.
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, ACTIVE, VALVE, CLOSED, WATER);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new IrrigationBlockEntity(pos, state); }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntityTypes.IRRIGATION.get()) return null;
        if (level.isClientSide) {
            return kind == Kind.SPRINKLER ? (world, pos, blockState, entity) -> {
                IrrigationBlockEntity sprinkler = (IrrigationBlockEntity) entity;
                sprinkler.tickSprinklerAnimation();
                com.agricraft.agricraft.client.ber.SprinklerBlockEntityRenderer.spawnDrops(sprinkler);
            } : null;
        }
        return (world, pos, blockState, entity) -> IrrigationBlockEntity.tick(world, pos, blockState, (IrrigationBlockEntity) entity);
    }

    private boolean connects(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof IrrigationBlock block && block.kind != Kind.SPRINKLER;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!canSurvive(defaultBlockState(), context.getLevel(), context.getClickedPos())) return null;
        return connections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    protected BlockState connections(BlockState state, LevelReader level, BlockPos pos) {
        if (!isChannel()) return state;
        return state.setValue(NORTH, connects(level, pos.north())).setValue(EAST, connects(level, pos.east()))
                .setValue(SOUTH, connects(level, pos.south())).setValue(WEST, connects(level, pos.west()));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return kind != Kind.SPRINKLER || level.getBlockState(pos.above()).getBlock() instanceof IrrigationBlock block && block.isChannel();
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!canSurvive(state, level, pos)) return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        return connections(state, level, pos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (kind == Kind.SPRINKLER) return Shapes.or(box(3, 4, 3, 13, 16, 13), box(5, 15, 5, 11, 21, 11));
        VoxelShape shape = Shapes.or(box(5, 5, 5, 11, 6, 11), box(5, 6, 5, 6, 11, 6),
                box(10, 6, 5, 11, 11, 6), box(5, 6, 10, 6, 11, 11), box(10, 6, 10, 11, 11, 11));
        BooleanProperty[] sides = {NORTH, EAST, SOUTH, WEST};
        for (int i = 0; i < 4; i++) {
            VoxelShape side = state.getValue(sides[i])
                    ? Shapes.or(box(5, 5, 0, 11, 6, 5), box(5, 6, 0, 6, 11, 5), box(10, 6, 0, 11, 11, 5))
                    : box(6, 6, 5, 10, 11, 6);
            if (kind == Kind.HOLLOW_CHANNEL && state.getValue(sides[i])) side = Shapes.or(side, box(6, 10, 0, 10, 11, 5));
            for (var bounds : side.toAabbs()) {
                double x0 = bounds.minX * 16, x1 = bounds.maxX * 16, z0 = bounds.minZ * 16, z1 = bounds.maxZ * 16;
                for (int turn = 0; turn < i; turn++) {
                    double oldX0 = x0, oldX1 = x1;
                    x0 = 16 - z1; x1 = 16 - z0; z0 = oldX0; z1 = oldX1;
                }
                shape = Shapes.or(shape, box(x0, bounds.minY * 16, z0, x1, bounds.maxY * 16, z1));
            }
        }
        if (kind == Kind.HOLLOW_CHANNEL) shape = Shapes.or(shape, box(6, 10, 6, 10, 11, 10));
        return shape;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!(level.getBlockEntity(pos) instanceof IrrigationBlockEntity irrigation)) return InteractionResult.PASS;
        if (kind == Kind.TANK && (held.is(Items.WATER_BUCKET) || held.is(Items.BUCKET))) {
            boolean filling = held.is(Items.WATER_BUCKET);
            // Consume rejected bucket interactions too. FAIL would let Minecraft try the
            // offhand and accidentally place a tank, or use the bucket to spill water.
            return IrrigationBuckets.interact(level.isClientSide, () -> irrigation.transferBucket(filling),
                    () -> player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(filling ? Items.BUCKET : Items.WATER_BUCKET))));
        }
        if (isChannel() && held.is(ModItems.CHANNEL_VALVE.get()) && !state.getValue(VALVE)) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(VALVE, true), UPDATE_ALL);
                if (!player.getAbilities().instabuild) held.shrink(1);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (isChannel() && state.getValue(VALVE) && held.isEmpty()) {
            if (!level.isClientSide) {
                if (player.isShiftKeyDown()) {
                    level.setBlock(pos, state.setValue(VALVE, false).setValue(CLOSED, false), UPDATE_ALL);
                    popResource(level, pos, new ItemStack(ModItems.CHANNEL_VALVE.get()));
                } else level.setBlock(pos, state.cycle(CLOSED), UPDATE_ALL);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (held.isEmpty()) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("agricraft.irrigation.contents", irrigation.getWater(), irrigation.getCapacity()), true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

}
