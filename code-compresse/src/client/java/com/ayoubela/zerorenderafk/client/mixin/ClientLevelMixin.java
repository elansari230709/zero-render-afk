package com.ayoubela.zerorenderafk.client.mixin;

import com.ayoubela.zerorenderafk.client.ZeroRenderAFKClient;
import com.ayoubela.zerorenderafk.client.access.LevelDataAccess;
import java.util.Deque;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin implements LevelDataAccess {
    @Shadow @Final private Deque<Runnable> lightUpdateQueue;

    @Override
    public void zerorenderafk$clearPendingLighting() {
        lightUpdateQueue.clear();
    }

    // Physics must not run against discarded chunks. Player sendChanges() is
    // called separately by Minecraft.tick() and remains enabled.
    @Inject(method = {"tick", "tickEntities", "animateTick"}, at = @At("HEAD"), cancellable = true)
    private void zerorenderafk$skipSimulation(CallbackInfo ci) {
        if (ZeroRenderAFKClient.isAfkEnabled()) ci.cancel();
    }

    @Inject(method = {"queueLightUpdate", "doAddParticle", "createFireworks", "trackExplosionEffects"},
            at = @At("HEAD"), cancellable = true)
    private void zerorenderafk$skipVisualWork(CallbackInfo ci) {
        if (ZeroRenderAFKClient.isAfkEnabled()) ci.cancel();
    }
}
