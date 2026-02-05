package net.conczin.immersive_optimization.client;

import net.conczin.immersive_optimization.Constants;
import net.conczin.immersive_optimization.TickScheduler;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

public class OptimizationDebugEntry implements DebugScreenEntry {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "info");

    @Override
    public void display(@NotNull DebugScreenDisplayer displayer, @Nullable Level level, @Nullable LevelChunk levelChunk, @Nullable LevelChunk levelChunk1) {
        if (level != null) {
            TickScheduler.LevelData data = TickScheduler.INSTANCE.getLevelData(level);
            if (data != null) {
                displayer.addLine("[IO] " + data.toLog());
            }
        }
    }
}
