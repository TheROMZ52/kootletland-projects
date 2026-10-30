package com.kootletland.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import com.kootletland.client.module.Module;
import com.kootletland.client.module.ModuleManager;
import com.kootletland.client.module.Setting;

public final class Config {
    /** Put the KootletLand server address here (or in the config file: serverip=...). Empty = no Join button. */
    public static final String DEFAULT_SERVER_IP = "";

    public static String serverIp = DEFAULT_SERVER_IP;
    public static boolean customMenu = true;
    private static File file;

    private Config() {}

    public static void init(File dir) {
        if (!dir.exists()) dir.mkdirs();
        file = new File(dir, "kootletclient.properties");
        load();
    }

    private static int num(String s, int def) {
        try { return s == null ? def : Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }

    public static void load() {
        Properties p = new Properties();
        if (file != null && file.exists()) {
            FileInputStream in = null;
            try {
                in = new FileInputStream(file);
                p.load(in);
            } catch (IOException ignored) {
            } finally {
                try { if (in != null) in.close(); } catch (IOException ignored) {}
            }
        }
        Theme.preset = Math.max(0, Math.min(Theme.PRESETS.length - 1, num(p.getProperty("theme"), 0)));
        serverIp = p.getProperty("serverip", DEFAULT_SERVER_IP).trim();
        customMenu = Boolean.parseBoolean(p.getProperty("custommenu", "true"));

        for (Module m : ModuleManager.all) {
            m.setEnabled(Boolean.parseBoolean(p.getProperty(m.id + ".enabled", String.valueOf(m.enabled))));
            if (m.hud) {
                m.x = num(p.getProperty(m.id + ".x"), m.x);
                m.y = num(p.getProperty(m.id + ".y"), m.y);
            }
            for (Setting s : m.settings) {
                String v = p.getProperty(m.id + "." + s.key);
                if (v != null) s.load(v);
            }
        }
    }

    public static void save() {
        if (file == null) return;
        Properties p = new Properties();
        p.setProperty("theme", String.valueOf(Theme.preset));
        p.setProperty("serverip", serverIp == null ? "" : serverIp);
        p.setProperty("custommenu", String.valueOf(customMenu));
        for (Module m : ModuleManager.all) {
            p.setProperty(m.id + ".enabled", String.valueOf(m.enabled));
            if (m.hud) {
                p.setProperty(m.id + ".x", String.valueOf(m.x));
                p.setProperty(m.id + ".y", String.valueOf(m.y));
            }
            for (Setting s : m.settings) p.setProperty(m.id + "." + s.key, s.save());
        }
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            p.store(out, "Kootlet Client settings");
        } catch (IOException ignored) {
        } finally {
            try { if (out != null) out.close(); } catch (IOException ignored) {}
        }
    }
}
