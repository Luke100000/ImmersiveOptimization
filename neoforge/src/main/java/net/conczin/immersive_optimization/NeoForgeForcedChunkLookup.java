package net.conczin.immersive_optimization;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager;

import java.util.Map;

public class NeoForgeForcedChunkLookup implements CommonClass.ForcedChunkLookup {
    @Override
    public LongSet getForcedChunks(ServerLevel level) {
        LongSet chunks = new LongOpenHashSet(level.getForcedChunks());

        ForcedChunksSavedData data = level.getDataStorage().get(ForcedChunksSavedData.factory(), "chunks");
        if (data == null) {
            return chunks;
        }

        addAll(chunks, data.getBlockForcedChunks());
        addAll(chunks, data.getEntityForcedChunks());
        return chunks;
    }

    private static <T extends Comparable<? super T>> void addAll(LongSet chunks, ForcedChunkManager.TicketTracker<T> tracker) {
        addAll(chunks, tracker.getChunks());
        addAll(chunks, tracker.getTickingChunks());
    }

    private static void addAll(LongSet chunks, Map<?, LongSet> tickets) {
        for (LongSet ticketChunks : tickets.values()) {
            chunks.addAll(ticketChunks);
        }
    }
}
