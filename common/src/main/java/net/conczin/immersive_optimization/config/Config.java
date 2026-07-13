package net.conczin.immersive_optimization.config;

import net.conczin.immersive_optimization.Constants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class Config extends JsonConfig {
    private static Config INSTANCE = loadOrCreate(new Config(), Config.class);

    public Config() {
        super(Constants.MOD_ID);
    }

    public static Config getInstance() {
        return INSTANCE;
    }

    @Override
    int getVersion() {
        return 2;
    }

    @SuppressWarnings("unused")
    public String _documentation = "https://github.com/Luke100000/ImmersiveOptimization/wiki";

    // Enable the mod. If you plan to not use it altogether, uninstall it.
    public boolean enableEntities = true;

    // Enable block entity optimization. A handful of mods, such as Create or Ad Astras logistics, may glitch.
    public boolean enableBlockEntities = false;

    // Slow down entities and block entities in force-loaded chunks.
    public boolean optimizeForceLoadedChunks = false;

    // Distance culling reduces the tick rate of entities when not visible due to render distance.
    // (Singleplayer only)
    public boolean enableDistanceCulling = true;
    // Tracking culling reduces the tick rate of entities when not tracked due to distance.
    public boolean enableTrackingCulling = true;
    // Viewport culling slows down entities when outside the camera perspective.
    // (Singleplayer only)
    public boolean enableViewportCulling = true;

    // Every blocksPerLevel, the tick rate will be reduced by 1, offset by initial minDistance to avoid visible glitches.
    // Smaller values increase server performance.
    public int minDistance = 6;
    public int blocksPerLevel = 64;
    public int blocksPerLevelDistanceCulled = 10;
    public int blocksPerLevelTrackingCulled = 10;
    public int blocksPerLevelViewportCulled = 20;
    public int maxLevel = 20;

    // The same for block entities, but without further culling.
    public int blocksPerLevelBlockEntities = 32;

    // The ms of the total server tick time before the server is considered stressed.
    // When stressed, the server will gradually increase the blockedPerLevel by at least minDecreaseFactor.
    // This may increase visual glitches with clients and is a last resort to avoid lag.
    // 0 to turn off.
    public int stressedThreshold = 45;
    public float minDecreaseFactor = 0.25f;

    // Set to "false" to disable scheduling on given dimensions.
    public Map<String, Boolean> dimensions;

    {
        dimensions = new HashMap<>();
        dimensions.put("minecraft:overworld", true);
        dimensions.put("minecraft:the_nether", true);
        dimensions.put("minecraft:the_end", true);
    }

    // Or entities. Rules accept resource locations, #tags, namespaces, or "*" and are checked in that order.
    // True whitelists matching entities, false blacklists them. Smaller matching tags take priority.
    public Map<String, Boolean> entities;

    private final transient Map<Identifier, Boolean> entityBlacklistCache = new ConcurrentHashMap<>();

    {
        entities = new HashMap<>();

        entities.put("minecraft:player", false);
        entities.put("minecraft:ender_dragon", false);
        entities.put("minecraft:ender_pearl", false);

        entities.put("fromanotherworld:starship", false);

        entities.put("create", false);
        entities.put("valkyrienskies", false);

        entities.put("#minecraft:arrows", false);

        entities.put("*", true);
    }

    public boolean isBlacklisted(EntityType<?> entityType) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        Boolean cached = entityBlacklistCache.get(id);
        if (cached != null) {
            return cached;
        }

        synchronized (this) {
            return entityBlacklistCache.computeIfAbsent(id, ignored -> resolveBlacklist(entityType, id));
        }
    }

    private boolean resolveBlacklist(EntityType<?> entityType, Identifier id) {
        // Lookup id
        Boolean enabled = entities.get(id.toString());
        if (enabled != null) return !enabled;

        // Lookup tags
        enabled = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entityType).tags()
                .filter(tag -> entities.containsKey("#" + tag.location()))
                .sorted(Comparator
                        .comparingInt(this::getTagSize)
                        .thenComparing(tag -> tag.location().toString()))
                .map(tag -> entities.get("#" + tag.location()))
                .findFirst()
                .orElse(null);
        if (enabled != null) return !enabled;

        // Lookup namespace
        enabled = entities.get(id.getNamespace());
        if (enabled != null) return !enabled;

        // Default
        return !entities.getOrDefault("*", true);
    }

    private int getTagSize(TagKey<EntityType<?>> tag) {
        return BuiltInRegistries.ENTITY_TYPE.get(tag).orElseThrow().size();
    }

    public synchronized boolean toggleEntityRule(String id, boolean enabled) {
        boolean added;
        if (Objects.equals(entities.get(id), enabled)) {
            entities.remove(id);
            added = false;
        } else {
            entities.put(id, enabled);
            added = true;
        }
        entityBlacklistCache.clear();
        return added;
    }

    // Projectiles are often client-side predicted, so culling them may cause glitches.
    public boolean cullProjectiles = false;

    public void reload() {
        INSTANCE = loadOrCreate(new Config(), Config.class);
    }
}
