package com.buuz135.transfer_labels.item;

import java.util.Locale;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.buuz135.transfer_labels.TransferLabels;
import com.buuz135.transfer_labels.filter.ILabelFilter;
import com.buuz135.transfer_labels.network.LabelSyncMessage;
import com.buuz135.transfer_labels.network.PacketHandler;
import com.buuz135.transfer_labels.storage.LabelStorage;
import com.buuz135.transfer_labels.util.BlockPos;

public abstract class TransferLabelItem extends Item {

    private final Mode mode;
    private final ResourceLocation texture;

    public TransferLabelItem(String type, Mode mode) {
        super();
        this.mode = mode;
        String registryName = type + "_"
            + this.mode.name()
                .toLowerCase(Locale.ROOT)
            + "_transfer_label";
        this.texture = new ResourceLocation(TransferLabels.MODID, "textures/items/" + registryName + ".png");
        this.setUnlocalizedName(TransferLabels.MODID + "." + registryName);
        this.setTextureName(TransferLabels.MODID + ":" + registryName);
        this.setCreativeTab(TransferLabels.TAB);
    }

    public Mode getMode() {
        return mode;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            ItemStack label = stack.copy();
            label.stackSize = 1;
            LabelStorage.addLabel(world, new BlockPos(x, y, z), ForgeDirection.getOrientation(side), label);
            stack.stackSize--;
            if (player instanceof EntityPlayerMP) {
                BlockPos playerPos = new BlockPos(
                    MathHelper.floor_double(player.posX),
                    MathHelper.floor_double(player.posY) - 1,
                    MathHelper.floor_double(player.posZ));
                PacketHandler.INSTANCE.sendTo(
                    new LabelSyncMessage(
                        LabelStorage.getStorageFor(world)
                            .saveNearby(playerPos, 100),
                        playerPos,
                        16),
                    (EntityPlayerMP) player);
            }
            return true;
        }

        return false;
    }

    public abstract ILabelFilter<?> createFilter();

    public ResourceLocation getTexture() {
        return texture;
    }

    public enum Mode {
        INSERT,
        EXTRACT;
    }
}
