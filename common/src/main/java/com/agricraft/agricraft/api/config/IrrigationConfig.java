package com.agricraft.agricraft.api.config;

import com.teamresourceful.resourcefulconfig.common.annotations.*;
import com.teamresourceful.resourcefulconfig.common.config.EntryType;

@Category(id = "irrigation", translation = "config.agricraft.irrigation")
public final class IrrigationConfig {
    @ConfigEntry(id = "water_per_tick", type = EntryType.INTEGER, translation = "config.agricraft.irrigation.water_per_tick")
    @IntRange(min = 1, max = 100)
    @Comment("Water consumed by each active sprinkler per tick, in millibuckets.")
    public static int waterPerTick = 5;

    @ConfigEntry(id = "growth_chance", type = EntryType.DOUBLE, translation = "config.agricraft.irrigation.growth_chance")
    @DoubleRange(min = 0, max = 1)
    @Comment("Chance to give a plant an extra random tick when its column is watered.")
    public static double growthChance = 0.1;
}
