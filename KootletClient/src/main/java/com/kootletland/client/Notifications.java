package com.kootletland.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

/** Little slide-in toasts in the bottom-right corner. */
public final class Notifications {
    private static final long LIFE = 3200L;

    private static class N {
        final String title, msg;
        final boolean good;
        final long born = System.currentTimeMillis();
        N(String t, String m, boolean g) { title = t; msg = m; good = g; }
    }

    private static final List<N> LIST = new ArrayList<N>();

    private Notifications() {}

    public static void push(String title, String msg, boolean good) {
        LIST.add(new N(title, msg, good));
        while (LIST.size() > 5) LIST.remove(0);
    }

    public static void render(Minecraft mc, int sw, int sh) {
        long now = System.currentTimeMillis();
        Iterator<N> it = LIST.iterator();
        while (it.hasNext()) {
            if (now - it.next().born > LIFE) it.remove();
        }
        int y = sh - 34;
        for (int i = LIST.size() - 1; i >= 0; i--) {
            N n = LIST.get(i);
            long age = now - n.born;
            float p = Math.min(Theme.ease(age / 300F), Theme.ease((LIFE - age) / 300F));
            int w = 132, h = 28;
            int x = sw - 6 - (int) (w * p);
            int col = n.good ? Theme.accent() : 0xFFFF4545;
            Gui.drawRect(x, y, x + w, y + h, 0xEE141419);
            Gui.drawRect(x, y, x + 3, y + h, col);
            mc.fontRendererObj.drawStringWithShadow(n.title, x + 9, y + 5, Theme.TEXT);
            mc.fontRendererObj.drawStringWithShadow(n.msg, x + 9, y + 16, Theme.MUTED);
            int bar = (int) ((w - 3) * (1F - age / (float) LIFE));
            Gui.drawRect(x + 3, y + h - 2, x + 3 + Math.max(0, bar), y + h, col);
            y -= h + 4;
        }
    }
}
