package com.wachi.wakes_sablecompat.accessor;

public interface BrickAccessor {

    int wakesSableCompat$getGlTexId();

    void wakesSableCompat$setGlTexId(int wakesSableCompat$glTexId);

    void wakesSableCompat$setTextureDirty(boolean dirty);

    boolean wakesSableCompat$isTextureDirty();
}
