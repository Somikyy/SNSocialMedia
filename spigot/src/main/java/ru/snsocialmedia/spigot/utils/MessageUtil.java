package ru.snsocialmedia.spigot.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Утилиты для отправки сообщений игрокам
 */
public class MessageUtil {

    private static final String PREFIX = "§7[§6SN§7] §r";

    /**
     * Отправляет сообщение игроку
     *
     * @param player  Игрок
     * @param message Сообщение
     */
    public static void sendMessage(Player player, String message) {
        player.sendMessage(PREFIX + message);
    }

    /**
     * Отправляет сообщение с префиксом гильдии
     *
     * @param player    Игрок
     * @param guildName Название гильдии
     * @param message   Сообщение
     */
    public static void sendGuildMessage(Player player, String guildName, String message) {
        player.sendMessage("§7[§6Гильдия: §e" + guildName + "§7] §r" + message);
    }

    /**
     * Отправляет сообщение в чат гильдии
     *
     * @param player     Игрок
     * @param senderName Имя отправителя
     * @param message    Сообщение
     */
    public static void sendGuildChatMessage(Player player, String senderName, String message) {
        player.sendMessage("§7[§aГЧат§7] §e" + senderName + "§7: §f" + message);
    }

    /**
     * Отправляет сообщение об ошибке игроку
     *
     * @param player  Игрок
     * @param message Сообщение
     */
    public static void sendErrorMessage(Player player, String message) {
        player.sendMessage(PREFIX + "§c" + message);
    }

    /**
     * Отправляет сообщение об успешном действии игроку
     *
     * @param player  Игрок
     * @param message Сообщение
     */
    public static void sendSuccessMessage(Player player, String message) {
        player.sendMessage(PREFIX + "§a" + message);
    }

    /**
     * Отправляет информационное сообщение игроку
     *
     * @param player  Игрок
     * @param message Сообщение
     */
    public static void sendInfoMessage(Player player, String message) {
        player.sendMessage(PREFIX + "§e" + message);
    }

    /**
     * Отправляет предупреждение игроку
     *
     * @param player  Игрок
     * @param message Сообщение
     */
    public static void sendWarningMessage(Player player, String message) {
        player.sendMessage(PREFIX + "§6" + message);
    }

    /**
     * Применяет цветовые коды к строке
     *
     * @param message Сообщение
     * @return Сообщение с примененными цветовыми кодами
     */
    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}