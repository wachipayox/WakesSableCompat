package com.wachi.wakes_sablecompat.mixin.sable;

import com.wachi.wakes_sablecompat.SubLevelWakesConfig;
import com.leclowndu93150.wakes.WakesClient;
import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.leclowndu93150.wakes.simulation.WakeNode;
import com.wachi.wakes_sablecompat.WakesSableCompatClient;
import com.wachi.wakes_sablecompat.accessor.WakeHandlerAccessor;
import com.wachi.wakes_sablecompat.accessor.WakeNodeAccessor;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.ClientLevelPlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(ClientSubLevel.class)
public abstract class ClientSublevelMixin {

    @Shadow public abstract Level getLevel();
    @Shadow public abstract ClientLevelPlot getPlot();

    @Shadow @Final private Pose3d renderPose;
    @Shadow public abstract BoundingBox3dc boundingBox();

    @Unique Double wakes$cameraDistance = null;

    @Unique int wakes$wakesInLastFrame = 0;

    @Unique Double wakes$lastWaterY = null;
    @Unique private List<BlockPos> wakes$solidOrdered = null;
    @Unique private Map<BlockPos, Double> wakes$widthMap = null;

    @Inject(at = @At("TAIL"), method = "tick", remap = false)
    private void tick(CallbackInfo info) {
        if (!getLevel().isClientSide || WakesConfig.GENERAL.disableMod.get() || !SubLevelWakesConfig.isEnabled()) return;

        boolean isPlayerShip = wakes$isPlayerShip();

        var gameTime = getLevel().getGameTime();
        if (gameTime % ((long)
                SubLevelWakesConfig.wakesTickRate() * (isPlayerShip ? 1 : 4)) != 0)
            return;

        WakeHandler wakeHandler = WakeHandler.getInstance(getLevel()).orElse(null);
        if (wakeHandler == null) return;

        Vec3 vel = Sable.HELPER.getVelocity(getLevel(), getPlot().getCenterBlock().getCenter());
        double velocity = vel.length();

        if (!SubLevelWakesConfig.enoughVelocity(velocity)) return;

        if ((gameTime % ((long) SubLevelWakesConfig.calculateBlocksTickRate()) == 0))
            wakes$lastWaterY = wakes$getWaterY();
        Double waterY = wakes$lastWaterY;

        if (waterY == null) return;
        WakesSableCompatClient.lastTickDistance.put(getPlot().getSubLevel(), wakes$getCameraDistance());

        AABB box = boundingBox().toMojang();
        box = box.setMaxY(waterY).setMinY(waterY);

        AABB subLevelBox = wakes$worldAabbToLocalAabb(box).inflate(0.1);

        int i = SubLevelWakesConfig.maxWakesPerTick() / (isPlayerShip ? 1 : 2);
        int[] counter = {0};

        List<BlockPos> solidOrdered;
        Map<BlockPos, Double> widthMap;

        if (wakes$solidOrdered == null || gameTime % ((long) SubLevelWakesConfig.calculateBlocksTickRate()) == 0) {
            solidOrdered = new ArrayList<>();
            widthMap = new HashMap<>();
            AABB finalBox = box;
            BlockPos.betweenClosedStream(subLevelBox)
                    .forEach(blockPos -> {
                        BlockState bState = getLevel().getBlockState(blockPos);
                        ;
                        if (!bState.isAir()) {
                            Vec3 curPos = Sable.HELPER.projectOutOfSubLevel(getLevel(), blockPos.getCenter());
                            if (curPos.y > finalBox.minY - 0.5 && curPos.y < finalBox.maxY + 0.5) {
                                var bPos = blockPos.immutable();
                                solidOrdered.add(bPos);

                                var shape = bState.getShape(getLevel(), blockPos);
                                if (!shape.isEmpty()) widthMap.put(bPos, shape.bounds().getXsize());
                            }
                        }
                    });

            solidOrdered.sort(
                    Comparator.comparingDouble(
                            (BlockPos bPos) -> bPos.getCenter().distanceTo(subLevelBox.getCenter())
                    ).reversed()
            );
            wakes$solidOrdered = solidOrdered;
            wakes$widthMap = widthMap;
        } else {
            solidOrdered = wakes$solidOrdered;
            widthMap = wakes$widthMap;
        }
        solidOrdered.forEach(blockPos -> {
            double width = widthMap.getOrDefault(blockPos, 0.5);
            Vec3 velIn = renderPose.transformNormalInverse(
                    Sable.HELPER.getVelocity(getLevel(), blockPos.getCenter())
                            .normalize()
            );

            Vec3 curPos = Sable.HELPER.projectOutOfSubLevel(
                    getLevel(), blockPos.getCenter()
            );

            if (
                    widthMap.containsKey(BlockPos.containing(renderPose.transformPositionInverse(curPos.add(1, 0, 0))))
                            && widthMap.containsKey(BlockPos.containing(renderPose.transformPositionInverse(curPos.add(-1, 0, 0))))
                            && widthMap.containsKey(BlockPos.containing(renderPose.transformPositionInverse(curPos.add(0, 0, 1))))
                            && widthMap.containsKey(BlockPos.containing(renderPose.transformPositionInverse(curPos.add(0, 0, -1))))
            ) {
                return;
            }

            if (counter[0] < i) {
                counter[0]++;
            } else return;

            velIn = velIn.multiply(width, 0, width);
            var renderPos = curPos.add(velIn);
            var prevPos = renderPos.subtract(velIn);

            for (WakeNode node : WakeNode.Factory.thickNodeTrail(
                    prevPos.x, prevPos.z,
                    renderPos.x, renderPos.z,
                    (int) Math.floor(waterY),
                    (float) WakesConfig.GENERAL.initialStrength.get(),
                    velocity,
                    (float) width)) {
                ((WakeNodeAccessor)node).wakesSableCompat$setMaxAge((int) Math.clamp(WakeNode.maxAge * (velocity * SubLevelWakesConfig.velMul()), SubLevelWakesConfig.minAge(), 30));
                ((WakeNodeAccessor)node).wakesSableCompat$setUpdateRate(wakes$getFrameRate());
                ((WakeHandlerAccessor) wakeHandler).wakesSableCompat$insertFromSublevel(node);
            }
        });
        wakes$wakesInLastFrame = counter[0];
    }

    @Unique
    private boolean wakes$isPlayerShip(){
        return Minecraft.getInstance().player instanceof Entity ent
                && (Sable.HELPER.getTrackingOrVehicleSubLevel(ent) instanceof SubLevel cSL ?
                cSL == getPlot().getSubLevel() : WakesSableCompatClient.isNearest(getPlot().getSubLevel()));
    }

    @Unique
    private int wakes$getFrameRate(){
        int wakesInFrame = wakes$wakesInLastFrame;

        if(wakes$isPlayerShip()) {
            if (wakesInFrame <= 1) return 1;
            else if (wakesInFrame <= 3) return 2;
            else return 5;
        }

        var dist = wakes$getCameraDistance();
        if(dist < 64) {
            if (wakesInFrame <= 1) return 3;
            else if (wakesInFrame <= 3) return 6;
            else return 15;
        }
        else return 24;
    }



    @Unique
    private double wakes$getCameraDistance(){
        if(!(Minecraft.getInstance().level instanceof ClientLevel level))
            return 0.0;

        var gameTime = level.getGameTime();
        if(wakes$cameraDistance == null || (gameTime + hashCode()) % 20 == 0) {
            WakesClient.LOGGER.debug("calculating camera distance");
            wakes$cameraDistance = boundingBox().toMojang().getCenter().distanceTo(
                    Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()
            );
        }

        return wakes$cameraDistance;
    }

    /// @returns first y coord with water surface (starting from bottom) founded in the sublevel hitbox (counts the fluidheight)
    @Unique
    public Double wakes$getWaterY(){
        AABB box = boundingBox().toMojang();

        BlockPos.MutableBlockPos bPos = new BlockPos.MutableBlockPos();
        for (double y = box.minY; y < box.maxY; y++)
            for (double x = box.minX; x < box.maxX; x++)
                for (double z = box.minZ; z < box.maxZ; z++) {
                    bPos.set(x, y, z);

                    if (
                            getLevel().isFluidAtPosition(bPos, fluidState ->
                                    WakesConfig.getFluidWhitelist().contains(fluidState.getType())
                                    && fluidState.isSource()
                                    && getLevel().isEmptyBlock(bPos.above())
                    )) {
                        double posY = Math.floor(y) + getLevel().getFluidState(bPos).getHeight(getLevel(), bPos);
                        if(posY > box.minY && posY < box.maxY) return posY;
                    }
                }
        return null;
    }

    @Unique
    private AABB wakes$worldAabbToLocalAabb(AABB worldBox) {
        Vec3[] corners = new Vec3[] {
                new Vec3(worldBox.minX, worldBox.minY, worldBox.minZ),
                new Vec3(worldBox.minX, worldBox.minY, worldBox.maxZ),
                new Vec3(worldBox.minX, worldBox.maxY, worldBox.minZ),
                new Vec3(worldBox.minX, worldBox.maxY, worldBox.maxZ),
                new Vec3(worldBox.maxX, worldBox.minY, worldBox.minZ),
                new Vec3(worldBox.maxX, worldBox.minY, worldBox.maxZ),
                new Vec3(worldBox.maxX, worldBox.maxY, worldBox.minZ),
                new Vec3(worldBox.maxX, worldBox.maxY, worldBox.maxZ)
        };

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (Vec3 corner : corners) {
            Vec3 local = renderPose.transformPositionInverse(corner);

            minX = Math.min(minX, local.x);
            minY = Math.min(minY, local.y);
            minZ = Math.min(minZ, local.z);

            maxX = Math.max(maxX, local.x);
            maxY = Math.max(maxY, local.y);
            maxZ = Math.max(maxZ, local.z);
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }


}
