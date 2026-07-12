package com.buuz135.transfer_labels.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.buuz135.transfer_labels.client.LabelShapes;
import com.buuz135.transfer_labels.network.PacketHandler;
import com.buuz135.transfer_labels.network.SingleLabelSyncMessage;
import com.buuz135.transfer_labels.util.BlockPos;
import com.buuz135.transfer_labels.util.INBTSerializable;
import com.buuz135.transfer_labels.util.InvHandler;

import cpw.mods.fml.common.network.NetworkRegistry;

public class LabelBlock implements INBTSerializable {

    private final BlockPos pos;
    private final World level;
    private final HashMap<ForgeDirection, LabelInstance> labels;

    public LabelBlock(BlockPos pos, World level) {
        this.pos = pos;
        this.level = level;
        this.labels = new HashMap<>();
    }

    public void setLabel(ForgeDirection direction, ItemStack label) {
        this.labels.put(direction, new LabelInstance(label, level, pos, direction, this));
    }

    public BlockPos getPos() {
        return pos;
    }

    public HashMap<ForgeDirection, LabelInstance> getLabels() {
        return labels;
    }

    public AxisAlignedBB collectShapes(ForgeDirection direction) {
        if (this.labels.containsKey(direction)) {
            AxisAlignedBB base = LabelShapes.get(direction);
            if (base != null) {
                return base.getOffsetBoundingBox(pos.getX(), pos.getY(), pos.getZ());
            }
        }
        return null;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound compoundTag = new NBTTagCompound();
        for (ForgeDirection direction : labels.keySet()) {
            LabelInstance labelInstance = labels.get(direction);
            NBTTagCompound labelInstanceCompoundTag = new NBTTagCompound();
            NBTTagCompound stackTag = new NBTTagCompound();
            if (labelInstance.getLabel() != null) {
                labelInstance.getLabel()
                    .writeToNBT(stackTag);
            }
            labelInstanceCompoundTag.setTag("Stack", stackTag);
            labelInstanceCompoundTag.setTag("Extra", labelInstance.serializeNBT());
            compoundTag.setTag(direction.name(), labelInstanceCompoundTag);
        }
        return compoundTag;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void deserializeNBT(NBTTagCompound compoundTag) {
        List<ForgeDirection> visitedDirections = new ArrayList<>();
        for (String s : (Set<String>) compoundTag.func_150296_c()) {
            NBTTagCompound labelInstanceCompoundTag = compoundTag.getCompoundTag(s);
            ForgeDirection direction = ForgeDirection.valueOf(s);
            visitedDirections.add(direction);
            LabelInstance instance = this.labels.get(direction);
            if (instance == null) {
                instance = new LabelInstance(
                    ItemStack.loadItemStackFromNBT(labelInstanceCompoundTag.getCompoundTag("Stack")),
                    level,
                    pos,
                    direction,
                    this);
                this.labels.put(direction, instance);
            }
            instance.deserializeNBT(labelInstanceCompoundTag.getCompoundTag("Extra"));
        }
        this.labels.keySet()
            .removeIf(direction -> !visitedDirections.contains(direction));
    }

    public void updateToNearby(EntityPlayer player) {
        if (player != null && !player.worldObj.isRemote) {
            PacketHandler.INSTANCE.sendToAllAround(
                new SingleLabelSyncMessage(pos, this.serializeNBT()),
                new NetworkRegistry.TargetPoint(level.provider.dimensionId, pos.getX(), pos.getY(), pos.getZ(), 32));
            LabelStorage.getStorageFor(level)
                .markDirty();
        }
    }

    public void remove(EntityPlayer player, ForgeDirection direction) {
        LabelInstance instance = this.getLabels()
            .remove(direction);
        if (instance != null && player != null) {
            if (instance.getLabel() != null) {
                InvHandler.giveItemToPlayer(
                    player,
                    instance.getLabel()
                        .copy());
            }
            ItemStack amount = instance.getAmountFilter()
                .getStackInSlot(0);
            ItemStack speed = instance.getSpeedFilter()
                .getStackInSlot(0);
            if (amount != null && amount.stackSize > 0) {
                InvHandler.giveItemToPlayer(player, amount.copy());
            }
            if (speed != null && speed.stackSize > 0) {
                InvHandler.giveItemToPlayer(player, speed.copy());
            }
        }
    }
}
