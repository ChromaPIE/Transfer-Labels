package com.buuz135.transfer_labels.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.oredict.OreDictionary;

/**
 * IItemHandler-style wrapper around 1.7.10 IInventory/ISidedInventory, mirroring the semantics of the modern
 * InvWrapper/SidedInvWrapper the source mod relies on.
 */
public class InvHandler {

    private final IInventory inv;
    private final ISidedInventory sided;
    private final int side;
    private final int[] slots;

    private InvHandler(IInventory inv, ForgeDirection side) {
        this.inv = inv;
        this.side = side.ordinal();
        if (inv instanceof ISidedInventory) {
            this.sided = (ISidedInventory) inv;
            int[] accessible = sided.getAccessibleSlotsFromSide(this.side);
            this.slots = accessible == null ? new int[0] : accessible;
        } else {
            this.sided = null;
            this.slots = new int[inv.getSizeInventory()];
            for (int i = 0; i < this.slots.length; i++) {
                this.slots[i] = i;
            }
        }
    }

    public static InvHandler get(World world, BlockPos pos, ForgeDirection side) {
        TileEntity tile = world.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
        if (!(tile instanceof IInventory)) return null;
        IInventory inv = (IInventory) tile;
        Block block = world.getBlock(pos.getX(), pos.getY(), pos.getZ());
        if (block instanceof BlockChest && tile instanceof TileEntityChest) {
            inv = wrapDoubleChest(world, pos, block, inv);
        }
        return new InvHandler(inv, side);
    }

    private static IInventory wrapDoubleChest(World world, BlockPos pos, Block block, IInventory inv) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (world.getBlock(x - 1, y, z) == block && world.getTileEntity(x - 1, y, z) instanceof TileEntityChest) {
            return new InventoryLargeChest(
                "container.chestDouble",
                (TileEntityChest) world.getTileEntity(x - 1, y, z),
                inv);
        }
        if (world.getBlock(x + 1, y, z) == block && world.getTileEntity(x + 1, y, z) instanceof TileEntityChest) {
            return new InventoryLargeChest(
                "container.chestDouble",
                inv,
                (TileEntityChest) world.getTileEntity(x + 1, y, z));
        }
        if (world.getBlock(x, y, z - 1) == block && world.getTileEntity(x, y, z - 1) instanceof TileEntityChest) {
            return new InventoryLargeChest(
                "container.chestDouble",
                (TileEntityChest) world.getTileEntity(x, y, z - 1),
                inv);
        }
        if (world.getBlock(x, y, z + 1) == block && world.getTileEntity(x, y, z + 1) instanceof TileEntityChest) {
            return new InventoryLargeChest(
                "container.chestDouble",
                inv,
                (TileEntityChest) world.getTileEntity(x, y, z + 1));
        }
        return inv;
    }

    public int getSlots() {
        return slots.length;
    }

    public ItemStack getStackInSlot(int index) {
        return inv.getStackInSlot(slots[index]);
    }

    public ItemStack extractItem(int index, int amount, boolean simulate) {
        if (amount <= 0) return null;
        int slot = slots[index];
        ItemStack existing = inv.getStackInSlot(slot);
        if (existing == null) return null;
        if (sided != null && !sided.canExtractItem(slot, existing, side)) return null;
        int toExtract = Math.min(Math.min(amount, existing.getMaxStackSize()), existing.stackSize);
        if (toExtract <= 0) return null;
        ItemStack extracted = existing.copy();
        extracted.stackSize = toExtract;
        if (!simulate) {
            existing.stackSize -= toExtract;
            if (existing.stackSize <= 0) {
                inv.setInventorySlotContents(slot, null);
            }
            inv.markDirty();
        }
        return extracted;
    }

    public ItemStack insertItem(int index, ItemStack stack, boolean simulate) {
        if (stack == null || stack.stackSize <= 0) return null;
        int slot = slots[index];
        if (!inv.isItemValidForSlot(slot, stack)) return stack;
        if (sided != null && !sided.canInsertItem(slot, stack, side)) return stack;
        ItemStack existing = inv.getStackInSlot(slot);
        int limit = Math.min(inv.getInventoryStackLimit(), stack.getMaxStackSize());
        if (existing != null) {
            if (!canCombine(existing, stack)) return stack;
            limit -= existing.stackSize;
        }
        if (limit <= 0) return stack;
        int toInsert = Math.min(limit, stack.stackSize);
        if (!simulate) {
            if (existing == null) {
                ItemStack copy = stack.copy();
                copy.stackSize = toInsert;
                inv.setInventorySlotContents(slot, copy);
            } else {
                existing.stackSize += toInsert;
            }
            inv.markDirty();
        }
        if (toInsert >= stack.stackSize) return null;
        ItemStack remainder = stack.copy();
        remainder.stackSize = stack.stackSize - toInsert;
        return remainder;
    }

    /** Equivalent of ItemHandlerHelper.insertItem: tries every slot in order, returns the remainder. */
    public static ItemStack insertItem(InvHandler handler, ItemStack stack, boolean simulate) {
        if (handler == null || stack == null) return stack;
        for (int i = 0; i < handler.getSlots() && stack != null; i++) {
            stack = handler.insertItem(i, stack, simulate);
        }
        return stack;
    }

    public static boolean canCombine(ItemStack a, ItemStack b) {
        return a.getItem() == b.getItem() && a.isStackable()
            && a.getItemDamage() == b.getItemDamage()
            && ItemStack.areItemStackTagsEqual(a, b);
    }

    /** 1.7.10 equivalent of modern ItemStack.isSameItem: same item, metadata-aware where metadata is a subtype. */
    public static boolean matchesItem(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.getItem() != b.getItem()) return false;
        return a.getItem()
            .isDamageable() || a.getItemDamage() == b.getItemDamage()
            || a.getItemDamage() == OreDictionary.WILDCARD_VALUE
            || b.getItemDamage() == OreDictionary.WILDCARD_VALUE;
    }

    public static void giveItemToPlayer(EntityPlayer player, ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) return;
        if (!player.inventory.addItemStackToInventory(stack) && stack.stackSize > 0) {
            player.dropPlayerItemWithRandomChoice(stack, false);
        }
        player.inventoryContainer.detectAndSendChanges();
    }
}
