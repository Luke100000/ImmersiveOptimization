package net.conczin.immersive_optimization;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraftforge.common.world.ForgeChunkManager;

import java.util.Map;

public class ForgeForcedChunkLookup implements CommonClass.ForcedChunkLookup {
    @Override
    public boolean isForced(ServerLevel level, long chunk) {
        if (level.getForcedChunks().contains(chunk)) {
            return true;
        }

        ForcedChunksSavedData data = level.getDataStorage().get(ForcedChunksSavedData::load, "chunks");
        if (data == null) {
            return false;
        }

        return contains(data.getBlockForcedChunks(), chunk) || contains(data.getEntityForcedChunks(), chunk);
    }

    private static <T extends Comparable<? super T>> boolean contains(ForgeChunkManager.TicketTracker<T> tracker, long chunk) {
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
