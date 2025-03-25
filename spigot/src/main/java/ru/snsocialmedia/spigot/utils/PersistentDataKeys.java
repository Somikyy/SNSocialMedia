package ru.snsocialmedia.spigot.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Ключи для персистентных данных
 */
public class PersistentDataKeys {

    // Ключ для текущей гильдии игрока
    public static NamespacedKey CURRENT_GUILD;

    /**
     * Инициализирует ключи с помощью плагина
     * 
     * @param plugin Экземпляр плагина
     */
    public static void init(Plugin plugin) {
        CURRENT_GUILD = new NamespacedKey(plugin, "current_guild");
    }
}