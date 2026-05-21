package com.wachi.wakes_sablecompat.mixin.wakes;

import com.leclowndu93150.wakes.config.WakesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Entity.class, priority = 1500)
public abstract class WakeSpawnerMixin {

    @Shadow
    public abstract AABB getBoundingBox();

    @Shadow
    public abstract double getX();

    @Shadow
    public abstract double getZ();

    @Shadow
    public abstract Level level();

    @Inject(
            method = "onFluidSurface()Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void sablewakes$replaceOnFluidSurface(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(wakesSableCompat$onFluidSurface());
    }

    @Unique
    private boolean wakesSableCompat$onFluidSurface() {
        AABB box = getBoundingBox();
        double hitboxMaxY = box.maxY;

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        for (int y = (int) Math.floor(box.minY); y <= (int) Math.floor(hitboxMaxY); y++) {
            blockPos.set(this.getX(), y, this.getZ());
            FluidState fluidState = level().getFluidState(blockPos), aboveFluidState = level().getFluidState(new BlockPos((int) this.getX(), y+1, (int) this.getZ()));

            if (!fluidState.isEmpty() && aboveFluidState.isEmpty() && WakesConfig.getFluidWhitelist().contains(fluidState.getType())) {
                double fluidHeight = (float)blockPos.getY() + fluidState.getHeight(level(), blockPos);
                return hitboxMaxY > fluidHeight;
            }
        }

        return false;
    }
}
