package com.wachi.wakes_sablecompat;

import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT)
public class WakesSableCompatClient {

    public static Map<ClientSubLevel, Double> lastTickDistance = new HashMap<>();

    public static ClientSubLevel nearest = null;

    public static boolean isNearest(ClientSubLevel sublevel){
        return nearest != null && nearest == sublevel;
    }

    @SubscribeEvent
    public static void onClientPreTick(ClientTickEvent.Pre event){
        lastTickDistance.clear();
    }

    @SubscribeEvent
    public static void onClientPostTick(ClientTickEvent.Post event){
        double minDist = 0;
        ClientSubLevel subLevel = null;

        for (var entry : lastTickDistance.entrySet()) {
            if(subLevel != null && entry.getValue() >= minDist) continue;
            minDist = entry.getValue();
            subLevel = entry.getKey();
        }
        nearest = subLevel;
    }
}
