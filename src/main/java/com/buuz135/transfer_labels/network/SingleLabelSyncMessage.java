package com.buuz135.transfer_labels.network;

import net.minecraft.nbt.NBTTagCompound;

import com.buuz135.transfer_labels.TransferLabels;
import com.buuz135.transfer_labels.util.BlockPos;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class SingleLabelSyncMessage implements IMessage {

    public BlockPos pos;
    public NBTTagCompound label;

    public SingleLabelSyncMessage() {}

    public SingleLabelSyncMessage(BlockPos pos, NBTTagCompound compoundTag) {
        this.pos = pos;
        this.label = compoundTag;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        this.label = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        ByteBufUtils.writeTag(buf, label);
    }

    public static class Handler implements IMessageHandler<SingleLabelSyncMessage, IMessage> {

        @Override
        public IMessage onMessage(SingleLabelSyncMessage message, MessageContext ctx) {
            TransferLabels.proxy.handleSingleLabelSync(message);
            return null;
        }
    }
}
