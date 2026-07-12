package com.buuz135.transfer_labels.client;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.apache.commons.lang3.tuple.Pair;

import com.buuz135.transfer_labels.storage.LabelBlock;
import com.buuz135.transfer_labels.util.BlockPos;

public class RayTraceUtils {

    public static Vec3 getEyePosition(EntityPlayer player) {
        // Client player posY already sits at eye level; server player posY is at the feet.
        double y = player.posY;
        if (player.worldObj.isRemote) {
            y += player.getEyeHeight() - player.getDefaultEyeHeight();
        } else {
            y += player.getEyeHeight();
        }
        return Vec3.createVectorHelper(player.posX, y, player.posZ);
    }

    public static Pair<LabelBlock, ForgeDirection> rayTraceLabels(List<LabelBlock> nearbyLabels, World world,
        EntityPlayer player, double blockReachDistance, BlockPos pos) {
        Vec3 eye = getEyePosition(player);
        Vec3 look = player.getLook(1.0F);
        Vec3 end = eye.addVector(
            look.xCoord * blockReachDistance,
            look.yCoord * blockReachDistance,
            look.zCoord * blockReachDistance);
        LabelBlock closest = null;
        ForgeDirection direction = null;
        double distance = Double.MAX_VALUE;
        if (pos != null) {
            Block block = world.getBlock(pos.getX(), pos.getY(), pos.getZ());
            MovingObjectPosition result = block.collisionRayTrace(world, pos.getX(), pos.getY(), pos.getZ(), eye, end);
            if (result != null && result.hitVec != null) {
                distance = eye.distanceTo(result.hitVec);
            }
        }
        for (LabelBlock nearbyLabel : nearbyLabels) {
            for (ForgeDirection direction2 : ForgeDirection.VALID_DIRECTIONS) {
                AxisAlignedBB shape = nearbyLabel.collectShapes(direction2);
                if (shape != null) {
                    MovingObjectPosition result = shape.calculateIntercept(eye, end);
                    if (result != null && result.hitVec != null && eye.distanceTo(result.hitVec) < distance) {
                        closest = nearbyLabel;
                        direction = direction2;
                        distance = eye.distanceTo(result.hitVec);
                    }
                }
            }
        }
        if (closest != null) {
            return Pair.of(closest, direction);
        }
        return null;
    }
}
