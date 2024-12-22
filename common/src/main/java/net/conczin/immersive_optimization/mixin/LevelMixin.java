package net.conczin.immersive_optimization.mixin;

import net.conczin.immersive_optimization.EntityProfiler;
import net.conczin.immersive_optimization.TickScheduler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(Level.class)
public abstract class LevelMixin {
    @Unique
    private long immersiveOptimization$time = 0;

    @Inject(method = "guardEntityTick(Ljava/util/function/Consumer;Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    public <T extends Entity> void immersiveOptimization$guardEntityTick(Consumer<T> $$0, T $$1, CallbackInfo ci) {
        immersiveOptimization$time = System.nanoTime();
    }

    @Inject(method = "guardEntityTick(Ljava/util/function/Consumer;Lnet/minecraft/world/entity/Entity;)V", at = @At("RETURN"))
    public <T extends Entity> void immersiveOptimization$guardEntityTickEnd(Consumer<T> $$0, T $$1, CallbackInfo ci) {
        if ($$1.level().isClientSide()) {
            EntityProfiler.CLIENT.logEntity($$1, System.nanoTime() - immersiveOptimization$time);
        } else {
            EntityProfiler.SERVER.logEntity($$1, System.nanoTime() - immersiveOptimization$time);
        }
    }

    @Inject(method = "shouldTickBlocksAt(J)Z", at = @At("HEAD"), cancellable = true)
    public void immersiveOptimization$shouldTickBlocksAt(long pos, CallbackInfoReturnable<Boolean> cir) {
        if (!TickScheduler.INSTANCE.shouldTick((Level) (Object) this, pos)) {
            cir.setReturnValue(false);
        }
    }
}