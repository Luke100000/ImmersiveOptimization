package net.conczin.immersive_optimization.mixin.client;

import net.conczin.immersive_optimization.TickScheduler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public class DebugHudMixin {
    @Inject(at = @At("RETURN"), method = "getGameInformation")
    protected void getLeftText(CallbackInfoReturnable<List<String>> info) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            TickScheduler.LevelData data = TickScheduler.INSTANCE.getLevelData(level);
            if (data != null) {
                info.getReturnValue().add("[IO] " + data.toLog());
            }
        }
    }
}
