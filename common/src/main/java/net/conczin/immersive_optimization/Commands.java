package net.conczin.immersive_optimization;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.conczin.immersive_optimization.config.Config;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class Commands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("io")
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("report")
                        .executes(context -> {
                            TickScheduler i = TickScheduler.INSTANCE;
                            StringBuilder sb = new StringBuilder();
                            sb.append("§l§a[Immersive Optimization Report]§r\n");
                            i.levelData.forEach((key, data) ->
                                    sb.append("%s rate: %.1f%%, %d stress, %d budgeted\n".formatted(
                                            new ResourceLocation(key).getPath(),
                                            data.totalTickRate / data.totalEntities * 100,
                                            data.lifeTimeStressedTicks,
                                            data.lifeTimeBudgetTicks
                                    )));
                            send(context, sb.toString());
                            return 0;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("profiler")
                        .executes(context -> {
                            StringBuilder sb = new StringBuilder();
                            sb.append("§l§a[Immersive Optimization Profiler]§r\n");
                            EntityProfiler.SERVER.getTopEntities(8).forEach(data -> sb.append(formatProfilerData(data)));
                            if (EntityProfiler.CLIENT.tick > 0) {
                                sb.append("§l§a[Client]§r\n");
                                EntityProfiler.CLIENT.getTopEntities(8).forEach(data -> sb.append(formatProfilerData(data)));
                            }
                            send(context, sb.toString());
                            return 0;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("config")
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("preset")
                                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("performance")
                                        .executes(context -> {
                                            Config c = Config.getInstance();
                                            c.minDistance = 6;
                                            c.blocksPerLevel = 16;
                                            c.blocksPerLevelDistanceCulled = 6;
                                            c.blocksPerLevelViewportCulled = 12;
                                            c.blocksPerLevelOcclusionCulled = 8;
                                            c.maxLevel = 40;
                                            return 0;
                                        })
                                )
                                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("default")
                                        .executes(context -> {
                                            Config c = Config.getInstance();
                                            c.minDistance = 8;
                                            c.blocksPerLevel = 24;
                                            c.blocksPerLevelDistanceCulled = 8;
                                            c.blocksPerLevelViewportCulled = 20;
                                            c.blocksPerLevelOcclusionCulled = 12;
                                            c.maxLevel = 20;
                                            return 0;
                                        })
                                )
                                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("quality")
                                        .executes(context -> {
                                            Config c = Config.getInstance();
                                            c.minDistance = 12;
                                            c.blocksPerLevel = 30;
                                            c.blocksPerLevelDistanceCulled = 10;
                                            c.blocksPerLevelViewportCulled = 25;
                                            c.blocksPerLevelOcclusionCulled = 15;
                                            c.maxLevel = 10;
                                            return 0;
                                        })
                                )
                        )
                        .then(toggle("enableEntities", enabled -> Config.getInstance().enableEntities = enabled))
                        .then(toggle("enableBlockEntities", enabled -> Config.getInstance().enableBlockEntities = enabled))
                        .then(toggle("enableDistanceCulling", enabled -> Config.getInstance().enableDistanceCulling = enabled))
                        .then(toggle("enableOcclusionCulling", enabled -> Config.getInstance().enableOcclusionCulling = enabled))
                        .then(toggle("enableViewportCulling", enabled -> Config.getInstance().enableViewportCulling = enabled))
                        .then(toggle("syncWithIntegratedServer", enabled -> Config.getInstance().syncWithIntegratedServer = enabled))
                        .then(toggle("enableBudget", enabled -> Config.getInstance().entityTickBudgetServer = enabled ? (new Config()).entityTickBudgetServer : 0))
                        .then(toggle("enabledStress", enabled -> Config.getInstance().stressedThreshold = enabled ? (new Config()).stressedThreshold : 0))
                )
        );
    }

    private static @NotNull String formatProfilerData(EntityProfiler.EntityData data) {
        return "%s %d * %2.1fns = %2.1fms, %2.1fms max\n".formatted(
                data.name,
                data.getSamples(),
                data.getAverage(),
                data.getTime() / 1_000_000.0,
                data.max / 1_000_000.0
        );
    }

    private static ArgumentBuilder<CommandSourceStack, ?> toggle(String name, Consumer<Boolean> consumer) {
        return LiteralArgumentBuilder.<CommandSourceStack>literal(name)
                .then(RequiredArgumentBuilder.<CommandSourceStack, Boolean>argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {
                            consumer.accept(BoolArgumentType.getBool(context, "enabled"));
                            TickScheduler.INSTANCE.reset();
                            Config.getInstance().save();
                            send(context, "Config updated!");
                            return 0;
                        })
                );
    }

    private static void send(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().sendSuccess(() -> Component.literal(message), true);
    }
}
