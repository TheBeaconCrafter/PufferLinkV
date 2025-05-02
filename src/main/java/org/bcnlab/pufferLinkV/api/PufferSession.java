// CODE TAKEN 1-1 from PufferSession.java in PufferLink (BungeeCord)

package org.bcnlab.pufferLinkV.api;

import com.google.gson.JsonObject;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.util.Arrays;

public class PufferSession {

    private final String email;
    private final String password;
    private final String host;

    private String sessionCookie;

    public PufferSession(String email, String password, String host) {
        this.email = email;
        this.password = password;
        this.host = host;
    }

    public boolean login() {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(host + "/auth/login");

            JsonObject credentials = new JsonObject();
            credentials.addProperty("email", email);
            credentials.addProperty("password", password);

            post.setEntity(new StringEntity(credentials.toString(), ContentType.APPLICATION_JSON));
            post.setHeader("Accept", "application/json");

            return client.execute(post, response -> {
                if (response.getCode() != 200) {
                    return false;
                }

                Header[] headers = response.getHeaders("Set-Cookie");
                sessionCookie = Arrays.stream(headers)
                        .map(Header::getValue)
                        .map(val -> val.split(";")[0])
                        .filter(s -> s.startsWith("puffer_auth="))
                        .findFirst()
                        .orElse(null);

                return sessionCookie != null;
            });

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getSessionCookie() {
        return sessionCookie;
    }

    public void setSessionCookie(String cookie) {
        this.sessionCookie = cookie;
    }

    public boolean reauth() {
        return login();
    }
}
