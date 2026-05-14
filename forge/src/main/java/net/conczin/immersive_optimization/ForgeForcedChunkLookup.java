package net.conczin.immersive_optimization;

import net.minecraft.server.level.ServerLevel;

public class ForgeForcedChunkLookup implements CommonClass.ForcedChunkLookup {
    @Override
    public boolean isForced(ServerLevel level, long chunk) {
        return level.getForceLoadedChunks().contains(chunk);
    }
}
