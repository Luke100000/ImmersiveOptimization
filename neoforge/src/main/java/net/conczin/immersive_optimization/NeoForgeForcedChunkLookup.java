package net.conczin.immersive_optimization;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager;

import java.util.Map;

public class NeoForgeForcedChunkLookup implements CommonClass.ForcedChunkLookup {
    @Override
    public boolean isForced(ServerLevel level, long chunk) {
        if (level.getForcedChunks().contains(chunk)) {
            return true;
        }

        ForcedChunksSavedData data = level.getDataStorage().get(ForcedChunksSavedData.factory(), "chunks");
        if (data == null) {
            return false;
        }

        return contains(data.getBlockForcedChunks(), chunk) || contains(data.getEntityForcedChunks(), chunk);
    }

    private static <T extends Comparable<? super T>> boolean contains(ForcedChunkManager.TicketTracker<T> tracker, long chunk) {
        return contains(tracker.getChunks(), chunk) || contains(tracker.getTickingChunks(), chunk);
    }

    private static boolean contains(Map<?, LongSet> tickets, long chunk) {
        for (LongSet chunks : tickets.values()) {
            if (chunks.contains(chunk)) {
                return true;
            }
        }
        return false;
    }
}
