package com.ayoubela.zerorenderafk.client.mixin;

import com.ayoubela.zerorenderafk.client.ZeroRenderAFKClient;
import java.util.Deque;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {
    @Shadow @Final private Deque<?> transientBlockQueue;

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void zerorenderafk$clearTransientReferences(CallbackInfo ci) {
        if (ZeroRenderAFKClient.isAfkEnabled()) transientBlockQueue.clear();
    }

    // The extractor is detached while AFK. Never dereference its null tracker.
    @Inject(method = {"setSectionDirty", "queueTransientBlock"},
            at = @At("HEAD"), cancellable = true)
    private void zerorenderafk$skipDirtyTracking(CallbackInfo ci) {
        if (ZeroRenderAFKClient.isAfkEnabled()) ci.cancel();
    }
}
