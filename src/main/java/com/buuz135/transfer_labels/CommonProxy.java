package com.buuz135.transfer_labels;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;

import com.buuz135.transfer_labels.network.LabelSyncMessage;
import com.buuz135.transfer_labels.network.SingleLabelSyncMessage;
import com.buuz135.transfer_labels.util.BlockPos;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
    }

    public void init(FMLInitializationEvent event) {
        LabelInteractEvents interactEvents = new LabelInteractEvents();
        MinecraftForge.EVENT_BUS.register(interactEvents);
        FMLCommonHandler.instance()
            .bus()
            .register(interactEvents);
    }

    public void postInit(FMLPostInitializationEvent event) {}

    // ==================== 客户端网络回调（服务端为空实现） ====================

    public void handleLabelSync(LabelSyncMessage message) {}

    public void handleSingleLabelSync(SingleLabelSyncMessage message) {}

    public Object createLabelGui(EntityPlayer player, World world, BlockPos pos, ForgeDirection direction) {
        return null;
    }
}
