package com.agricraft.agricraft.common.block.entity;

/** Integer water accounting, independent of Minecraft so conservation can be tested directly. */
public final class IrrigationFlow {
    private IrrigationFlow() {}

    public static int transfer(int source, int sourceCapacity, int target, int targetCapacity, boolean downward) {
        if (sourceCapacity <= 0 || targetCapacity <= 0) return 0;
        long difference = (long) source * targetCapacity - (long) target * sourceCapacity;
        int desired = downward ? source : (int) Math.max(0L, difference / ((long) sourceCapacity + targetCapacity));
        return Math.max(0, Math.min(100, Math.min(desired, Math.min(source, targetCapacity - target))));
    }

    public static int visibleLevel(int water, int capacity) {
        return visibleLevel(water, capacity, 4);
    }

    public static int visibleLevel(long water, long capacity, int steps) {
        return capacity <= 0 || water <= 0 ? 0 : (int) Math.min(steps, 1 + (water * steps - 1) / capacity);
    }
}
