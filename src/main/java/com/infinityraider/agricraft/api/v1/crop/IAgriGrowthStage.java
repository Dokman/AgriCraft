package com.infinityraider.agricraft.api.v1.crop;

import com.infinityraider.agricraft.api.v1.misc.IAgriRegisterable;

import javax.annotation.Nonnull;

public interface IAgriGrowthStage extends IAgriRegisterable<IAgriGrowthStage> {
    /**
     * @return true if this growth stage signifies a mature plant (meaning it does not have any further growth stages
     */
    boolean isMature();

    /**
     * @return true if a plant of this growth stage will drop its seed when broken
     */
    boolean canDropSeed();

    /**
     * @return The next growth stage after a successful growth tick (returns itself if this is the final growth stage).
     */
    @Nonnull
    IAgriGrowthStage getNextStage();

    /**
     * @return the growth percentage corresponding to this growth stage, between 0 and 1 (both inclusive),
     * in which 1 means mature
     */
    double growthPercentage();

    /**
     * Internal only, used for the default no growth stage property when no plant is planted
     * @return if this is an actual growth stage
     */
    default boolean isGrowthStage() {
        return true;
    }
}