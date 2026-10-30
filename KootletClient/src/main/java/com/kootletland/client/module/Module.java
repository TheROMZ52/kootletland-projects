package com.kootletland.client.module;

import java.util.ArrayList;
import java.util.List;

import com.kootletland.client.Theme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

public abstract class Module {
    public final String name;
    public final String id;
    public final String description;
    public final Category category;
    public final boolean hud;
    public boolean enabled;
    public float anim;
    public int x, y;
    public int width = 60, height = 14;
    public final List<Setting> settings = new ArrayList<Setting>();

    // common HUD settings (null for non-HUD modules)
    public Setting.Num scale;
    public Setting.Bool background;
    public Setting.Mode color;

    protected Module(String name, String description, Category category, boolean hud,
                     boolean enabledByDefault, int x, int y) {
        this.name = name;
        this.id = name.replace(" ", "").toLowerCase();
        this.description = description;
        this.category = category;
        this.hud = hud;
        this.enabled = enabledByDefault;
        this.anim = enabledByDefault ? 1F : 0F;
        this.x = x;
        this.y = y;
        if (hud) {
            scale = addNum("Scale", 1.0, 0.5, 2.0, 0.1);
            background = addBool("Background", true);
            color = addMode("Color", "White", "Accent", "Rainbow");
        }
    }

    protected Setting.Bool addBool(String n, boolean def) {
        Setting.Bool s = new Setting.Bool(n, def);
        settings.add(s);
        return s;
    }

    protected Setting.Num addNum(String n, double def, double min, double max, double step) {
        Setting.Num s = new Setting.Num(n, def, min, max, step);
        settings.add(s);
        return s;
    }

    protected Setting.Mode addMode(String n, String... modes) {
        Setting.Mode s = new Setting.Mode(n, modes);
        settings.add(s);
        return s;
    }

    protected void drop(Setting s) { settings.remove(s); }

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean e) {
        if (enabled == e) return;
        enabled = e;
        if (e) onEnable(); else onDisable();
    }

    public void onEnable() {}
    public void onDisable() {}
    public void tick(Minecraft mc) {}
    public void render(Minecraft mc, boolean editing) {}

    public float scaleF() { return scale == null ? 1F : scale.f(); }
    public int sw() { return (int) (width * scaleF()); }
    public int sh() { return (int) (height * scaleF()); }

    /** Draws the HUD element with its scale applied around its top-left corner. */
    public final void draw(Minecraft mc, boolean editing) {
        float s = scaleF();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, 0F);
        GlStateManager.scale(s, s, 1F);
        GlStateManager.translate((float) -x, (float) -y, 0F);
        render(mc, editing);
        GlStateManager.popMatrix();
    }

    protected void box() {
        if (background == null || background.value) {
            Gui.drawRect(x, y, x + width, y + height, Theme.BG);
            Gui.drawRect(x, y, x + 2, y + height, Theme.accent());
        }
    }

    protected void drawText(Minecraft mc, String s, int tx, int ty) {
        int mode = color == null ? 0 : color.index;
        if (mode == 2) Theme.drawChroma(mc.fontRendererObj, s, tx, ty);
        else mc.fontRendererObj.drawStringWithShadow(s, tx, ty, mode == 1 ? Theme.accent() : Theme.TEXT);
    }
}
