package com.buuz135.transfer_labels.filter.extras;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

import com.buuz135.transfer_labels.util.INBTSerializable;

/**
 * 1.7.10 counterpart of the source's item tag filter extra: item tags map to Ore Dictionary names.
 */
public class TagFilterExtra implements INBTSerializable {

    private final List<String> extra;

    public TagFilterExtra(int amount) {
        this.extra = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            this.extra.add(null);
        }
    }

    public static List<String> getTags(ItemStack stack) {
        if (stack == null) return Collections.emptyList();
        int[] ids = OreDictionary.getOreIDs(stack);
        List<String> names = new ArrayList<>();
        for (int id : ids) {
            names.add(OreDictionary.getOreName(id));
        }
        return names;
    }

    public void initTag(int slot, ItemStack item) {
        List<String> tags = getTags(item);
        if (tags.isEmpty()) {
            this.extra.set(slot, null);
        } else {
            this.extra.set(slot, tags.get(0));
        }
    }

    public void nextTag(int slot, ItemStack item) {
        List<String> tags = getTags(item);
        if (tags.isEmpty()) {
            this.extra.set(slot, null);
            return;
        }

        String currentTag = this.extra.get(slot);
        if (currentTag == null) {
            this.extra.set(slot, tags.get(0));
            return;
        }

        int currentIndex = tags.indexOf(currentTag);
        if (currentIndex == -1 || currentIndex == tags.size() - 1) {
            this.extra.set(slot, tags.get(0));
        } else {
            this.extra.set(slot, tags.get(currentIndex + 1));
        }
    }

    public void previousTag(int slot, ItemStack item) {
        List<String> tags = getTags(item);
        if (tags.isEmpty()) {
            this.extra.set(slot, null);
            return;
        }

        String currentTag = this.extra.get(slot);
        if (currentTag == null) {
            this.extra.set(slot, tags.get(tags.size() - 1));
            return;
        }

        int currentIndex = tags.indexOf(currentTag);
        if (currentIndex == -1 || currentIndex == 0) {
            this.extra.set(slot, tags.get(tags.size() - 1));
        } else {
            this.extra.set(slot, tags.get(currentIndex - 1));
        }
    }

    public List<String> getExtra() {
        return extra;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound compoundTag = new NBTTagCompound();
        for (int i = 0; i < this.extra.size(); i++) {
            if (this.extra.get(i) != null) compoundTag.setString(i + "", this.extra.get(i));
        }
        return compoundTag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound compoundTag) {
        for (int i = 0; i < this.extra.size(); i++) {
            if (compoundTag.hasKey(i + "")) {
                this.extra.set(i, compoundTag.getString(i + ""));
            } else {
                this.extra.set(i, null);
            }
        }
    }
}
