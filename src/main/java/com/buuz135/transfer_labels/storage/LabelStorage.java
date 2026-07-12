package com.buuz135.transfer_labels.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraftforge.common.util.ForgeDirection;

import com.buuz135.transfer_labels.util.BlockPos;

public class LabelStorage extends WorldSavedData {

    public static final String DATA_NAME = "TRANSFER_LABELS_LABEL_STORAGE";

    private final HashMap<BlockPos, LabelBlock> labelBlocks = new HashMap<>();
    private World level;
    private NBTTagCompound pendingLoad;

    public LabelStorage(String name) {
        super(name);
    }

    public LabelStorage(World level) {
        super(DATA_NAME);
        this.level = level;
    }

    public static LabelStorage getStorageFor(World level) {
        LabelStorage storage = (LabelStorage) level.perWorldStorage.loadData(LabelStorage.class, DATA_NAME);
        if (storage == null) {
            storage = new LabelStorage(level);
            level.perWorldStorage.setData(DATA_NAME, storage);
        }
        storage.bindLevel(level);
        return storage;
    }

    private void bindLevel(World level) {
        this.level = level;
        if (this.pendingLoad != null) {
            NBTTagCompound tag = this.pendingLoad;
            this.pendingLoad = null;
            this.load(tag);
        }
    }

    public static List<LabelBlock> getNearbyLabels(World level, BlockPos pos, int distance) {
        List<LabelBlock> result = new ArrayList<>();
        for (LabelBlock labelBlock : getStorageFor(level).getLabelBlocks()) {
            if (labelBlock.getPos()
                .distSqr(pos) <= distance) {
                result.add(labelBlock);
            }
        }
        return result;
    }

    public static void addLabel(World level, BlockPos blockPos, ForgeDirection direction, ItemStack label) {
        LabelStorage storage = getStorageFor(level);
        storage.labelBlocks.computeIfAbsent(blockPos, pos -> new LabelBlock(pos, level))
            .setLabel(direction, label);
        storage.markDirty();
    }

    public static void removeLabel(EntityPlayer player, World level, BlockPos blockPos, ForgeDirection direction) {
        LabelStorage storage = getStorageFor(level);
        LabelBlock labelBlock = storage.labelBlocks.computeIfAbsent(blockPos, pos -> new LabelBlock(pos, level));
        labelBlock.remove(player, direction);
        if (labelBlock.getLabels()
            .isEmpty()) {
            storage.labelBlocks.remove(blockPos);
        }
        storage.markDirty();
    }

    public List<LabelBlock> getLabelBlocks() {
        return new ArrayList<>(labelBlocks.values());
    }

    public HashMap<BlockPos, LabelBlock> getLabelBlocksMap() {
        return labelBlocks;
    }

    public NBTTagCompound saveNearby(BlockPos pos, int distance) {
        NBTTagCompound compoundTag = new NBTTagCompound();
        for (LabelBlock labelBlock : getLabelBlocks()) {
            if (labelBlock.getPos()
                .distSqr(pos) <= distance) {
                compoundTag.setTag(
                    labelBlock.getPos()
                        .asLong() + "",
                    labelBlock.serializeNBT());
            }
        }
        return compoundTag;
    }

    @SuppressWarnings("unchecked")
    public void loadNearby(BlockPos anchor, int distance, NBTTagCompound compoundTag) {
        List<LabelBlock> nearby = new ArrayList<>();
        for (LabelBlock labelBlock : getLabelBlocks()) {
            if (labelBlock.getPos()
                .distSqr(anchor) <= distance) {
                nearby.add(labelBlock);
            }
        }
        List<BlockPos> visitedPositions = new ArrayList<>();
        for (String s : (Set<String>) compoundTag.func_150296_c()) {
            BlockPos pos = BlockPos.fromLong(Long.parseLong(s));
            visitedPositions.add(pos);
            if (labelBlocks.containsKey(pos)) {
                labelBlocks.get(pos)
                    .deserializeNBT(compoundTag.getCompoundTag(s));
            } else {
                LabelBlock label = new LabelBlock(pos, level);
                label.deserializeNBT(compoundTag.getCompoundTag(s));
                labelBlocks.put(pos, label);
            }
        }
        for (LabelBlock labelBlock : nearby) {
            if (!visitedPositions.contains(labelBlock.getPos())) {
                labelBlocks.remove(labelBlock.getPos());
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound compoundTag) {
        NBTTagCompound labels = new NBTTagCompound();
        for (BlockPos pos : labelBlocks.keySet()) {
            labels.setTag(
                pos.asLong() + "",
                labelBlocks.get(pos)
                    .serializeNBT());
        }
        compoundTag.setTag("Labels", labels);
    }

    @Override
    public void readFromNBT(NBTTagCompound compoundTag) {
        // The world reference is not available yet when the save handler instantiates this
        // via reflection, so defer parsing until getStorageFor() binds the world.
        if (this.level != null) {
            load(compoundTag);
        } else {
            this.pendingLoad = (NBTTagCompound) compoundTag.copy();
        }
    }

    @SuppressWarnings("unchecked")
    public void load(NBTTagCompound compoundTag) {
        NBTTagCompound labels = compoundTag.getCompoundTag("Labels");
        List<BlockPos> visitedPositions = new ArrayList<>();
        for (String s : (Set<String>) labels.func_150296_c()) {
            BlockPos pos = BlockPos.fromLong(Long.parseLong(s));
            visitedPositions.add(pos);
            if (this.labelBlocks.containsKey(pos)) {
                this.labelBlocks.get(pos)
                    .deserializeNBT(labels.getCompoundTag(s));
            } else {
                LabelBlock label = new LabelBlock(pos, level);
                label.deserializeNBT(labels.getCompoundTag(s));
                labelBlocks.put(pos, label);
            }
        }
        this.labelBlocks.keySet()
            .removeIf(pos -> !visitedPositions.contains(pos));
    }

    public World getLevel() {
        return level;
    }
}
