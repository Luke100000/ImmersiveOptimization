package net.conczin.immersive_optimization;

import net.conczin.immersive_optimization.config.Config;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTickList;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TickScheduler {
    public static TickScheduler INSTANCE = new TickScheduler();

    public boolean isLocalServer = false;
    public Frustum frustum;

    public static class LevelData {
        public boolean active;
        public boolean client;

        public long time = 0;
        public long tick = 0;
        public long budget = 50_000_000;
        public boolean outOfBudget = false;

        public double totalTickRate = 0;
        public int totalEntities = 0;

        public int stressedTicks = 0;
        public int lifeTimeStressedTicks = 0;
        public int lifeTimeBudgetTicks = 0;

        public Map<Integer, Long> lastUpdates = new ConcurrentHashMap<>();
        public Map<Integer, Integer> stressed = new ConcurrentHashMap<>();
        public Map<Integer, Integer> priorities = new ConcurrentHashMap<>();
        public Map<Long, Integer> blockEntityPriorities = new ConcurrentHashMap<>();

        public LevelData(String identifier) {
            active = Config.getInstance().dimensions.getOrDefault(identifier, true);
        }
    }

    public final Map<String, LevelData> levelData = new ConcurrentHashMap<>();

    public LevelData getLevelData(Level level) {
        // If possible link the client level to the server level to remove overhead
        if (level.isClientSide && (!isLocalServer || !Config.getInstance().syncWithIntegratedServer)) {
            LevelData data = levelData.computeIfAbsent("local", LevelData::new);
            data.client = true;
            return data;
        } else {
            return levelData.computeIfAbsent(level.dimension().location().toString(), LevelData::new);
        }
    }

    public void reset() {
        levelData.clear();
        frustum = null;
    }

    public void prepare(boolean client) {
        // Count total entities
        int totalEntities = 0;
        for (LevelData data : levelData.values()) {
            if (data.client == client) {
                totalEntities += data.totalEntities;
            }
        }

        // And assign a budget to each level
        for (LevelData data : levelData.values()) {
            if (data.client == client) {
                double budget = client ? Config.getInstance().entityTickBudgetClient : Config.getInstance().entityTickBudgetServer;
                data.budget = budget > 0 ? (long) (budget * data.totalEntities / (totalEntities + 1) * 1_000_000) : 0;
            }
        }
    }

    public void prepareLevel(Level level, EntityTickList tickingEntities) {
        LevelData data = getLevelData(level);

        // Skip linked levels, the server will handle it
        if (data.client != level.isClientSide) {
            return;
        }

        data.time = System.nanoTime();
        data.tick = level.getGameTime();

        // Update level stress status
        MinecraftServer server = level.getServer();
        int stressedThreshold = Config.getInstance().stressedThreshold;
        boolean stressed = stressedThreshold > 0 && server != null && server.getAverageTickTime() > stressedThreshold;
        if (data.outOfBudget || stressed) {
            data.stressedTicks++;
            if (stressed) data.lifeTimeStressedTicks++;
            if (data.outOfBudget) data.lifeTimeBudgetTicks++;
            data.outOfBudget = false;
        } else {
            data.stressedTicks = Math.max(0, data.stressedTicks - 1);
        }

        if (data.tick % 20 == 0 && data.totalEntities > 0) {
            data.totalTickRate = 0;
            data.totalEntities = 0;
        }

        // Clear block entity priorities every 10 seconds
        if (data.tick % 200 == 0) {
            data.blockEntityPriorities.clear();
        }

        // Update entity priorities (distributed over 20 ticks)
        tickingEntities.forEach(entity -> {
            if ((data.tick + entity.getId()) % 20 == 0) {
                int priority = getPriority(data, level, entity);
                if (priority > 0) {
                    data.priorities.put(entity.getId(), priority);
                } else {
                    data.priorities.remove(entity.getId());
                }

                data.totalTickRate += 1.0 / Math.max(1, priority);
                data.totalEntities += 1;
            }
        });
    }

    public boolean shouldTick(Entity entity) {
        LevelData data = getLevelData(entity.level());

        int priority = data.priorities.getOrDefault(entity.getId(), 0);
        if (priority <= 1) {
            return true;
        }

        boolean notSynced = data.client == entity.level().isClientSide;

        if (notSynced) {
            // No more resources, apply stress instead
            if (data.outOfBudget) {
                data.stressed.put(entity.getId(), data.stressed.getOrDefault(entity.getId(), 0) + 1);
                return false;
            }

            // Check if we are still within budget
            if (data.budget > 0 && System.nanoTime() - data.time > data.budget) {
                data.outOfBudget = true;
            }
        }

        int stress = data.stressed.getOrDefault(entity.getId(), 0);
        if ((data.tick + entity.getId()) % Math.max(1, priority - stress) == 0) {
            if (entity.level().isClientSide()) {
                data.lastUpdates.put(entity.getId(), data.tick);
            }

            // Relax again
            if (stress > 0 && notSynced) {
                data.stressed.put(entity.getId(), stress - 1);
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean shouldTick(Level level, long pos) {
        if (!Config.getInstance().enableBlockEntities) {
            return true;
        }
        LevelData data = getLevelData(level);
        int priority = data.blockEntityPriorities.computeIfAbsent(pos, p -> this.getBlockEntityPriority(level, p));
        return priority < 1 || (level.getGameTime() + pos) % priority == 0;
    }

    public int getPriority(LevelData data, Level level, Entity entity) {
        if (!Config.getInstance().enableEntities) {
            return 0;
        }

        // Blacklist entities
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        if (!Config.getInstance().entities.getOrDefault(id, true)) {
            return 0;
        }

        // Find the closest player
        double closestPlayer = Double.MAX_VALUE;
        for (Player player : level.players()) {
            double dx = player.getX() - entity.getX();
            double dy = player.getY() - entity.getY();
            double dz = player.getZ() - entity.getZ();
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance < closestPlayer) {
                closestPlayer = distance;
            }
        }

        int blocksPerLevel = Config.getInstance().blocksPerLevel;

        // TODO: Implement occlusion culling

        // View distance culling
        if (Config.getInstance().enableDistanceCulling && !entity.shouldRenderAtSqrDistance(closestPlayer)) {
            blocksPerLevel = Math.min(blocksPerLevel, Config.getInstance().blocksPerLevelDistanceCulled);
        }

        // Frustum culling (Only single player or client world)
        if (Config.getInstance().enableViewportCulling &&
            (data.client || level.players().size() == 1)
            && frustum != null
            && !frustum.isVisible(entity.getBoundingBox())) {
            blocksPerLevel = Math.min(blocksPerLevel, Config.getInstance().blocksPerLevelViewportCulled);
        }

        // Assign an optimization level
        int antiStress = (int) Math.sqrt(data.stressedTicks);
        int finalBlocksPerLevel = blocksPerLevel - data.stressedTicks - antiStress;
        int distanceLevel = (int) ((Math.sqrt(closestPlayer) - Config.getInstance().minDistance) / Math.max(2, finalBlocksPerLevel));

        // And clamp it to sane numbers
        return Math.min(Config.getInstance().maxLevel, Math.max(1, distanceLevel + 1));
    }

    private int getBlockEntityPriority(Level level, long p) {
        int x = ChunkPos.getX(p);
        int z = ChunkPos.getZ(p);
        double minDistance = Double.MAX_VALUE;
        for (Player player : level.players()) {
            double dx = SectionPos.blockToSectionCoord(player.getX()) - x;
            double dz = SectionPos.blockToSectionCoord(player.getZ()) - z;
            double distance = dx * dx + dz * dz;
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        return Math.min((int) ((Math.sqrt(minDistance * 16) - Config.getInstance().minDistance) / Config.getInstance().blocksPerLevel) + 1, Config.getInstance().maxLevel);
    }
}
