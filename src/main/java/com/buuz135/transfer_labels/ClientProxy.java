package com.buuz135.transfer_labels;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;

import com.buuz135.transfer_labels.client.LabelClientEvents;
import com.buuz135.transfer_labels.client.gui.LabelGui;
import com.buuz135.transfer_labels.container.LabelContainer;
import com.buuz135.transfer_labels.network.LabelSyncMessage;
import com.buuz135.transfer_labels.network.SingleLabelSyncMessage;
import com.buuz135.transfer_labels.network.Tasks;
import com.buuz135.transfer_labels.storage.LabelBlock;
import com.buuz135.transfer_labels.storage.LabelInstance;
import com.buuz135.transfer_labels.storage.client.LabelClientStorage;
import com.buuz135.transfer_labels.util.BlockPos;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        LabelClientEvents clientEvents = new LabelClientEvents();
        MinecraftForge.EVENT_BUS.register(clientEvents);
        FMLCommonHandler.instance()
            .bus()
            .register(clientEvents);
    }

    @Override
    public void handleLabelSync(LabelSyncMessage message) {
        Tasks.CLIENT.add(() -> {
            WorldClient world = Minecraft.getMinecraft().theWorld;
            if (world != null) {
                LabelClientStorage.getStorage(world)
                    .loadNearby(message.anchor, message.distance, message.labels);
            }
        });
    }

    @Override
    public void handleSingleLabelSync(SingleLabelSyncMessage message) {
        Tasks.CLIENT.add(() -> {
            WorldClient world = Minecraft.getMinecraft().theWorld;
            if (world != null) {
                LabelClientStorage.getStorage(world)
                    .getLabelBlocksMap()
                    .computeIfAbsent(message.pos, blockPos -> new LabelBlock(blockPos, world))
                    .deserializeNBT(message.label);
            }
        });
    }

    @Override
    public Object createLabelGui(EntityPlayer player, World world, BlockPos pos, ForgeDirection direction) {
        LabelInstance instance = GuiHandler.findLabel(LabelClientStorage.getStorage(world), pos, direction);
        if (instance == null) return null;
        return new LabelGui(new LabelContainer(instance, player.inventory), instance);
    }
}
