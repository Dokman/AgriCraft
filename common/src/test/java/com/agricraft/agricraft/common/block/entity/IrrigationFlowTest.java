package com.agricraft.agricraft.common.block.entity;

import java.util.Random;

/** Run with assertions enabled; no game or loader is needed for the hydraulic invariants. */
public class IrrigationFlowTest {
    public static void main(String[] args) {
        assert IrrigationFlow.transfer(1000, 16000, 0, 500, false) == 30;
        assert IrrigationFlow.transfer(0, 16000, 500, 500, false) == 0;
        assert IrrigationFlow.transfer(16000, 16000, 500, 500, false) == 0;
        assert IrrigationFlow.transfer(20, 16000, 0, 16000, true) == 20;
        assert IrrigationFlow.transfer(16000, 16000, 15990, 16000, true) == 10;
        assert IrrigationFlow.visibleLevel(0, 16000) == 0;
        assert IrrigationFlow.visibleLevel(1, 16000) == 1;
        assert IrrigationFlow.visibleLevel(4000, 16000) == 1;
        assert IrrigationFlow.visibleLevel(4001, 16000) == 2;
        assert IrrigationFlow.visibleLevel(16000, 16000) == 4;
        for (int bucket = 1; bucket <= 16; bucket++) {
            assert IrrigationFlow.visibleLevel(bucket * 1000L, 16000L, 16) == bucket;
            assert IrrigationFlow.visibleLevel(bucket * 1000L * 9, 16000L * 9, 16) == bucket;
        }
        assert IrrigationFlow.visibleLevel(0, 144000, 16) == 0;
        assert IrrigationFlow.visibleLevel(1000, 144000, 16) == 1;

        Random random = new Random(42);
        for (int i = 0; i < 100000; i++) {
            int aCapacity = random.nextBoolean() ? 16000 : 500;
            int bCapacity = random.nextBoolean() ? 16000 : 500;
            int a = random.nextInt(aCapacity + 1), b = random.nextInt(bCapacity + 1);
            int transfer = IrrigationFlow.transfer(a, aCapacity, b, bCapacity, random.nextBoolean());
            assert transfer >= 0 && transfer <= 100;
            assert a - transfer >= 0 && b + transfer <= bCapacity;
            assert (a - transfer) + (b + transfer) == a + b;
        }

        // A tank feeding six channels and a sprinkler: transfer and consumption conserve every mB.
        int[] capacities = {16000, 500, 500, 500, 500, 500, 500};
        int[] water = {16000, 0, 0, 0, 0, 0, 0};
        int consumed = 0;
        for (int tick = 0; tick < 10000; tick++) {
            for (int i = 0; i < water.length - 1; i++) {
                int moved = IrrigationFlow.transfer(water[i], capacities[i], water[i+1], capacities[i+1], false);
                water[i] -= moved;
                water[i+1] += moved;
            }
            if (water[6] >= 5) { water[6] -= 5; consumed += 5; }
            int total = consumed;
            for (int i = 0; i < water.length; i++) { assert water[i] >= 0 && water[i] <= capacities[i]; total += water[i]; }
            assert total == 16000;
        }
        assert consumed > 10000 : "Water must reach the far sprinkler";
        System.out.println("Irrigation flow: all checks passed (100000 randomized transfers and 10000 network ticks).");
    }
}
