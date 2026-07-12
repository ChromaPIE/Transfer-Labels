package com.buuz135.transfer_labels.storage.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.World;

import com.buuz135.transfer_labels.storage.LabelBlock;
import com.buuz135.transfer_labels.storage.LabelStorage;
import com.buuz135.transfer_labels.util.BlockPos;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LabelClientStorage {

    public static LabelStorage LABELS;

    public static List<LabelBlock> getLabelBlocks(World clientLevel) {
        return getStorage(clientLevel).getLabelBlocks();
    }

    public static LabelStorage getStorage(World clientLevel) {
        if (LABELS == null || LABELS.getLevel() != clientLevel) {
            LABELS = new LabelStorage(clientLevel);
        }
        return LABELS;
    }

    public static List<LabelBlock> getNearbyLabels(World level, BlockPos pos, int distance) {
        List<LabelBlock> result = new ArrayList<>();
        for (LabelBlock labelBlock : getLabelBlocks(level)) {
            if (labelBlock.getPos()
                .distSqr(pos) <= distance) {
                result.add(labelBlock);
            }
        }
        return result;
    }
}
