package com.wachi.wakes_sablecompat.mixin.wakes.simulation;

import com.leclowndu93150.wakes.simulation.QuadTree;
import com.leclowndu93150.wakes.simulation.QueueSet;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.leclowndu93150.wakes.simulation.WakeNode;
import com.wachi.wakes_sablecompat.accessor.WakeHandlerAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(WakeHandler.class)
public abstract class WakeHandlerMixin implements WakeHandlerAccessor {

    @Shadow
    public static boolean resolutionResetScheduled;

    @Shadow
    protected abstract int getArrayIndex(int y);

    @Shadow
    private QuadTree[] trees;

    @Shadow
    private QueueSet<WakeNode>[] toBeInserted;

    @Override
    public void wakesSableCompat$insertFromSublevel(WakeNode node) {
        if (resolutionResetScheduled) return;

        int i = this.getArrayIndex(node.y);
        if (i < 0) return;

        if (this.trees[i] == null) this.trees[i] = new QuadTree(node.y);

        this.toBeInserted[i].add(node);
    }
}
