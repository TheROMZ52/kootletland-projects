package com.kootletland.client.gui;

import java.io.IOException;
import java.util.Random;

import com.kootletland.client.Config;
import com.kootletland.client.KootletClient;
import com.kootletland.client.Theme;

import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.client.GuiModList;

/** Animated KootletLand main menu that replaces the vanilla one. */
public class KootletMenu extends GuiScreen {
    private static final int N = 70;
    private final float[] ppx = new float[N], ppy = new float[N], psz = new float[N],
            psp = new float[N], pph = new float[N];
    private final Random rnd = new Random();
    private final long openTime = System.currentTimeMillis();
    private long lastNs = System.nanoTime();

    private final int[] ids = new int[6];
    private final String[] labels = new String[6];
    private final int[] bxs = new int[6], bys = new int[6], bws = new int[6];
    private final float[] hov = new float[6];
    private int count;
    private int logoY, titleY, subY;
    private static final int BH = 22;

    private void spawn(int i, boolean anywhere) {
        ppx[i] = rnd.nextFloat() * width;
        ppy[i] = anywhere ? rnd.nextFloat() * height : height + 4;
        psz[i] = 1 + rnd.nextInt(3);
        psp[i] = 8F + rnd.nextFloat() * 28F;
        pph[i] = rnd.nextFloat() * 6.28F;
    }

    private void add(int id, String label, int x, int y, int w) {
        ids[count] = id;
        labels[count] = label;
        bxs[count] = x;
        bys[count] = y;
        bws[count] = w;
        count++;
    }

    @Override
    public void initGui() {
        for (int i = 0; i < N; i++) spawn(i, true);
        count = 0;
        boolean join = Config.serverIp != null && !Config.serverIp.isEmpty();
        int total = 48 + 8 + 27 + 14 + 26 + (join ? 28 : 0) + 56 + BH;
        int top = Math.max(6, (height - total) / 2);
        logoY = top;
        titleY = top + 56;
        subY = titleY + 29;
        int y = subY + 26;
        int x = (width - 190) / 2;
        if (join) {
            add(0, "Join KootletLand", x, y, 190);
            y += 28;
        }
        add(1, "Singleplayer", x, y, 190);
        y += 28;
        add(2, "Multiplayer", x, y, 190);
        y += 28;
        add(3, "Options", x, y, 60);
        add(5, "Mods", x + 65, y, 60);
        add(4, "Quit", x + 130, y, 60);
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        long ns = System.nanoTime();
        float dt = Math.min(0.1F, (ns - lastNs) / 1.0e9F);
        lastNs = ns;
        float t = (System.currentTimeMillis() - openTime) / 1000F;

        drawGradientRect(0, 0, width, height, 0xFF07070B, Theme.scale(Theme.accent(), 0.16F) | 0xFF000000);
        drawGradientRect(0, height / 2, width, height, 0x00000000, Theme.alpha(Theme.accent(), 0x40));

        // floating crumbs
        float parX = (mx - width / 2F);
        for (int i = 0; i < N; i++) {
            ppy[i] -= psp[i] * dt;
            float sway = (float) Math.sin(t * 0.8F + pph[i]) * 6F;
            if (ppy[i] < -4F) spawn(i, false);
            int x = (int) (ppx[i] + sway + parX * psz[i] * 0.01F);
            int y = (int) ppy[i];
            int alpha = 40 + (int) (psz[i] * 28F);
            drawRect(x, y, x + (int) psz[i], y + (int) psz[i], Theme.alpha(Theme.accent(), alpha));
        }

        // logo + title
        int bob = (int) (Math.sin(t * 2F) * 2F);
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableBlend();
        mc.getTextureManager().bindTexture(Theme.LOGO);
        drawModalRectWithCustomSizedTexture((width - 48) / 2, logoY + bob, 0, 0, 48, 48, 48, 48);

        String title = "KOOTLET";
        int tcol = Theme.lerp(Theme.accent(), 0xFFFFFFFF, ((float) Math.sin(t * 2F) + 1F) / 4F);
        GlStateManager.pushMatrix();
        GlStateManager.translate(width / 2F, titleY, 0F);
        GlStateManager.scale(3F, 3F, 1F);
        fontRendererObj.drawStringWithShadow(title, -fontRendererObj.getStringWidth(title) / 2F, 0F, tcol);
        GlStateManager.popMatrix();

        String sub = "C L I E N T";
        int sw = fontRendererObj.getStringWidth(sub);
        fontRendererObj.drawStringWithShadow(sub, (width - sw) / 2F, subY + 2, Theme.MUTED);
        drawRect(width / 2 - sw / 2 - 30, subY + 6, width / 2 - sw / 2 - 6, subY + 7, Theme.accent());
        drawRect(width / 2 + sw / 2 + 6, subY + 6, width / 2 + sw / 2 + 30, subY + 7, Theme.accent());

        // buttons
        for (int i = 0; i < count; i++) {
            boolean over = mx >= bxs[i] && mx < bxs[i] + bws[i] && my >= bys[i] && my < bys[i] + BH;
            hov[i] += ((over ? 1F : 0F) - hov[i]) * Math.min(1F, dt * 14F);
            boolean primary = ids[i] == 0;
            int bg = primary ? Theme.lerp(Theme.accentDark(), Theme.accent(), hov[i])
                    : Theme.lerp(0xCC16161C, 0xCC25252E, hov[i]);
            drawRect(bxs[i], bys[i], bxs[i] + bws[i], bys[i] + BH, bg);
            if (!primary) {
                drawRect(bxs[i], bys[i], bxs[i] + (int) (bws[i] * hov[i]), bys[i] + BH,
                        Theme.alpha(Theme.accent(), 0x55));
                drawRect(bxs[i], bys[i], bxs[i] + 2, bys[i] + BH, Theme.accent());
            }
            int lw = fontRendererObj.getStringWidth(labels[i]);
            fontRendererObj.drawStringWithShadow(labels[i], bxs[i] + (bws[i] - lw) / 2F, bys[i] + 7,
                    primary ? 0xFF000000 : Theme.TEXT);
        }
        if (Config.serverIp != null && !Config.serverIp.isEmpty()) {
            String ip = Config.serverIp;
            fontRendererObj.drawString(ip, (width - fontRendererObj.getStringWidth(ip)) / 2, bys[0] + BH + 2, 0x66FFFFFF);
        }

        String left = "Kootlet Client v" + KootletClient.VERSION;
        fontRendererObj.drawStringWithShadow(left, 6, height - 12, Theme.MUTED);
        String right = "Minecraft 1.8.9";
        fontRendererObj.drawStringWithShadow(right, width - fontRendererObj.getStringWidth(right) - 6, height - 12, Theme.MUTED);

        float fade = 1F - Theme.ease((System.currentTimeMillis() - openTime) / 700F);
        if (fade > 0.01F) drawRect(0, 0, width, height, ((int) (fade * 255F)) << 24);

        super.drawScreen(mx, my, pt);
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        if (btn != 0) return;
        for (int i = 0; i < count; i++) {
            if (mx >= bxs[i] && mx < bxs[i] + bws[i] && my >= bys[i] && my < bys[i] + BH) {
                switch (ids[i]) {
                    case 0:
                        mc.displayGuiScreen(new GuiConnecting(this, mc, new ServerData("KootletLand", Config.serverIp, false)));
                        break;
                    case 1: mc.displayGuiScreen(new GuiSelectWorld(this)); break;
                    case 2: mc.displayGuiScreen(new GuiMultiplayer(this)); break;
                    case 3: mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings)); break;
                    case 4: mc.shutdown(); break;
                    case 5: mc.displayGuiScreen(new GuiModList(this)); break;
                    default: break;
                }
                return;
            }
        }
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        // no ESC on the main menu
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
