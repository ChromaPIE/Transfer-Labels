package com.buuz135.transfer_labels.util;

import net.minecraftforge.common.util.ForgeDirection;

public final class BlockPos {

    private final int x;
    private final int y;
    private final int z;

    public BlockPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public BlockPos offset(ForgeDirection direction) {
        return new BlockPos(x + direction.offsetX, y + direction.offsetY, z + direction.offsetZ);
    }

    public double distSqr(BlockPos other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public long asLong() {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (z & 0x3FFFFFF);
    }

    public static BlockPos fromLong(long serialized) {
        int x = (int) (serialized >> 38);
        int y = (int) ((serialized << 26) >> 52);
        int z = (int) ((serialized << 38) >> 38);
        return new BlockPos(x, y, z);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BlockPos)) return false;
        BlockPos other = (BlockPos) obj;
        return x == other.x && y == other.y && z == other.z;
    }

    @Override
    public int hashCode() {
        return (y + z * 31) * 31 + x;
    }

    @Override
    public String toString() {
        return "BlockPos{" + x + ", " + y + ", " + z + "}";
    }
}
