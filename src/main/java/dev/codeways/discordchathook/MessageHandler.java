package dev.codeways.discordchathook;

import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.server.BroadcastMessageEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageHandler implements Listener {

    private final HttpClient _client = HttpClient.newHttpClient();
    private final DiscordChatHook _plugin;
    MessageHandler(DiscordChatHook plugin) {
        _plugin = plugin;
    }

    private void SendDiscordMessage(String msg, Player player) {
        String url = _plugin.getConfig().getString("url");
        if(url == null || url.isEmpty()) return;

        boolean debug = _plugin.getConfig().getBoolean("debug");
        boolean usePlayerName = _plugin.getConfig().getBoolean("usePlayerName");
        boolean useSkinRestorer = _plugin.getConfig().getBoolean("useSkinRestorer");
        String textureUrl = "https://mc-heads.net/avatar/"+(usePlayerName ? clearFormatting(player.getName()) : player.getUniqueId());

        if(useSkinRestorer) {
            textureUrl = SkinRestorerHelper.GetSkinUrl(_plugin, player, textureUrl);
        }

        if(debug) {
            _plugin.getLogger().info("useName: " + usePlayerName + " | avatar: " + textureUrl);
            _plugin.getLogger().info("sending " + msg);
        }

        SendDiscordMessage(url, msg, player.getName(), textureUrl);
    }

    private void SendDiscordMessage(String url, String msg, String name, String textureUrl) {
        boolean debug = _plugin.getConfig().getBoolean("debug");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\n" +
                        "\t\"content\": \""+escape(msg)+"\",\n" +
                        "\t\"username\": \""+clearFormatting(name)+"\",\n" +
                        "\t\"avatar_url\": \""+textureUrl+"\"\n" +
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

    private String clearFormatting(String text) {
        return text.replaceAll("§\\w", "");
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

    @EventHandler
    public void OnChatBroadcast(BroadcastMessageEvent event) {
        String url = _plugin.getConfig().getString("url");
        String broadcastImageUrl = _plugin.getConfig().getString("broadcastImageUrl");
        if(url == null || url.isEmpty()) return;
        SendDiscordMessage(url, clearFormatting(event.getMessage()), "Broadcast", broadcastImageUrl);
    }

    @EventHandler
    public void OnServerCommand(ServerCommandEvent event) {
        if(event.getCommand().startsWith("say")) {
            String url = _plugin.getConfig().getString("url");
            String broadcastImageUrl = _plugin.getConfig().getString("serverImageUrl");
            if (url == null || url.isEmpty()) return;

            SendDiscordMessage(url, clearFormatting(event.getCommand().replaceFirst("say", "")), "Server", broadcastImageUrl);
            _plugin.getLogger().info("command: " + event.getCommand().replaceFirst("say", ""));
        }
    }
}
