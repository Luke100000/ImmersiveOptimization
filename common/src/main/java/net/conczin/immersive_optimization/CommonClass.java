package net.conczin.immersive_optimization;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ServerLevel;

public class CommonClass {
    public static void init() {
        // No-op
    }

    public static ForcedChunkLookup forcedChunkLookup = level -> new LongOpenHashSet(level.getForcedChunks());

    public static LongSet getForcedChunks(ServerLevel level) {
        return forcedChunkLookup.getForcedChunks(level);
    }

    public interface ForcedChunkLookup {
        LongSet getForcedChunks(ServerLevel level);
    }
}
