package com.agricraft.agricraft.compat.jade;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.common.block.IrrigationBlock;
import com.agricraft.agricraft.common.block.entity.IrrigationBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Uses Jade's server data channel so contents remain accurate on multiplayer clients. */
public final class IrrigationComponentProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    public static final IrrigationComponentProvider INSTANCE = new IrrigationComponentProvider();
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AgriApi.MOD_ID, "irrigation");
    private static final String DATA = "agricraft:irrigation";

    private IrrigationComponentProvider() { }

    @Override
    public ResourceLocation getUid() { return ID; }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof IrrigationBlockEntity irrigation)) return;
        var state = irrigation.getBlockState();
        if (!(state.getBlock() instanceof IrrigationBlock block)) return;
        var contents = irrigation.getDisplayContents();
        CompoundTag data = new CompoundTag();
        data.putLong("water", contents.water());
        data.putLong("capacity", contents.capacity());
        data.putBoolean("valve", block.isChannel() && state.getValue(IrrigationBlock.VALVE));
        data.putBoolean("closed", irrigation.isValveClosed());
        data.putBoolean("sprinkler", block.kind == IrrigationBlock.Kind.SPRINKLER);
        data.putBoolean("active", state.getValue(IrrigationBlock.ACTIVE));
        tag.put(DATA, data);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!accessor.getServerData().contains(DATA, net.minecraft.nbt.Tag.TAG_COMPOUND)) return;
        CompoundTag data = accessor.getServerData().getCompound(DATA);
        if (data.getBoolean("sprinkler")) {
            tooltip.add(Component.translatable("agricraft.tooltip.jade.irrigation.sprinkler."
                    + (data.getBoolean("active") ? "active" : "inactive")));
            return;
        }
        tooltip.add(Component.translatable("agricraft.irrigation.contents",
                data.getLong("water"), data.getLong("capacity")));
        if (data.getBoolean("valve")) {
            tooltip.add(Component.translatable("agricraft.tooltip.jade.irrigation.valve."
                    + (data.getBoolean("closed") ? "closed" : "open")));
        }
    }
}
