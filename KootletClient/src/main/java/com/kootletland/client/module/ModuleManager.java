package com.kootletland.client.module;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    public static final List<Module> all = new ArrayList<Module>();
    public static Modules.Zoom zoom;
    public static Modules.Crosshair crosshair;
    public static Modules.NoFire noFire;
    public static Modules.BlockOutline blockOutline;
    public static Modules.ChatTimestamps chatTime;

    private ModuleManager() {}

    public static void init() {
        // HUD
        all.add(new Modules.Watermark());
        all.add(new Modules.Fps());
        all.add(new Modules.Ping());
        all.add(new Modules.Coords());
        all.add(new Modules.Direction());
        all.add(new Modules.Speed());
        all.add(new Modules.Memory());
        all.add(new Modules.Clock());
        // PvP
        all.add(new Modules.Cps());
        all.add(new Modules.Keystrokes());
        all.add(new Modules.Combo());
        all.add(new Modules.Reach());
        all.add(new Modules.TargetHud());
        all.add(new Modules.ArmorStatus());
        all.add(new Modules.PotionStatus());
        // Visual
        crosshair = new Modules.Crosshair();
        all.add(crosshair);
        all.add(new Modules.Fullbright());
        zoom = new Modules.Zoom();
        all.add(zoom);
        noFire = new Modules.NoFire();
        all.add(noFire);
        blockOutline = new Modules.BlockOutline();
        all.add(blockOutline);
        // Utility
        all.add(new Modules.ToggleSprint());
        chatTime = new Modules.ChatTimestamps();
        all.add(chatTime);
    }
}
