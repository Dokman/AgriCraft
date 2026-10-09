package com.agricraft.agricraft.common.block.entity;

import java.util.List;

/** Creative supplies must fill every layer without changing ordinary reservoir behavior. */
public class CreativeIrrigationTest {
    private static class Tank implements IrrigationReservoir.Member {
        int water;
        final int y;
        final boolean creative;
        boolean infinite;
        Tank(int y, boolean creative) { this.y = y; this.creative = creative; }
        public int getTankY() { return y; }
        public int getWater() { return water; }
        public int getCapacity() { return 16000; }
        public boolean isCreativeSource() { return creative; }
        public void setInfiniteSupply(boolean value) { infinite = value; }
        public void changeWater(int amount) { water = infinite ? getCapacity() : Math.max(0, Math.min(getCapacity(), water + amount)); }
    }
    public static void main(String[] args) {
        Tank source = new Tank(1, true), lower = new Tank(0, false), upper = new Tank(2, false);
        List<Tank> reservoir = List.of(lower, source, upper);
        IrrigationReservoir.balance(reservoir);
        for (Tank tank : reservoir) assert tank.water == tank.getCapacity();
        for (int tick = 0; tick < 10000; tick++) {
            lower.changeWater(-100);
            upper.changeWater(-100);
            assert IrrigationBuckets.transfer(reservoir, false);
            assert !IrrigationBuckets.transfer(reservoir, true);
            for (Tank tank : reservoir) assert tank.water == tank.getCapacity();
        }
        IrrigationReservoir.balance(List.of(lower, upper));
        lower.changeWater(-1000);
        assert lower.water == 15000 : "Removing the creative source must restore normal consumption";
        assert !upper.infinite && !lower.infinite;
        int stored = lower.water + upper.water;
        IrrigationReservoir.balance(List.of(lower, upper));
        assert lower.water + upper.water == stored : "Ordinary reservoirs must conserve water";
        System.out.println("Creative reservoir: all layers full, infinite consumption, bucket extraction and source removal passed.");
    }
}
