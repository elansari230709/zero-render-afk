package com.ayoubela.zerorenderafk.client.mixin;

import com.ayoubela.zerorenderafk.client.ZeroRenderAFKClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow @Final private DeltaTracker.Timer deltaTracker;

    @Redirect(method = "runTick(Z)V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/Minecraft;renderFrame(Z)V"), require = 1)
    private void zerorenderafk$renderOrWait(Minecraft client, boolean advanceGameTime) {
        if (client.level == null || client.player == null
            || client.gui.screen() != null || client.gui.overlay() != null) {
            ZeroRenderAFKClient.disableAfk();
        }
        if (!ZeroRenderAFKClient.isAfkEnabled()) {
            client.renderFrame(advanceGameTime);
            return;
        }
        this.deltaTracker.advanceRealTime(Util.getMillis());
        // Incoming world-light work is filtered and existing work was purged.
        // Do not call level.update() in this low-memory version.
        RenderSystem.executePendingTasks();
        try {
            Thread.sleep(50L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            client.stop();
        }
    }

    @Inject(method = {"pick", "handleKeybinds"}, at = @At("HEAD"), cancellable = true)
    private void zerorenderafk$skipWorldInput(CallbackInfo ci) {
        // F8 is consumed by END_CLIENT_TICK, outside handleKeybinds().
        if (ZeroRenderAFKClient.isAfkEnabled()) ci.cancel();
    }

    @Redirect(method = "tick()V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/GameRenderer;tick()V"), require = 1)
    private void zerorenderafk$skipRendererTick(GameRenderer renderer) {
        if (!ZeroRenderAFKClient.isAfkEnabled()) renderer.tick();
    }

    @Redirect(method = "tick()V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/particle/ParticleEngine;tick()V"), require = 1)
    private void zerorenderafk$skipParticleTick(ParticleEngine particles) {
        if (!ZeroRenderAFKClient.isAfkEnabled()) particles.tick();
    }
}
