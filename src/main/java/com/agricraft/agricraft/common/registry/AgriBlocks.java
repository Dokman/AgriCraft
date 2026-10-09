package com.agricraft.agricraft.common.registry;

import com.agricraft.agricraft.common.block.CropBlock;
import com.agricraft.agricraft.common.block.CreativeIrrigationTankBlock;
import com.agricraft.agricraft.common.block.SeedAnalyzerBlock;
import com.agricraft.agricraft.common.block.IrrigationBlock;
import com.agricraft.agricraft.common.block.IrrigationTankBlock;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.ApiStatus;

import static com.agricraft.agricraft.common.registry.AgriRegistries.BLOCKS;

public interface AgriBlocks {

	DeferredBlock<CropBlock> CROP = BLOCKS.register("crop", CropBlock::new);
	DeferredBlock<SeedAnalyzerBlock> SEED_ANALYZER = BLOCKS.register("seed_analyzer", SeedAnalyzerBlock::new);

	DeferredBlock<IrrigationTankBlock> IRRIGATION_TANK = BLOCKS.register("irrigation_tank", IrrigationTankBlock::new);
	DeferredBlock<CreativeIrrigationTankBlock> CREATIVE_IRRIGATION_TANK = BLOCKS.register("creative_irrigation_tank", CreativeIrrigationTankBlock::new);
	DeferredBlock<IrrigationBlock> IRRIGATION_CHANNEL = BLOCKS.register("irrigation_channel", () -> new IrrigationBlock(IrrigationBlock.Kind.CHANNEL));
	DeferredBlock<IrrigationBlock> IRRIGATION_CHANNEL_HOLLOW = BLOCKS.register("irrigation_channel_hollow", () -> new IrrigationBlock(IrrigationBlock.Kind.HOLLOW_CHANNEL));
	DeferredBlock<IrrigationBlock> SPRINKLER = BLOCKS.register("sprinkler", () -> new IrrigationBlock(IrrigationBlock.Kind.SPRINKLER));

	@ApiStatus.Internal
	static void register() {}

}
