package net.conczin.immersive_optimization.mixin.client;

import net.conczin.immersive_optimization.TickScheduler;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.client.renderer.LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(method = "prepareCullFrustum(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/client/renderer/culling/Frustum;", at = @At("RETURN"))
    private void immersiveOptimization$prepareCullFrustum(Matrix4f frustumMatrix, Matrix4f projectionMatrix, Vec3 cameraPosition, CallbackInfoReturnable<Frustum> cir) {
        TickScheduler.INSTANCE.frustum = aabb -> cir.getReturnValue().isVisible(aabb.inflate(1.0));
    }
}
