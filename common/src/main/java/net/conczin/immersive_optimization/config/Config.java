package net.conczin.immersive_optimization.config;

import net.conczin.immersive_optimization.Constants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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

    private final transient Map<EntityType<?>, Boolean> entityBlacklistCache = new ConcurrentHashMap<>();

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

    // The same for block entities, only active with enableBlockEntities.
    // Rules accept block entity type ids, block ids, #block tags, namespaces, or "*" and are checked in that order.
    public Map<String, Boolean> blockEntities;

    // Bumped whenever block entity rules change, so tickers know to resolve their rule again.
    public static final AtomicInteger BLOCK_ENTITY_RULE_VERSION = new AtomicInteger();

    {
        blockEntities = new HashMap<>();

        blockEntities.put("the_bumblezone:essence_block", false);
        blockEntities.put("the_bumblezone:potion_candle", false);

        blockEntities.put("*", true);
    }

    public boolean isBlacklisted(EntityType<?> entityType) {
        Boolean cached = entityBlacklistCache.get(entityType);
        if (cached != null) {
            return cached;
        }
        return entityBlacklistCache.computeIfAbsent(entityType, this::resolveBlacklist);
    }

    private boolean resolveBlacklist(EntityType<?> entityType) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);

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

    public boolean isBlacklisted(BlockEntity blockEntity) {
        Identifier typeId = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
        Boolean enabled = blockEntities.get(typeId.toString());
        if (enabled != null) return !enabled;

        var block = blockEntity.getBlockState().getBlock();
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
        enabled = blockEntities.get(blockId.toString());
        if (enabled != null) return !enabled;

        enabled = BuiltInRegistries.BLOCK.wrapAsHolder(block).tags()
                .filter(tag -> blockEntities.containsKey("#" + tag.location()))
                .sorted(Comparator.comparing(tag -> tag.location().toString()))
                .map(tag -> blockEntities.get("#" + tag.location()))
                .findFirst()
                .orElse(null);
        if (enabled != null) return !enabled;

        enabled = blockEntities.get(typeId.getNamespace());
        if (enabled != null) return !enabled;

        return !blockEntities.getOrDefault("*", true);
    }

    public synchronized boolean toggleBlockEntityRule(String id, boolean enabled) {
        boolean added = toggleRule(blockEntities, id, enabled);
        BLOCK_ENTITY_RULE_VERSION.incrementAndGet();
        return added;
    }

    public synchronized boolean toggleEntityRule(String id, boolean enabled) {
        boolean added = toggleRule(entities, id, enabled);
        entityBlacklistCache.clear();
        return added;
    }

    private static boolean toggleRule(Map<String, Boolean> rules, String id, boolean enabled) {
        if (Objects.equals(rules.get(id), enabled)) {
            rules.remove(id);
            return false;
        }
        rules.put(id, enabled);
        return true;
    }

    // Projectiles are often client-side predicted, so culling them may cause glitches.
    public boolean cullProjectiles = false;

    public void reload() {
        INSTANCE = loadOrCreate(new Config(), Config.class);
        BLOCK_ENTITY_RULE_VERSION.incrementAndGet();
    }
}
