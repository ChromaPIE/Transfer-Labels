package com.buuz135.transfer_labels.filter.extras;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;

import com.buuz135.transfer_labels.util.INBTSerializable;

public class NumberFilterExtra implements INBTSerializable {

    private final List<Integer> extra;

    public NumberFilterExtra(int amount) {
        this.extra = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            this.extra.add(1);
        }
    }

    public void add(int slot, int amount) {
        if (slot < 0 || slot >= this.extra.size()) return;
        this.extra.set(slot, Math.max(0, Math.min(this.extra.get(slot) + amount, Integer.MAX_VALUE - 1)));
    }

    public List<Integer> getExtra() {
        return extra;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound compoundTag = new NBTTagCompound();
        for (int i = 0; i < this.extra.size(); i++) {
            compoundTag.setInteger(i + "", this.extra.get(i));
        }
        return compoundTag;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void deserializeNBT(NBTTagCompound compoundTag) {
        for (String s : (Set<String>) compoundTag.func_150296_c()) {
            int slot = Integer.parseInt(s);
            if (slot >= 0 && slot < this.extra.size()) {
                this.extra.set(slot, compoundTag.getInteger(s));
            }
        }
    }
}
