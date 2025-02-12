package net.conczin.immersive_optimization.mixin;

import net.conczin.immersive_optimization.TickScheduler;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "loadLevel()V", at = @At("HEAD"))
    private void immersiveOptimization$loadLevel(CallbackInfo ci) {
        TickScheduler.setServer((MinecraftServer) (Object) this);
    }
}
