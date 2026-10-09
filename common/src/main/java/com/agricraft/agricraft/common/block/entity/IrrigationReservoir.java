package com.agricraft.agricraft.common.block.entity;

import java.util.List;
import java.util.TreeMap;

/** A joined reservoir shares water instantly, filling lower layers before upper layers. */
public final class IrrigationReservoir {
    private IrrigationReservoir() {}

    public interface Member extends IrrigationBuckets.Tank {
        int getTankY();
    }

    public static void balance(List<? extends Member> tanks) {
        long remaining = 0;
        TreeMap<Integer, java.util.ArrayList<Member>> layers = new TreeMap<>();
        for (Member tank : tanks) {
            remaining += tank.getWater();
            layers.computeIfAbsent(tank.getTankY(), unused -> new java.util.ArrayList<>()).add(tank);
        }
        for (var layer : layers.values()) {
            long capacity = layer.stream().mapToLong(Member::getCapacity).sum();
            long contents = Math.min(remaining, capacity);
            remaining -= contents;
            long allocated = 0, cumulativeCapacity = 0;
            for (Member tank : layer) {
                cumulativeCapacity += tank.getCapacity();
                long next = contents * cumulativeCapacity / capacity;
                int target = (int) (next - allocated);
                allocated = next;
                if (target != tank.getWater()) tank.changeWater(target - tank.getWater());
            }
        }
    }
}
