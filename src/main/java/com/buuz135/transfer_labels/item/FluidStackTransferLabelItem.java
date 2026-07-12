package com.buuz135.transfer_labels.item;

import net.minecraftforge.fluids.FluidStack;

import com.buuz135.transfer_labels.Config;
import com.buuz135.transfer_labels.filter.FilterSlot;
import com.buuz135.transfer_labels.filter.FluidFilter;
import com.buuz135.transfer_labels.filter.ILabelFilter;

public class FluidStackTransferLabelItem extends TransferLabelItem {

    public FluidStackTransferLabelItem(Mode mode) {
        super("fluidstack", mode);
    }

    @Override
    public ILabelFilter<FluidStack> createFilter() {
        FluidFilter filter = new FluidFilter("fluid_filter", Config.filterSlots, this.getMode());

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
