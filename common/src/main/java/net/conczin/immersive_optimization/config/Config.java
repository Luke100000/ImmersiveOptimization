package net.conczin.immersive_optimization.config;

import net.conczin.immersive_optimization.Constants;

import java.util.Map;

public final class Config extends JsonConfig {
    private static final Config INSTANCE = loadOrCreate(new Config(Constants.MOD_ID), Config.class);

    public Config(String name) {
        super(name);
    }

    public static Config getInstance() {
        return INSTANCE;
    }

    // Enable the mod. If you plan to not use it altogether, uninstall it.
    public boolean enable = true;

    // Occlusion culling highly improves visual quality by prioritizing visible entities.
    public boolean enableOcclusionCulling = true;
    public boolean enableViewportCulling = true;

    // Sync the tick rate with the integrated server (if playing single player).
    // This reduces some visual glitches and CPU overhead, and makes use of frustum culling (when #players <= 1).
    public boolean syncWithIntegratedServer = true;

    // Every blocksPerLevel, the tick rate will be reduced by 1, offset by initial minDistance to avoid visible glitches.
    // Smaller values increase server performance.
    // Theoretically it is safe to increase this to large values like 100, then rely on the stressed mechanic below only.
    public int minDistance = 12;
    public int blocksPerLevel = 24;
    public int blocksPerLevelOcclusionCulled = 6;
    public int blocksPerLevelViewportCulled = 12;
    public int maxLevel = 20;

    // When the budget is exceeded, the server will skip all remaining entities, and prioritize them next tick.
    // This math may is slightly biased towards the end of the list.
    public int entityTickBudgetServer = 30;
    public int entityTickBudgetClient = 10;

    // The ms of total server tick time before the server is considered stressed.
    // When stressed, the server will gradually increase the blockedPerLevel.
    // This may increase visual glitches with clients and is a last resort to avoid lag.
    public int stressedThreshold = 45;

    // Set to "false" to disable scheduling on given dimensions.
    public Map<String, Boolean> dimensions = Map.of(
            "minecraft:overworld", true,
            "minecraft:the_nether", true,
            "minecraft:the_end", true
    );

    // Or entities.
    public Map<String, Boolean> entities = Map.of(
            "minecraft:player", false,
            "minecraft:ender_dragon", false,
            "minecraft:arrow", false
    );
}
