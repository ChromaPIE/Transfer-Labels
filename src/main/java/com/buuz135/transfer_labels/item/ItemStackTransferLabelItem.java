package com.buuz135.transfer_labels.item;

import net.minecraft.item.ItemStack;

import com.buuz135.transfer_labels.Config;
import com.buuz135.transfer_labels.filter.FilterSlot;
import com.buuz135.transfer_labels.filter.ILabelFilter;
import com.buuz135.transfer_labels.filter.ItemFilter;

public class ItemStackTransferLabelItem extends TransferLabelItem {

    public ItemStackTransferLabelItem(Mode mode) {
        super("itemstack", mode);
    }

    @Override
    public ILabelFilter<ItemStack> createFilter() {
        ItemFilter filter = new ItemFilter("item_filter", Config.filterSlots, this.getMode());

        int slotSize = 18;
        int startX = 43;
        int startY = 20;

        for (int i = 0; i < filter.getFilterSlots().length; i++) {
            int x = startX + (i % 5) * slotSize;
            int y = startY + (i / 5) * slotSize;
            filter.setFilterSlot(i, new FilterSlot<>(x, y, i, null));
        }

        return filter;
    }
}
