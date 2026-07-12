package com.buuz135.transfer_labels.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class PacketHandler {

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("transfer_labels");

    public static void init() {
        INSTANCE.registerMessage(LabelSyncMessage.Handler.class, LabelSyncMessage.class, 0, Side.CLIENT);
        INSTANCE.registerMessage(SingleLabelSyncMessage.Handler.class, SingleLabelSyncMessage.class, 1, Side.CLIENT);
        INSTANCE.registerMessage(LabelButtonMessage.Handler.class, LabelButtonMessage.class, 2, Side.SERVER);
    }
}
