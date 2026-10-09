package com.agricraft.agricraft.client;

import com.agricraft.agricraft.AgriClientApiImpl;
import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.AgriClientApi;
import com.agricraft.agricraft.client.ber.CropBlockEntityRenderer;
import com.agricraft.agricraft.client.ber.SeedAnalyzerEntityRenderer;
import com.agricraft.agricraft.client.bewlr.AgriSeedBEWLR;
import com.agricraft.agricraft.client.gui.MagnifyingGlassOverlay;
import com.agricraft.agricraft.client.gui.SeedAnalyzerScreen;
import com.agricraft.agricraft.common.item.SeedBagItem;
import com.agricraft.agricraft.common.item.TrowelItem;
import com.agricraft.agricraft.common.registry.AgriBlockEntities;
import com.agricraft.agricraft.common.registry.AgriBlocks;
import com.agricraft.agricraft.common.registry.AgriItems;
import com.agricraft.agricraft.common.registry.AgriMenuTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.Map;

@EventBusSubscriber(modid = AgriApi.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class AgriCraftClient {

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		AgriClientApi.set(new AgriClientApiImpl());
		ItemProperties.register(AgriItems.TROWEL.get(), ResourceLocation.fromNamespaceAndPath("agricraft", "plant"), (itemStack, clientLevel, livingEntity, i) -> {
			if (!itemStack.isEmpty() && itemStack.getItem() instanceof TrowelItem trowel && trowel.hasPlant(itemStack)) {
				return 1;
			} else {
				return 0;
			}
		});
		ItemProperties.register(AgriItems.SEED_BAG.get(), ResourceLocation.fromNamespaceAndPath("agricraft", "seed_bag"), (itemStack, clientLevel, livingEntity, i) -> {
			if (!itemStack.isEmpty() && itemStack.getItem() instanceof SeedBagItem) {
				if (SeedBagItem.isFilled(itemStack)) {
					return 1;
				} else if (SeedBagItem.isEmpty(itemStack)) {
					return 0.5F;
				}
				return 0;
			} else {
				return 0.5F;
			}
		});
		ItemBlockRenderTypes.setRenderLayer(AgriBlocks.SEED_ANALYZER.get(), RenderType.cutout());
	}

	@SubscribeEvent
	public static void registerMenuScreen(RegisterMenuScreensEvent event) {
		event.register(AgriMenuTypes.SEED_ANALYZER_MENU.get(), SeedAnalyzerScreen::new);
	}

	@SubscribeEvent
	public static void loadModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(com.agricraft.agricraft.client.ber.SprinklerBlockEntityRenderer.HEAD));
		// https://discord.com/channels/313125603924639766/983834532904042537/1104441106248253592
		for (Map.Entry<ResourceLocation, Resource> entry : FileToIdConverter.json("models/seed").listMatchingResources(Minecraft.getInstance().getResourceManager()).entrySet()) {
			ResourceLocation seed = ResourceLocation.parse(entry.getKey().toString().replace("models/seed", "seed").replace(".json", ""));
			event.register(ModelResourceLocation.standalone(seed));
		}
		for (Map.Entry<ResourceLocation, Resource> entry : FileToIdConverter.json("models/crop").listMatchingResources(Minecraft.getInstance().getResourceManager()).entrySet()) {
			ResourceLocation crop = ResourceLocation.parse(entry.getKey().toString().replace("models/crop", "crop").replace(".json", ""));
			event.register(ModelResourceLocation.standalone(crop));
		}
		for (Map.Entry<ResourceLocation, Resource> entry : FileToIdConverter.json("models/weed").listMatchingResources(Minecraft.getInstance().getResourceManager()).entrySet()) {
			ResourceLocation weed = ResourceLocation.parse(entry.getKey().toString().replace("models/weed", "weed").replace(".json", ""));
			event.register(ModelResourceLocation.standalone(weed));
		}
		// add the crop sticks models else they're not loaded
		event.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("agricraft", "block/wooden_crop_sticks")));
		event.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("agricraft", "block/iron_crop_sticks")));
		event.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("agricraft", "block/obsidian_crop_sticks")));
		event.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("agricraft", "block/wooden_cross_crop_sticks")));
		event.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("agricraft", "block/iron_cross_crop_sticks")));
		event.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("agricraft", "block/obsidian_cross_crop_sticks")));
	}

	@SubscribeEvent
	public static void registerBer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AgriBlockEntities.IRRIGATION.get(), com.agricraft.agricraft.client.ber.SprinklerBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(AgriBlockEntities.CROP.get(), CropBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(AgriBlockEntities.SEED_ANALYZER.get(), SeedAnalyzerEntityRenderer::new);
	}

	@SubscribeEvent
	public static void registerBewlr(RegisterClientExtensionsEvent event) {
		event.registerItem(new IClientItemExtensions() {
			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				return AgriSeedBEWLR.INSTANCE;
			}
		}, AgriItems.SEED.get());
	}

	@SubscribeEvent
	public static void registerGuiOverlays(RegisterGuiLayersEvent event) {
		event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(AgriApi.MOD_ID, "magnifying_glass_info"), (guiGraphics, deltaTracker) -> MagnifyingGlassOverlay.renderOverlay(guiGraphics, deltaTracker));
	}

}
