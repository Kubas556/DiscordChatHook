package dev.codeways.discordchathook;

import org.bukkit.plugin.java.JavaPlugin;

public final class DiscordChatHook extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("Loaded!");
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new MessageHandler(this), this);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
