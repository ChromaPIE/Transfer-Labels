package com.buuz135.transfer_labels.filter;

import java.util.HashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.buuz135.transfer_labels.util.BlockPos;
import com.buuz135.transfer_labels.util.INBTSerializable;

public interface ILabelFilter<T> extends INBTSerializable {

    String getName();

    boolean acceptsAsFilter(ItemStack filter);

    void setFilter(int slot, ItemStack stack);

    void setFilterSlot(int slot, FilterSlot<T> filterSlot);

    FilterSlot<T>[] getFilterSlots();

    Type getType();

    void toggleFilterMode();

    FilterType getFilterType();

    HashMap<String, INBTSerializable> getSavedFilters();

    void handleButtonMessage(int id, EntityPlayer player, NBTTagCompound tag);

    void work(World world, BlockPos pos, ForgeDirection direction, int amount);

    enum Type {

        WHITELIST,
        BLACKLIST;

        public boolean test(boolean matches) {
            return this == WHITELIST ? matches : !matches;
        }
    }
}
