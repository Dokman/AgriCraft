package com.agricraft.agricraft.common.block.entity;

import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.world.InteractionResult;

/** An all-or-nothing bucket operation across a connected reservoir. */
public final class IrrigationBuckets {
    private IrrigationBuckets() {}

    public interface Tank {
        int getWater();
        int getCapacity();
        void changeWater(int amount);
    }

    public static InteractionResult interact(boolean clientSide, BooleanSupplier transfer, Runnable exchangeBucket) {
        if (clientSide) return InteractionResult.SUCCESS;
        if (transfer.getAsBoolean()) exchangeBucket.run();
        // Both success and refusal stop vanilla item use and offhand fallback.
        return InteractionResult.CONSUME;
    }

    public static boolean transfer(List<? extends Tank> tanks, boolean filling) {
        long available = 0;
        for (Tank tank : tanks) available += filling ? tank.getCapacity() - tank.getWater() : tank.getWater();
        if (available < 1000) return false;
        int remaining = 1000;
        for (Tank tank : tanks) {
            int amount = Math.min(remaining, filling ? tank.getCapacity() - tank.getWater() : tank.getWater());
            if (amount > 0) tank.changeWater(filling ? amount : -amount);
            remaining -= amount;
            if (remaining == 0) break;
        }
        return true;
    }
}
