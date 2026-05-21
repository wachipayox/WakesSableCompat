package com.wachi.wakes_sablecompat.mixin.wakes.simulation;

import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.leclowndu93150.wakes.simulation.WakeNode;
import com.wachi.wakes_sablecompat.accessor.WakeNodeAccessor;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WakeNode.class)
public class WakeNodeMixin implements WakeNodeAccessor {

    @Shadow
    public static int maxAge;
    @Unique private Integer wakesSableCompat$maxAge = null;
    @Unique private int wakesSableCompat$updateRate = 1;

    @Override
    public void wakesSableCompat$setMaxAge(int maxAge) {
        this.wakesSableCompat$maxAge = maxAge;
    }

    @Override
    public int wakesSableCompat$getMaxAge() {
        return this.wakesSableCompat$maxAge != null ? this.wakesSableCompat$maxAge : maxAge;
    }

    @Override
    public void wakesSableCompat$setUpdateRate(int rate) {
        this.wakesSableCompat$updateRate = rate;
    }

    @Override
    public int wakesSableCompat$getUpdateRate() {
        return this.wakesSableCompat$updateRate;
    }

    @Redirect(
            method = "tick",
            at = @At(
                    value = "FIELD",
                    target = "com/leclowndu93150/wakes/simulation/WakeNode.maxAge : I",
                    opcode = Opcodes.GETSTATIC,
                    remap = false
            ), remap = false
    )
    private int replaceMaxAge() {
        return wakesSableCompat$getMaxAge();
    }

    @Redirect(
            method = "floodFill",
            at = @At(
                    value = "INVOKE",
                    target = "com/leclowndu93150/wakes/simulation/WakeHandler.insert (Lcom/leclowndu93150/wakes/simulation/WakeNode;)V",
                    remap = false
            ), remap = false
    )
    public void addTickRateAndAgeToChildren(WakeHandler instance, WakeNode node){
        ((WakeNodeAccessor) node).wakesSableCompat$setMaxAge(wakesSableCompat$getMaxAge());
        ((WakeNodeAccessor) node).wakesSableCompat$setUpdateRate(wakesSableCompat$getUpdateRate());
        instance.insert(node);
    }
}
