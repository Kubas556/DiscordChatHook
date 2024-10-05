package dev.codeways.discordchathook;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MessageHandler implements Listener {

    private final HttpClient _client = HttpClient.newHttpClient();
    private final DiscordChatHook _plugin;
    MessageHandler(DiscordChatHook plugin) {
        _plugin = plugin;
    }

    private void SendDiscordMessage(String msg, Player player) {
        String url = _plugin.getConfig().getString("url");
        boolean debug = _plugin.getConfig().getBoolean("debug");

        if(url == null || url.isEmpty()) return;

        if(debug) {
            _plugin.getLogger().info("sending " + msg);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\n" +
                        "\t\"content\": \""+escape(msg)+"\",\n" +
                        "\t\"username\": \""+player.getName()+"\",\n" +
                        "\t\"avatar_url\": \"https://mc-heads.net/avatar/"+player.getUniqueId()+"\"\n" +
                        "}"))
                .build();

        try {
            HttpResponse<String> response = _client.send(request, HttpResponse.BodyHandlers.ofString());
            if(debug) {
                _plugin.getLogger().info(response.statusCode() + " " + response.body());
            }
        } catch (InterruptedException | IOException e) {
            _plugin.getLogger().warning("failed to send message to discord hook");
        }
    }

    private String escape(String text) {

        StringBuilder builder = new StringBuilder();
        for (char c: text.toCharArray()) {
            if (c == '"') {
                builder.append("\\\"");
                continue;
            }

            if(c == '\\') {
                builder.append("\\\\");
                continue;
            }

            if((int)c <= 0x1f) {
                builder.append(String.format("\\u00%02x", (int)c));
                continue;
            }

            builder.append(c);
        }

        return builder.toString();
    }

    @EventHandler
    public void OnPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        SendDiscordMessage(event.getMessage(), player);
    }
}
