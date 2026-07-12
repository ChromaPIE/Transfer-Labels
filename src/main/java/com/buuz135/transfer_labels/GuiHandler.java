package com.buuz135.transfer_labels;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.buuz135.transfer_labels.container.LabelContainer;
import com.buuz135.transfer_labels.storage.LabelBlock;
import com.buuz135.transfer_labels.storage.LabelInstance;
import com.buuz135.transfer_labels.util.BlockPos;

import cpw.mods.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler {

    // The gui id encodes the label's facing; x/y/z carry the label position.
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        LabelInstance instance = findLabel(
            com.buuz135.transfer_labels.storage.LabelStorage.getStorageFor(world),
            new BlockPos(x, y, z),
            ForgeDirection.getOrientation(id));
        return instance == null ? null : new LabelContainer(instance, player.inventory);
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return TransferLabels.proxy
            .createLabelGui(player, world, new BlockPos(x, y, z), ForgeDirection.getOrientation(id));
    }

    public static LabelInstance findLabel(com.buuz135.transfer_labels.storage.LabelStorage storage, BlockPos pos,
        ForgeDirection direction) {
        LabelBlock labelBlock = storage.getLabelBlocksMap()
            .get(pos);
        if (labelBlock == null) return null;
        return labelBlock.getLabels()
            .get(direction);
    }
}
