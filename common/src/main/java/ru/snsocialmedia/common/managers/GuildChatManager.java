package ru.snsocialmedia.common.managers;

import ru.snsocialmedia.common.database.DatabaseManager;
import ru.snsocialmedia.common.models.guild.Guild;
import ru.snsocialmedia.common.models.guild.GuildRole;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Менеджер для управления чатом гильдии
 */
public class GuildChatManager {
    private final DatabaseManager dbManager;
    private final GuildManager guildManager;
    private final Logger logger;

    // Карта для хранения состояния режима чата гильдии для каждого игрока
    private final Map<UUID, Boolean> guildChatEnabled = new ConcurrentHashMap<>();

    public GuildChatManager(DatabaseManager dbManager, GuildManager guildManager, Logger logger) {
        this.dbManager = dbManager;
        this.guildManager = guildManager;
        this.logger = logger;
    }

    /**
     * Переключает режим чата гильдии для игрока
     * 
     * @param playerId UUID игрока
     * @return новое состояние режима чата (true - включен, false - выключен)
     */
    public boolean toggleGuildChat(UUID playerId) {
        boolean newState = !isGuildChatEnabled(playerId);
        guildChatEnabled.put(playerId, newState);
        logger.info("Игрок " + playerId + " " + (newState ? "включил" : "выключил") + " режим чата гильдии");
        return newState;
    }

    /**
     * Проверяет, включен ли режим чата гильдии у игрока
     * 
     * @param playerId UUID игрока
     * @return true, если режим чата гильдии включен
     */
    public boolean isGuildChatEnabled(UUID playerId) {
        return guildChatEnabled.getOrDefault(playerId, false);
    }

    /**
     * Отправляет сообщение в чат гильдии
     * 
     * @param guildId    ID гильдии
     * @param senderName Имя отправителя
     * @param senderId   UUID отправителя
     * @param message    Текст сообщения
     * @return true, если сообщение успешно отправлено
     */
    public boolean sendGuildChatMessage(UUID guildId, String senderName, UUID senderId, String message) {
        if (guildId == null) {
            logger.warning("Попытка отправить сообщение в чат несуществующей гильдии: " + senderId);
            return false;
        }

        // Получаем гильдию по ID
        Guild guild = guildManager.getGuild(guildId);
        if (guild == null) {
            logger.warning("Гильдия с ID " + guildId + " не найдена");
            return false;
        }

        // Получаем гильдию игрока
        Guild playerGuild = guildManager.getPlayerGuild(senderId);
        if (playerGuild == null || !playerGuild.getId().equals(guildId)) {
            logger.warning("Игрок " + senderId + " пытается отправить сообщение в чат гильдии, не являясь её членом");
            return false;
        }

        // Определяем роль игрока в гильдии
        String rolePrefix = "§7[Участник] ";
        if (guild.getLeader().equals(senderId)) {
            rolePrefix = "§6[Лидер] ";
        } else if (guild.getMembers().containsKey(senderId) &&
                guild.getMembers().get(senderId) == GuildRole.OFFICER) {
            rolePrefix = "§e[Офицер] ";
        }

        // Формируем сообщение с префиксом гильдии и ролью отправителя
        String formattedMessage = String.format("§8[§6%s§8] %s§e%s§f: %s",
                guild.getTag(), rolePrefix, senderName, message);

        logger.info("Отправка сообщения в чат гильдии " + guild.getName() + " от " + senderName + ": " + message);

        // Здесь должен быть код для отправки сообщения всем членам гильдии через прокси
        // В рамках демонстрации просто возвращаем true
        return true;
    }

    /**
     * Выключает режим чата гильдии для игрока при выходе из игры
     * 
     * @param playerId UUID игрока
     */
    public void disableGuildChat(UUID playerId) {
        guildChatEnabled.remove(playerId);
    }

    /**
     * Очищает кэш состояний чата
     */
    public void clearCache() {
        guildChatEnabled.clear();
    }
}