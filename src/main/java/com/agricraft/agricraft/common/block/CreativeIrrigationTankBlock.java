package com.agricraft.agricraft.common.block;

/** Creative-only reservoir source, joining ordinary tanks with the same exterior walls. */
public final class CreativeIrrigationTankBlock extends IrrigationTankBlock {
    public CreativeIrrigationTankBlock() {
        registerDefaultState(defaultBlockState().setValue(WATER, 16));
    }
}
