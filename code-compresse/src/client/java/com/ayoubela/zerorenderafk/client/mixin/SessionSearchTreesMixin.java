package com.ayoubela.zerorenderafk.client.mixin;

import com.ayoubela.zerorenderafk.client.ZeroRenderAFKClient;
import com.ayoubela.zerorenderafk.client.access.SearchDataAccess;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.multiplayer.SessionSearchTrees;
import net.minecraft.client.searchtree.SearchTree;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(SessionSearchTrees.class)
public abstract class SessionSearchTreesMixin implements SearchDataAccess {

    @Shadow
    @Final
    private Map<?, Runnable> reloaders;

    @Shadow
    private CompletableFuture<?> creativeByNameSearch;

    @Shadow
    private CompletableFuture<?> creativeByTagSearch;

    @Shadow
    private CompletableFuture<?> recipeSearch;

    @Override
    public void zerorenderafk$releaseSearchData() {
        // Release callbacks that capture worlds, recipe books and item lists.
        reloaders.clear();

        CompletableFuture<?> oldNames = creativeByNameSearch;
        CompletableFuture<?> oldTags = creativeByTagSearch;
        CompletableFuture<?> oldRecipes = recipeSearch;

        // Keep getters usable without retaining the previous search trees.
        creativeByNameSearch =
            CompletableFuture.completedFuture(SearchTree.empty());
        creativeByTagSearch =
            CompletableFuture.completedFuture(SearchTree.empty());
        recipeSearch =
            CompletableFuture.completedFuture(SearchTree.empty());

        oldNames.cancel(false);
        oldTags.cancel(false);
        oldRecipes.cancel(false);
    }

    @Inject(
        method = {
            "updateRecipes",
            "updateCreativeTags",
            "updateCreativeTooltips"
        },
        at = @At("HEAD"),
        cancellable = true
    )
    private void zerorenderafk$skipSearchRebuild(CallbackInfo ci) {
        if (ZeroRenderAFKClient.isAfkEnabled()) {
            ci.cancel();
        }
    }
}