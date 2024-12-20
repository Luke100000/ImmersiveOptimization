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
                                    sb.append(" %s: Rate: %.1f%%, %d stress, %d budgeted\n".formatted(
                                            new ResourceLocation(key).getPath(),
                                            data.averageSmoothedTickRate * 100,
                                            data.lifeTimeStressedTicks,
                                            data.lifeTimeBudgetTicks
                                    )));
                            send(context, sb.toString());
                            return 0;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("profiler")
                        .executes(context -> {
                            send(context, "A table of the top 10 most expensive entities will be shown here.");
                            return 0;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("config")
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("preset")
                                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("performance")
                                        .executes(context -> {
                                            Config c = Config.getInstance();
                                            c.minDistance = 10;
                                            c.blocksPerLevel = 16;
                                            c.blocksPerLevelViewportCulled = 12;
                                            c.blocksPerLevelOcclusionCulled = 8;
                                            c.maxLevel = 40;
                                            return 0;
                                        })
                                )
                                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("default")
                                        .executes(context -> {
                                            Config c = Config.getInstance();
                                            c.minDistance = 16;
                                            c.blocksPerLevel = 24;
                                            c.blocksPerLevelViewportCulled = 16;
                                            c.blocksPerLevelOcclusionCulled = 12;
                                            c.maxLevel = 20;
                                            return 0;
                                        })
                                )
                                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("quality")
                                        .executes(context -> {
                                            Config c = Config.getInstance();
                                            c.minDistance = 24;
                                            c.blocksPerLevel = 24;
                                            c.blocksPerLevelViewportCulled = 20;
                                            c.blocksPerLevelOcclusionCulled = 16;
                                            c.maxLevel = 10;
                                            return 0;
                                        })
                                )
                        )
                        .then(toggle("enableEntities", enabled -> Config.getInstance().enableEntities = enabled))
                        .then(toggle("enableBlockEntities", enabled -> Config.getInstance().enableBlockEntities = enabled))
                        .then(toggle("enableOcclusionCulling", enabled -> Config.getInstance().enableOcclusionCulling = enabled))
                        .then(toggle("enableViewportCulling", enabled -> Config.getInstance().enableViewportCulling = enabled))
                        .then(toggle("syncWithIntegratedServer", enabled -> Config.getInstance().syncWithIntegratedServer = enabled))
                        .then(toggle("enableBudget", enabled -> Config.getInstance().entityTickBudgetServer = enabled ? (new Config()).entityTickBudgetServer : 0))
                        .then(toggle("enabledStress", enabled -> Config.getInstance().stressedThreshold = enabled ? (new Config()).stressedThreshold : 0))
                )
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
