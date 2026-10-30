package com.kootletland.client.module;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.kootletland.client.ClickTracker;
import com.kootletland.client.CombatTracker;
import com.kootletland.client.Theme;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;

public final class Modules {
    private Modules() {}

    /* ===================== HUD: one-liners ===================== */

    public static abstract class TextModule extends Module {
        protected TextModule(String name, String desc, Category c, boolean def, int x, int y) {
            super(name, desc, c, true, def, x, y);
        }
        protected abstract String text(Minecraft mc);

        @Override
        public void render(Minecraft mc, boolean editing) {
            String s = text(mc);
            width = mc.fontRendererObj.getStringWidth(s) + 10;
            height = 14;
            box();
            drawText(mc, s, x + 5, y + 3);
        }
    }

    public static class Fps extends TextModule {
        public Fps() { super("FPS", "Frames per second", Category.HUD, true, 4, 26); }
        @Override protected String text(Minecraft mc) { return "FPS: " + Minecraft.getDebugFPS(); }
    }

    public static class Ping extends TextModule {
        public Ping() { super("Ping", "Latency to the server", Category.HUD, true, 4, 42); }
        @Override protected String text(Minecraft mc) {
            NetHandlerPlayClient nh = mc.getNetHandler();
            if (nh == null || mc.thePlayer == null) return "Ping: -";
            NetworkPlayerInfo info = nh.getPlayerInfo(mc.thePlayer.getUniqueID());
            return "Ping: " + (info == null ? "-" : info.getResponseTime() + "ms");
        }
    }

    public static class Coords extends TextModule {
        public Coords() { super("Coords", "Your XYZ position", Category.HUD, true, 4, 58); }
        @Override protected String text(Minecraft mc) {
            if (mc.thePlayer == null) return "XYZ: -";
            return "XYZ: " + (int) Math.floor(mc.thePlayer.posX) + " "
                    + (int) Math.floor(mc.thePlayer.posY) + " "
                    + (int) Math.floor(mc.thePlayer.posZ);
        }
    }

    public static class Direction extends TextModule {
        private static final String[] DIRS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        public Direction() { super("Direction", "Compass direction you face", Category.HUD, false, 110, 4); }
        @Override protected String text(Minecraft mc) {
            if (mc.thePlayer == null) return "Facing: -";
            int i = MathHelper.floor_double(mc.thePlayer.rotationYaw * 8.0F / 360.0F + 0.5D) & 7;
            return "Facing: " + DIRS[i];
        }
    }

    public static class Speed extends TextModule {
        private double bps;
        public Speed() { super("Speed", "Blocks per second", Category.HUD, false, 180, 4); }
        @Override public void tick(Minecraft mc) {
            EntityPlayerSP p = mc.thePlayer;
            double dx = p.posX - p.lastTickPosX, dz = p.posZ - p.lastTickPosZ;
            bps = bps * 0.7 + Math.sqrt(dx * dx + dz * dz) * 20.0 * 0.3;
        }
        @Override protected String text(Minecraft mc) {
            return "Speed: " + (Math.round(bps * 10.0) / 10.0) + " b/s";
        }
    }

    public static class Memory extends TextModule {
        public Memory() { super("Memory", "Java memory usage", Category.HUD, false, 250, 4); }
        @Override protected String text(Minecraft mc) {
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory() - rt.freeMemory()) >> 20;
            long max = rt.maxMemory() >> 20;
            return "Mem: " + used + "/" + max + " MB";
        }
    }

    public static class Clock extends TextModule {
        private final Setting.Bool use24;
        private final SimpleDateFormat f24 = new SimpleDateFormat("HH:mm");
        private final SimpleDateFormat f12 = new SimpleDateFormat("hh:mm a");
        public Clock() {
            super("Clock", "Real-world time", Category.HUD, false, 320, 4);
            use24 = addBool("24 Hour", true);
        }
        @Override protected String text(Minecraft mc) {
            return (use24.value ? f24 : f12).format(new Date());
        }
    }

    public static class Watermark extends Module {
        public Watermark() { super("Watermark", "Kootlet Client logo on screen", Category.HUD, true, true, 4, 4); drop(color); }
        @Override
        public void render(Minecraft mc, boolean editing) {
            String s = "Kootlet Client";
            width = 30 + mc.fontRendererObj.getStringWidth(s);
            height = 18;
            box();
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.enableBlend();
            mc.getTextureManager().bindTexture(Theme.LOGO);
            Gui.drawModalRectWithCustomSizedTexture(x + 6, y + 1, 0, 0, 16, 16, 16, 16);
            mc.fontRendererObj.drawStringWithShadow(s, x + 26, y + 5, Theme.accent());
        }
    }

    /* ===================== PvP ===================== */

    public static class Cps extends TextModule {
        public Cps() { super("CPS", "Clicks per second (left | right)", Category.PVP, true, 4, 74); }
        @Override protected String text(Minecraft mc) {
            return "CPS: " + ClickTracker.cps(0) + " | " + ClickTracker.cps(1);
        }
    }

    public static class Combo extends TextModule {
        public Combo() { super("Combo", "Hits in a row without being hit", Category.PVP, true, 4, 90); }
        @Override protected String text(Minecraft mc) { return "Combo: " + CombatTracker.combo; }
    }

    public static class Reach extends TextModule {
        public Reach() { super("Reach", "Distance of your last hit", Category.PVP, true, 4, 106); }
        @Override protected String text(Minecraft mc) {
            return "Reach: " + (CombatTracker.reach > 0 ? String.format(Locale.US, "%.2f", CombatTracker.reach) : "-");
        }
    }

    public static class Keystrokes extends Module {
        private final float[] a = new float[6];
        public Keystrokes() {
            super("Keystrokes", "WASD + mouse buttons with CPS", Category.PVP, true, true, 4, 126);
            drop(background);
            drop(color);
        }

        private void key(Minecraft mc, int i, int kx, int ky, int kw, int kh, String label, boolean down) {
            a[i] += ((down ? 1F : 0F) - a[i]) * 0.35F;
            Gui.drawRect(kx, ky, kx + kw, ky + kh, Theme.lerp(Theme.BG, Theme.alpha(Theme.accent(), 0xDD), a[i]));
            int tw = mc.fontRendererObj.getStringWidth(label);
            mc.fontRendererObj.drawString(label, kx + (kw - tw) / 2, ky + (kh - 8) / 2,
                    Theme.lerp(0xFFFFFFFF, 0xFF000000, a[i]));
        }

        @Override
        public void render(Minecraft mc, boolean editing) {
            width = 76;
            height = 72;
            key(mc, 0, x + 26, y, 24, 24, "W", mc.gameSettings.keyBindForward.isKeyDown());
            key(mc, 1, x, y + 26, 24, 24, "A", mc.gameSettings.keyBindLeft.isKeyDown());
            key(mc, 2, x + 26, y + 26, 24, 24, "S", mc.gameSettings.keyBindBack.isKeyDown());
            key(mc, 3, x + 52, y + 26, 24, 24, "D", mc.gameSettings.keyBindRight.isKeyDown());
            key(mc, 4, x, y + 52, 37, 20, "LMB " + ClickTracker.cps(0), Mouse.isButtonDown(0));
            key(mc, 5, x + 39, y + 52, 37, 20, "RMB " + ClickTracker.cps(1), Mouse.isButtonDown(1));
        }
    }

    public static class ArmorStatus extends Module {
        public ArmorStatus() {
            super("Armor Status", "Armor pieces with durability", Category.PVP, true, true, 100, 40);
            drop(color);
        }

        @Override
        public void render(Minecraft mc, boolean editing) {
            if (mc.thePlayer == null) return;
            boolean any = false;
            for (ItemStack s : mc.thePlayer.inventory.armorInventory) if (s != null) any = true;
            if (!any && !editing) return;

            width = 64;
            height = 4 * 18 + 4;
            box();
            RenderHelper.enableGUIStandardItemLighting();
            for (int i = 0; i < 4; i++) {
                ItemStack st = mc.thePlayer.inventory.armorInventory[3 - i];
                if (st == null) continue;
                int iy = y + 3 + i * 18;
                mc.getRenderItem().renderItemAndEffectIntoGUI(st, x + 5, iy);
                if (st.getMaxDamage() > 0) {
                    int left = st.getMaxDamage() - st.getItemDamage();
                    float ratio = left / (float) st.getMaxDamage();
                    int c = Theme.lerp(0xFFFF4545, 0xFF3DDC84, ratio);
                    mc.fontRendererObj.drawStringWithShadow(String.valueOf(left), x + 25, iy + 5, c);
                }
            }
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableLighting();
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.color(1F, 1F, 1F, 1F);
        }
    }

    public static class PotionStatus extends Module {
        public PotionStatus() {
            super("Potion Status", "Active effects with timers", Category.PVP, true, true, 100, 122);
            drop(color);
        }

        private String label(PotionEffect e) {
            return I18n.format(e.getEffectName()) + " " + (e.getAmplifier() + 1) + "  " + Potion.getDurationString(e);
        }

        @Override
        public void render(Minecraft mc, boolean editing) {
            if (mc.thePlayer == null) return;
            FontRenderer fr = mc.fontRendererObj;
            Collection<PotionEffect> fx = mc.thePlayer.getActivePotionEffects();
            if (fx.isEmpty()) {
                if (!editing) return;
                width = fr.getStringWidth("Potion Status") + 10;
                height = 14;
                box();
                fr.drawStringWithShadow("Potion Status", x + 5, y + 3, Theme.MUTED);
                return;
            }
            int maxW = 40;
            for (PotionEffect e : fx) maxW = Math.max(maxW, fr.getStringWidth(label(e)));
            width = maxW + 10;
            height = fx.size() * 12 + 4;
            box();
            int i = 0;
            for (PotionEffect e : fx) {
                Potion p = Potion.potionTypes[e.getPotionID()];
                int color = (p != null ? p.getLiquidColor() : 0xFFFFFF) | 0xFF000000;
                fr.drawStringWithShadow(label(e), x + 5, y + 4 + i * 12, color);
                i++;
            }
        }
    }

    public static class TargetHud extends Module {
        private float shown = -1F;
        private EntityLivingBase last;

        public TargetHud() {
            super("Target HUD", "Health + face of who you're fighting", Category.PVP, true, true, 170, 40);
            drop(color);
        }

        @Override
        public void render(Minecraft mc, boolean editing) {
            EntityLivingBase t = CombatTracker.target;
            boolean live = t != null && System.currentTimeMillis() - CombatTracker.targetTime < 5000L;
            if (!live && !editing) {
                shown = -1F;
                last = null;
                return;
            }
            FontRenderer fr = mc.fontRendererObj;
            String name = live ? t.getName() : "Steve";
            float hp = live ? t.getHealth() : 20F;
            float max = live ? Math.max(1F, t.getMaxHealth()) : 20F;
            EntityLivingBase cur = live ? t : null;
            if (cur != last || shown < 0F) {
                shown = hp;
                last = cur;
            }
            shown += (hp - shown) * 0.12F;

            width = 140;
            height = 40;
            box();

            ResourceLocation skin = null;
            if (live && t instanceof AbstractClientPlayer) skin = ((AbstractClientPlayer) t).getLocationSkin();
            if (skin != null) {
                GlStateManager.color(1F, 1F, 1F, 1F);
                mc.getTextureManager().bindTexture(skin);
                Gui.drawScaledCustomSizeModalRect(x + 6, y + 6, 8, 8, 8, 8, 28, 28, 64, 64);
                Gui.drawScaledCustomSizeModalRect(x + 6, y + 6, 40, 8, 8, 8, 28, 28, 64, 64);
            } else {
                Gui.drawRect(x + 6, y + 6, x + 34, y + 34, Theme.alpha(Theme.accent(), 0x55));
            }

            fr.drawStringWithShadow(name, x + 40, y + 6, Theme.TEXT);
            float ratio = Math.max(0F, Math.min(1F, shown / max));
            int bx = x + 40, by = y + 18, bw = 94;
            Gui.drawRect(bx, by, bx + bw, by + 6, 0xFF2A2A32);
            Gui.drawRect(bx, by, bx + (int) (bw * ratio), by + 6, Theme.lerp(0xFFFF4545, 0xFF3DDC84, ratio));
            fr.drawStringWithShadow(String.format(Locale.US, "%.1f / %.1f", hp, max), x + 40, y + 28, Theme.MUTED);

            if (live && mc.thePlayer != null) {
                float mine = mc.thePlayer.getHealth();
                String tag = mine > hp ? "WINNING" : (mine < hp ? "LOSING" : "DRAW");
                int tc = mine > hp ? 0xFF3DDC84 : (mine < hp ? 0xFFFF4545 : Theme.MUTED);
                fr.drawStringWithShadow(tag, x + width - 5 - fr.getStringWidth(tag), y + 6, tc);
            }
        }
    }

    /* ===================== Visual ===================== */

    public static class Crosshair extends Module {
        private final Setting.Num size, gap, thick;
        private final Setting.Bool dot, outline;
        private final Setting.Mode mode;

        public Crosshair() {
            super("Crosshair", "Custom crosshair (vanilla one gets hidden)", Category.VISUAL, false, true, 0, 0);
            size = addNum("Size", 5, 1, 15, 1);
            gap = addNum("Gap", 3, 0, 10, 1);
            thick = addNum("Thickness", 1, 1, 4, 1);
            dot = addBool("Dot", false);
            outline = addBool("Outline", true);
            mode = addMode("Color", "Accent", "White", "Green", "Red", "Rainbow");
        }

        private int col() {
            switch (mode.index) {
                case 0: return Theme.accent();
                case 1: return 0xFFFFFFFF;
                case 2: return 0xFF3DDC84;
                case 3: return 0xFFFF4545;
                default: return Theme.chroma(0L);
            }
        }

        public void drawAt(int cx, int cy) {
            int s = size.i(), g = gap.i(), t = thick.i(), c = col();
            int o = t / 2;
            for (int pass = outline.value ? 0 : 1; pass < 2; pass++) {
                int e = pass == 0 ? 1 : 0;
                int cc = pass == 0 ? 0xFF000000 : c;
                Gui.drawRect(cx - g - s - e, cy - o - e, cx - g + e, cy - o + t + e, cc);
                Gui.drawRect(cx + g - e, cy - o - e, cx + g + s + e, cy - o + t + e, cc);
                Gui.drawRect(cx - o - e, cy - g - s - e, cx - o + t + e, cy - g + e, cc);
                Gui.drawRect(cx - o - e, cy + g - e, cx - o + t + e, cy + g + s + e, cc);
                if (dot.value) Gui.drawRect(cx - o - e, cy - o - e, cx - o + t + e, cy - o + t + e, cc);
            }
        }
    }

    public static class Fullbright extends Module {
        private float saved = 0.5F;
        public Fullbright() { super("Fullbright", "Max brightness, no dark caves", Category.VISUAL, false, false, 0, 0); }
        @Override public void tick(Minecraft mc) {
            if (mc.gameSettings.gammaSetting < 100F) {
                saved = mc.gameSettings.gammaSetting;
                mc.gameSettings.gammaSetting = 100F;
            }
        }
        @Override public void onDisable() { Minecraft.getMinecraft().gameSettings.gammaSetting = saved; }
    }

    public static class Zoom extends Module {
        public final Setting.Num level;
        public final Setting.Bool smooth, scroll, cinematic;
        public float current = 1F;

        public Zoom() {
            super("Zoom", "Hold C to zoom, scroll wheel to adjust", Category.VISUAL, false, true, 0, 0);
            level = addNum("Level", 4, 1.5, 10, 0.5);
            smooth = addBool("Smooth", true);
            scroll = addBool("Scroll", true);
            cinematic = addBool("Cinematic", false);
        }
    }

    public static class NoFire extends Module {
        public NoFire() { super("No Fire", "Hides the fire overlay on your screen", Category.VISUAL, false, true, 0, 0); }
    }

    public static class BlockOutline extends Module {
        private final Setting.Mode mode;
        private final Setting.Num width2;

        public BlockOutline() {
            super("Block Outline", "Colored outline on the block you look at", Category.VISUAL, false, true, 0, 0);
            mode = addMode("Color", "Accent", "Rainbow", "White");
            width2 = addNum("Width", 2, 1, 5, 0.5);
        }

        public void onHighlight(Minecraft mc, DrawBlockHighlightEvent e) {
            if (e.target == null || e.target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                    || mc.theWorld == null) return;
            BlockPos pos = e.target.getBlockPos();
            Block block = mc.theWorld.getBlockState(pos).getBlock();
            if (block.getMaterial() == Material.air) return;
            e.setCanceled(true);

            block.setBlockBoundsBasedOnState(mc.theWorld, pos);
            EntityPlayer p = e.player;
            double dx = p.lastTickPosX + (p.posX - p.lastTickPosX) * e.partialTicks;
            double dy = p.lastTickPosY + (p.posY - p.lastTickPosY) * e.partialTicks;
            double dz = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * e.partialTicks;
            AxisAlignedBB bb = block.getSelectedBoundingBox(mc.theWorld, pos)
                    .expand(0.002D, 0.002D, 0.002D).offset(-dx, -dy, -dz);

            int c = mode.index == 0 ? Theme.accent() : (mode.index == 1 ? Theme.chroma(0L) : 0xFFFFFFFF);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.color(((c >> 16) & 255) / 255F, ((c >> 8) & 255) / 255F, (c & 255) / 255F, 0.9F);
            GL11.glLineWidth(width2.f());
            GlStateManager.disableTexture2D();
            GlStateManager.depthMask(false);
            RenderGlobal.drawSelectionBoundingBox(bb);
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.color(1F, 1F, 1F, 1F);
        }
    }

    /* ===================== Utility ===================== */

    public static class ToggleSprint extends Module {
        public ToggleSprint() { super("Toggle Sprint", "Auto-sprint while holding forward", Category.UTILITY, false, true, 0, 0); }
        @Override public void tick(Minecraft mc) {
            EntityPlayerSP p = mc.thePlayer;
            if (p == null || mc.currentScreen != null) return;
            if (mc.gameSettings.keyBindForward.isKeyDown() && !p.isSneaking()
                    && !p.isCollidedHorizontally && !p.isUsingItem()
                    && p.getFoodStats().getFoodLevel() > 6) {
                p.setSprinting(true);
            }
        }
    }

    public static class ChatTimestamps extends Module {
        public final Setting.Bool use24;
        public ChatTimestamps() {
            super("Chat Time", "Adds [HH:mm] before every chat message", Category.UTILITY, false, true, 0, 0);
            use24 = addBool("24 Hour", true);
        }
    }
}
