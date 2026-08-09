package org.bcnlab.pufferLinkV.command;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.Player;
import org.bcnlab.pufferLinkV.api.PufferClient;
import org.bcnlab.pufferLinkV.PufferLinkV;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

public class CloudGuiListener {

    private final PufferClient client;
    private final PufferLinkV plugin;
    public static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from("beaconlabs:cloud_gui_action");

    public CloudGuiListener(PufferClient client, PufferLinkV plugin) {
        this.client = client;
        this.plugin = plugin;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL)) return;
        
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            String uuidStr = in.readUTF();
            String action = in.readUTF();
            String serverId = in.readUTF();

            Player player = plugin.getProxy().getPlayer(java.util.UUID.fromString(uuidStr)).orElse(null);

            if (player == null || !player.hasPermission("pufferlink.use")) {
                return;
            }

            if ("START".equalsIgnoreCase(action)) {
                client.startServer(serverId, success -> {
                    player.sendMessage(plugin.getPrefix().append(
                            Component.text(success ? "Starting server..." : "Failed to start server.",
                                    success ? NamedTextColor.GREEN : NamedTextColor.RED)));
                });
            } else if ("STOP".equalsIgnoreCase(action)) {
                client.stopServer(serverId, success -> {
                    player.sendMessage(plugin.getPrefix().append(
                            Component.text(success ? "Stopping server..." : "Failed to stop server.",
                                    success ? NamedTextColor.GREEN : NamedTextColor.RED)));
                });
            } else if ("RESTART".equalsIgnoreCase(action)) {
                client.restartServer(
                        serverId,
                        success -> player.sendMessage(plugin.getPrefix().append(
                                Component.text(success ? "Restarting server..." : "Failed to restart server.",
                                        success ? NamedTextColor.GREEN : NamedTextColor.RED))),
                        message -> player.sendMessage(plugin.getPrefix().append(Component.text(message))));
            } else if ("INFO".equalsIgnoreCase(action)) {
                String serverName = in.available() > 0 ? in.readUTF() : serverId;
                client.getServerStatus(serverId, status -> {
                    try {
                        java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
                        java.io.DataOutputStream out = new java.io.DataOutputStream(b);
                        out.writeUTF(serverId);
                        out.writeUTF(serverName);
                        if (status != null) {
                            out.writeBoolean(status.get("running").getAsBoolean());
                            out.writeBoolean(status.get("installing").getAsBoolean());
                        } else {
                            out.writeBoolean(false);
                            out.writeBoolean(false);
                        }
                        com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier identifier = com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier.from("beaconlabs:cloud_gui_submenu");
                        player.getCurrentServer().ifPresent(s -> s.sendPluginMessage(identifier, b.toByteArray()));
                    } catch (Exception e) {}
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
