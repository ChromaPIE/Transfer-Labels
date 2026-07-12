package com.buuz135.transfer_labels.filter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidContainerItem;
import net.minecraftforge.fluids.IFluidHandler;

import com.buuz135.transfer_labels.Config;
import com.buuz135.transfer_labels.filter.extras.FluidTagFilterExtra;
import com.buuz135.transfer_labels.filter.extras.NumberFilterExtra;
import com.buuz135.transfer_labels.item.TransferLabelItem;
import com.buuz135.transfer_labels.util.BlockPos;
import com.buuz135.transfer_labels.util.INBTSerializable;

import cpw.mods.fml.common.registry.GameRegistry;

public class FluidFilter implements ILabelFilter<FluidStack> {

    private final FilterSlot<FluidStack>[] filter;

    private Type type;
    private int pointer;
    private final String name;
    private final HashMap<String, INBTSerializable> savedFilters;
    private FilterType filterType;
    private final TransferLabelItem.Mode mode;

    @SuppressWarnings("unchecked")
    public FluidFilter(String name, int filterSize, TransferLabelItem.Mode mode) {
        this.name = name;
        this.filter = new FilterSlot[filterSize];
        this.mode = mode;
        this.type = Type.BLACKLIST;
        this.pointer = 0;
        this.filterType = FilterType.NORMAL;
        this.savedFilters = new HashMap<>();
        this.savedFilters.put(FilterType.REGULATING.getName(), new NumberFilterExtra(filterSize));
        this.savedFilters.put(FilterType.EXACT_COUNT.getName(), new NumberFilterExtra(filterSize));
        this.savedFilters.put(FilterType.TAG.getName(), new FluidTagFilterExtra(filterSize));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean acceptsAsFilter(ItemStack filter) {
        return true;
    }

    /** Extracts the contained fluid from an item stack (bucket or IFluidContainerItem). */
    public static FluidStack getFluidFromItem(ItemStack stack) {
        if (stack == null) return null;
        FluidStack fluidStack = FluidContainerRegistry.getFluidForFilledItem(stack);
        if (fluidStack == null && stack.getItem() instanceof IFluidContainerItem) {
            fluidStack = ((IFluidContainerItem) stack.getItem()).getFluid(stack);
        }
        return fluidStack;
    }

    @Override
    public void setFilter(int slot, ItemStack stack) {
        if (slot < 0 || slot >= filter.length) return;

        FluidStack fluidStack = getFluidFromItem(stack);

        filter[slot].setFilter(fluidStack == null ? null : fluidStack.copy());
        NumberFilterExtra numberFilterExtra = (NumberFilterExtra) this.savedFilters
            .get(FilterType.EXACT_COUNT.getName());
        numberFilterExtra.getExtra()
            .set(slot, fluidStack == null ? 0 : fluidStack.amount);
        numberFilterExtra = (NumberFilterExtra) this.savedFilters.get(FilterType.REGULATING.getName());
        numberFilterExtra.getExtra()
            .set(slot, fluidStack == null ? 0 : fluidStack.amount);
        FluidTagFilterExtra extra = (FluidTagFilterExtra) this.savedFilters.get(FilterType.TAG.getName());
        extra.initTag(slot, fluidStack);
    }

    @Override
    public void setFilterSlot(int slot, FilterSlot<FluidStack> filterSlot) {
        if (slot < 0 || slot >= filter.length) return;
        this.filter[slot] = filterSlot;
    }

    @Override
    public FilterSlot<FluidStack>[] getFilterSlots() {
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
                FluidTagFilterExtra tagFilterExtra = (FluidTagFilterExtra) this.savedFilters
                    .get(this.filterType.getName());
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
        for (FilterSlot<FluidStack> fluidStackFilterSlot : this.filter) {
            if (fluidStackFilterSlot != null && fluidStackFilterSlot.getFilter() != null) {
                NBTTagCompound stackTag = new NBTTagCompound();
                fluidStackFilterSlot.getFilter()
                    .writeToNBT(stackTag);
                filterTag.setTag(fluidStackFilterSlot.getFilterID() + "", stackTag);
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
        for (FilterSlot<FluidStack> filterSlot : this.filter) {
            if (filterSlot != null) filterSlot.setFilter(null);
        }
        for (String key : (Set<String>) filterTag.func_150296_c()) {
            int slot = Integer.parseInt(key);
            if (slot >= 0 && slot < this.filter.length) {
                this.filter[slot].setFilter(FluidStack.loadFluidStackFromNBT(filterTag.getCompoundTag(key)));
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

        // Get the fluid handlers for both block entities
        IFluidHandler sourceHandler;
        ForgeDirection sourceSide;
        IFluidHandler targetHandler;
        ForgeDirection targetSide;

        if (this.mode == TransferLabelItem.Mode.EXTRACT) {
            targetHandler = getFluidHandler(world, oppositePos);
            targetSide = oppositeDirection;
            sourceHandler = getFluidHandler(world, pos);
            sourceSide = direction;
        } else {
            sourceHandler = getFluidHandler(world, oppositePos);
            sourceSide = oppositeDirection;
            targetHandler = getFluidHandler(world, pos);
            targetSide = direction;
        }

        if (sourceHandler == null || targetHandler == null) return;

        transferFluids(
            sourceHandler,
            sourceSide,
            targetHandler,
            targetSide,
            (int) Math.min(Integer.MAX_VALUE, (long) amount * Config.fluidTransferMultiplier));
    }

    private static IFluidHandler getFluidHandler(World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
        return tile instanceof IFluidHandler ? (IFluidHandler) tile : null;
    }

    private void transferFluids(IFluidHandler sourceHandler, ForgeDirection sourceSide, IFluidHandler targetHandler,
        ForgeDirection targetSide, int defaultMaxAmount) {
        FluidTankInfo[] tanks = sourceHandler.getTankInfo(sourceSide);
        if (tanks == null) return;
        for (FluidTankInfo tank : tanks) {
            int maxAmount = defaultMaxAmount;
            FluidStack sourceStack = tank.fluid;
            if (sourceStack == null || sourceStack.amount <= 0) continue;

            // Make a copy to simulate extraction
            FluidStack drainStack = sourceStack.copy();
            drainStack.amount = maxAmount;

            // Simulate draining
            FluidStack drained = sourceHandler.drain(sourceSide, drainStack, false);
            if (drained == null || drained.amount <= 0) continue;
            if (!passesFilter(drained)) continue;

            // Calculate max transfer amount based on filter type
            maxAmount = calculateMaxTransferAmount(drained, targetHandler, targetSide, defaultMaxAmount);

            // If maxAmount is 0, skip this fluid
            if (maxAmount <= 0) continue;

            // Update drain amount
            drainStack.amount = maxAmount;
            drained = sourceHandler.drain(sourceSide, drainStack, false);
            if (drained == null || drained.amount <= 0) continue;

            // Simulate filling
            int filled = targetHandler.fill(targetSide, drained, false);
            if (filled <= 0) continue;

            // Actual transfer
            FluidStack actualDrain = sourceHandler.drain(sourceSide, new FluidStack(drained.getFluid(), filled), true);
            if (actualDrain != null && actualDrain.amount > 0) {
                targetHandler.fill(targetSide, actualDrain, true);
            }
            break;
        }
    }

    private int calculateMaxTransferAmount(FluidStack sourceStack, IFluidHandler targetHandler,
        ForgeDirection targetSide, int defaultMaxAmount) {
        int maxAmount = defaultMaxAmount;

        // If the filter is regulating, calculate the max amount based on what's already in the target
        if (this.filterType == FilterType.REGULATING) {
            for (FilterSlot<FluidStack> filterSlot : this.filter) {
                if (filterSlot != null && filterSlot.getFilter() != null
                    && areFluidStacksEqual(filterSlot.getFilter(), sourceStack)) {
                    // Count how much of this fluid is already in the target
                    int currentAmount = 0;
                    FluidTankInfo[] targetTanks = targetHandler.getTankInfo(targetSide);
                    if (targetTanks != null) {
                        for (FluidTankInfo targetTank : targetTanks) {
                            FluidStack targetStack = targetTank.fluid;
                            if (targetStack != null && areFluidStacksEqual(targetStack, filterSlot.getFilter())) {
                                currentAmount += targetStack.amount;
                            }
                        }
                    }

                    NumberFilterExtra numberFilterExtra = (NumberFilterExtra) this.savedFilters
                        .get(FilterType.REGULATING.getName());
                    int desiredAmount = numberFilterExtra.getExtra()
                        .get(filterSlot.getFilterID());
                    int neededAmount = Math.max(0, desiredAmount - currentAmount);
                    maxAmount = Math.min(neededAmount, defaultMaxAmount);
                    break;
                }
            }
        }
        // If the filter is exact count, only transfer if we can match the exact amount
        else if (this.filterType == FilterType.EXACT_COUNT) {
            for (FilterSlot<FluidStack> filterSlot : this.filter) {
                if (filterSlot != null && filterSlot.getFilter() != null
                    && areFluidStacksEqual(filterSlot.getFilter(), sourceStack)) {
                    NumberFilterExtra numberFilterExtra = (NumberFilterExtra) this.savedFilters
                        .get(FilterType.EXACT_COUNT.getName());
                    int exactAmount = numberFilterExtra.getExtra()
                        .get(filterSlot.getFilterID());
                    if (exactAmount > defaultMaxAmount) { // If the exact amount is too high, we can't transfer anything
                        maxAmount = 0;
                        break;
                    }
                    maxAmount = exactAmount;
                    break;
                }
            }
        }

        return maxAmount;
    }

    private boolean areFluidStacksEqual(FluidStack stack1, FluidStack stack2) {
        return stack1.isFluidEqual(stack2);
    }

    private boolean passesFilter(FluidStack stack) {
        // If there are no filter slots, allow all fluids
        if (this.filter.length == 0) return true;

        boolean matches = false;
        for (FilterSlot<FluidStack> filterSlot : this.filter) {
            if (filterSlot == null || filterSlot.getFilter() == null) continue;

            boolean fluidMatches = false;

            if (this.filterType == FilterType.NORMAL || this.filterType == FilterType.REGULATING
                || this.filterType == FilterType.EXACT_COUNT) {
                fluidMatches = filterSlot.getFilter()
                    .isFluidEqual(stack);
            } else if (this.filterType == FilterType.MOD) {
                fluidMatches = getFluidModId(stack.getFluid()).equals(
                    getFluidModId(
                        filterSlot.getFilter()
                            .getFluid()));
            } else if (this.filterType == FilterType.TAG) {
                FluidTagFilterExtra tagFilterExtra = (FluidTagFilterExtra) this.savedFilters
                    .get(FilterType.TAG.getName());
                String tag = tagFilterExtra.getExtra()
                    .get(filterSlot.getFilterID());
                if (tag != null) {
                    fluidMatches = FluidTagFilterExtra.getTags(stack)
                        .contains(tag);
                }
            }

            if (fluidMatches) {
                matches = true;
                break;
            }
        }
        // Apply whitelist/blacklist logic
        return this.type.test(matches);
    }

    /** 1.7.10 fluids carry no mod id; derive it from the fluid's block where possible. */
    public static String getFluidModId(Fluid fluid) {
        if (fluid == null) return "unknown";
        Block block = fluid.getBlock();
        if (block != null) {
            GameRegistry.UniqueIdentifier identifier = GameRegistry.findUniqueIdentifierFor(block);
            if (identifier != null) return identifier.modId;
        }
        return "unknown";
    }
}
