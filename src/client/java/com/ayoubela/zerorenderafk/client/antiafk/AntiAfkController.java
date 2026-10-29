package com.ayoubela.zerorenderafk.client.antiafk;

import com.ayoubela.zerorenderafk.client.ZeroRenderAFKClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Game-thread only. Runs from player synchronization, before the tick-end packet. */
public final class AntiAfkController {
    private static final Logger LOG = LoggerFactory.getLogger("zerorenderafk");
    private static AntiAfkConfig config;
    private static ActionClock jumpClock, rotationClock, commandClock, chatClock;
    private static JumpTrajectory trajectory;
    private static boolean jumpSuspended, alternateOut;
    private static long lastJumpStep;
    private static double startX, startY, startZ;
    private static float baseYaw;

    public static void start(AntiAfkConfig settings, LocalPlayer player) {
        stop();
        if (!settings.enabled) return;
        config = settings.copy();
        long now = System.nanoTime();
        jumpClock = new ActionClock(config.jumpSeconds, now);
        rotationClock = new ActionClock(config.rotationSeconds, now);
        commandClock = new ActionClock(config.commandSeconds, now);
        chatClock = new ActionClock(config.chatSeconds, now);
        baseYaw = player.getYRot();
        LOG.info("[Zero Render AFK] Anti-AFK started. Jump={}, rotation={}, command={}, chat={}",
            config.jump, config.rotation, config.command, config.chat);
    }
    public static void stop() {
        config = null; trajectory = null;
        jumpClock = rotationClock = commandClock = chatClock = null;
        jumpSuspended = false; alternateOut = false;
    }
    public static void corrected(LocalPlayer player) {
        if (config == null) return;
        // Never return to an obsolete jump origin after a server correction.
        trajectory = null;
        jumpSuspended = true;
        if (player != null) baseYaw = player.getYRot();
        alternateOut = false;
        rotationClock.postpone(System.nanoTime());
        LOG.info("[Zero Render AFK] Server correction: packet jumps disabled for this AFK session.");
    }
    public static boolean beforeSync(LocalPlayer player) {
        if (config == null || !ZeroRenderAFKClient.canRunAntiAfk()) return false;
        if (!player.connection.getConnection().isConnected()) { stop(); return false; }
        try { return run(player, System.nanoTime()); }
        catch (RuntimeException e) {
            LOG.error("[Zero Render AFK] Anti-AFK failed; disconnecting", e);
            ZeroRenderAFKClient.disableAfk();
            return true;
        }
    }
    private static boolean run(LocalPlayer p, long now) {
        boolean rotated = false;
        if (config.rotation && rotationClock.due(now)) {
            float yaw;
            if (config.alternate) {
                alternateOut = !alternateOut;
                yaw = baseYaw + (alternateOut ? (float) config.yawStep : 0);
            } else yaw = p.getYRot() + (float) config.yawStep;
            yaw = ((yaw + 180) % 360 + 360) % 360 - 180;
            p.setYRot(yaw);
            p.setXRot((float) config.pitch);
            rotated = true;
        }

        if (config.command && commandClock.due(now)) p.connection.sendCommand(config.normalizedCommand());
        if (config.chat && chatClock.due(now)) p.connection.sendChat(config.chatText);

        if (config.jump && !jumpSuspended && trajectory == null && jumpClock.due(now)) {
            if (p.onGround() && !p.isPassenger() && !p.isInWater() && !p.isDeadOrDying()) {
                startX = p.getX(); startY = p.getY(); startZ = p.getZ();
                trajectory = new JumpTrajectory(config.jumpHeight);
                lastJumpStep = now - 50_000_000L;
            }
        }
        if (trajectory != null) {
            if (now - lastJumpStep >= 50_000_000L) {
                lastJumpStep = now;
                double offset = trajectory.nextOffset();
                boolean landed = trajectory.finished();
                p.setPos(startX, startY + offset, startZ);
                p.setOnGround(landed);
                p.connection.send(new ServerboundMovePlayerPacket.PosRot(
                    new Vec3(p.getX(), p.getY(), p.getZ()), p.getYRot(), p.getXRot(), landed, false
                ));
                if (landed) { trajectory = null; jumpClock.postpone(now); }
            } else if (rotated) {
                sendRotation(p);
            }
            // Avoid vanilla emitting a contradictory position during a jump.
            return true;
        }
        if (rotated) { sendRotation(p); return true; }
        return false; // Keep vanilla stationary position reminders.
    }
    private static void sendRotation(LocalPlayer p) {
        p.connection.send(new ServerboundMovePlayerPacket.Rot(
            p.getYRot(), p.getXRot(), p.onGround(), p.horizontalCollision
        ));
    }
}
