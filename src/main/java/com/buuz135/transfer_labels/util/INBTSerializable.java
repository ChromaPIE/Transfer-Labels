package com.buuz135.transfer_labels.util;

import net.minecraft.nbt.NBTTagCompound;

public interface INBTSerializable {

    NBTTagCompound serializeNBT();

    void deserializeNBT(NBTTagCompound tag);
}
