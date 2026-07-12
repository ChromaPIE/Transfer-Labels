package com.buuz135.transfer_labels;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import org.apache.commons.lang3.tuple.Pair;

import com.buuz135.transfer_labels.client.RayTraceUtils;
import com.buuz135.transfer_labels.item.TransferLabelItem;
import com.buuz135.transfer_labels.network.LabelSyncMessage;
import com.buuz135.transfer_labels.network.PacketHandler;
import com.buuz135.transfer_labels.network.Tasks;
import com.buuz135.transfer_labels.storage.LabelBlock;
import com.buuz135.transfer_labels.storage.LabelStorage;
import com.buuz135.transfer_labels.util.BlockPos;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;

public class LabelInteractEvents {

    public static final List<DelayedEvent> SERVER_UPDATE = new ArrayList<>();

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            Tasks.drain(Tasks.SERVER);
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.side != Side.SERVER || !(event.world instanceof WorldServer)) return;
        WorldServer serverLevel = (WorldServer) event.world;
        if (event.phase == TickEvent.Phase.START) {
            for (LabelBlock value : LabelStorage.getStorageFor(serverLevel)
                .getLabelBlocks()) {
                if (serverLevel.blockExists(
                    value.getPos()
                        .getX(),
                    value.getPos()
                        .getY(),
                    value.getPos()
                        .getZ())) {
                    for (com.buuz135.transfer_labels.storage.LabelInstance label : value.getLabels()
                        .values()) {
                        label.work(serverLevel);
                    }
                }
            }
            if (serverLevel.getTotalWorldTime() % 10 == 0) {
                for (Object playerObj : serverLevel.playerEntities) {
                    if (playerObj instanceof EntityPlayerMP) {
                        EntityPlayerMP player = (EntityPlayerMP) playerObj;
                        BlockPos playerPos = getOnPos(player);
                        PacketHandler.INSTANCE.sendTo(
                            new LabelSyncMessage(
                                LabelStorage.getStorageFor(serverLevel)
                                    .saveNearby(playerPos, 100),
                                playerPos,
                                16),
                            player);
                    }
                }
            }
        } else if (event.phase == TickEvent.Phase.END) {
            int delay = 2;
            for (DelayedEvent delayedEvent : SERVER_UPDATE) {
                if (serverLevel.getTotalWorldTime() > (delayedEvent.time + delay)
                    && delayedEvent.world == serverLevel) {
                    serverLevel.destroyBlockInWorldPartially(
                        delayedEvent.entityId,
                        delayedEvent.pos.getX(),
                        delayedEvent.pos.getY(),
                        delayedEvent.pos.getZ(),
                        -1);
                }
            }
            SERVER_UPDATE.removeIf(
                delayedEvent -> serverLevel.getTotalWorldTime() > (delayedEvent.time + delay)
                    && delayedEvent.world == serverLevel);
        }
    }

    public static BlockPos getOnPos(EntityPlayer player) {
        return new BlockPos(
            MathHelper.floor_double(player.posX),
            MathHelper.floor_double(player.posY) - 1,
            MathHelper.floor_double(player.posZ));
    }

    public static boolean isHoldingAccessor(ItemStack stack) {
        return stack != null && stack.getItem() == TransferLabels.LABEL_ACCESSOR;
    }

    public static boolean isValidInteraction(ItemStack stack) {
        return stack != null && (stack.getItem() instanceof TransferLabelItem || isHoldingAccessor(stack));
    }

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event) {
        World world = event.world;
        if (world.isRemote || !(event.entityPlayer instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.entityPlayer;
        ItemStack held = player.getHeldItem();
        boolean holdingAccessor = isHoldingAccessor(held);
        if (!isValidInteraction(held)) return;

        BlockPos eventPos = new BlockPos(event.x, event.y, event.z);
        double distance = player.theItemInWorldManager.getBlockReachDistance();

        if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            List<LabelBlock> nearbyLabels = LabelStorage.getNearbyLabels(world, eventPos, 20);
            Pair<LabelBlock, ForgeDirection> pair = RayTraceUtils
                .rayTraceLabels(nearbyLabels, world, player, distance, holdingAccessor ? null : eventPos);
            if (pair != null) {
                event.setCanceled(true);
                player.openGui(
                    TransferLabels.instance,
                    pair.getRight()
                        .ordinal(),
                    world,
                    pair.getLeft()
                        .getPos()
                        .getX(),
                    pair.getLeft()
                        .getPos()
                        .getY(),
                    pair.getLeft()
                        .getPos()
                        .getZ());
            }
        } else if (event.action == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK) {
            List<LabelBlock> nearbyLabels = LabelStorage.getNearbyLabels(world, eventPos, (int) (distance * distance));
            Pair<LabelBlock, ForgeDirection> pair = RayTraceUtils
                .rayTraceLabels(nearbyLabels, world, player, distance, holdingAccessor ? null : eventPos);
            if (pair != null) {
                event.setCanceled(true);
                LabelStorage.removeLabel(
                    player,
                    world,
                    pair.getLeft()
                        .getPos(),
                    pair.getRight());
                BlockPos playerPos = getOnPos(player);
                PacketHandler.INSTANCE.sendTo(
                    new LabelSyncMessage(
                        LabelStorage.getStorageFor(world)
                            .saveNearby(playerPos, 100),
                        playerPos,
                        16),
                    player);
                world.playSoundAtEntity(player, "random.pop", 1F, 1F);
                SERVER_UPDATE.add(new DelayedEvent(world, player.getEntityId(), eventPos, world.getTotalWorldTime()));
            }
        }
    }

    public static class DelayedEvent {

        public final World world;
        public final int entityId;
        public final BlockPos pos;
        public final long time;

        public DelayedEvent(World world, int entityId, BlockPos pos, long time) {
            this.world = world;
            this.entityId = entityId;
            this.pos = pos;
            this.time = time;
        }
    }
}
