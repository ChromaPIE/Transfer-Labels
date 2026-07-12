package com.buuz135.transfer_labels.filter.extras;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import com.buuz135.transfer_labels.util.INBTSerializable;

/**
 * 1.7.10 has no fluid tags; the fluid's registry name acts as its single pseudo-tag.
 */
public class FluidTagFilterExtra implements INBTSerializable {

    private final List<String> extra;

    public FluidTagFilterExtra(int amount) {
        this.extra = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            this.extra.add(null);
        }
    }

    public static List<String> getTags(FluidStack stack) {
        if (stack == null || stack.getFluid() == null) return Collections.emptyList();
        return Collections.singletonList(
            stack.getFluid()
                .getName());
    }

    public void initTag(int slot, FluidStack fluid) {
        List<String> tags = getTags(fluid);
        this.extra.set(slot, tags.isEmpty() ? null : tags.get(0));
    }

    public void nextTag(int slot, FluidStack fluid) {
        cycleTag(slot, fluid, true);
    }

    public void previousTag(int slot, FluidStack fluid) {
        cycleTag(slot, fluid, false);
    }

    private void cycleTag(int slot, FluidStack fluid, boolean forward) {
        List<String> tags = getTags(fluid);
        if (tags.isEmpty()) {
            this.extra.set(slot, null);
            return;
        }

        String currentTag = this.extra.get(slot);
        if (currentTag == null) {
            this.extra.set(slot, forward ? tags.get(0) : tags.get(tags.size() - 1));
            return;
        }

        int currentIndex = tags.indexOf(currentTag);
        if (forward) {
            this.extra.set(
                slot,
                (currentIndex == -1 || currentIndex == tags.size() - 1) ? tags.get(0) : tags.get(currentIndex + 1));
        } else {
            this.extra.set(
                slot,
                (currentIndex == -1 || currentIndex == 0) ? tags.get(tags.size() - 1) : tags.get(currentIndex - 1));
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
