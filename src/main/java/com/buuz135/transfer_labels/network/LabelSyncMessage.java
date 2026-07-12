package com.buuz135.transfer_labels.network;

import net.minecraft.nbt.NBTTagCompound;

import com.buuz135.transfer_labels.TransferLabels;
import com.buuz135.transfer_labels.util.BlockPos;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class LabelSyncMessage implements IMessage {

    public NBTTagCompound labels;
    public BlockPos anchor;
    public int distance;

    public LabelSyncMessage() {}

    public LabelSyncMessage(NBTTagCompound compoundTag, BlockPos anchor, int distance) {
        this.labels = compoundTag;
        this.anchor = anchor;
        this.distance = distance;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.anchor = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        this.distance = buf.readInt();
        this.labels = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(anchor.getX());
        buf.writeInt(anchor.getY());
        buf.writeInt(anchor.getZ());
        buf.writeInt(distance);
        ByteBufUtils.writeTag(buf, labels);
    }

    public static class Handler implements IMessageHandler<LabelSyncMessage, IMessage> {

        @Override
        public IMessage onMessage(LabelSyncMessage message, MessageContext ctx) {
            TransferLabels.proxy.handleLabelSync(message);
            return null;
        }
    }
}
