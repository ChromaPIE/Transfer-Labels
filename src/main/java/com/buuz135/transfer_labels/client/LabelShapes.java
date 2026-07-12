package com.buuz135.transfer_labels.client;

import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;

public class LabelShapes {

    public static final AxisAlignedBB NORTH = AxisAlignedBB.getBoundingBox(0.0D, 0.0D, -0.005D, 1.0D, 1.0D, 0.005D);
    public static final AxisAlignedBB SOUTH = AxisAlignedBB.getBoundingBox(0.0D, 0.0D, 0.995D, 1.0D, 1.0D, 1.005D);
    public static final AxisAlignedBB EAST = AxisAlignedBB.getBoundingBox(0.995D, 0.0D, 0.0D, 1.005D, 1.0D, 1.0D);
    public static final AxisAlignedBB WEST = AxisAlignedBB.getBoundingBox(-0.005D, 0.0D, 0.0D, 0.005D, 1.0D, 1.0D);
    public static final AxisAlignedBB UP = AxisAlignedBB.getBoundingBox(0.0D, 0.995D, 0.0D, 1.0D, 1.005D, 1.0D);
    public static final AxisAlignedBB DOWN = AxisAlignedBB.getBoundingBox(0.0D, -0.005D, 0.0D, 1.0D, 0.005D, 1.0D);

    public static AxisAlignedBB get(ForgeDirection direction) {
        switch (direction) {
            case NORTH:
                return NORTH;
            case SOUTH:
                return SOUTH;
            case EAST:
                return EAST;
            case WEST:
                return WEST;
            case UP:
                return UP;
            case DOWN:
                return DOWN;
            default:
                return null;
        }
    }
}
