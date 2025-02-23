package net.conczin.immersive_optimization.mixin.client;

import net.conczin.immersive_optimization.TickScheduler;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.renderer.LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow
    private Frustum cullingFrustum;

    @Inject(method = "prepareCullFrustum(Lnet/minecraft/world/phys/Vec3;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V", at = @At("RETURN"))
    private void immersiveOptimization$prepareCullFrustum(Vec3 $$0, Matrix4f $$1, Matrix4f $$2, CallbackInfo ci) {
        if (TickScheduler.INSTANCE.frustum == null) {
            TickScheduler.INSTANCE.frustum = aabb -> cullingFrustum.isVisible(aabb.inflate(1.0));
        }
    }
}
