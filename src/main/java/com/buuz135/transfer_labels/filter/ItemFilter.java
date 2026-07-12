package com.buuz135.transfer_labels.filter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.buuz135.transfer_labels.filter.extras.NumberFilterExtra;
import com.buuz135.transfer_labels.filter.extras.TagFilterExtra;
import com.buuz135.transfer_labels.item.TransferLabelItem;
import com.buuz135.transfer_labels.util.BlockPos;
import com.buuz135.transfer_labels.util.INBTSerializable;
import com.buuz135.transfer_labels.util.InvHandler;

import cpw.mods.fml.common.registry.GameRegistry;

public class ItemFilter implements ILabelFilter<ItemStack> {

    private final FilterSlot<ItemStack>[] filter;

    private Type type;
    private int pointer;
    private final String name;
    private final HashMap<String, INBTSerializable> savedFilters;
    private FilterType filterType;
    private final TransferLabelItem.Mode mode;

    @SuppressWarnings("unchecked")
    public ItemFilter(String name, int filterSize, TransferLabelItem.Mode mode) {
        this.name = name;
        this.filter = new FilterSlot[filterSize];
        this.mode = mode;
        this.type = Type.BLACKLIST;
        this.pointer = 0;
        this.filterType = FilterType.NORMAL;
        this.savedFilters = new HashMap<>();
        this.savedFilters.put(FilterType.REGULATING.getName(), new NumberFilterExtra(filterSize));
        this.savedFilters.put(FilterType.EXACT_COUNT.getName(), new NumberFilterExtra(filterSize));
        this.savedFilters.put(FilterType.TAG.getName(), new TagFilterExtra(filterSize));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean acceptsAsFilter(ItemStack filter) {
        return true;
    }

    @Override
    public void setFilter(int slot, ItemStack stack) {
        if (slot < 0 || slot >= filter.length) return;
        filter[slot].setFilter(stack);
        NumberFilterExtra numberFilterExtra = (NumberFilterExtra) this.savedFilters
            .get(FilterType.EXACT_COUNT.getName());
        numberFilterExtra.getExtra()
            .set(slot, stack == null ? 0 : stack.stackSize);
        numberFilterExtra = (NumberFilterExtra) this.savedFilters.get(FilterType.REGULATING.getName());
        numberFilterExtra.getExtra()
            .set(slot, stack == null ? 0 : stack.stackSize);
        TagFilterExtra extra = (TagFilterExtra) this.savedFilters.get(FilterType.TAG.getName());
        extra.initTag(slot, stack);
    }

    @Override
    public void setFilterSlot(int slot, FilterSlot<ItemStack> filterSlot) {
        if (slot < 0 || slot >= filter.length) return;
        this.filter[slot] = filterSlot;
    }

    @Override
    public FilterSlot<ItemStack>[] getFilterSlots() {
        return filter;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public void toggleFilterMode() {
        if (this.type == Type.WHITELIST) {
            this.type = Type.BLACKLIST;
        } else {
            this.type = Type.WHITELIST;
        }
    }

    @Override
    public void handleButtonMessage(int id, EntityPlayer player, NBTTagCompound compoundTag) {
        if (compoundTag.hasKey("Scrollable_Name") && compoundTag.getString("Scrollable_Name")
            .equals("filter_selector")) {
            double scroll = compoundTag.getDouble("Scroll");
            if (scroll > 0) {
                this.filterType = FilterType.getPrevious(this.filterType.getName());
            } else if (scroll < 0) {
                this.filterType = FilterType.getNext(this.filterType.getName());
            }
        }
        if (compoundTag.hasKey("FilterAmount")) {
            int slot = compoundTag.getInteger("FilterAmount");
            int amount = (int) compoundTag.getDouble("Scroll");
            if (this.filterType == FilterType.EXACT_COUNT || this.filterType == FilterType.REGULATING) {
                NumberFilterExtra numberFilterExtra = (NumberFilterExtra) this.savedFilters
                    .get(this.filterType.getName());
                numberFilterExtra.add(slot, amount);
            }
        }
        if (compoundTag.hasKey("FilterTag")) {
            int slot = compoundTag.getInteger("FilterTag");
            int scroll = (int) compoundTag.getDouble("Scroll");
            if (this.filterType == FilterType.TAG && slot >= 0 && slot < this.filter.length) {
                TagFilterExtra tagFilterExtra = (TagFilterExtra) this.savedFilters.get(this.filterType.getName());
                if (scroll > 0) {
                    tagFilterExtra.previousTag(slot, this.filter[slot].getFilter());
                } else if (scroll < 0) {
                    tagFilterExtra.nextTag(slot, this.filter[slot].getFilter());
                }
            }
        }
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound compoundNBT = new NBTTagCompound();
        compoundNBT.setInteger("Pointer", pointer);
        NBTTagCompound filterTag = new NBTTagCompound();
        for (FilterSlot<ItemStack> itemStackFilterSlot : this.filter) {
            if (itemStackFilterSlot != null && itemStackFilterSlot.getFilter() != null) {
                NBTTagCompound stackTag = new NBTTagCompound();
                itemStackFilterSlot.getFilter()
                    .writeToNBT(stackTag);
                filterTag.setTag(itemStackFilterSlot.getFilterID() + "", stackTag);
            }
        }
        compoundNBT.setTag("Filter", filterTag);
        compoundNBT.setString("Type", type.name());
        compoundNBT.setString("FilterType", this.filterType.getName());
        NBTTagCompound savedFiltersNBT = new NBTTagCompound();
        for (Map.Entry<String, INBTSerializable> entry : this.savedFilters.entrySet()) {
            savedFiltersNBT.setTag(
                entry.getKey(),
                entry.getValue()
                    .serializeNBT());
        }
        compoundNBT.setTag("SavedFilters", savedFiltersNBT);
        return compoundNBT;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        pointer = nbt.getInteger("Pointer");
        NBTTagCompound filterTag = nbt.getCompoundTag("Filter");
        for (FilterSlot<ItemStack> filterSlot : this.filter) {
            if (filterSlot != null) filterSlot.setFilter(null);
        }
        for (String key : (Set<String>) filterTag.func_150296_c()) {
            int slot = Integer.parseInt(key);
            if (slot >= 0 && slot < this.filter.length) {
                this.filter[slot].setFilter(ItemStack.loadItemStackFromNBT(filterTag.getCompoundTag(key)));
            }
        }
        this.type = Type.valueOf(nbt.getString("Type"));
        this.filterType = FilterType.getByName(nbt.getString("FilterType"));
        NBTTagCompound savedFiltersNBT = nbt.getCompoundTag("SavedFilters");
        for (Map.Entry<String, INBTSerializable> entry : this.savedFilters.entrySet()) {
            entry.getValue()
                .deserializeNBT(savedFiltersNBT.getCompoundTag(entry.getKey()));
        }
    }

    @Override
    public FilterType getFilterType() {
        return filterType;
    }

    @Override
    public HashMap<String, INBTSerializable> getSavedFilters() {
        return savedFilters;
    }

    @Override
    public void work(World world, BlockPos pos, ForgeDirection direction, int amount) {
        ForgeDirection oppositeDirection = direction.getOpposite();
        BlockPos oppositePos = pos.offset(direction);
        if (!world.blockExists(pos.getX(), pos.getY(), pos.getZ())
            || !world.blockExists(oppositePos.getX(), oppositePos.getY(), oppositePos.getZ())) return;

        // Get the item handlers for both block entities
        InvHandler sourceHandler;
        InvHandler targetHandler;

        if (this.mode == TransferLabelItem.Mode.EXTRACT) {
            targetHandler = InvHandler.get(world, oppositePos, oppositeDirection);
            sourceHandler = InvHandler.get(world, pos, direction);
        } else {
            sourceHandler = InvHandler.get(world, oppositePos, oppositeDirection);
            targetHandler = InvHandler.get(world, pos, direction);
        }

        if (sourceHandler == null || targetHandler == null) return;

        transferItems(sourceHandler, targetHandler, amount);
    }

    private void transferItems(InvHandler sourceHandler, InvHandler targetHandler, int defaultMaxAmount) {
        for (int sourceSlot = 0; sourceSlot < sourceHandler.getSlots(); sourceSlot++) {
            int maxAmount = defaultMaxAmount;
            ItemStack sourceStack = sourceHandler.extractItem(sourceSlot, maxAmount, true);
            if (sourceStack == null) continue;
            if (!passesFilter(sourceStack)) continue;

            maxAmount = calculateMaxTransferAmount(
                sourceStack,
                sourceHandler,
                targetHandler,
                sourceSlot,
                defaultMaxAmount);

            // If maxAmount is 0, skip this item
            if (maxAmount <= 0) continue;

            // For EXACT_COUNT filter type with multiple slots
            if (this.filterType == FilterType.EXACT_COUNT) {
                // Find the matching filter slot to get the exact amount
                int exactAmount = 0;
                for (FilterSlot<ItemStack> filterSlot : this.filter) {
                    if (filterSlot != null && filterSlot.getFilter() != null
                        && InvHandler.matchesItem(sourceStack, filterSlot.getFilter())) {
                        NumberFilterExtra numberFilterExtra = (NumberFilterExtra) this.savedFilters
                            .get(FilterType.EXACT_COUNT.getName());
                        exactAmount = numberFilterExtra.getExtra()
                            .get(filterSlot.getFilterID());
                        break;
                    }
                }
                // If the current slot doesn't have enough items, extract from multiple slots
                if (sourceStack.stackSize < exactAmount) {
                    handleExactCountMultiSlotTransfer(
                        sourceHandler,
                        targetHandler,
                        sourceStack,
                        sourceSlot,
                        exactAmount);
                    return;
                }
            }

            // Standard extraction for single-slot cases
            sourceStack = sourceHandler.extractItem(sourceSlot, maxAmount, true);
            if (sourceStack == null) continue;

            ItemStack simulatedResult = InvHandler.insertItem(targetHandler, sourceStack.copy(), true);
            if (simulatedResult != null && this.filterType == FilterType.EXACT_COUNT) {
                continue;
            }
            int amountToExtract = sourceStack.stackSize - (simulatedResult == null ? 0 : simulatedResult.stackSize);
            if (amountToExtract > 0) {
                sourceHandler.extractItem(sourceSlot, amountToExtract, false);
                ItemStack toInsert = sourceStack.copy();
                toInsert.stackSize = amountToExtract;
                InvHandler.insertItem(targetHandler, toInsert, false);
                break;
            }
        }
    }

    /**
     * Calculates the maximum amount of items to transfer based on the current filter type.
     */
    private int calculateMaxTransferAmount(ItemStack sourceStack, InvHandler sourceHandler, InvHandler targetHandler,
        int sourceSlot, int defaultMaxAmount) {
        int maxAmount = defaultMaxAmount;

        // If the filter is regulating, calculate the max amount based on what's already in the target
        if (this.filterType == FilterType.REGULATING) {
            for (FilterSlot<ItemStack> filterSlot : this.filter) {
                if (filterSlot != null && filterSlot.getFilter() != null
                    && InvHandler.matchesItem(sourceStack, filterSlot.getFilter())) {
                    // Count how many of this item are already in the target
                    int currentCount = 0;
                    for (int targetSlot = 0; targetSlot < targetHandler.getSlots(); targetSlot++) {
                        ItemStack targetStack = targetHandler.getStackInSlot(targetSlot);
                        if (targetStack != null && InvHandler.matchesItem(targetStack, filterSlot.getFilter())) {
                            currentCount += targetStack.stackSize;
                        }
                    }

                    NumberFilterExtra numberFilterExtra = (NumberFilterExtra) this.savedFilters
                        .get(FilterType.REGULATING.getName());
                    int desiredAmount = numberFilterExtra.getExtra()
                        .get(filterSlot.getFilterID());
                    int neededAmount = Math.max(0, desiredAmount - currentCount);
                    maxAmount = Math.min(neededAmount, 4);
                    break;
                }
            }
        }
        // If the filter is exact count, only transfer if the source stack count matches the filter count exactly
        // or if we can combine items from multiple slots to match the exact count
        else if (this.filterType == FilterType.EXACT_COUNT) {
            for (FilterSlot<ItemStack> filterSlot : this.filter) {
                if (filterSlot != null && filterSlot.getFilter() != null
                    && InvHandler.matchesItem(sourceStack, filterSlot.getFilter())) {
                    NumberFilterExtra numberFilterExtra = (NumberFilterExtra) this.savedFilters
                        .get(FilterType.EXACT_COUNT.getName());
                    int exactAmount = numberFilterExtra.getExtra()
                        .get(filterSlot.getFilterID());
                    if (exactAmount > defaultMaxAmount) { // If the exact amount is too high, we can't transfer anything
                        maxAmount = 0;
                        break;
                    }
                    if (sourceStack.stackSize >= exactAmount) {
                        maxAmount = exactAmount;
                    } else {
                        // If the source stack count is less than the exact amount, check if we can combine items from
                        // multiple slots
                        int availableAmount = sourceStack.stackSize;
                        for (int otherSlot = 0; otherSlot < sourceHandler.getSlots(); otherSlot++) {
                            if (otherSlot == sourceSlot) continue;
                            ItemStack otherStack = sourceHandler.getStackInSlot(otherSlot);
                            if (otherStack != null && InvHandler.matchesItem(otherStack, sourceStack)) {
                                availableAmount += otherStack.stackSize;
                                if (availableAmount >= exactAmount) {
                                    break;
                                }
                            }
                        }
                        if (availableAmount >= exactAmount) {
                            maxAmount = exactAmount;
                        } else {
                            maxAmount = 0;
                        }
                    }
                    break;
                }
            }
        }

        return maxAmount;
    }

    /**
     * Handles the transfer of items from multiple source slots to match an exact count requirement.
     */
    private boolean handleExactCountMultiSlotTransfer(InvHandler sourceHandler, InvHandler targetHandler,
        ItemStack sourceStack, int sourceSlot, int exactAmount) {
        // First, simulate the entire operation to make sure everything fits
        ItemStack combinedStack = sourceStack.copy();
        int remainingNeeded = exactAmount - combinedStack.stackSize;
        // Track which slots we'll extract from and how much
        HashMap<Integer, Integer> extractionPlan = new HashMap<>();
        extractionPlan.put(sourceSlot, combinedStack.stackSize);
        // Simulate extraction from other slots as needed
        for (int otherSlot = 0; otherSlot < sourceHandler.getSlots() && remainingNeeded > 0; otherSlot++) {
            if (otherSlot == sourceSlot) continue;
            ItemStack otherStack = sourceHandler.getStackInSlot(otherSlot);
            if (otherStack != null && InvHandler.matchesItem(otherStack, combinedStack)) {
                int toExtract = Math.min(remainingNeeded, otherStack.stackSize);
                extractionPlan.put(otherSlot, toExtract);
                remainingNeeded -= toExtract;
            }
        }
        // If we couldn't gather enough items, skip this transfer
        if (remainingNeeded > 0) {
            return false;
        }
        // Simulate insertion into target
        combinedStack.stackSize = exactAmount;
        ItemStack simulatedResult = InvHandler.insertItem(targetHandler, combinedStack.copy(), true);
        // If we can't insert all items, skip this transfer
        if (simulatedResult != null) {
            return false;
        }
        // Now perform the actual extraction and insertion
        combinedStack = null;
        for (Map.Entry<Integer, Integer> entry : extractionPlan.entrySet()) {
            int slot = entry.getKey();
            int amount = entry.getValue();

            ItemStack extracted = sourceHandler.extractItem(slot, amount, false);
            if (extracted == null) continue;

            if (combinedStack == null) {
                combinedStack = extracted;
            } else {
                combinedStack.stackSize += extracted.stackSize;
            }
        }
        if (combinedStack == null) return false;
        combinedStack.stackSize = exactAmount;
        InvHandler.insertItem(targetHandler, combinedStack, false);
        return true;
    }

    private boolean passesFilter(ItemStack stack) {
        // If there are no filter slots, allow all items
        if (this.filter.length == 0) return true;

        // Check if the item matches any of the filter slots
        boolean matches = false;
        for (FilterSlot<ItemStack> filterSlot : this.filter) {
            if (filterSlot == null || filterSlot.getFilter() == null) continue;

            boolean itemMatches = false;

            if (this.filterType == FilterType.NORMAL || this.filterType == FilterType.REGULATING
                || this.filterType == FilterType.EXACT_COUNT) {
                // Exact item match (count is handled separately for exact count/regulating)
                itemMatches = InvHandler.matchesItem(stack, filterSlot.getFilter());
            } else if (this.filterType == FilterType.MOD) {
                // Mod filter: match mod ID
                itemMatches = getModId(stack.getItem()).equals(
                    getModId(
                        filterSlot.getFilter()
                            .getItem()));
            } else if (this.filterType == FilterType.TAG) {
                // Tag filter: match ore dictionary name
                TagFilterExtra tagFilterExtra = (TagFilterExtra) this.savedFilters.get(FilterType.TAG.getName());
                String tag = tagFilterExtra.getExtra()
                    .get(filterSlot.getFilterID());
                if (tag != null) {
                    itemMatches = TagFilterExtra.getTags(stack)
                        .contains(tag);
                }
            }

            if (itemMatches) {
                matches = true;
                break;
            }
        }
        // Apply whitelist/blacklist logic
        return this.type.test(matches);
    }

    public static String getModId(Item item) {
        GameRegistry.UniqueIdentifier identifier = GameRegistry.findUniqueIdentifierFor(item);
        return identifier == null ? "unknown" : identifier.modId;
    }
}
