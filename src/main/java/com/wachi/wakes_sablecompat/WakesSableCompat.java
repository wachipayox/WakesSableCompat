package com.wachi.wakes_sablecompat;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(WakesSableCompat.MODID)
public class WakesSableCompat {
    public static final String MODID = "wakes_sablecompat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WakesSableCompat(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

        modEventBus.addListener(this::client);
    }

    public void client(FMLClientSetupEvent event){
        SubLevelWakesConfig.loadClientConfig();
    }
}
