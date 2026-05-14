package net.conczin.immersive_optimization;

import net.minecraft.server.level.ServerLevel;

public class CommonClass {
    public static void init() {
        // No-op
    }

    public static ForcedChunkLookup forcedChunkLookup = (level, chunk) -> level.getForceLoadedChunks().contains(chunk);

    public static boolean isForceLoaded(ServerLevel level, long chunk) {
        return forcedChunkLookup.isForced(level, chunk);
    }

    public interface ForcedChunkLookup {
        boolean isForced(ServerLevel level, long chunk);
    }
}
