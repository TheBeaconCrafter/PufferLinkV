// CODE TAKEN 1-1 from PufferClient.java in PufferLink (BungeeCord)
// What changed?
// plugin.getLogger().warning changed to .warn

package org.bcnlab.pufferLinkV.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.bcnlab.pufferLinkV.PufferLinkV;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PufferClient {
    private final String host;
    private final PufferLinkV plugin;
    private final PufferSession session;
    private String sessionCookie;

    public PufferClient(String host, String sessionCookie, PufferLinkV plugin) {
        this.host = host;
        this.sessionCookie = sessionCookie;
        this.plugin = plugin;
        this.session = null;
    }

    public PufferClient(String host, PufferSession session, PufferLinkV plugin) {
        this.host = host;
        this.session = session;
        this.sessionCookie = session.getSessionCookie();
        this.plugin = plugin;
    }

    private boolean handleAuthFailure() {
        if (session == null) {
            return false;
        }

        if (session.reauth()) {
            this.sessionCookie = session.getSessionCookie();
            return true;
        }
        return false;
    }

    private CloseableHttpResponse executeWithRetry(CloseableHttpClient client, HttpGet request) throws Exception {
        CloseableHttpResponse response = client.execute(request);
        if (response.getCode() == 401 && handleAuthFailure()) {
            // Update the auth cookie and retry
            request.setHeader("Cookie", sessionCookie);
            response.close();
            return client.execute(request);
        }
        return response;
    }

    private CloseableHttpResponse executeWithRetry(CloseableHttpClient client, HttpPost request) throws Exception {
        CloseableHttpResponse response = client.execute(request);
        if (response.getCode() == 401 && handleAuthFailure()) {
            // Update the auth cookie and retry
            request.setHeader("Cookie", sessionCookie);
            response.close();
            return client.execute(request);
        }
        return response;
    }

    public void printAllServers() {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(host + "/api/servers");
            get.setHeader("Accept", "application/json");
            get.setHeader("Cookie", sessionCookie);

            client.execute(get, response -> {
                if (response.getCode() != 200) {
                    plugin.getLogger().warn("Failed to get servers, code: " + response.getCode());
                    return null;
                }

                JsonObject responseJson = JsonParser.parseReader(new InputStreamReader(response.getEntity().getContent())).getAsJsonObject();
                JsonArray servers = responseJson.getAsJsonArray("servers");

                if (servers == null || servers.size() == 0) {
                    plugin.getLogger().warn("No servers found.");
                } else {
                    System.out.println("Found " + servers.size() + " servers:");
                    for (JsonElement el : servers) {
                        JsonObject server = el.getAsJsonObject();
                        System.out.println("- " + server.get("name").getAsString());
                    }
                }

                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // List all servers
    public void listServers(Consumer<List<JsonObject>> callback) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(host + "/api/servers");
            get.setHeader("Accept", "application/json");
            get.setHeader("Cookie", sessionCookie);

            CloseableHttpResponse response = executeWithRetry(client, get);
            if (response.getCode() != 200) {
                plugin.getLogger().warn("Failed to get servers, code: " + response.getCode());
                callback.accept(Collections.emptyList());
                return;
            }

            JsonObject responseJson = JsonParser.parseReader(new InputStreamReader(response.getEntity().getContent())).getAsJsonObject();
            JsonArray servers = responseJson.getAsJsonArray("servers");

            List<JsonObject> serverList = new ArrayList<>();
            if (servers != null) {
                for (JsonElement el : servers) {
                    serverList.add(el.getAsJsonObject());
                }
            }
            callback.accept(serverList);
        } catch (Exception e) {
            e.printStackTrace();
            callback.accept(Collections.emptyList());
        }
    }

    // Get the status of a server
    public void getServerStatus(String id, Consumer<JsonObject> callback) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(host + "/api/servers/" + id + "/status");
            get.setHeader("Accept", "application/json");
            get.setHeader("Cookie", sessionCookie);

            CloseableHttpResponse response = executeWithRetry(client, get);
            if (response.getCode() != 200) {
                plugin.getLogger().warn("Failed to get server status, code: " + response.getCode());
                callback.accept(null);
                return;
            }

            JsonObject statusJson = JsonParser.parseReader(new InputStreamReader(response.getEntity().getContent())).getAsJsonObject();
            callback.accept(statusJson);
        } catch (Exception e) {
            e.printStackTrace();
            callback.accept(null);
        }
    }

    // Send a command to the server console
    public void sendConsoleCommand(String id, String command, Consumer<Boolean> callback) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(host + "/api/servers/" + id + "/console");
            post.setHeader("Accept", "application/json");
            post.setHeader("Cookie", sessionCookie);
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");
            StringEntity entity = new StringEntity(command);
            post.setEntity(entity);

            CloseableHttpResponse response = executeWithRetry(client, post);
            int statusCode = response.getCode();
            callback.accept(statusCode == 204 || statusCode == 200);
        } catch (Exception e) {
            e.printStackTrace();
            callback.accept(false);
        }
    }

    // Start a server
    public void startServer(String id, Consumer<Boolean> callback) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(host + "/api/servers/" + id + "/start");
            post.setHeader("Accept", "application/json");
            post.setHeader("Cookie", sessionCookie);

            CloseableHttpResponse response = executeWithRetry(client, post);
            int statusCode = response.getCode();
            callback.accept(statusCode == 204 || statusCode == 200 || statusCode == 202);
        } catch (Exception e) {
            e.printStackTrace();
            callback.accept(false);
        }
    }

    // Stop a server
    public void stopServer(String id, Consumer<Boolean> callback) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(host + "/api/servers/" + id + "/stop");
            post.setHeader("Accept", "application/json");
            post.setHeader("Cookie", sessionCookie);

            CloseableHttpResponse response = executeWithRetry(client, post);
            int statusCode = response.getCode();
            callback.accept(statusCode == 204 || statusCode == 200 || statusCode == 202);
        } catch (Exception e) {
            e.printStackTrace();
            callback.accept(false);
        }
    }    // Restart a server
    public void restartServer(String id, Consumer<Boolean> callback, Consumer<String> messageCallback) {
        stopServer(id, stopSuccess -> {
            if (!stopSuccess) {
                String message = "Failed to stop server for restart";
                plugin.getLogger().warn(message);
                messageCallback.accept("§c" + message);
                callback.accept(false);
                return;
            }

            String message = "Server stopped, waiting for offline status...";
            plugin.getLogger().info(message);
            messageCallback.accept("§e" + message);
            checkStatusAndStart(id, callback, messageCallback, 0);
        });
    }

    private void checkStatusAndStart(String id, Consumer<Boolean> callback, Consumer<String> messageCallback, int attempts) {
        if (attempts >= 6) {
            String message = "Server failed to go offline after maximum attempts";
            plugin.getLogger().warn(message);
            messageCallback.accept("§c" + message);
            callback.accept(false);
            return;
        }

        getServerStatus(id, status -> {
            if (status == null) {
                String message = "Failed to get server status";
                plugin.getLogger().warn(message);
                messageCallback.accept("§c" + message);
                callback.accept(false);
                return;
            }

            boolean isRunning = status.get("running").getAsBoolean();
            if (!isRunning) {
                String message = "Server is offline, starting it up...";
                plugin.getLogger().info(message);
                messageCallback.accept("§a" + message);
                startServer(id, success -> {
                    if (success) {
                        messageCallback.accept("§aServer restart completed successfully!");
                    } else {
                        messageCallback.accept("§cFailed to start the server");
                    }
                    callback.accept(success);
                });
            } else {
                /*
                    * This logic has been modified from the original PufferLink (BungeeCord) code.
                 */
                String message = "Server is still running, waiting 5 seconds... (Attempt " + (attempts + 1) + "/6)";
                plugin.getLogger().info(message);
                messageCallback.accept("§e" + message);
                plugin.getProxy().getScheduler().buildTask(plugin, () -> {
                    checkStatusAndStart(id, callback, messageCallback, attempts + 1);
                }).delay(5, TimeUnit.SECONDS).schedule();
            }
        });
    }
}
