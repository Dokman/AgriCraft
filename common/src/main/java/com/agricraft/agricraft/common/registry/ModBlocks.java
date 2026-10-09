package com.agricraft.agricraft.common.registry;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.common.block.CropBlock;
import com.agricraft.agricraft.common.block.CreativeIrrigationTankBlock;
import com.agricraft.agricraft.common.block.IrrigationBlock;
import com.agricraft.agricraft.common.block.IrrigationTankBlock;
import com.agricraft.agricraft.common.block.SeedAnalyzerBlock;
import com.agricraft.agricraft.common.util.Platform;
import com.agricraft.agricraft.common.util.PlatformRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

public class ModBlocks {
	public static final PlatformRegistry<Block> BLOCKS = Platform.get().createRegistry(BuiltInRegistries.BLOCK, AgriApi.MOD_ID);

	public static final PlatformRegistry.Entry<Block> CROP = BLOCKS.register("crop", CropBlock::new);
	public static final PlatformRegistry.Entry<Block> SEED_ANALYZER = BLOCKS.register("seed_analyzer", SeedAnalyzerBlock::new);
	public static final PlatformRegistry.Entry<Block> IRRIGATION_TANK = BLOCKS.register("irrigation_tank", IrrigationTankBlock::new);
	public static final PlatformRegistry.Entry<Block> CREATIVE_IRRIGATION_TANK = BLOCKS.register("creative_irrigation_tank", CreativeIrrigationTankBlock::new);
	public static final PlatformRegistry.Entry<Block> IRRIGATION_CHANNEL = BLOCKS.register("irrigation_channel", () -> new IrrigationBlock(IrrigationBlock.Kind.CHANNEL));
	public static final PlatformRegistry.Entry<Block> IRRIGATION_CHANNEL_HOLLOW = BLOCKS.register("irrigation_channel_hollow", () -> new IrrigationBlock(IrrigationBlock.Kind.HOLLOW_CHANNEL));
	public static final PlatformRegistry.Entry<Block> SPRINKLER = BLOCKS.register("sprinkler", () -> new IrrigationBlock(IrrigationBlock.Kind.SPRINKLER));

}
