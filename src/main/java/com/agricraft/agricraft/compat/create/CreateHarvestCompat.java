package com.agricraft.agricraft.compat.create;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.config.AgriCraftConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;

/** Optional Create bridge that collects harvests without destroying crop sticks. */
public final class CreateHarvestCompat {
    private CreateHarvestCompat() {}

    private static final ClassValue<Field> WORLD = new ClassValue<>() {
        @Override
        protected Field computeValue(Class<?> type) {
            try { return type.getField("world"); }
            catch (NoSuchFieldException exception) { throw new IllegalStateException("Unsupported Create MovementContext", exception); }
        }
    };

    private static final ClassValue<Method> DROP_ITEM = new ClassValue<>() {
        @Override
        protected Method computeValue(Class<?> type) {
            for (Method method : type.getMethods()) {
                Class<?>[] parameters = method.getParameterTypes();
                if ((method.getName().equals("collectOrDropItem") || method.getName().equals("dropItem")) && parameters.length == 2
                        && parameters[0].getName().equals("com.simibubi.create.content.contraptions.behaviour.MovementContext")
                        && parameters[1] == ItemStack.class) return method;
            }
            throw new IllegalStateException("Unsupported Create harvester: item collector not found");
        }
    };

    public static boolean visit(Object behaviour, Object context, BlockPos pos) {
        if (!AgriCraftConfig.COMPAT_CREATE.get()) return false;
        try {
            Level world = (Level) WORLD.get(context.getClass()).get(context);
            if (world.isClientSide) return false;
            var crop = AgriApi.get().getCrop(world, pos);
            if (crop.isEmpty()) return false;
            // Protect empty sticks and immature crops from Create's normal block destruction.
            if (crop.get().canBeHarvested()) {
                Method drop = DROP_ITEM.get(behaviour.getClass());
                harvest(crop.get(), stack -> {
                    try { drop.invoke(behaviour, context, stack); }
                    catch (IllegalAccessException | InvocationTargetException exception) {
                        throw new IllegalStateException("Create could not collect an AgriCraft harvest", exception);
                    }
                });
            }
            return true;
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Create MovementContext world is inaccessible", exception);
        }
    }

    public static boolean harvest(com.agricraft.agricraft.api.crop.AgriCrop crop, Consumer<ItemStack> output) {
        if (!crop.canBeHarvested()) return false;
        crop.getHarvestProducts(output);
        crop.setGrowthStage(crop.getPlant().getGrowthStageAfterHarvest());
        crop.getPlant().onHarvest(crop, null);
        return true;
    }
}
