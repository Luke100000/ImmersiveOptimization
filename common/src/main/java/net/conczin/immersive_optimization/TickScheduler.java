package net.conczin.immersive_optimization;

import com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import com.logisticscraft.occlusionculling.util.Vec3d;
import net.conczin.immersive_optimization.config.Config;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TickScheduler {
    public static final TickScheduler INSTANCE = new TickScheduler();

    public static final int ENTITY_UPDATE_TIME_RANGE = 27;
    public static final int CLEAR_BLOCK_ENTITIES_INTERVAL = 207;
    public static final int CLEAR_ENTITIES_INTERVAL = 12007;

    public FrustumProxy frustum;

    public interface FrustumProxy {
        boolean isVisible(AABB aabb);
    }

    public static class LevelData {
        public boolean active;

        public long time = 0;
        public long tick = 0;
        public long budget = 50_000_000;
        public boolean outOfBudget = false;

        // TODO: current and previous tick rate
        public double totalTickRate = 0;
        public int totalEntities = 0;
        public int totalDistanceCulledEntities = 0;
        public int totalViewportCulledEntities = 0;
        public int totalOcclusionCulledEntities = 0;

        public int stressedTicks = 0;
        public int lifeTimeStressedTicks = 0;
        public int lifeTimeBudgetTicks = 0;

        public Map<Integer, Integer> stressed = new HashMap<>();
        public Map<Integer, Integer> priorities = new HashMap<>();

        public Map<Long, Integer> blockEntityPriorities = new HashMap<>();

        public Map<Integer, Long> lastPlayerDistance = new HashMap<>();
        public Map<Integer, OcclusionCullingInstance> cullingInstances = new HashMap<>();

        public LevelData(String identifier) {
            active = Config.getInstance().dimensions.getOrDefault(identifier, true);
        }
    }

    public final Map<String, LevelData> levelData = new ConcurrentHashMap<>();

    public LevelData getLevelData(Level level) {
        return levelData.computeIfAbsent(level.dimension().location().toString(), LevelData::new);
    }

    public void reset() {
        levelData.clear();
        frustum = null;
    }

    protected Vec3d minCorner = new Vec3d(0, 0, 0);
    protected Vec3d maxCorner = new Vec3d(0, 0, 0);
    protected Vec3d eyePosition = new Vec3d(0, 0, 0);

    public void tick() {
        // Count total entities
        int totalEntities = 0;
        for (LevelData data : levelData.values()) {
            totalEntities += data.totalEntities;
        }

        // And assign a budget to each level
        for (LevelData data : levelData.values()) {
            double budget = Config.getInstance().entityTickBudget;
            data.budget = budget > 0 ? (long) (budget * data.totalEntities / (totalEntities + 1) * 1_000_000) : 0;
        }
    }

    public void prepareLevel(Level level, EntityTickList tickingEntities) {
        LevelData data = getLevelData(level);
        data.time = System.nanoTime();
        data.tick = level.getGameTime();

        // Update level stress status
        MinecraftServer server = level.getServer();
        int stressedThreshold = Config.getInstance().stressedThreshold;
        boolean stressed = stressedThreshold > 0 && server != null && server.tickTimes[(server.getTickCount() - 1) % 100] > stressedThreshold;
        if (data.outOfBudget || stressed) {
            data.stressedTicks++;
            if (stressed) data.lifeTimeStressedTicks++;
            if (data.outOfBudget) data.lifeTimeBudgetTicks++;
            data.outOfBudget = false;
        } else {
            data.stressedTicks = Math.max(0, data.stressedTicks - 1);
        }

        if (data.tick % ENTITY_UPDATE_TIME_RANGE == 0 && data.totalEntities > 0) {
            data.totalTickRate = 0;
            data.totalEntities = 0;
            data.totalDistanceCulledEntities = 0;
            data.totalViewportCulledEntities = 0;
            data.totalOcclusionCulledEntities = 0;

            // Prepare occlusion culling caches
            if (Config.getInstance().enableOcclusionCulling) {
                for (Player player : level.players()) {
                    int id = player.getId();
                    Vec3 pos = player.position();
                    long position = (long) pos.x + (long) pos.y * 1024 + (long) pos.z * 1024 * 1024;
                    if (!data.lastPlayerDistance.containsKey(id) || data.lastPlayerDistance.get(id) != position) {
                        data.lastPlayerDistance.put(id, position);
                        if (data.cullingInstances.containsKey(id)) {
                            data.cullingInstances.get(id).resetCache();
                        } else {
                            data.cullingInstances.put(id, new OcclusionCullingInstance(Config.getInstance().occlusionCullingDistance, new Provider(level)));
                        }
                    }
                }
            }
        }

        // Clear priorities every n seconds to avoid memory leaks
        if (data.tick % CLEAR_ENTITIES_INTERVAL == 0) {
            data.stressed.clear();
            data.priorities.clear();
            data.cullingInstances.clear();
            data.totalTickRate = 0;
        }
        if (data.tick % CLEAR_BLOCK_ENTITIES_INTERVAL == 0) {
            data.blockEntityPriorities.clear();
        }

        // Update entity priorities (distributed over n ticks)
        tickingEntities.forEach(entity -> {
            if ((data.tick + entity.getId()) % ENTITY_UPDATE_TIME_RANGE == 0) {
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
        if (entity.noCulling) {
            return true;
        }

        LevelData data = getLevelData(entity.level());

        int priority = data.priorities.getOrDefault(entity.getId(), 0);
        if (priority <= 1) {
            return true;
        }

        // No more resources, apply stress instead
        if (data.outOfBudget) {
            data.stressed.put(entity.getId(), data.stressed.getOrDefault(entity.getId(), 0) + 1);
            return false;
        }

        // Check if we are still within budget
        if (data.budget > 0 && System.nanoTime() - data.time > data.budget) {
            data.outOfBudget = true;
        }

        int stress = data.stressed.getOrDefault(entity.getId(), 0);
        if ((data.tick + entity.getId()) % Math.max(1, priority - stress) == 0) {
            // Relax again
            if (stress > 0) {
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
        double closestDistance = Double.MAX_VALUE;
        Player closestPlayer = null;
        for (Player player : level.players()) {
            double dx = player.getX() - entity.getX();
            double dy = player.getY() - entity.getY();
            double dz = player.getZ() - entity.getZ();
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance < closestDistance) {
                closestDistance = distance;
                closestPlayer = player;
            }
        }

        AABB box = entity.getBoundingBox();
        int blocksPerLevel = Config.getInstance().blocksPerLevel;

        // View distance culling
        if (Config.getInstance().enableDistanceCulling && !entity.shouldRenderAtSqrDistance(closestDistance)) {
            blocksPerLevel = Math.min(blocksPerLevel, Config.getInstance().blocksPerLevelDistanceCulled);
            data.totalDistanceCulledEntities++;
        }

        // Frustum culling (Only available for integrated servers)
        if (Config.getInstance().enableViewportCulling &&
            level.players().size() == 1
            && frustum != null
            && !frustum.isVisible(box)) {
            blocksPerLevel = Math.min(blocksPerLevel, Config.getInstance().blocksPerLevelViewportCulled);
            data.totalViewportCulledEntities++;
        }

        // Occlusion culling
        if (Config.getInstance().enableOcclusionCulling && closestPlayer != null && box.getSize() < 10 && closestDistance < Math.pow(Config.getInstance().occlusionCullingDistance - 1, 2) && data.cullingInstances.containsKey(closestPlayer.getId())) {
            OcclusionCullingInstance cullingInstance = data.cullingInstances.get(closestPlayer.getId());
            Vec3 eye = closestPlayer.getEyePosition();

            minCorner.x = box.minX;
            minCorner.y = box.minY;
            minCorner.z = box.minZ;
            maxCorner.x = box.maxX;
            maxCorner.y = box.maxY;
            maxCorner.z = box.maxZ;
            eyePosition.x = eye.x;
            eyePosition.y = eye.y;
            eyePosition.z = eye.z;

            if (!cullingInstance.isAABBVisible(minCorner, maxCorner, eyePosition)) {
                blocksPerLevel = Math.min(blocksPerLevel, Config.getInstance().blocksPerLevelOcclusionCulled);
                data.totalOcclusionCulledEntities++;
            }
        }

        // Assign an optimization level
        int antiStress = (int) Math.sqrt(data.stressedTicks);
        int finalBlocksPerLevel = blocksPerLevel - data.stressedTicks - antiStress;
        int distanceLevel = (int) ((Math.sqrt(closestDistance) - Config.getInstance().minDistance) / Math.max(2, finalBlocksPerLevel));

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
