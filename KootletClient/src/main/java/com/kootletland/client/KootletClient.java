package com.kootletland.client;

import java.io.File;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

import com.kootletland.client.module.ModuleManager;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = KootletClient.MODID, name = "Kootlet Client", version = KootletClient.VERSION,
        clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class KootletClient {
    public static final String MODID = "kootletclient";
    public static final String VERSION = "1.1.0";
    public static KeyBinding openKey;
    public static KeyBinding zoomKey;
    private File configDir;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        configDir = e.getModConfigurationDirectory();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        openKey = new KeyBinding("Open Menu", Keyboard.KEY_RSHIFT, "Kootlet Client");
        zoomKey = new KeyBinding("Zoom", Keyboard.KEY_C, "Kootlet Client");
        ClientRegistry.registerKeyBinding(openKey);
        ClientRegistry.registerKeyBinding(zoomKey);

        ModuleManager.init();
        Config.init(configDir);

        MinecraftForge.EVENT_BUS.register(new EventHandler());
        Display.setTitle("Kootlet Client " + VERSION + " | KootletLand");
    }
}
