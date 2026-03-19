package dev.codeways.discordchathook;

import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkinRestorerHelper {
    private static final Pattern skinIdPattern = Pattern.compile("\\/(\\w+)$");

    public static String GetSkinUrl(Plugin plugin, Player player, String defaultUrl) {
        boolean debug = plugin.getConfig().getBoolean("debug");
        String textureUrl = defaultUrl;
        try {
            SkinsRestorer skinsRestorerAPI = SkinsRestorerProvider.get();

            PlayerStorage playerStorage = skinsRestorerAPI.getPlayerStorage();
            try {
                Optional<SkinProperty> property = playerStorage.getSkinForPlayer(player.getUniqueId(), player.getName());

                if (property.isPresent()) {
                    if (debug) {
                        plugin.getLogger().info(property.get().getValue());
                    }
                    String skinUrl = PropertyUtils.getSkinTextureUrl(property.get());
                    if (debug) {
                        plugin.getLogger().info(skinUrl);
                    }
                    if (!skinUrl.isEmpty()) {
                        Matcher matcher = skinIdPattern.matcher(skinUrl);
                        if (matcher.find()) {
                            String match = matcher.group(1);
                            if (debug) {
                                plugin.getLogger().info("match: " + match);
                            }
                            textureUrl = "https://mc-heads.net/avatar/" + match;
                        }
                    }
                }
            } catch (DataRequestException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return textureUrl;
    }
}
