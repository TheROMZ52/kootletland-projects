package com.kootletland.client;

import java.awt.Color;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;

public final class Theme {
    public static final String[] PRESET_NAMES = {"Kootlet", "Ocean", "Purple", "Pink", "Mint", "Red", "Gold"};
    public static final int[] PRESETS = {
            0xFFFF9F1C, 0xFF21C8F6, 0xFFA855F7, 0xFFFF4D8D, 0xFF3DDC84, 0xFFFF4545, 0xFFFFD43B};
    public static int preset = 0;

    public static final int BG = 0x99121216;
    public static final int PANEL = 0xF0141419;
    public static final int PANEL2 = 0xFF1B1B22;
    public static final int ROW = 0xFF202028;
    public static final int ROW_HOVER = 0xFF2A2A34;
    public static final int OFF = 0xFF3A3A44;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int MUTED = 0xFFA0A0A8;
    public static final ResourceLocation LOGO = new ResourceLocation("kootletclient", "logo.png");

    private Theme() {}

    public static int accent() { return PRESETS[preset]; }
    public static int accentDark() { return scale(accent(), 0.62F); }

    public static int alpha(int c, int a) { return (a << 24) | (c & 0xFFFFFF); }

    public static int scale(int c, float f) {
        int a = (c >>> 24) & 255;
        int r = Math.min(255, (int) (((c >> 16) & 255) * f));
        int g = Math.min(255, (int) (((c >> 8) & 255) * f));
        int b = Math.min(255, (int) ((c & 255) * f));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0F, Math.min(1F, t));
        int aa = (int) (((a >>> 24) & 255) * (1 - t) + ((b >>> 24) & 255) * t);
        int ar = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int ag = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int ab = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (aa << 24) | (ar << 16) | (ag << 8) | ab;
    }

    public static float ease(float t) {
        t = Math.max(0F, Math.min(1F, t));
        float k = 1F - t;
        return 1F - k * k * k;
    }

    public static int chroma(long offset) {
        float h = ((System.currentTimeMillis() / 14L + offset) % 360L) / 360F;
        return Color.HSBtoRGB(h, 0.72F, 1F);
    }

    public static void drawChroma(FontRenderer fr, String s, int x, int y) {
        float cx = x;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            fr.drawStringWithShadow(String.valueOf(c), cx, y, chroma((long) (cx * 3)));
            cx += fr.getCharWidth(c);
        }
    }
}
