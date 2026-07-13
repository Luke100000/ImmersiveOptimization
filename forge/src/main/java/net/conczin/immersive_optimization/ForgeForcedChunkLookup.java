package net.conczin.immersive_optimization;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ServerLevel;

public class ForgeForcedChunkLookup implements CommonClass.ForcedChunkLookup {
    @Override
    public LongSet getForcedChunks(ServerLevel level) {
        return new LongOpenHashSet(level.getForceLoadedChunks());
    }
}
