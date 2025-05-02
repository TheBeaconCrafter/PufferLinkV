package org.bcnlab.pufferLinkV.monitor;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import org.bcnlab.pufferLinkV.PufferLinkV;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ServerMonitor {
    private final PufferLinkV plugin;
    private final ProxyServer proxy;
    private final Map<String, Boolean> serverStatus = new ConcurrentHashMap<>();
    private ScheduledTask task;
    private final String permission = "pufferlink.notify";

    @Inject
    public ServerMonitor(PufferLinkV plugin, ProxyServer proxy) {
        this.plugin = plugin;
        this.proxy = proxy;
    }

    public void start() {
        proxy.getAllServers().forEach(server -> {
            serverStatus.put(server.getServerInfo().getName(), false); // assume offline initially
        });

        task = proxy.getScheduler().buildTask(plugin, this::checkServers)
                .repeat(10, TimeUnit.SECONDS)
                .schedule();
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    private void checkServers() {
        for (RegisteredServer server : proxy.getAllServers()) {
            server.ping().whenComplete((result, error) -> {
                boolean isOnline = error == null && result != null;
                String name = server.getServerInfo().getName();
                boolean wasOnline = serverStatus.getOrDefault(name, false);

                if (isOnline != wasOnline) {
                    serverStatus.put(name, isOnline);

                    Component msg = Component.text()
                            .append(plugin.getPrefix())
                            .append(Component.text("[", net.kyori.adventure.text.format.NamedTextColor.GRAY))
                            .append(Component.text(isOnline ? "+" : "-", isOnline ? net.kyori.adventure.text.format.NamedTextColor.GREEN : net.kyori.adventure.text.format.NamedTextColor.RED))
                            .append(Component.text("] ", net.kyori.adventure.text.format.NamedTextColor.GRAY))
                            .append(Component.text("Server ", net.kyori.adventure.text.format.NamedTextColor.RED))
                            .append(Component.text(name, net.kyori.adventure.text.format.NamedTextColor.GOLD))
                            .append(Component.text(isOnline ? " has connected to" : " has disconnected from", net.kyori.adventure.text.format.NamedTextColor.RED))
                            .append(Component.text(" the proxy.", net.kyori.adventure.text.format.NamedTextColor.RED))
                            .build();

                    proxy.getConsoleCommandSource().sendMessage(msg);
                    proxy.getAllPlayers().stream()
                            .filter(p -> p.hasPermission(permission))
                            .forEach(p -> p.sendMessage(msg));
                }
            });
        }
    }
}
