package dev.codeways.discordchathook;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.BroadcastMessageEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;

public class MessageHandler implements Listener {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient _client = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
    private final DiscordChatHook _plugin;

    private final String _postUrl;
    private final boolean _debug;

    MessageHandler(DiscordChatHook plugin) {
        _plugin = plugin;
        _postUrl = _plugin.getConfig().getString("url");
        _debug = _plugin.getConfig().getBoolean("debug");
    }

    private void scheduleDiscordSend(Runnable sendTask) {
        _plugin.getServer().getScheduler().runTaskAsynchronously(_plugin, () -> {
            try {
                sendTask.run();
            } catch (Exception e) {
                _plugin.getLogger().log(Level.WARNING, "Failed to send message to Discord webhook", e);
            }
        });
    }

    private void SendDiscordMessage(String msg, Player player) {
        if (_postUrl == null || _postUrl.isEmpty() || msg == null) return;

        boolean usePlayerName = _plugin.getConfig().getBoolean("usePlayerName");
        boolean useSkinRestorer = _plugin.getConfig().getBoolean("useSkinRestorer");
        String playerName = player.getName();
        String textureUrl = "https://mc-heads.net/avatar/" + (usePlayerName ? clearFormatting(playerName) : player.getUniqueId());

        if (useSkinRestorer) {
            textureUrl = SkinRestorerHelper.GetSkinUrl(_plugin, player, textureUrl);
        }

        if (_debug) {
            _plugin.getLogger().info("useName: " + usePlayerName + " | avatar: " + textureUrl);
            _plugin.getLogger().info("sending " + msg);
        }

        SendDiscordMessage(_postUrl, msg, playerName, textureUrl);
    }

    private void SendDiscordMessage(String url, String msg, String name, String textureUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\n" +
                            "\t\"content\": \"" + escape(msg) + "\",\n" +
                            "\t\"username\": \"" + clearFormatting(name) + "\",\n" +
                            "\t\"avatar_url\": \"" + textureUrl + "\"\n" +
                            "}"))
                    .build();

            HttpResponse<String> response = _client.send(request, HttpResponse.BodyHandlers.ofString());
            if (_debug) {
                _plugin.getLogger().info(response.statusCode() + " " + response.body());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            _plugin.getLogger().log(Level.WARNING, "Discord webhook request interrupted", e);
        } catch (IOException e) {
            _plugin.getLogger().log(Level.WARNING, "Failed to send message to Discord webhook (network unavailable?)", e);
        } catch (Exception e) {
            _plugin.getLogger().log(Level.WARNING, "Failed to send message to Discord webhook", e);
        }
    }

    private String clearFormatting(String text) {
        if (text == null) return "";
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
        String message = event.getMessage();
        if (message == null) return;

        Player player = event.getPlayer();
        scheduleDiscordSend(() -> SendDiscordMessage(message, player));
    }

    @EventHandler
    public void OnPlayerJoin(PlayerJoinEvent event) {
        if (_postUrl == null || _postUrl.isEmpty()) return;

        String joinImageUrl = _plugin.getConfig().getString("joinImageUrl");
        Player player = event.getPlayer();
        String playerName = clearFormatting(player.getName());
        scheduleDiscordSend(() -> SendDiscordMessage(_postUrl, "**Joined the server!**", playerName, joinImageUrl));
    }

    @EventHandler
    public void OnPlayerQuit(PlayerQuitEvent event) {
        if (_postUrl == null || _postUrl.isEmpty()) return;

        String leaveImageUrl = _plugin.getConfig().getString("leaveImageUrl");
        Player player = event.getPlayer();
        String playerName = clearFormatting(player.getName());
        scheduleDiscordSend(() -> SendDiscordMessage(_postUrl, "**Left the server!**", playerName, leaveImageUrl));
    }

    @EventHandler
    public void OnChatBroadcast(BroadcastMessageEvent event) {
        if (_postUrl == null || _postUrl.isEmpty()) return;

        String broadcastImageUrl = _plugin.getConfig().getString("broadcastImageUrl");
        String message = event.getMessage();
        if (message == null) return;

        String formattedMessage = clearFormatting(message);
        scheduleDiscordSend(() -> SendDiscordMessage(_postUrl, formattedMessage, "Broadcast", broadcastImageUrl));
    }

    @EventHandler
    public void OnServerCommand(ServerCommandEvent event) {
        if (!event.getCommand().startsWith("say")) return;
        if (_postUrl == null || _postUrl.isEmpty()) return;

        String broadcastImageUrl = _plugin.getConfig().getString("serverImageUrl");
        String formattedMessage = clearFormatting(event.getCommand().replaceFirst("say ", ""));
        scheduleDiscordSend(() -> SendDiscordMessage(_postUrl, formattedMessage, "Server", broadcastImageUrl));

        if (_debug) {
            _plugin.getLogger().info("command: " + formattedMessage);
        }
    }
}
