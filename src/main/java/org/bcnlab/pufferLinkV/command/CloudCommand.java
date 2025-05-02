package org.bcnlab.pufferLinkV.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import com.velocitypowered.api.proxy.ProxyServer;
import org.bcnlab.pufferLinkV.PufferLinkV;
import org.bcnlab.pufferLinkV.api.PufferClient;
import com.google.gson.JsonObject;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class CloudCommand implements SimpleCommand {
    private final PufferClient client;
    private final PufferLinkV plugin;
    private final ProxyServer proxy;
    private final Map<String, CachedStatus> statusCache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = 10000;

    private static class CachedStatus {
        final String statusIndicator;
        final long timestamp;
        final boolean isRunning;

        CachedStatus(String indicator, boolean running) {
            this.statusIndicator = indicator;
            this.timestamp = System.currentTimeMillis();
            this.isRunning = running;
        }

        boolean isValid() {
            return System.currentTimeMillis() - timestamp < CACHE_DURATION;
        }
    }

    public CloudCommand(PufferLinkV plugin, PufferClient client, ProxyServer proxy) {
        this.client = client;
        this.plugin = plugin;
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            source.sendMessage(
                    plugin.getPrefix()
                            .append(Component.text("PufferLink Version ", NamedTextColor.RED))
                            .append(Component.text(plugin.getVersion() + " ", NamedTextColor.GOLD))
                            .append(Component.text("by ItsBeacon", NamedTextColor.RED))
            );
            source.sendMessage(
                    plugin.getPrefix()
                            .append(Component.text("Usage: /cloud <list|up|status|console|start|stop|restart>", NamedTextColor.GRAY))
            );
            return;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                client.listServers(servers -> processServerList(source, servers, false));
                break;

            case "up":
                client.listServers(servers -> processServerList(source, servers, true));
                break;

            case "status":
                if (args.length < 2) {
                    source.sendMessage(plugin.getPrefix().append(Component.text("Usage: /cloud status <id>", NamedTextColor.RED)));
                    return;
                }
                client.getServerStatus(args[1], status -> {
                    if (status == null) {
                        source.sendMessage(plugin.getPrefix().append(Component.text("Server not found or error occurred.", NamedTextColor.RED)));
                        return;
                    }
                    source.sendMessage(plugin.getPrefix().append(Component.text("Server Status for " + args[1] + ":", NamedTextColor.GREEN)));
                    source.sendMessage(Component.text("Running: " + status.get("running").getAsBoolean(), NamedTextColor.GRAY));
                    source.sendMessage(Component.text("Installing: " + status.get("installing").getAsBoolean(), NamedTextColor.GRAY));
                });
                break;

            case "console":
                if (args.length < 3) {
                    source.sendMessage(plugin.getPrefix().append(Component.text("Usage: /cloud console <id> <command>", NamedTextColor.RED)));
                    return;
                }
                String id = args[1];
                String command = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                client.sendConsoleCommand(id, command, success -> {
                    source.sendMessage(plugin.getPrefix().append(Component.text(success ? "Command sent to console." : "Failed to send command.", success ? NamedTextColor.GREEN : NamedTextColor.RED)));
                });
                break;

            case "start":
            case "stop":
            case "restart":
                if (args.length < 2) {
                    source.sendMessage(plugin.getPrefix().append(Component.text("Usage: /cloud restart <id>", NamedTextColor.RED)));
                    return;
                }
                client.restartServer(
                        args[1],
                        success -> source.sendMessage(plugin.getPrefix().append(Component.text(success ? "Restarting server..." : "Failed to restart server.", success ? NamedTextColor.GREEN : NamedTextColor.RED))),
                        message -> source.sendMessage(plugin.getPrefix().append(Component.text(message)))
                );
                break;

            default:
                source.sendMessage(plugin.getPrefix().append(Component.text("Unknown subcommand. Use list, up, status, console, start, stop, or restart", NamedTextColor.RED)));
                break;
        }
    }

    private void processServerList(CommandSource source, List<JsonObject> servers, boolean onlyRunning) {
        if (servers.isEmpty()) {
            source.sendMessage(plugin.getPrefix().append(Component.text("No servers found.", NamedTextColor.RED)));
            return;
        }

        source.sendMessage(plugin.getPrefix().append(Component.text("Available Servers:", NamedTextColor.GREEN)));

        for (JsonObject server : servers) {
            String id = server.get("id").getAsString();
            String name = server.get("name").getAsString();
            client.getServerStatus(id, status -> {
                boolean running = status != null && status.get("running").getAsBoolean();
                boolean installing = status != null && status.get("installing").getAsBoolean();
                if (onlyRunning && !running) return;

                String indicator = installing ? "§eI" : running ? "§aU" : "§cD";
                String line = "→ " + indicator + " " + name + " (ID: " + id + ")";
                source.sendMessage(Component.text(line));
            });
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("pufferlink.use");
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length == 1) {
            return Arrays.asList("list", "up", "status", "console", "start", "stop", "restart");
        }
        return Collections.emptyList();
    }
}