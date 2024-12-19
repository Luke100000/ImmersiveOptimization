package net.conczin.immersive_optimization.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.conczin.immersive_optimization.TickScheduler;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(net.minecraft.client.renderer.LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow
    private Frustum cullingFrustum;

    @ModifyArgs(method = "renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"))
    private void immersiveOptimization$renderLevel(Args args) {
        Entity entity = args.get(0);
        TickScheduler.LevelData data = TickScheduler.INSTANCE.getLevelData(entity.level());
        int priority = data.priorities.getOrDefault(entity.getId(), 0);
        if (priority > 1) {
            // Adjust the tickTime to match for the extended tick duration
            long last = data.lastUpdates.getOrDefault(entity.getId(), 0L);
            float delta = Math.max(0, entity.level().getGameTime() - last - 1);
            float oldTickTime = args.get(4);
            args.set(4, (delta + oldTickTime) / priority);

            if (entity instanceof LivingEntity living && living.hurtTime > 0) {
                System.out.println("Hurt time: " + living.getId() + "  " + delta + "   " + priority + "  " + (delta + oldTickTime) / priority);
            }
        }
    }

    @Inject(method = "prepareCullFrustum(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;Lorg/joml/Matrix4f;)V", at = @At("RETURN"))
    private void immersiveOptimization$prepareCullFrustum(PoseStack $$0, Vec3 $$1, Matrix4f $$2, CallbackInfo ci) {
        TickScheduler.INSTANCE.frustum = cullingFrustum;
    }
}
