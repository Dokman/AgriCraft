package com.infinityraider.agricraft.plugins.theoneprobe;

import com.infinityraider.agricraft.AgriCraft;
import com.infinityraider.agricraft.api.v1.AgriApi;
import com.infinityraider.agricraft.reference.AgriToolTips;
import com.infinityraider.agricraft.reference.Names;
import mcjty.theoneprobe.api.*;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.Function;

public class TheOneProbeCompat implements Function<ITheOneProbe, Void> {
    private static final TheOneProbeCompat INSTANCE = new TheOneProbeCompat();

    public static TheOneProbeCompat getInstance() {
        return INSTANCE;
    }

    private TheOneProbeCompat() {}

    @Override
    public Void apply(ITheOneProbe probe) {
        probe.registerProvider(new BlockProbeInfoProvider());
        return null;
    }

    private static final class BlockProbeInfoProvider implements IProbeInfoProvider {
        private final String id;

        private BlockProbeInfoProvider () {
            this.id = AgriCraft.instance.getModId() + ":" + Names.Mods.THE_ONE_PROBE;
        }

        @Override
        public String getID() {
            return this.id;
        }

        @Override
        public void addProbeInfo(ProbeMode mode, IProbeInfo info, PlayerEntity player,
                                 World world, BlockState state, IProbeHitData hitData) {
            this.addCropProbeInfo(info, player, world, hitData.getPos(), mode);
            this.addSoilProbeInfo(info, world, hitData.getPos());
        }

        protected void addCropProbeInfo(IProbeInfo info, PlayerEntity player, World world, BlockPos pos, ProbeMode mode) {
            AgriApi.getCrop(world, pos).ifPresent(crop -> {
                // Add data including full genome if in creative mode
                if(mode == ProbeMode.DEBUG) {
                    crop.addDisplayInfo(info::text);
                    info.text(AgriToolTips.GENOME);
                    crop.getGenome().map(genome -> {
                        genome.addDisplayInfo(info::text);
                        return true;
                    }).orElseGet(() ->{
                        info.text(AgriToolTips.UNKNOWN);
                        return false;
                    });
                } else {
                    // add crop data
                    if(this.shouldAddInfo(player)) {
                        crop.addDisplayInfo(info::text);
                    }
                }
            });
        }

        protected boolean shouldAddInfo(PlayerEntity player) {
            if (AgriCraft.instance.getConfig().doesMagnifyingGlassControlTOP()) {
                return AgriCraft.instance.proxy().isMagnifyingGlassObserving(player);
            }
            return true;
        }

        protected void addSoilProbeInfo(IProbeInfo info, World world, BlockPos pos) {
            AgriApi.getSoilRegistry().valueOf(world.getBlockState(pos)).ifPresent(soil ->
                    soil.addDisplayInfo(info::text));
        }
    }
}
