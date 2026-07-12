package com.buuz135.transfer_labels.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.buuz135.transfer_labels.TransferLabels;
import com.buuz135.transfer_labels.container.LabelContainer;
import com.buuz135.transfer_labels.filter.FilterSlot;
import com.buuz135.transfer_labels.filter.FilterType;
import com.buuz135.transfer_labels.filter.FluidFilter;
import com.buuz135.transfer_labels.filter.ILabelFilter;
import com.buuz135.transfer_labels.filter.extras.FluidTagFilterExtra;
import com.buuz135.transfer_labels.filter.extras.NumberFilterExtra;
import com.buuz135.transfer_labels.filter.extras.TagFilterExtra;
import com.buuz135.transfer_labels.network.LabelButtonMessage;
import com.buuz135.transfer_labels.network.PacketHandler;
import com.buuz135.transfer_labels.storage.LabelInstance;
import com.buuz135.transfer_labels.util.NumberUtils;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LabelGui extends GuiContainer {

    private static final ResourceLocation TEXTURES = new ResourceLocation(
        TransferLabels.MODID,
        "textures/gui/textures.png");
    private static final RenderItem ITEM_RENDER = new RenderItem();

    // 1.7.10 dye firework colors, matching the modern DyeColor values used by the source
    private static final int COLOR_BLUE = 2437522;
    private static final int COLOR_GREEN = 3887386;
    private static final int COLOR_RED = 11743532;
    private static final int COLOR_PURPLE = 8073150;
    private static final int COLOR_YELLOW = 14602026;
    private static final int COLOR_LIME = 4312372;

    private static final int SELECTOR_X = 13;
    private static final int SELECTOR_Y = 28;
    private static final int WHITELIST_X = 13;
    private static final int WHITELIST_Y = 64;

    private final LabelInstance instance;

    public LabelGui(LabelContainer container, LabelInstance instance) {
        super(container);
        this.instance = instance;
        this.xSize = 176;
        this.ySize = 184;
    }

    private ILabelFilter<?> getFilter() {
        return instance.getFilter();
    }

    // ==================== 背景层 ====================

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawPanel(guiLeft, guiTop, xSize, ySize);

        // Player inventory slot frames
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotFrame(guiLeft + 7 + col * 18, guiTop + 101 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotFrame(guiLeft + 7 + col * 18, guiTop + 159);
        }

        // Upgrade slots (amount = purple, speed = lime)
        drawUpgradeSlot(
            guiLeft + 144,
            guiTop + 29,
            COLOR_PURPLE,
            instance.getAmountFilter()
                .getStackInSlot(0));
        drawUpgradeSlot(
            guiLeft + 144,
            guiTop + 65,
            COLOR_LIME,
            instance.getSpeedFilter()
                .getStackInSlot(0));

        ILabelFilter<?> filter = getFilter();
        if (filter != null) {
            drawFilterGrid(filter);

            // drawRect leaves GL_BLEND disabled; the button textures have transparent
            // pixels that render black unless blending is restored first.
            GL11.glColor4f(1F, 1F, 1F, 1F);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            mc.getTextureManager()
                .bindTexture(TEXTURES);
            // Filter type selector button
            drawTexturedModalRect(guiLeft + SELECTOR_X, guiTop + SELECTOR_Y, getSelectorU(filter), 0, 20, 20);

            // Whitelist/blacklist toggle button
            drawTexturedModalRect(
                guiLeft + WHITELIST_X,
                guiTop + WHITELIST_Y,
                filter.getType() == ILabelFilter.Type.WHITELIST ? 0 : 20,
                0,
                20,
                20);
            GL11.glDisable(GL11.GL_BLEND);
        }
    }

    private int getSelectorU(ILabelFilter<?> filter) {
        switch (filter.getFilterType()
            .getName()) {
            case "normal":
                return 40;
            case "regulating":
                return 60;
            case "exact_count":
                return 80;
            case "mod":
                return 100;
            case "tag":
                return 120;
            default:
                return 40;
        }
    }

    private int getFilterFillColor(ILabelFilter<?> filter) {
        int rgb;
        switch (filter.getFilterType()
            .getName()) {
            case "regulating":
                rgb = COLOR_GREEN;
                break;
            case "exact_count":
                rgb = COLOR_RED;
                break;
            case "mod":
                rgb = COLOR_PURPLE;
                break;
            case "tag":
                rgb = COLOR_YELLOW;
                break;
            default:
                rgb = COLOR_BLUE;
                break;
        }
        return 0x80000000 | rgb;
    }

    private void drawFilterGrid(ILabelFilter<?> filter) {
        int fillColor = getFilterFillColor(filter);
        int i = 0;
        for (FilterSlot<?> filterSlot : filter.getFilterSlots()) {
            if (filterSlot != null) {
                int sx = guiLeft + filterSlot.getX();
                int sy = guiTop + filterSlot.getY();
                drawSlotFrame(sx, sy);
                drawRect(sx + 1, sy + 1, sx + 17, sy + 17, fillColor);
                GL11.glColor4f(1F, 1F, 1F, 1F);

                Object content = filterSlot.getFilter();
                if (content != null) {
                    if (content instanceof ItemStack) {
                        drawGhostItem((ItemStack) content, sx + 1, sy + 1);
                    } else if (content instanceof FluidStack) {
                        drawFluid((FluidStack) content, sx + 1, sy + 1);
                    }

                    if (filter.getFilterType() == FilterType.EXACT_COUNT
                        || filter.getFilterType() == FilterType.REGULATING) {
                        NumberFilterExtra extra = (NumberFilterExtra) filter.getSavedFilters()
                            .get(
                                filter.getFilterType()
                                    .getName());
                        String amount = NumberUtils.getFormatedBigNumber(
                            extra.getExtra()
                                .get(i));
                        GL11.glPushMatrix();
                        GL11.glTranslated(sx + 17 - fontRendererObj.getStringWidth(amount) / 2D, sy + 13, 200.0D);
                        GL11.glScalef(0.5F, 0.5F, 0.5F);
                        fontRendererObj.drawStringWithShadow(amount, 0, 0, 0xFFFFFF);
                        GL11.glPopMatrix();
                    }
                    if (filter.getFilterType() == FilterType.TAG) {
                        String text = getSelectedTag(filter, i, content);
                        if (text == null) text = "";
                        GL11.glPushMatrix();
                        enableScissor(sx + 2, sy, sx + 16, sy + 17);
                        GL11.glTranslated(sx + 1, sy + 6, 200.0D);
                        GL11.glScalef(0.5F, 0.5F, 0.5F);
                        renderScrollingString(text, 2, 0, 30, 32, 0xFFFFFF);
                        GL11.glDisable(GL11.GL_SCISSOR_TEST);
                        GL11.glPopMatrix();
                    }
                }
            }
            ++i;
        }
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    private String getSelectedTag(ILabelFilter<?> filter, int slot, Object content) {
        Object extra = filter.getSavedFilters()
            .get(FilterType.TAG.getName());
        if (extra instanceof TagFilterExtra) {
            String tag = ((TagFilterExtra) extra).getExtra()
                .get(slot);
            if (tag != null) return tag;
            List<String> tags = TagFilterExtra.getTags((ItemStack) content);
            return tags.isEmpty() ? "" : tags.get(0);
        } else if (extra instanceof FluidTagFilterExtra) {
            String tag = ((FluidTagFilterExtra) extra).getExtra()
                .get(slot);
            if (tag != null) return tag;
            List<String> tags = FluidTagFilterExtra.getTags((FluidStack) content);
            return tags.isEmpty() ? "" : tags.get(0);
        }
        return "";
    }

    private List<String> getContentTags(Object content) {
        if (content instanceof ItemStack) return TagFilterExtra.getTags((ItemStack) content);
        if (content instanceof FluidStack) return FluidTagFilterExtra.getTags((FluidStack) content);
        return new ArrayList<>();
    }

    private void drawUpgradeSlot(int x, int y, int rgb, ItemStack current) {
        drawSlotFrame(x, y);
        drawRect(x + 1, y + 1, x + 17, y + 17, 0x80000000 | rgb);
        GL11.glColor4f(1F, 1F, 1F, 1F);
        if (current == null && instance.getLabel() != null) {
            // Ghost of the accepted upgrade item
            drawGhostItem(instance.getLabel(), x + 1, y + 1);
            GL11.glPushMatrix();
            GL11.glTranslatef(0, 0, 250);
            drawRect(x + 1, y + 1, x + 17, y + 17, 0x808B8B8B);
            GL11.glPopMatrix();
            GL11.glColor4f(1F, 1F, 1F, 1F);
        }
    }

    private void drawGhostItem(ItemStack stack, int x, int y) {
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        ITEM_RENDER.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), stack, x, y);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    private void drawFluid(FluidStack fluidStack, int x, int y) {
        if (fluidStack.getFluid() == null) return;
        IIcon icon = fluidStack.getFluid()
            .getIcon(fluidStack);
        if (icon == null) icon = fluidStack.getFluid()
            .getStillIcon();
        if (icon == null) return;
        int color = fluidStack.getFluid()
            .getColor(fluidStack);
        mc.getTextureManager()
            .bindTexture(TextureMap.locationBlocksTexture);
        GL11.glColor4f(((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F, (color & 0xFF) / 255F, 1F);
        drawTexturedModelRectFromIcon(x, y, icon, 16, 16);
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    // ==================== 前景层 ====================

    @SuppressWarnings("unchecked")
    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // Title, mirrors the source MenuProvider display name
        fontRendererObj.drawString(
            "Label " + instance.getFacing()
                .name()
                .toLowerCase(Locale.ROOT),
            8,
            6,
            0x404040);

        // Small captions over the upgrade slots
        drawSmallCenteredText(StatCollector.translateToLocal("tooltip.transfer_labels.slot.amount"), 153, 30);
        drawSmallCenteredText(StatCollector.translateToLocal("tooltip.transfer_labels.slot.speed"), 153, 66);

        ILabelFilter<?> filter = getFilter();
        if (filter == null) return;

        int rmx = mouseX - guiLeft;
        int rmy = mouseY - guiTop;

        // Filter slot hover overlay + tooltip
        int i = 0;
        for (FilterSlot<?> filterSlot : filter.getFilterSlots()) {
            if (filterSlot != null && rmx > filterSlot.getX() + 1
                && rmx < filterSlot.getX() + 16
                && rmy > filterSlot.getY() + 1
                && rmy < filterSlot.getY() + 16) {
                GL11.glPushMatrix();
                GL11.glTranslated(0, 0, 200);
                drawRect(
                    filterSlot.getX() + 1,
                    filterSlot.getY() + 1,
                    filterSlot.getX() + 17,
                    filterSlot.getY() + 17,
                    0x80FFFFFF);
                GL11.glPopMatrix();
                GL11.glColor4f(1F, 1F, 1F, 1F);

                Object content = filterSlot.getFilter();
                ItemStack carried = mc.thePlayer.inventory.getItemStack();
                if (content != null && carried == null) {
                    List<String> tooltip = new ArrayList<>();
                    if (content instanceof ItemStack) {
                        tooltip.addAll(
                            ((ItemStack) content).getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips));
                    } else if (content instanceof FluidStack) {
                        tooltip.add(((FluidStack) content).getLocalizedName());
                    }
                    if (filter.getFilterType() == FilterType.EXACT_COUNT
                        || filter.getFilterType() == FilterType.REGULATING) {
                        NumberFilterExtra extra = (NumberFilterExtra) filter.getSavedFilters()
                            .get(
                                filter.getFilterType()
                                    .getName());
                        tooltip.add(
                            EnumChatFormatting.GRAY + "Amount: "
                                + extra.getExtra()
                                    .get(i)
                                + (content instanceof FluidStack ? " mb" : ""));
                    }
                    if (filter.getFilterType() == FilterType.TAG) {
                        List<String> stackTags = getContentTags(content);
                        String selected = getSelectedTag(filter, i, content);
                        tooltip.add(EnumChatFormatting.GRAY + "Selected Tag: ");
                        if (!stackTags.isEmpty()) {
                            for (String tag : stackTags) {
                                tooltip.add(
                                    " " + (tag.equalsIgnoreCase(selected)
                                        ? EnumChatFormatting.GOLD + "[" + tag + EnumChatFormatting.GOLD + "]"
                                        : EnumChatFormatting.GRAY + tag));
                            }
                        } else {
                            tooltip.add(EnumChatFormatting.GRAY + " None");
                        }
                        tooltip
                            .add(EnumChatFormatting.DARK_GRAY + StatCollector.translateToLocal("filter.type.scroll"));
                    }
                    this.func_146283_a(tooltip, rmx, rmy);
                    GL11.glDisable(GL11.GL_LIGHTING);
                    GL11.glColor4f(1F, 1F, 1F, 1F);
                }
            }
            ++i;
        }

        // Filter type selector hover
        if (isInArea(rmx, rmy, SELECTOR_X, SELECTOR_Y, 20, 20)) {
            drawRect(SELECTOR_X + 2, SELECTOR_Y + 2, SELECTOR_X + 18, SELECTOR_Y + 17, 0x80FFFFFF);
            GL11.glColor4f(1F, 1F, 1F, 1F);
            List<String> lines = new ArrayList<>();
            String current = StatCollector.translateToLocal(
                filter.getFilterType()
                    .getDisplayName());
            for (FilterType filterType : FilterType.FILTERS) {
                for (String line : filterType.getTooltip()) {
                    lines.add(
                        line.contains(current) ? EnumChatFormatting.GOLD + "[" + line + EnumChatFormatting.GOLD + "]"
                            : EnumChatFormatting.GRAY + line);
                }
            }
            String scrollHint = EnumChatFormatting.DARK_GRAY + StatCollector.translateToLocal("filter.type.scroll");
            lines.add(
                scrollHint.contains(current)
                    ? EnumChatFormatting.GOLD + "[" + scrollHint + EnumChatFormatting.GOLD + "]"
                    : EnumChatFormatting.GRAY + scrollHint);
            this.func_146283_a(lines, rmx, rmy);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glColor4f(1F, 1F, 1F, 1F);
        }

        // Whitelist/blacklist button hover
        if (isInArea(rmx, rmy, WHITELIST_X, WHITELIST_Y, 18, 18)) {
            drawRect(WHITELIST_X + 2, WHITELIST_Y + 2, WHITELIST_X + 18, WHITELIST_Y + 17, 0x80FFFFFF);
            GL11.glColor4f(1F, 1F, 1F, 1F);
        }
    }

    private void drawSmallCenteredText(String text, int x, int y) {
        GL11.glPushMatrix();
        GL11.glTranslatef(0, 0, 300);
        float scaling = 0.5F;
        GL11.glScalef(scaling, scaling, scaling);
        drawCenteredString(fontRendererObj, text, (int) (x / scaling), (int) (y / scaling), 0xFFFFFF);
        GL11.glPopMatrix();
    }

    private boolean isInArea(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    // ==================== 交互 ====================

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        ILabelFilter<?> filter = getFilter();
        if (filter != null) {
            for (FilterSlot<?> filterSlot : filter.getFilterSlots()) {
                if (filterSlot != null && mouseX > guiLeft + filterSlot.getX() + 1
                    && mouseX < guiLeft + filterSlot.getX() + 16
                    && mouseY > guiTop + filterSlot.getY() + 1
                    && mouseY < guiTop + filterSlot.getY() + 16) {
                    NBTTagCompound compoundNBT = new NBTTagCompound();
                    compoundNBT.setString("Name", filter.getName());
                    compoundNBT.setInteger("Slot", filterSlot.getFilterID());
                    ItemStack carried = mc.thePlayer.inventory.getItemStack();
                    if (carried != null) {
                        NBTTagCompound stackTag = new NBTTagCompound();
                        carried.writeToNBT(stackTag);
                        compoundNBT.setTag("Filter", stackTag);
                    }
                    sendButton(-2, compoundNBT);
                    return;
                }
            }
            if (isInArea(mouseX - guiLeft, mouseY - guiTop, WHITELIST_X, WHITELIST_Y, 18, 18)) {
                sendButton(54571, new NBTTagCompound());
                playClickSound(1.0F);
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0) return;
        ILabelFilter<?> filter = getFilter();
        if (filter == null) return;

        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        double scroll = Integer.signum(dWheel);
        int rmx = mouseX - guiLeft;
        int rmy = mouseY - guiTop;

        // Filter type selector
        if (isInArea(rmx, rmy, SELECTOR_X, SELECTOR_Y, 20, 20)) {
            NBTTagCompound compoundNBT = new NBTTagCompound();
            compoundNBT.setString("Scrollable_Name", "filter_selector");
            compoundNBT.setDouble("Scroll", scroll);
            sendButton(-7, compoundNBT);
            playClickSound(2.0F);
            return;
        }

        // Filter slots
        boolean isFluid = filter instanceof FluidFilter;
        int i = 0;
        for (FilterSlot<?> filterSlot : filter.getFilterSlots()) {
            if (filterSlot != null && rmx > filterSlot.getX() + 1
                && rmx < filterSlot.getX() + 16
                && rmy > filterSlot.getY() + 1
                && rmy < filterSlot.getY() + 16) {
                if (filter.getFilterType() == FilterType.EXACT_COUNT
                    || filter.getFilterType() == FilterType.REGULATING) {
                    double multiplier = 1;
                    boolean alt = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
                    if (isFluid) {
                        multiplier *= (GuiScreen.isShiftKeyDown() ? 10 : 1) * (GuiScreen.isCtrlKeyDown() ? 10 : 1)
                            * (alt ? 10 : 1);
                    } else {
                        multiplier *= (GuiScreen.isShiftKeyDown() ? 8 : 1) * (GuiScreen.isCtrlKeyDown() ? 8 : 1)
                            * (alt ? 16 : 1);
                    }
                    NBTTagCompound compoundNBT = new NBTTagCompound();
                    compoundNBT.setInteger("FilterAmount", i);
                    compoundNBT.setDouble("Scroll", scroll * multiplier);
                    sendButton(-7, compoundNBT);
                    playClickSound(2.0F);
                    return;
                }
                if (filter.getFilterType() == FilterType.TAG) {
                    NBTTagCompound compoundNBT = new NBTTagCompound();
                    compoundNBT.setInteger("FilterTag", i);
                    compoundNBT.setDouble("Scroll", scroll);
                    sendButton(-7, compoundNBT);
                    playClickSound(2.0F);
                    return;
                }
            }
            ++i;
        }
    }

    private void sendButton(int id, NBTTagCompound tag) {
        PacketHandler.INSTANCE.sendToServer(new LabelButtonMessage(instance.getPos(), instance.getFacing(), id, tag));
    }

    private void playClickSound(float pitch) {
        mc.getSoundHandler()
            .playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("random.click"), pitch));
    }

    // ==================== 绘制工具 ====================

    /** Programmatic vanilla-style panel (beveled gray box). */
    private void drawPanel(int x, int y, int w, int h) {
        // outline
        drawRect(x + 2, y, x + w - 2, y + 1, 0xFF000000);
        drawRect(x + 2, y + h - 1, x + w - 2, y + h, 0xFF000000);
        drawRect(x, y + 2, x + 1, y + h - 2, 0xFF000000);
        drawRect(x + w - 1, y + 2, x + w, y + h - 2, 0xFF000000);
        drawRect(x + 1, y + 1, x + 2, y + 2, 0xFF000000);
        drawRect(x + w - 2, y + 1, x + w - 1, y + 2, 0xFF000000);
        drawRect(x + 1, y + h - 2, x + 2, y + h - 1, 0xFF000000);
        drawRect(x + w - 2, y + h - 2, x + w - 1, y + h - 1, 0xFF000000);
        // fill
        drawRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);
        // bevel
        drawRect(x + 2, y + 1, x + w - 2, y + 3, 0xFFFFFFFF);
        drawRect(x + 1, y + 2, x + 3, y + h - 2, 0xFFFFFFFF);
        drawRect(x + 2, y + h - 3, x + w - 2, y + h - 1, 0xFF555555);
        drawRect(x + w - 3, y + 2, x + w - 1, y + h - 2, 0xFF555555);
        // corner blends
        drawRect(x + w - 3, y + 2, x + w - 2, y + 3, 0xFFC6C6C6);
        drawRect(x + 2, y + h - 3, x + 3, y + h - 2, 0xFFC6C6C6);
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    /** Vanilla-style 18x18 slot frame at (x, y). */
    private void drawSlotFrame(int x, int y) {
        drawRect(x, y, x + 17, y + 1, 0xFF373737);
        drawRect(x, y + 1, x + 1, y + 17, 0xFF373737);
        drawRect(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        drawRect(x + 17, y + 1, x + 18, y + 17, 0xFFFFFFFF);
        drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        drawRect(x + 17, y, x + 18, y + 1, 0xFF8B8B8B);
        drawRect(x, y + 17, x + 1, y + 18, 0xFF8B8B8B);
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    private void enableScissor(int x, int y, int x2, int y2) {
        ScaledResolution resolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int scale = resolution.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
            x * scale,
            mc.displayHeight - y2 * scale,
            Math.max(0, (x2 - x) * scale),
            Math.max(0, (y2 - y) * scale));
    }

    /** Port of the source's renderScrollingString: bounces long text back and forth. */
    private void renderScrollingString(String text, int minX, int minY, int maxX, int maxY, int color) {
        int textWidth = fontRendererObj.getStringWidth(text);
        int j = (minY + maxY - 9) / 2 + 1;
        int k = maxX - minX;
        if (textWidth > k) {
            int l = textWidth - k;
            double d0 = System.currentTimeMillis() / 1000.0D;
            double d1 = Math.max(l * 0.5D, 3.0D);
            double d2 = Math.sin((Math.PI / 2D) * Math.cos((Math.PI * 2D) * d0 / d1)) / 2.0D + 0.5D;
            double d3 = d2 * l;
            fontRendererObj.drawStringWithShadow(text, minX - (int) d3, j, color);
        } else {
            int centerX = (minX + maxX) / 2;
            int i1 = MathHelper.clamp_int(centerX, minX + textWidth / 2, maxX - textWidth / 2);
            fontRendererObj.drawStringWithShadow(text, i1 - textWidth / 2, j, color);
        }
    }
}
