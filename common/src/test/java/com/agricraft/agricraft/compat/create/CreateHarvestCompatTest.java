package com.agricraft.agricraft.compat.create;

import com.agricraft.agricraft.api.config.CompatConfig;
import com.agricraft.agricraft.api.crop.AgriCrop;
import com.agricraft.agricraft.api.crop.AgriGrowthStage;
import com.agricraft.agricraft.api.plant.AgriPlant;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class CreateHarvestCompatTest {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        AgriPlant plant = new AgriPlant.Builder().stages(2, 4, 6, 8).harvest(1).build();
        AtomicBoolean mature = new AtomicBoolean(true);
        AtomicReference<AgriGrowthStage> stage = new AtomicReference<>();
        AgriCrop crop = (AgriCrop) Proxy.newProxyInstance(AgriCrop.class.getClassLoader(), new Class[]{AgriCrop.class}, (proxy, method, arguments) -> {
            return switch (method.getName()) {
                case "canBeHarvested" -> mature.get();
                case "getPlant" -> plant;
                case "getHarvestProducts" -> { ((Consumer<ItemStack>) arguments[0]).accept(new ItemStack(Items.WHEAT, 3)); yield null; }
                case "setGrowthStage" -> { stage.set((AgriGrowthStage) arguments[0]); yield null; }
                case "harvest" -> {
                    assert arguments[1] == null : "Automated harvest must not impersonate a player";
                    yield InvocationHandler.invokeDefault(proxy, method, arguments);
                }
                default -> throw new UnsupportedOperationException("Unexpected crop mutation: " + method.getName());
            };
        });
        List<ItemStack> output = new ArrayList<>();
        assert CreateHarvestCompat.harvest(crop, output::add);
        assert output.size() == 1 && output.get(0).is(Items.WHEAT) && output.get(0).getCount() == 3;
        assert stage.get().index() == plant.getGrowthStageAfterHarvest().index()
                && stage.get().total() == plant.getGrowthStageAfterHarvest().total() : "Use the plant's configured harvest stage";
        stage.set(null);
        output.clear();
        mature.set(false);
        assert !CreateHarvestCompat.harvest(crop, output::add);
        assert stage.get() == null && output.isEmpty() : "Immature crops must remain untouched";
        CompatConfig.enableCreate = false;
        assert !CreateHarvestCompat.visit(null, null, null) : "Disabled integration must not access Create";
        CompatConfig.enableCreate = true;
        System.out.println("Create harvest: products, configured regrowth, immature protection and optional disable passed.");
    }
}
