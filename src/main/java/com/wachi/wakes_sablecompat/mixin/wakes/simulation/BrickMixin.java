package com.wachi.wakes_sablecompat.mixin.wakes.simulation;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.simulation.Brick;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.leclowndu93150.wakes.simulation.WakeNode;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.TextureUtil;
import com.wachi.wakes_sablecompat.accessor.BrickAccessor;
import com.wachi.wakes_sablecompat.accessor.LightTextureAccessor;
import com.wachi.wakes_sablecompat.accessor.WakeNodeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.Level;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Brick.class)
public abstract class BrickMixin implements BrickAccessor {

    @Shadow public long imgPtr;

    @Shadow
    public boolean hasPopulatedPixels;

    @Shadow
    public abstract void initTexture(int res);

    @Shadow
    public int texRes;
    @Shadow
    @Final
    public int dim;

    @Shadow
    public abstract WakeNode get(int x, int z);

    @Unique public int wakesSableCompat$glTexId = -1;
    @Unique public boolean wakesSableCompat$textureDirty = false;

    @Override
    public int wakesSableCompat$getGlTexId() {
        return wakesSableCompat$glTexId;
    }

    @Override
    public void wakesSableCompat$setGlTexId(int wakesSableCompat$glTexId) {
        this.wakesSableCompat$glTexId = wakesSableCompat$glTexId;
    }

    @Override
    public void wakesSableCompat$setTextureDirty(boolean dirty){
        this.wakesSableCompat$textureDirty = dirty;
    }

    @Override
    public boolean wakesSableCompat$isTextureDirty(){
        return this.wakesSableCompat$textureDirty;
    }

    @Inject(method = "initTexture", at = @At("TAIL"), remap = false)
    public void postInitTexture(int res, CallbackInfo ci, @Local(name = "size") long size){
        MemoryUtil.memSet(this.imgPtr, 0, size);
        wakesSableCompat$textureDirty = true;
    }

    @Inject(method = "deallocTexture", at = @At("TAIL"), remap = false)
    public void postDeallocTexture(CallbackInfo ci){
        if (wakesSableCompat$glTexId != -1) {
            TextureUtil.releaseTextureId(wakesSableCompat$glTexId);
            wakesSableCompat$glTexId = -1;
        }

        this.hasPopulatedPixels = false;
        wakesSableCompat$textureDirty = false;
    }

    /**
     * @author wachi
     * @reason Add per-node update rate skipping inside pixel population loop.
     */
    @Overwrite(remap = false)
    public void populatePixels() {
        if (this.imgPtr == -1L) {
            this.initTexture(WakeHandler.resolution.res);
        }

        if(!(Minecraft.getInstance().level instanceof Level world)) return;
        long gameTime = world.getGameTime();

        for(int z = 0; z < this.dim; ++z) {
            for(int x = 0; x < this.dim; ++x) {
                WakeNode node = this.get(x, z);
                if(node != null && (gameTime + node.hashCode()) % ((WakeNodeAccessor)node).wakesSableCompat$getUpdateRate() != 0) continue;

                int lightCol =  LightTexture.FULL_BRIGHT;
                int fluidColor = 0;
                float opacity = 0.0F;
                if (node != null) {
                    fluidColor = BiomeColors.getAverageWaterColor(world, node.blockPos());
                    int lightCoordinate = LevelRenderer.getLightColor(world, node.blockPos());
                    lightCol = ((LightTextureAccessor) Minecraft.getInstance().gameRenderer.lightTexture())
                            .wakesSableCompat$getLightColor()
                            .getPixelRGBA(
                                    LightTexture.block(lightCoordinate),
                                    LightTexture.sky(lightCoordinate)
                            );
                    opacity = (float)((-Math.pow(node.t, 2.0F) + (double)1.0F) * (Double) WakesConfig.APPEARANCE.wakeOpacity.get());
                }

                long nodeOffset = (long)this.texRes * 4L * ((long)z * (long)this.dim * (long)this.texRes + (long)x);

                for(int r = 0; r < this.texRes; ++r) {
                    for(int c = 0; c < this.texRes; ++c) {
                        int color = 0;
                        if (node != null) {
                            color = node.simulationNode.getPixelColor(c, r, fluidColor, lightCol, opacity);
                        }

                        long pixelOffset = 4L * ((long)r * (long)this.dim * (long)this.texRes + (long)c);
                        MemoryUtil.memPutInt(this.imgPtr + nodeOffset + pixelOffset, color);
                    }
                }
            }
        }

        this.hasPopulatedPixels = true;
        this.wakesSableCompat$textureDirty = true;
    }
}
