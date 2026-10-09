package com.agricraft.agricraft.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaCommonRegistration;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/** Exercises the real Jade provider with server packets, without starting a game client. */
public class IrrigationComponentProviderTest {
    private static List<Component> tooltip(CompoundTag serverData) {
        BlockAccessor accessor = (BlockAccessor) Proxy.newProxyInstance(BlockAccessor.class.getClassLoader(),
                new Class[]{BlockAccessor.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getServerData")) return serverData;
                    throw new AssertionError("Tooltip must use server data: " + method.getName());
                });
        List<Component> lines = new ArrayList<>();
        ITooltip tooltip = (ITooltip) Proxy.newProxyInstance(ITooltip.class.getClassLoader(),
                new Class[]{ITooltip.class}, (proxy, method, args) -> {
                    if (method.getName().equals("add") && args[0] instanceof Component component) {
                        lines.add(component);
                        return null;
                    }
                    throw new AssertionError(method.getName());
                });
        IrrigationComponentProvider.INSTANCE.appendTooltip(tooltip, accessor, null);
        return lines;
    }

    private static String key(Component component) {
        return ((TranslatableContents) component.getContents()).getKey();
    }

    public static void main(String[] args) {
        CompoundTag packet = new CompoundTag();
        assert tooltip(packet).isEmpty() : "Do not guess contents before the server reply";
        CompoundTag data = new CompoundTag();
        data.putLong("water", 24000);
        data.putLong("capacity", 64000);
        packet.put("agricraft:irrigation", data);
        var lines = tooltip(packet);
        assert lines.size() == 1;
        assert key(lines.get(0)).equals("agricraft.irrigation.contents");
        var contents = (TranslatableContents) lines.get(0).getContents();
        assert contents.getArgs()[0].equals(24000L);
        assert contents.getArgs()[1].equals(64000L) : "Show the full reservoir's capacity";

        data.putBoolean("valve", true);
        assert key(tooltip(packet).get(1)).endsWith("valve.open");
        data.putBoolean("closed", true);
        assert key(tooltip(packet).get(1)).endsWith("valve.closed");
        data.putBoolean("valve", false);
        data.putLong("water", 0);
        data.putLong("capacity", 500);
        assert tooltip(packet).size() == 1 : "A channel without a valve must not show a valve status";
        data.putBoolean("sprinkler", true);
        lines = tooltip(packet);
        assert lines.size() == 1 && key(lines.get(0)).endsWith("sprinkler.inactive");
        data.putBoolean("active", true);
        assert key(tooltip(packet).get(0)).endsWith("sprinkler.active");

        List<Object[]> registrations = new ArrayList<>();
        IWailaCommonRegistration registration = (IWailaCommonRegistration) Proxy.newProxyInstance(
                IWailaCommonRegistration.class.getClassLoader(), new Class[]{IWailaCommonRegistration.class},
                (proxy, method, values) -> { registrations.add(values); return null; });
        new AgriCraftJadePlugin().register(registration);
        assert registrations.size() == 1;
        assert registrations.get(0)[0] == IrrigationComponentProvider.INSTANCE;
        System.out.println("Jade irrigation: server contents, full tank capacity, valves, sprinkler status and registration passed.");
    }
}
