package com.agricraft.agricraft.common.registry;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.common.block.entity.CropBlockEntity;
import com.agricraft.agricraft.common.block.entity.IrrigationBlockEntity;
import com.agricraft.agricraft.common.block.entity.SeedAnalyzerBlockEntity;
import com.agricraft.agricraft.common.util.Platform;
import com.agricraft.agricraft.common.util.PlatformRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntityTypes {
	public static final PlatformRegistry<BlockEntityType<?>> BLOCK_ENTITY_TYPES = Platform.get().createRegistry(BuiltInRegistries.BLOCK_ENTITY_TYPE, AgriApi.MOD_ID);

	public static final PlatformRegistry.Entry<BlockEntityType<CropBlockEntity>> CROP = BLOCK_ENTITY_TYPES.register("crop", () -> BlockEntityType.Builder.of((blockPos, blockState) -> Platform.get().createCropBlockEntity(blockPos, blockState), ModBlocks.CROP.get()).build(null));
	public static final PlatformRegistry.Entry<BlockEntityType<SeedAnalyzerBlockEntity>> SEED_ANALYZER = BLOCK_ENTITY_TYPES.register("seed_analyzer", () -> BlockEntityType.Builder.of(SeedAnalyzerBlockEntity::new, ModBlocks.SEED_ANALYZER.get()).build(null));
	public static final PlatformRegistry.Entry<BlockEntityType<IrrigationBlockEntity>> IRRIGATION = BLOCK_ENTITY_TYPES.register("irrigation", () -> BlockEntityType.Builder.of(IrrigationBlockEntity::new, ModBlocks.IRRIGATION_TANK.get(), ModBlocks.IRRIGATION_CHANNEL.get(), ModBlocks.IRRIGATION_CHANNEL_HOLLOW.get(), ModBlocks.SPRINKLER.get()).build(null));

}
