package com.buuz135.transfer_labels.storage;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.buuz135.transfer_labels.Config;
import com.buuz135.transfer_labels.filter.ILabelFilter;
import com.buuz135.transfer_labels.item.TransferLabelItem;
import com.buuz135.transfer_labels.util.BlockPos;
import com.buuz135.transfer_labels.util.INBTSerializable;
import com.buuz135.transfer_labels.util.InvHandler;

public class LabelInstance implements INBTSerializable {

    private final ItemStack label;
    private final World level;
    private final BlockPos pos;
    private final ForgeDirection facing;
    private ILabelFilter<?> filter;
    private final LabelBlock parent;
    private final UpgradeInventory amountFilter;
    private final UpgradeInventory speedFilter;

    public LabelInstance(ItemStack label, World world, BlockPos pos, ForgeDirection direction, LabelBlock parent) {
        this.label = label;
        this.level = world;
        this.pos = pos;
        this.facing = direction;
        this.parent = parent;
        if (label != null && label.getItem() instanceof TransferLabelItem) {
            this.filter = ((TransferLabelItem) label.getItem()).createFilter();
        }
        this.amountFilter = new UpgradeInventory(this, "amountFilter", Config.amountUpgrades);
        this.speedFilter = new UpgradeInventory(this, "speedFilter", Config.speedUpgrades);
    }

    public void handleButtonMessage(int i, EntityPlayer player, NBTTagCompound compoundTag) {
        if (i == -2 && this.filter != null) {
            int slot = compoundTag.getInteger("Slot");
            ItemStack stack = compoundTag.hasKey("Filter")
                ? ItemStack.loadItemStackFromNBT(compoundTag.getCompoundTag("Filter"))
                : null;
            this.filter.setFilter(slot, stack);
        }
        if (i == -7 && this.filter != null) {
            this.filter.handleButtonMessage(i, player, compoundTag);
        }
        if (i == 54571 && this.filter != null) {
            this.filter.toggleFilterMode();
        }
        parent.updateToNearby(player);
    }

    public ItemStack getLabel() {
        return label;
    }

    public ILabelFilter<?> getFilter() {
        return filter;
    }

    public BlockPos getPos() {
        return pos;
    }

    public ForgeDirection getFacing() {
        return facing;
    }

    public World getLevel() {
        return level;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound compoundTag = new NBTTagCompound();
        if (this.filter != null) {
            compoundTag.setTag("Filter", this.filter.serializeNBT());
        }
        compoundTag.setTag("AmountFilter", this.amountFilter.serializeNBT());
        compoundTag.setTag("SpeedFilter", this.speedFilter.serializeNBT());
        return compoundTag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound compoundTag) {
        if (this.filter != null) {
            this.filter.deserializeNBT(compoundTag.getCompoundTag("Filter"));
        }
        this.amountFilter.deserializeNBT(compoundTag.getCompoundTag("AmountFilter"));
        this.speedFilter.deserializeNBT(compoundTag.getCompoundTag("SpeedFilter"));
    }

    public void work(World level) {
        if (this.filter != null && level.getTotalWorldTime() % (20 - getSpeed()) == 0) {
            this.filter.work(level, this.pos, this.facing, Config.baseItemTransferAmount + getAmount());
        }
    }

    public int getSpeed() {
        ItemStack stack = this.speedFilter.getStackInSlot(0);
        return Math.min(stack == null ? 0 : stack.stackSize, Config.speedUpgrades) / 2;
    }

    public int getAmount() {
        ItemStack stack = this.amountFilter.getStackInSlot(0);
        return Math.min(stack == null ? 0 : stack.stackSize, Config.amountUpgrades);
    }

    public void markComponentDirty() {
        if (!this.level.isRemote) {
            LabelStorage.getStorageFor(this.level)
                .markDirty();
        }
    }

    public UpgradeInventory getAmountFilter() {
        return amountFilter;
    }

    public UpgradeInventory getSpeedFilter() {
        return speedFilter;
    }

    /** Single-slot upgrade inventory that only accepts more of this label's item. */
    public static class UpgradeInventory implements IInventory, INBTSerializable {

        private final LabelInstance owner;
        private final String name;
        private final int limit;
        private ItemStack stack;

        public UpgradeInventory(LabelInstance owner, String name, int limit) {
            this.owner = owner;
            this.name = name;
            this.limit = Math.max(1, limit);
        }

        @Override
        public int getSizeInventory() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? stack : null;
        }

        @Override
        public ItemStack decrStackSize(int slot, int amount) {
            if (slot != 0 || stack == null) return null;
            if (stack.stackSize <= amount) {
                ItemStack result = stack;
                stack = null;
                markDirty();
                return result;
            }
            ItemStack result = stack.splitStack(amount);
            markDirty();
            return result;
        }

        @Override
        public ItemStack getStackInSlotOnClosing(int slot) {
            return null;
        }

        @Override
        public void setInventorySlotContents(int slot, ItemStack stack) {
            if (slot != 0) return;
            this.stack = stack;
            markDirty();
        }

        @Override
        public String getInventoryName() {
            return name;
        }

        @Override
        public boolean hasCustomInventoryName() {
            return false;
        }

        @Override
        public int getInventoryStackLimit() {
            return limit;
        }

        @Override
        public void markDirty() {
            owner.markComponentDirty();
        }

        @Override
        public boolean isUseableByPlayer(EntityPlayer player) {
            return true;
        }

        @Override
        public void openInventory() {}

        @Override
        public void closeInventory() {}

        @Override
        public boolean isItemValidForSlot(int slot, ItemStack stack) {
            return slot == 0 && InvHandler.matchesItem(stack, owner.getLabel());
        }

        @Override
        public NBTTagCompound serializeNBT() {
            NBTTagCompound tag = new NBTTagCompound();
            if (stack != null) {
                NBTTagCompound stackTag = new NBTTagCompound();
                stack.writeToNBT(stackTag);
                tag.setTag("Stack", stackTag);
            }
            return tag;
        }

        @Override
        public void deserializeNBT(NBTTagCompound tag) {
            stack = tag.hasKey("Stack") ? ItemStack.loadItemStackFromNBT(tag.getCompoundTag("Stack")) : null;
        }
    }
}
