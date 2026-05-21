package com.wachi.wakes_sablecompat;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class SubLevelWakesConfig {

    private static boolean enabled;
    private static double minVel;
    private static int trailsTickRate, maxTrailsPerShip;

    public static boolean isEnabled() {return enabled;}

    public static boolean enoughVelocity(double vel){
        return vel > minVel;
    }

    public static int wakesTickRate(){
        return trailsTickRate;
    }

    public static int calculateBlocksTickRate(){
        return 2;
    }

    public static int maxWakesPerTick(){
        return maxTrailsPerShip;
    }

    public static double velMul(){
        return 0.4;
    }

    public static int minAge(){
        return 8;
    }

    @SubscribeEvent
    public static void configReload(ModConfigEvent.Reloading event) {
        if(event.getConfig().getModId().equals(WakesSableCompat.MODID))
            if (event.getConfig().getType().equals(ModConfig.Type.CLIENT))
                loadClientConfig();
    }

    public static void loadClientConfig(){
        enabled = ClientConfig.SUBLEVEL_TRAILS.getAsBoolean();
        minVel = ClientConfig.TRIGGER_VELOCITY.getAsDouble();
        trailsTickRate = ClientConfig.WAKES_TICK_RATE.getAsInt();
        maxTrailsPerShip = ClientConfig.MAX_TRAILS_PER_SHIP.getAsInt();
    }

}
