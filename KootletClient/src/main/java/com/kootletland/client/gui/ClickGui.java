package com.kootletland.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.kootletland.client.Config;
import com.kootletland.client.KootletClient;
import com.kootletland.client.Notifications;
import com.kootletland.client.Theme;
import com.kootletland.client.module.Category;
import com.kootletland.client.module.Module;
import com.kootletland.client.module.ModuleManager;
import com.kootletland.client.module.Setting;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

public class ClickGui extends GuiScreen {
    private static final int PW = 360, PH = 220, SW = 76, LW = 140, RH = 20, ROWS = 8;
    private static final String[] TABS = {"HUD", "PvP", "Visual", "Utility", "Theme"};
    private static int lastTab = 0;

    private int tab = lastTab;
    private Module selected;
    private final float[] tabAnim = new float[5];
    private final long openTime = System.currentTimeMillis();
    private Module dragging;
    private int dragDX, dragDY;
    private Setting.Num sliding;
    private int slideX, slideW;
    private int scroll;

    private int px() { return (width - PW) / 2; }
    private int py() { return (height - PH) / 2; }
    private int listX() { return px() + SW + 6; }
    private int listY() { return py() + 30; }
    private int setX() { return listX() + LW + 6; }
    private int setW() { return PW - SW - LW - 18; }
    private int setRowY(int i) { return py() + 58 + i * 22; }

    private boolean hit(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private int snap(int v, int target) { return Math.abs(v - target) <= 4 ? target : v; }

    private boolean inPanel(int mx, int my) { return hit(mx, my, px(), py(), PW, PH); }

    private List<Module> current() {
        List<Module> out = new ArrayList<Module>();
        if (tab >= Category.values().length) return out;
        Category c = Category.values()[tab];
        for (Module m : ModuleManager.all) if (m.category == c) out.add(m);
        return out;
    }

    private void selectFirst() {
        List<Module> l = current();
        selected = l.isEmpty() ? null : l.get(0);
        scroll = 0;
    }

    private void outline(int x1, int y1, int x2, int y2, int c) {
        drawRect(x1, y1, x2, y1 + 1, c);
        drawRect(x1, y2 - 1, x2, y2, c);
        drawRect(x1, y1, x1 + 1, y2, c);
        drawRect(x2 - 1, y1, x2, y2, c);
    }

    private void centered(String s, int cx, int y, int color) {
        fontRendererObj.drawStringWithShadow(s, cx - fontRendererObj.getStringWidth(s) / 2F, y, color);
    }

    private void drawSwitch(int sx, int sy, float anim) {
        drawRect(sx, sy, sx + 24, sy + 10, Theme.lerp(Theme.OFF, Theme.accent(), anim));
        int kx = sx + 1 + (int) (14 * anim);
        drawRect(kx, sy + 1, kx + 8, sy + 9, 0xFFFFFFFF);
    }

    @Override
    public void initGui() {
        if (selected == null) selectFirst();
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        float a = Theme.ease((System.currentTimeMillis() - openTime) / 220F);
        drawRect(0, 0, width, height, ((int) (0x88 * a)) << 24);

        for (Module m : ModuleManager.all) {
            if (m.hud && m.enabled) {
                m.draw(mc, true);
                outline(m.x - 1, m.y - 1, m.x + m.sw() + 1, m.y + m.sh() + 1,
                        m == dragging ? Theme.accent() : 0x66FFFFFF);
            }
        }

        float s = 0.92F + 0.08F * a;
        GlStateManager.pushMatrix();
        GlStateManager.translate(width / 2F, height / 2F, 0F);
        GlStateManager.scale(s, s, 1F);
        GlStateManager.translate(-width / 2F, -height / 2F, 0F);
        drawPanel(mx, my);
        GlStateManager.popMatrix();

        Notifications.render(mc, width, height);
        super.drawScreen(mx, my, pt);
    }

    private void drawPanel(int mx, int my) {
        int x = px(), y = py();
        drawRect(x, y, x + PW, y + PH, Theme.PANEL);
        drawRect(x, y, x + SW, y + PH, Theme.PANEL2);
        drawRect(x, y, x + PW, y + 2, Theme.accent());

        // sidebar: logo + tabs
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableBlend();
        mc.getTextureManager().bindTexture(Theme.LOGO);
        drawModalRectWithCustomSizedTexture(x + (SW - 32) / 2, y + 8, 0, 0, 32, 32, 32, 32);
        centered("KOOTLET", x + SW / 2, y + 43, Theme.accent());
        centered("CLIENT", x + SW / 2, y + 53, Theme.MUTED);

        for (int i = 0; i < TABS.length; i++) {
            int ty = y + 70 + i * 22;
            tabAnim[i] += ((i == tab ? 1F : 0F) - tabAnim[i]) * 0.25F;
            boolean over = hit(mx, my, x, ty, SW, 20);
            drawRect(x + 4, ty, x + SW - 4, ty + 20,
                    Theme.lerp(over ? Theme.ROW_HOVER : Theme.PANEL2, Theme.alpha(Theme.accent(), 0x40), tabAnim[i]));
            drawRect(x + 4, ty, x + 6, ty + (int) (20 * tabAnim[i]), Theme.accent());
            fontRendererObj.drawStringWithShadow(TABS[i], x + 12, ty + 6,
                    i == tab ? Theme.TEXT : Theme.MUTED);
        }
        centered("RShift = close", x + SW / 2, y + PH - 12, Theme.MUTED);

        if (tab >= Category.values().length) {
            drawThemeTab(mx, my);
            return;
        }

        // module list
        fontRendererObj.drawStringWithShadow(TABS[tab].toUpperCase(), listX(), py() + 12, Theme.accent());
        List<Module> mods = current();
        for (int i = scroll; i < Math.min(mods.size(), scroll + ROWS); i++) {
            Module m = mods.get(i);
            int ry = listY() + (i - scroll) * 22;
            boolean over = hit(mx, my, listX(), ry, LW, RH);
            boolean sel = m == selected;
            drawRect(listX(), ry, listX() + LW, ry + RH, sel ? 0xFF262630 : (over ? Theme.ROW_HOVER : Theme.ROW));
            if (sel) drawRect(listX(), ry, listX() + 2, ry + RH, Theme.accent());
            fontRendererObj.drawStringWithShadow(m.name, listX() + 8, ry + 6, Theme.TEXT);
            m.anim += ((m.enabled ? 1F : 0F) - m.anim) * 0.25F;
            drawSwitch(listX() + LW - 30, ry + 5, m.anim);
        }

        if (mods.size() > ROWS) {
            int trackH = ROWS * 22 - 2;
            int thumbH = Math.max(12, trackH * ROWS / mods.size());
            int thumbY = listY() + (trackH - thumbH) * scroll / Math.max(1, mods.size() - ROWS);
            drawRect(listX() + LW + 1, listY(), listX() + LW + 3, listY() + trackH, Theme.OFF);
            drawRect(listX() + LW + 1, thumbY, listX() + LW + 3, thumbY + thumbH, Theme.accent());
        }

        // settings pane
        int sx = setX(), sw = setW();
        drawRect(sx - 3, py() + 8, px() + PW - 6, py() + PH - 8, 0xFF1E1E26);
        if (selected == null) return;
        fontRendererObj.drawStringWithShadow(selected.name, sx + 3, py() + 12, Theme.accent());
        @SuppressWarnings("unchecked")
        List<String> lines = fontRendererObj.listFormattedStringToWidth(selected.description, sw - 6);
        for (int i = 0; i < lines.size() && i < 3; i++) {
            fontRendererObj.drawString(lines.get(i), sx + 3, py() + 25 + i * 9, Theme.MUTED);
        }
        for (int i = 0; i < selected.settings.size(); i++) {
            Setting st = selected.settings.get(i);
            int ry = setRowY(i);
            boolean over = hit(mx, my, sx, ry, sw, 20);
            if (over) drawRect(sx, ry, sx + sw, ry + 20, 0x22FFFFFF);
            if (st instanceof Setting.Bool) {
                Setting.Bool b = (Setting.Bool) st;
                b.anim += ((b.value ? 1F : 0F) - b.anim) * 0.25F;
                fontRendererObj.drawStringWithShadow(b.name, sx + 3, ry + 6, Theme.TEXT);
                drawSwitch(sx + sw - 27, ry + 5, b.anim);
            } else if (st instanceof Setting.Mode) {
                Setting.Mode md = (Setting.Mode) st;
                fontRendererObj.drawStringWithShadow(md.name, sx + 3, ry + 6, Theme.TEXT);
                int pw = fontRendererObj.getStringWidth(md.get()) + 10;
                int pxx = sx + sw - 3 - pw;
                drawRect(pxx, ry + 3, pxx + pw, ry + 17, over ? 0xFF4A4A56 : Theme.OFF);
                fontRendererObj.drawStringWithShadow(md.get(), pxx + 5, ry + 6, Theme.accent());
            } else if (st instanceof Setting.Num) {
                Setting.Num n = (Setting.Num) st;
                fontRendererObj.drawStringWithShadow(n.name, sx + 3, ry + 2, Theme.TEXT);
                String v = n.display();
                fontRendererObj.drawStringWithShadow(v, sx + sw - 3 - fontRendererObj.getStringWidth(v), ry + 2, Theme.accent());
                int tx = sx + 3, tw = sw - 6;
                float r = (float) ((n.value - n.min) / (n.max - n.min));
                drawRect(tx, ry + 13, tx + tw, ry + 17, Theme.OFF);
                drawRect(tx, ry + 13, tx + (int) (tw * r), ry + 17, Theme.accent());
                int kx = tx + (int) (tw * r) - 2;
                drawRect(kx, ry + 11, kx + 4, ry + 19, 0xFFFFFFFF);
            }
        }
    }

    private void drawThemeTab(int mx, int my) {
        int lx = listX(), ly = listY();
        fontRendererObj.drawStringWithShadow("ACCENT COLOR", lx, py() + 12, Theme.accent());
        for (int i = 0; i < Theme.PRESETS.length; i++) {
            int sx = lx + (i % 4) * 68, sy = ly + 4 + (i / 4) * 40;
            boolean over = hit(mx, my, sx, sy, 62, 32);
            drawRect(sx, sy, sx + 62, sy + 32, Theme.PRESETS[i]);
            if (over) outline(sx - 1, sy - 1, sx + 63, sy + 33, 0xFFFFFFFF);
            if (i == Theme.preset) outline(sx - 2, sy - 2, sx + 64, sy + 34, 0xFFFFFFFF);
            centered(Theme.PRESET_NAMES[i], sx + 31, sy + 12, 0xFF000000);
        }
        int iy = ly + 96;
        fontRendererObj.drawString("Drag HUD boxes outside the menu to move them", lx, iy, Theme.MUTED);
        fontRendererObj.drawString("Zoom key + scroll wheel changes zoom level", lx, iy + 12, Theme.MUTED);
        String ip = (Config.serverIp == null || Config.serverIp.isEmpty()) ? "not set (config: serverip)" : Config.serverIp;
        fontRendererObj.drawString("Server: " + ip, lx, iy + 24, Theme.MUTED);
        fontRendererObj.drawString("Config: .minecraft/config/kootletclient.properties", lx, iy + 36, Theme.MUTED);
        int rbx = lx, rby = py() + PH - 34;
        boolean ro = hit(mx, my, rbx, rby, 120, 18);
        drawRect(rbx, rby, rbx + 120, rby + 18, ro ? Theme.accent() : Theme.accentDark());
        centered("Reset HUD positions", rbx + 60, rby + 5, 0xFF000000);
    }

    private void applySlide(int mx) {
        if (sliding == null) return;
        double r = Math.max(0, Math.min(1, (mx - slideX) / (double) slideW));
        sliding.set(sliding.min + r * (sliding.max - sliding.min));
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        if (btn != 0) return;

        if (inPanel(mx, my)) {
            for (int i = 0; i < TABS.length; i++) {
                if (hit(mx, my, px(), py() + 70 + i * 22, SW, 20)) {
                    tab = i;
                    lastTab = i;
                    selectFirst();
                    return;
                }
            }
            if (tab >= Category.values().length) {
                for (int i = 0; i < Theme.PRESETS.length; i++) {
                    int sx = listX() + (i % 4) * 68, sy = listY() + 4 + (i / 4) * 40;
                    if (hit(mx, my, sx, sy, 62, 32)) {
                        Theme.preset = i;
                        Config.save();
                        Notifications.push("Theme", Theme.PRESET_NAMES[i], true);
                        return;
                    }
                }
                if (hit(mx, my, listX(), py() + PH - 34, 120, 18)) {
                    for (Module m : ModuleManager.all) if (m.hud) m.resetPos();
                    Config.save();
                    Notifications.push("HUD", "Positions reset", true);
                }
                return;
            }
            List<Module> mods = current();
            for (int i = scroll; i < Math.min(mods.size(), scroll + ROWS); i++) {
                int ry = listY() + (i - scroll) * 22;
                if (hit(mx, my, listX(), ry, LW, RH)) {
                    Module m = mods.get(i);
                    if (mx >= listX() + LW - 34) {
                        m.toggle();
                        Config.save();
                        Notifications.push(m.name, m.enabled ? "Enabled" : "Disabled", m.enabled);
                    } else {
                        selected = m;
                    }
                    return;
                }
            }
            if (selected != null) {
                for (int i = 0; i < selected.settings.size(); i++) {
                    int ry = setRowY(i);
                    if (!hit(mx, my, setX(), ry, setW(), 20)) continue;
                    Setting st = selected.settings.get(i);
                    if (st instanceof Setting.Bool) {
                        Setting.Bool b = (Setting.Bool) st;
                        b.value = !b.value;
                    } else if (st instanceof Setting.Mode) {
                        ((Setting.Mode) st).cycle();
                    } else if (st instanceof Setting.Num) {
                        sliding = (Setting.Num) st;
                        slideX = setX() + 3;
                        slideW = setW() - 6;
                        applySlide(mx);
                    }
                    Config.save();
                    return;
                }
            }
            return;
        }

        for (int i = ModuleManager.all.size() - 1; i >= 0; i--) {
            Module m = ModuleManager.all.get(i);
            if (m.hud && m.enabled && hit(mx, my, m.x, m.y, m.sw(), m.sh())) {
                dragging = m;
                dragDX = mx - m.x;
                dragDY = my - m.y;
                return;
            }
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int btn, long time) {
        if (sliding != null) {
            applySlide(mx);
        } else if (dragging != null) {
            int w = dragging.sw(), h = dragging.sh();
            int nx = mx - dragDX, ny = my - dragDY;
            nx = snap(nx, 0);
            nx = snap(nx, width - w);
            nx = snap(nx, (width - w) / 2);
            ny = snap(ny, 0);
            ny = snap(ny, height - h);
            ny = snap(ny, (height - h) / 2);
            for (Module o : ModuleManager.all) {
                if (o == dragging || !o.hud || !o.enabled) continue;
                nx = snap(nx, o.x);
                nx = snap(nx, o.x + o.sw() - w);
                nx = snap(nx, o.x + o.sw() + 2);
                nx = snap(nx, o.x - w - 2);
                ny = snap(ny, o.y);
                ny = snap(ny, o.y + o.sh() - h);
                ny = snap(ny, o.y + o.sh() + 2);
                ny = snap(ny, o.y - h - 2);
            }
            dragging.x = Math.max(0, Math.min(width - w, nx));
            dragging.y = Math.max(0, Math.min(height - h, ny));
        }
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        if (sliding != null || dragging != null) {
            sliding = null;
            dragging = null;
            Config.save();
        }
        super.mouseReleased(mx, my, state);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0 && tab < Category.values().length) {
            int max = Math.max(0, current().size() - ROWS);
            scroll = Math.max(0, Math.min(max, scroll + (d > 0 ? -1 : 1)));
        }
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_RSHIFT) {
            mc.displayGuiScreen(null);
            if (mc.currentScreen == null) mc.setIngameFocus();
            return;
        }
        super.keyTyped(c, key);
    }

    @Override
    public void onGuiClosed() {
        Config.save();
        while (KootletClient.openKey.isPressed()) { /* drain so the menu doesn't reopen */ }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
