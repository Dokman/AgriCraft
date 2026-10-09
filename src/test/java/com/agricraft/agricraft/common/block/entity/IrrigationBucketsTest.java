package com.agricraft.agricraft.common.block.entity;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class IrrigationBucketsTest {
    private static class Tank implements IrrigationReservoir.Member {
        int water;
        int y;
        Tank(int water) { this.water = water; }
        Tank(int water, int y) { this.water = water; this.y = y; }
        public int getTankY() { return y; }
        public int getWater() { return water; }
        public int getCapacity() { return 16000; }
        public void changeWater(int amount) { water += amount; assert water >= 0 && water <= getCapacity(); }
    }

    public static void main(String[] args) {
        Tank clicked = new Tank(16000), neighbor = new Tank(0);
        assert IrrigationBuckets.transfer(List.of(clicked, neighbor), true);
        assert clicked.water == 16000 && neighbor.water == 1000 : "A full clicked block must use the rest of the reservoir";
        clicked.water = 0;
        assert IrrigationBuckets.transfer(List.of(clicked, neighbor), false);
        assert clicked.water == 0 && neighbor.water == 0 : "An empty clicked block must draw from the rest of the reservoir";
        clicked.water = 15500; neighbor.water = 15500;
        assert IrrigationBuckets.transfer(List.of(clicked, neighbor), true);
        assert clicked.water == 16000 && neighbor.water == 16000;
        AtomicInteger exchanged = new AtomicInteger();
        assert IrrigationBuckets.interact(false, () -> IrrigationBuckets.transfer(List.of(clicked, neighbor), true), exchanged::incrementAndGet).consumesAction();
        assert exchanged.get() == 0 && clicked.water == 16000 && neighbor.water == 16000 : "Full reservoirs must block offhand use without exchanging the bucket";
        clicked.water = 400; neighbor.water = 599;
        assert !IrrigationBuckets.transfer(List.of(clicked, neighbor), false);
        assert clicked.water == 400 && neighbor.water == 599 : "A rejected drain must not discard partial water";
        assert IrrigationBuckets.interact(false, () -> false, exchanged::incrementAndGet).consumesAction();
        assert exchanged.get() == 0;
        assert IrrigationBuckets.interact(true, () -> { throw new AssertionError("Client must not alter water"); },
                () -> { throw new AssertionError("Client must not exchange buckets"); }).consumesAction();
        Random random = new Random(42);
        for (int i = 0; i < 10000; i++) {
            clicked.water = random.nextInt(16001); neighbor.water = random.nextInt(16001);
            int before = clicked.water + neighbor.water;
            boolean filling = random.nextBoolean();
            boolean success = IrrigationBuckets.transfer(List.of(clicked, neighbor), filling);
            assert clicked.water + neighbor.water == before + (success ? filling ? 1000 : -1000 : 0);
        }
        Tank a = new Tank(16000), b = new Tank(0), c = new Tank(0), d = new Tank(0);
        IrrigationReservoir.balance(List.of(a, b, c, d));
        assert a.water == 4000 && b.water == 4000 && c.water == 4000 && d.water == 4000 : "Joined tanks must share their contents immediately";
        // Removing one member must not reset the water or destroy the other members.
        IrrigationReservoir.balance(List.of(a, b, c));
        assert a.water + b.water + c.water == 12000;
        IrrigationReservoir.balance(List.of(a));
        IrrigationReservoir.balance(List.of(b, c));
        assert a.water == 4000 && b.water + c.water == 8000 : "Split reservoirs retain their own water";
        Tank bottom = new Tank(0, -10), top = new Tank(16000, -9);
        IrrigationReservoir.balance(List.of(top, bottom));
        assert bottom.water == 16000 && top.water == 0 : "Shared reservoirs fill from the bottom";
        for (int i = 0; i < 10000; i++) {
            bottom.water = random.nextInt(16001); top.water = random.nextInt(16001);
            int before = bottom.water + top.water;
            IrrigationReservoir.balance(List.of(top, bottom));
            assert before == bottom.water + top.water;
            assert top.water == 0 || bottom.water == 16000;
        }
        System.out.println("Reservoir buckets: shared capacity, exact transfers, refusals, offhand protection and 10000 random operations passed.");
    }
}
