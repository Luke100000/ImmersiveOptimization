package net.conczin.immersive_optimization.mixin;

import net.conczin.immersive_optimization.TickScheduler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(Level.class)
public abstract class LevelMixin {
    @Inject(method = "guardEntityTick(Ljava/util/function/Consumer;Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    public <T extends Entity> void immersiveOptimization$guardEntityTick(Consumer<T> $$0, T $$1, CallbackInfo ci) {
        if (!TickScheduler.INSTANCE.shouldTick($$1)) {
            ci.cancel();
        }
    }

    @Inject(method = "shouldTickBlocksAt(J)Z", at = @At("HEAD"), cancellable = true)
    public void immersiveOptimization$shouldTickBlocksAt(long $$0, CallbackInfoReturnable<Boolean> cir) {
        if (!TickScheduler.INSTANCE.shouldTick((Level)(Object)this, $$0)) {
            cir.setReturnValue(false);
        }
    }
}
