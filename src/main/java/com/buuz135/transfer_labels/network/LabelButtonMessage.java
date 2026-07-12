package com.buuz135.transfer_labels.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.buuz135.transfer_labels.storage.LabelBlock;
import com.buuz135.transfer_labels.storage.LabelInstance;
import com.buuz135.transfer_labels.storage.LabelStorage;
import com.buuz135.transfer_labels.util.BlockPos;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Client -> server GUI interaction, the counterpart of Titanium's ButtonClickNetworkMessage + locator. */
public class LabelButtonMessage implements IMessage {

    public BlockPos pos;
    public ForgeDirection direction;
    public int buttonId;
    public NBTTagCompound tag;

    public LabelButtonMessage() {}

    public LabelButtonMessage(BlockPos pos, ForgeDirection direction, int buttonId, NBTTagCompound tag) {
        this.pos = pos;
        this.direction = direction;
        this.buttonId = buttonId;
        this.tag = tag;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        this.direction = ForgeDirection.getOrientation(buf.readByte());
        this.buttonId = buf.readInt();
        this.tag = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        buf.writeByte(direction.ordinal());
        buf.writeInt(buttonId);
        ByteBufUtils.writeTag(buf, tag);
    }

    public static class Handler implements IMessageHandler<LabelButtonMessage, IMessage> {

        @Override
        public IMessage onMessage(LabelButtonMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            Tasks.SERVER.add(() -> {
                if (player.worldObj == null) return;
                if (player.getDistanceSq(message.pos.getX() + 0.5, message.pos.getY() + 0.5, message.pos.getZ() + 0.5)
                    > 64 * 64) return;
                LabelBlock labelBlock = LabelStorage.getStorageFor(player.worldObj)
                    .getLabelBlocksMap()
                    .get(message.pos);
                if (labelBlock == null) return;
                LabelInstance instance = labelBlock.getLabels()
                    .get(message.direction);
                if (instance == null) return;
                instance.handleButtonMessage(message.buttonId, player, message.tag);
            });
            return null;
        }
    }
}
