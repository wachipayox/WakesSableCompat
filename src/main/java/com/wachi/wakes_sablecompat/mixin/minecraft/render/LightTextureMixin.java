package com.wachi.wakes_sablecompat.mixin.minecraft.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.wachi.wakes_sablecompat.accessor.LightTextureAccessor;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LightTexture.class)
public class LightTextureMixin implements LightTextureAccessor {

    @Shadow @Final private NativeImage lightPixels;

    @Override
    public NativeImage wakesSableCompat$getLightColor(){
        return lightPixels;
    }
}
