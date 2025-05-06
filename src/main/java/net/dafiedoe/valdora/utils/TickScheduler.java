package net.dafiedoe.valdora.utils;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TickScheduler {
    private static final Queue<Runnable> NEXT_TICK_TASKS = new ConcurrentLinkedQueue<>();
    private static final Map<Integer, Queue<Runnable>> FUTURE_TICK_TASKS = new ConcurrentHashMap<>();

    static {
        ServerTickEvents.END_SERVER_TICK.register(TickScheduler::onEndServerTick);
    }

    public static void runNextTick(Runnable task) {
        NEXT_TICK_TASKS.add(task);
    }

    public static void runNextTick(int ticksLater, Runnable task) {
        if (ticksLater <= 1) {
            runNextTick(task);
        } else {
            FUTURE_TICK_TASKS
                    .computeIfAbsent(ticksLater, k -> new ConcurrentLinkedQueue<>())
                    .add(task);
        }
    }

    private static void onEndServerTick(MinecraftServer server) {
        Runnable task;
        while ((task = NEXT_TICK_TASKS.poll()) != null) {
            task.run();
        }

        Map<Integer, Queue<Runnable>> updatedMap = new ConcurrentHashMap<>();
        FUTURE_TICK_TASKS.forEach((ticksLeft, tasks) -> {
            int newTicksLeft = ticksLeft - 1;
            if (newTicksLeft <= 1) {
                NEXT_TICK_TASKS.addAll(tasks);
            } else {
                updatedMap.computeIfAbsent(newTicksLeft, k -> new ConcurrentLinkedQueue<>()).addAll(tasks);
            }
        });
        FUTURE_TICK_TASKS.clear();
        FUTURE_TICK_TASKS.putAll(updatedMap);
    }
}
