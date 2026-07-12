package com.buuz135.transfer_labels.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import com.buuz135.transfer_labels.LabelInteractEvents;
import com.buuz135.transfer_labels.TransferLabels;
import com.buuz135.transfer_labels.item.TransferLabelItem;
import com.buuz135.transfer_labels.network.Tasks;
import com.buuz135.transfer_labels.storage.LabelBlock;
import com.buuz135.transfer_labels.storage.LabelInstance;
import com.buuz135.transfer_labels.storage.client.LabelClientStorage;
import com.buuz135.transfer_labels.util.BlockPos;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LabelClientEvents {

    public static final List<LabelInteractEvents.DelayedEvent> CLIENT_UPDATE = new ArrayList<>();

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            Tasks.drain(Tasks.CLIENT);
        } else {
            WorldClient world = Minecraft.getMinecraft().theWorld;
            if (world == null) {
                CLIENT_UPDATE.clear();
                return;
            }
            int delay = 2;
            for (LabelInteractEvents.DelayedEvent delayedEvent : CLIENT_UPDATE) {
                if (world.getTotalWorldTime() > (delayedEvent.time + delay) && delayedEvent.world == world) {
                    world.destroyBlockInWorldPartially(
                        delayedEvent.entityId,
                        delayedEvent.pos.getX(),
                        delayedEvent.pos.getY(),
                        delayedEvent.pos.getZ(),
                        -1);
                }
            }
            CLIENT_UPDATE.removeIf(
                delayedEvent -> world.getTotalWorldTime() > (delayedEvent.time + delay) && delayedEvent.world == world);
        }
    }

    @SubscribeEvent
    public void blockOverlayEvent(DrawBlockHighlightEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = event.player;
        if (player == null || event.target == null) return;
        double distance = mc.playerController.getBlockReachDistance();
        ItemStack held = player.getHeldItem();
        boolean isHoldingAccessor = LabelInteractEvents.isHoldingAccessor(held);
        boolean isHoldingLabel = isValidClientInteraction(held);
        if (isHoldingLabel) {
            BlockPos targetPos = new BlockPos(event.target.blockX, event.target.blockY, event.target.blockZ);
            List<LabelBlock> nearbyLabels = LabelClientStorage
                .getNearbyLabels(mc.theWorld, targetPos, (int) (distance * distance));
            Pair<LabelBlock, ForgeDirection> pair = RayTraceUtils
                .rayTraceLabels(nearbyLabels, mc.theWorld, player, distance, isHoldingAccessor ? null : targetPos);
            if (pair != null) {
                event.setCanceled(true);

                float partialTicks = event.partialTicks;
                double camX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
                double camY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
                double camZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

                AxisAlignedBB box = pair.getLeft()
                    .collectShapes(pair.getRight());
                if (box == null) return;

                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.35F);
                GL11.glLineWidth(2.0F);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glDepthMask(false);
                drawOutlinedBoundingBox(
                    box.expand(0.002D, 0.002D, 0.002D)
                        .getOffsetBoundingBox(-camX, -camY, -camZ));
                GL11.glDepthMask(true);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }

    private static void drawOutlinedBoundingBox(AxisAlignedBB bb) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_LINE_STRIP);
        tessellator.addVertex(bb.minX, bb.minY, bb.minZ);
        tessellator.addVertex(bb.maxX, bb.minY, bb.minZ);
        tessellator.addVertex(bb.maxX, bb.minY, bb.maxZ);
        tessellator.addVertex(bb.minX, bb.minY, bb.maxZ);
        tessellator.addVertex(bb.minX, bb.minY, bb.minZ);
        tessellator.draw();
        tessellator.startDrawing(GL11.GL_LINE_STRIP);
        tessellator.addVertex(bb.minX, bb.maxY, bb.minZ);
        tessellator.addVertex(bb.maxX, bb.maxY, bb.minZ);
        tessellator.addVertex(bb.maxX, bb.maxY, bb.maxZ);
        tessellator.addVertex(bb.minX, bb.maxY, bb.maxZ);
        tessellator.addVertex(bb.minX, bb.maxY, bb.minZ);
        tessellator.draw();
        tessellator.startDrawing(GL11.GL_LINES);
        tessellator.addVertex(bb.minX, bb.minY, bb.minZ);
        tessellator.addVertex(bb.minX, bb.maxY, bb.minZ);
        tessellator.addVertex(bb.maxX, bb.minY, bb.minZ);
        tessellator.addVertex(bb.maxX, bb.maxY, bb.minZ);
        tessellator.addVertex(bb.maxX, bb.minY, bb.maxZ);
        tessellator.addVertex(bb.maxX, bb.maxY, bb.maxZ);
        tessellator.addVertex(bb.minX, bb.minY, bb.maxZ);
        tessellator.addVertex(bb.minX, bb.maxY, bb.maxZ);
        tessellator.draw();
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null || mc.theWorld == null) return;

        BlockPos currentPos = new BlockPos(
            (int) Math.floor(player.posX),
            (int) Math.floor(player.posY),
            (int) Math.floor(player.posZ));
        ItemStack held = player.getHeldItem();
        boolean isHoldingAccessor = LabelInteractEvents.isHoldingAccessor(held);
        boolean isHoldingLabel = isValidClientInteraction(held);
        int transparentAlpha = 100;
        int alpha = isHoldingLabel ? 255 : transparentAlpha;
        List<LabelBlock> nearbyLabels = LabelClientStorage.getNearbyLabels(mc.theWorld, currentPos, 20 * 20);
        if (nearbyLabels.isEmpty()) return;

        float partialTicks = event.partialTicks;
        EntityLivingBase view = mc.renderViewEntity;
        double camX = view.lastTickPosX + (view.posX - view.lastTickPosX) * partialTicks;
        double camY = view.lastTickPosY + (view.posY - view.lastTickPosY) * partialTicks;
        double camZ = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * partialTicks;

        GL11.glPushMatrix();
        GL11.glTranslated(-camX, -camY, -camZ);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        // See-through mode when sneaking with a label or holding the accessor
        if ((isHoldingLabel && player.isSneaking()) || isHoldingAccessor) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        } else {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }

        float size = 1.0F;
        Tessellator tessellator = Tessellator.instance;
        for (LabelBlock label : nearbyLabels) {
            double x = label.getPos()
                .getX();
            double y = label.getPos()
                .getY();
            double z = label.getPos()
                .getZ();

            GL11.glPushMatrix();
            GL11.glTranslated(x, y, z);

            for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
                LabelInstance instance = label.getLabels()
                    .get(direction);
                if (instance == null) continue;
                mc.getTextureManager()
                    .bindTexture(getTexture(instance.getLabel()));
                tessellator.startDrawingQuads();
                tessellator.setColorRGBA(255, 255, 255, alpha);
                switch (direction) {
                    case NORTH:
                        tessellator.addVertexWithUV(0, 0, -0.001F, 1, 1);
                        tessellator.addVertexWithUV(0, size, -0.001F, 1, 0);
                        tessellator.addVertexWithUV(size, size, -0.001F, 0, 0);
                        tessellator.addVertexWithUV(size, 0, -0.001F, 0, 1);
                        break;
                    case SOUTH:
                        tessellator.addVertexWithUV(size, 0, 1.001F, 1, 1);
                        tessellator.addVertexWithUV(size, size, 1.001F, 1, 0);
                        tessellator.addVertexWithUV(0, size, 1.001F, 0, 0);
                        tessellator.addVertexWithUV(0, 0, 1.001F, 0, 1);
                        break;
                    case WEST:
                        tessellator.addVertexWithUV(-0.001F, 0, size, 1, 1);
                        tessellator.addVertexWithUV(-0.001F, size, size, 1, 0);
                        tessellator.addVertexWithUV(-0.001F, size, 0, 0, 0);
                        tessellator.addVertexWithUV(-0.001F, 0, 0, 0, 1);
                        break;
                    case EAST:
                        tessellator.addVertexWithUV(1.001F, 0, 0, 1, 1);
                        tessellator.addVertexWithUV(1.001F, size, 0, 1, 0);
                        tessellator.addVertexWithUV(1.001F, size, size, 0, 0);
                        tessellator.addVertexWithUV(1.001F, 0, size, 0, 1);
                        break;
                    case UP:
                        tessellator.addVertexWithUV(0, 1.001F, 0, 0, 0);
                        tessellator.addVertexWithUV(0, 1.001F, size, 0, 1);
                        tessellator.addVertexWithUV(size, 1.001F, size, 1, 1);
                        tessellator.addVertexWithUV(size, 1.001F, 0, 1, 0);
                        break;
                    case DOWN:
                        tessellator.addVertexWithUV(0, -0.001F, size, 0, 0);
                        tessellator.addVertexWithUV(0, -0.001F, 0, 0, 1);
                        tessellator.addVertexWithUV(size, -0.001F, 0, 1, 1);
                        tessellator.addVertexWithUV(size, -0.001F, size, 1, 0);
                        break;
                    default:
                        break;
                }
                tessellator.draw();
            }

            GL11.glPopMatrix();
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }

    public static ResourceLocation getTexture(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof TransferLabelItem) {
            return ((TransferLabelItem) stack.getItem()).getTexture();
        }
        return new ResourceLocation(TransferLabels.MODID, "textures/items/itemstack_insert_transfer_label.png");
    }

    public static boolean isValidClientInteraction(ItemStack stack) {
        return stack != null
            && (stack.getItem() instanceof TransferLabelItem || LabelInteractEvents.isHoldingAccessor(stack));
    }

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event) {
        if (!event.world.isRemote) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = event.entityPlayer;
        ItemStack held = player.getHeldItem();
        BlockPos eventPos = new BlockPos(event.x, event.y, event.z);
        double distance = mc.playerController.getBlockReachDistance();

        // NOTE: unlike modern versions, cancelling RIGHT_CLICK_BLOCK on the 1.7.10 client would
        // swallow the C08 placement packet entirely and the server would never see the click, so
        // right clicks are left untouched here and resolved authoritatively on the server.
        if (event.action == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK && held != null
            && held.getItem() instanceof TransferLabelItem) {
            List<LabelBlock> nearbyLabels = LabelClientStorage
                .getNearbyLabels(event.world, eventPos, (int) (distance * distance));
            Pair<LabelBlock, ForgeDirection> pair = RayTraceUtils
                .rayTraceLabels(nearbyLabels, event.world, player, distance, eventPos);
            if (pair != null) {
                event.setCanceled(true);
                CLIENT_UPDATE.add(
                    new LabelInteractEvents.DelayedEvent(
                        event.world,
                        player.getEntityId(),
                        eventPos,
                        event.world.getTotalWorldTime()));
            }
        }
    }
}
