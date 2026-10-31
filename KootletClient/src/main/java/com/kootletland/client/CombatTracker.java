package com.kootletland.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;

/** Tracks your current target, combo and last hit reach (all client-side, read-only info). */
public final class CombatTracker {
    public static EntityLivingBase target;
    public static long targetTime;
    public static int combo;
    public static double reach;
    public static long lastHit;
    private static int lastTargetHurt, lastSelfHurt;

    private CombatTracker() {}

    public static void onAttack(Minecraft mc, Entity t) {
        if (!(t instanceof EntityLivingBase) || mc.thePlayer == null) return;
        if (target != t) lastTargetHurt = 0;
        target = (EntityLivingBase) t;
        targetTime = System.currentTimeMillis();
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && mop.hitVec != null) {
            reach = mop.hitVec.distanceTo(mc.thePlayer.getPositionEyes(1.0F));
        }
    }

    public static void tick(Minecraft mc) {
        EntityPlayerSP p = mc.thePlayer;
        if (p == null || mc.theWorld == null) {
            target = null;
            combo = 0;
            return;
        }
        long now = System.currentTimeMillis();
        if (p.hurtTime > lastSelfHurt) combo = 0;
        lastSelfHurt = p.hurtTime;

        if (target != null) {
            if (target.hurtTime > lastTargetHurt && now - targetTime < 1000L) {
                combo++;
                lastHit = now;
            }
            lastTargetHurt = target.hurtTime;
            if (now - targetTime > 6000L) target = null;
        }
        if (combo > 0 && now - lastHit > 5000L) combo = 0;
    }
}
