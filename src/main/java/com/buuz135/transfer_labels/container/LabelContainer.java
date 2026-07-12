package com.buuz135.transfer_labels.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.buuz135.transfer_labels.storage.LabelInstance;
import com.buuz135.transfer_labels.util.BlockPos;
import com.buuz135.transfer_labels.util.InvHandler;

public class LabelContainer extends Container {

    private final LabelInstance instance;

    public LabelContainer(LabelInstance instance, InventoryPlayer playerInventory) {
        this.instance = instance;

        // Upgrade slots (amount at 145,30 / speed at 145,66 like the source InventoryComponents)
        this.addSlotToContainer(new UpgradeSlot(instance.getAmountFilter(), 0, 145, 30));
        this.addSlotToContainer(new UpgradeSlot(instance.getSpeedFilter(), 0, 145, 66));

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 102 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlotToContainer(new Slot(playerInventory, col, 8 + col * 18, 160));
        }
    }

    public LabelInstance getInstance() {
        return instance;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        BlockPos pos = instance.getPos();
        return player.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64 * 64;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = (Slot) this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return null;
        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();

        if (index < 2) {
            if (!this.mergeItemStack(stack, 2, 38, false)) return null;
        } else {
            mergeToUpgradeSlot(stack, 0);
            if (stack.stackSize > 0) {
                mergeToUpgradeSlot(stack, 1);
            }
            if (stack.stackSize == copy.stackSize) return null;
        }

        if (stack.stackSize == 0) {
            slot.putStack(null);
        } else {
            slot.onSlotChanged();
        }
        if (stack.stackSize == copy.stackSize) return null;
        slot.onPickupFromSlot(player, stack);
        return copy;
    }

    /** Vanilla mergeItemStack ignores slot stack limits, so upgrade slots need their own merge. */
    private boolean mergeToUpgradeSlot(ItemStack stack, int slotIndex) {
        if (stack == null || stack.stackSize <= 0) return false;
        Slot slot = (Slot) this.inventorySlots.get(slotIndex);
        if (!slot.isItemValid(stack)) return false;
        ItemStack existing = slot.getStack();
        int limit = Math.min(slot.getSlotStackLimit(), stack.getMaxStackSize());
        if (existing == null) {
            int move = Math.min(limit, stack.stackSize);
            if (move <= 0) return false;
            ItemStack put = stack.copy();
            put.stackSize = move;
            slot.putStack(put);
            stack.stackSize -= move;
            return true;
        } else if (InvHandler.canCombine(existing, stack)) {
            int move = Math.min(limit - existing.stackSize, stack.stackSize);
            if (move <= 0) return false;
            existing.stackSize += move;
            stack.stackSize -= move;
            slot.onSlotChanged();
            return true;
        }
        return false;
    }

    public static class UpgradeSlot extends Slot {

        public UpgradeSlot(IInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return inventory.isItemValidForSlot(getSlotIndex(), stack);
        }
    }
}
