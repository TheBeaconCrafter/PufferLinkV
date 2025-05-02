package org.bcnlab.pufferLinkV;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bcnlab.pufferLinkV.api.PufferClient;
import org.bcnlab.pufferLinkV.api.PufferSession;
import org.bcnlab.pufferLinkV.command.CloudCommand;
import org.bcnlab.pufferLinkV.monitor.ServerMonitor;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Plugin(id = "pufferlinkv", name = "PufferLinkV", version = "1.0", description = "Port of PufferLink to Velocity", url = "bcnlab.org", authors = {"Vincent Wackler"})
public class PufferLinkV {
    @Inject
    @DataDirectory
    private Path dataDirectory;

    private String apiUrl;
    private String email;
    private String password;
    private boolean enableMonitor = true;

    private ConfigurationNode config;

    private String prefix;
    private final String version = "1.0";

    @Inject
    private Logger logger;

    @Inject
    private ProxyServer proxy;

    private ServerMonitor serverMonitor;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("PufferLinkV v" + version + " initializing...");
        Path configFile = dataDirectory.resolve("config.yml");

        try {
            if (!Files.exists(configFile)) {
                Files.createDirectories(dataDirectory);
                Files.copy(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("config.yml")), configFile);
            }

            ConfigurationLoader<?> loader = YamlConfigurationLoader.builder()
                    .path(configFile)
                    .build();

            config = loader.load();

            // Read values from config
            prefix = config.node("prefix").getString("&3Cloud &8» &r");
            apiUrl = config.node("api-url").getString("");
            email = config.node("email").getString("");
            password = config.node("password").getString("");
            enableMonitor = config.node("enable-monitor").getBoolean(true);

            if (apiUrl.isEmpty() || email.isEmpty() || password.isEmpty()) {
                logger.warn("Missing config values! Please fill in 'api-url', 'email', and 'password'.");
                return;
            }

            PufferSession session = new PufferSession(email, password, apiUrl);
            if (!session.login()) {
                logger.error("Login to PufferPanel failed!");
                return;
            }

            PufferClient client = new PufferClient(apiUrl, session, this);
            CloudCommand cloudCommand = new CloudCommand(this, client, proxy);
            proxy.getCommandManager().register("cloud", cloudCommand);
            proxy.getCommandManager().register("puffer", cloudCommand);

            if (enableMonitor) {
                serverMonitor = new ServerMonitor(this, proxy);
                serverMonitor.start();
                logger.info("Server monitor started.");
            }

            logger.info("Logged into PufferPanel!");
            client.printAllServers();
            
            logger.info("PufferLinkV v" + version + " has been successfully initialized!");

        } catch (IOException e) {
            logger.error("Failed to load config!", e);
            prefix = "&4ConfigError &8» ";
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    public Component getPrefix() {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(prefix);
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public boolean isMonitorEnabled() {
        return enableMonitor;
    }

    public String getVersion() {
        return version;
    }
}
