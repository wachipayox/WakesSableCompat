package com.wachi.wakes_sablecompat;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SUBLEVEL_TRAILS = BUILDER
            .comment("Whether to render trails behind physics objects in water (The whole mod purpose)")
            .define("sublevel_water_trails", true);

    public static final ModConfigSpec.DoubleValue TRIGGER_VELOCITY = BUILDER
            .comment("Minimum velocity to start drawing trails")
            .defineInRange("minimum_velocity", 0.6, 0.1, Double.MAX_VALUE);

    public static final ModConfigSpec.IntValue MAX_TRAILS_PER_SHIP = BUILDER
            .comment("Max number of trails a ship can have")
            .comment("(heavy performance impact)")
            .defineInRange("max_trails_per_ship", 24, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue WAKES_TICK_RATE = BUILDER
            .comment("How many ticks between wakes calculations")
            .comment("The higher this option is set, the worse the quality of the trails will be.")
            .comment("(medium performance impact)")
            .defineInRange("wakes_tick_rate", 1, 1, Integer.MAX_VALUE);

    static final ModConfigSpec SPEC = BUILDER.build();
}
