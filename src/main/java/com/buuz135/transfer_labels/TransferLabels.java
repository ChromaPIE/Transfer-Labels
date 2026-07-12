package com.buuz135.transfer_labels;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

import com.buuz135.transfer_labels.item.FluidStackTransferLabelItem;
import com.buuz135.transfer_labels.item.ItemStackTransferLabelItem;
import com.buuz135.transfer_labels.item.TransferLabelItem;
import com.buuz135.transfer_labels.network.PacketHandler;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(
    modid = TransferLabels.MODID,
    version = Tags.VERSION,
    name = "Transfer Labels",
    acceptedMinecraftVersions = "[1.7.10]")
public class TransferLabels {

    public static final String MODID = "transfer_labels";

    @Mod.Instance(MODID)
    public static TransferLabels instance;

    @SidedProxy(
        clientSide = "com.buuz135.transfer_labels.ClientProxy",
        serverSide = "com.buuz135.transfer_labels.CommonProxy")
    public static CommonProxy proxy;

    public static final CreativeTabs TAB = new CreativeTabs(MODID) {

        @Override
        public Item getTabIconItem() {
            return ITEMSTACK_EXTRACT_LABEL;
        }
    };

    public static Item ITEMSTACK_INSERT_LABEL;
    public static Item ITEMSTACK_EXTRACT_LABEL;
    public static Item FLUIDSTACK_INSERT_LABEL;
    public static Item FLUIDSTACK_EXTRACT_LABEL;
    public static Item LABEL_ACCESSOR;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);

        ITEMSTACK_INSERT_LABEL = new ItemStackTransferLabelItem(TransferLabelItem.Mode.INSERT);
        ITEMSTACK_EXTRACT_LABEL = new ItemStackTransferLabelItem(TransferLabelItem.Mode.EXTRACT);
        FLUIDSTACK_INSERT_LABEL = new FluidStackTransferLabelItem(TransferLabelItem.Mode.INSERT);
        FLUIDSTACK_EXTRACT_LABEL = new FluidStackTransferLabelItem(TransferLabelItem.Mode.EXTRACT);
        LABEL_ACCESSOR = new Item().setMaxStackSize(1)
            .setUnlocalizedName(MODID + ".label_accessor")
            .setTextureName(MODID + ":label_accessor")
            .setCreativeTab(TAB);

        GameRegistry.registerItem(ITEMSTACK_INSERT_LABEL, "itemstack_insert_transfer_label");
        GameRegistry.registerItem(ITEMSTACK_EXTRACT_LABEL, "itemstack_extract_transfer_label");
        GameRegistry.registerItem(FLUIDSTACK_INSERT_LABEL, "fluidstack_insert_transfer_label");
        GameRegistry.registerItem(FLUIDSTACK_EXTRACT_LABEL, "fluidstack_extract_transfer_label");
        GameRegistry.registerItem(LABEL_ACCESSOR, "label_accessor");

        PacketHandler.init();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
        registerRecipes();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    private void registerRecipes() {
        if (OreDictionary.getOres("chestWood")
            .isEmpty()) {
            OreDictionary.registerOre("chestWood", Blocks.chest);
        }
        OreDictionary.registerOre("transferLabel", ITEMSTACK_INSERT_LABEL);
        OreDictionary.registerOre("transferLabel", ITEMSTACK_EXTRACT_LABEL);
        OreDictionary.registerOre("transferLabel", FLUIDSTACK_INSERT_LABEL);
        OreDictionary.registerOre("transferLabel", FLUIDSTACK_EXTRACT_LABEL);

        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                new ItemStack(ITEMSTACK_INSERT_LABEL, 2),
                " H ",
                " C ",
                " R ",
                'H',
                Blocks.hopper,
                'C',
                "chestWood",
                'R',
                Items.redstone));
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                new ItemStack(ITEMSTACK_EXTRACT_LABEL, 2),
                " R ",
                " C ",
                " H ",
                'H',
                Blocks.hopper,
                'C',
                "chestWood",
                'R',
                Items.redstone));
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                new ItemStack(FLUIDSTACK_INSERT_LABEL, 2),
                " H ",
                " C ",
                " R ",
                'H',
                Blocks.hopper,
                'C',
                Items.bucket,
                'R',
                Items.redstone));
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                new ItemStack(FLUIDSTACK_EXTRACT_LABEL, 2),
                " R ",
                " C ",
                " H ",
                'H',
                Blocks.hopper,
                'C',
                Items.bucket,
                'R',
                Items.redstone));

        GameRegistry.addShapelessRecipe(new ItemStack(ITEMSTACK_EXTRACT_LABEL), new ItemStack(ITEMSTACK_INSERT_LABEL));
        GameRegistry.addShapelessRecipe(new ItemStack(ITEMSTACK_INSERT_LABEL), new ItemStack(ITEMSTACK_EXTRACT_LABEL));
        GameRegistry
            .addShapelessRecipe(new ItemStack(FLUIDSTACK_EXTRACT_LABEL), new ItemStack(FLUIDSTACK_INSERT_LABEL));
        GameRegistry
            .addShapelessRecipe(new ItemStack(FLUIDSTACK_INSERT_LABEL), new ItemStack(FLUIDSTACK_EXTRACT_LABEL));

        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                new ItemStack(LABEL_ACCESSOR),
                "RCR",
                " R ",
                " R ",
                'R',
                "ingotCopper",
                'C',
                "transferLabel"));
    }
}
