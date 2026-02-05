package net.conczin.immersive_optimization;

import net.conczin.immersive_optimization.client.OptimizationDebugEntry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;

public class ImmersiveOptimization implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> Commands.register(dispatcher));

        DebugScreenEntries.register(OptimizationDebugEntry.ID, new OptimizationDebugEntry());
    }
}
