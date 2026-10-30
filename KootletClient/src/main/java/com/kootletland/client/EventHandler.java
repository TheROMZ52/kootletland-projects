package com.kootletland.client;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.kootletland.client.gui.ClickGui;
import com.kootletland.client.gui.KootletMenu;
import com.kootletland.client.module.Module;
import com.kootletland.client.module.ModuleManager;
import com.kootletland.client.module.Modules;
import com.kootletland.client.module.Setting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class EventHandler {
    private final Minecraft mc = Minecraft.getMinecraft();
    private boolean zooming, prevSmooth, welcomed;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;

        if (mc.currentScreen == null) {
            while (KootletClient.openKey.isPressed()) {
                mc.displayGuiScreen(new ClickGui());
            }
        }

        CombatTracker.tick(mc);
        if (mc.thePlayer == null) return;

        if (!welcomed) {
            welcomed = true;
            Notifications.push("Kootlet Client", "Press RSHIFT for the menu", true);
        }

        for (Module m : ModuleManager.all) {
            if (m.enabled) m.tick(mc);
        }

        Modules.Zoom z = ModuleManager.zoom;
        boolean want = z.enabled && mc.currentScreen == null && KootletClient.zoomKey.isKeyDown();
        if (want && !zooming) {
            zooming = true;
            prevSmooth = mc.gameSettings.smoothCamera;
            if (z.cinematic.value) mc.gameSettings.smoothCamera = true;
        } else if (!want && zooming) {
            zooming = false;
            mc.gameSettings.smoothCamera = prevSmooth;
            Config.save();
        }
    }

    @SubscribeEvent
    public void onFov(EntityViewRenderEvent.FOVModifier e) {
        Modules.Zoom z = ModuleManager.zoom;
        float target = zooming ? (float) z.level.value : 1F;
        float speed = z.smooth.value ? 0.2F : 1F;
        z.current += (target - z.current) * speed;
        if (Math.abs(z.current - 1F) > 0.002F) e.setFOV(e.getFOV() / z.current);
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (e.buttonstate) ClickTracker.click(e.button);
        Modules.Zoom z = ModuleManager.zoom;
        if (e.dwheel != 0 && zooming && z.scroll.value) {
            Setting.Num l = z.level;
            l.set(l.value + (e.dwheel > 0 ? 0.5 : -0.5));
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent e) {
        if (e.entityPlayer == mc.thePlayer) CombatTracker.onAttack(mc, e.target);
    }

    @SubscribeEvent
    public void onCrosshair(RenderGameOverlayEvent.Pre e) {
        if (e.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
        Modules.Crosshair c = ModuleManager.crosshair;
        if (!c.enabled || mc.gameSettings.thirdPersonView != 0 || mc.gameSettings.showDebugInfo) return;
        e.setCanceled(true);
        c.drawAt(e.resolution.getScaledWidth() / 2, e.resolution.getScaledHeight() / 2);
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post e) {
        if (e.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (mc.currentScreen instanceof ClickGui) return;
        GlStateManager.color(1F, 1F, 1F, 1F);
        if (mc.thePlayer != null && !mc.gameSettings.showDebugInfo) {
            for (Module m : ModuleManager.all) {
                if (m.hud && m.enabled) m.draw(mc, false);
            }
        }
        Notifications.render(mc, e.resolution.getScaledWidth(), e.resolution.getScaledHeight());
    }

    @SubscribeEvent
    public void onFireOverlay(RenderBlockOverlayEvent e) {
        if (ModuleManager.noFire.enabled && e.overlayType == RenderBlockOverlayEvent.OverlayType.FIRE) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onHighlight(DrawBlockHighlightEvent e) {
        if (ModuleManager.blockOutline.enabled) ModuleManager.blockOutline.onHighlight(mc, e);
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        Modules.ChatTimestamps ct = ModuleManager.chatTime;
        if (!ct.enabled || e.type == 2) return;
        String t = new SimpleDateFormat(ct.use24.value ? "HH:mm" : "hh:mm a").format(new Date());
        ChatComponentText prefix = new ChatComponentText("\u00a77[" + t + "] \u00a7r");
        prefix.appendSibling(e.message);
        e.message = prefix;
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent e) {
        if (Config.customMenu && e.gui != null && e.gui.getClass() == GuiMainMenu.class) {
            e.gui = new KootletMenu();
        }
    }
}
