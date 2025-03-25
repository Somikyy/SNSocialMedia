package ru.snsocialmedia.spigot.listeners;

import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;

import ru.snsocialmedia.common.models.guild.Guild;
import ru.snsocialmedia.spigot.SNSocialMediaSpigot;
import ru.snsocialmedia.spigot.utils.MessageUtil;
import ru.snsocialmedia.spigot.utils.PersistentDataKeys;

/**
 * Обработчик события входа игрока на сервер
 */
public class PlayerJoinListener implements Listener {

    private final SNSocialMediaSpigot plugin;

    /**
     * Конструктор
     *
     * @param plugin Экземпляр плагина
     */
    public PlayerJoinListener(SNSocialMediaSpigot plugin) {
        this.plugin = plugin;
    }

    /**
     * Обрабатывает вход игрока на сервер
     *
     * @param event Событие входа игрока
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        try {
            plugin.getLogger().info("Игрок " + player.getName() + " вошел на сервер. UUID: " + player.getUniqueId());

            // Проверяем, есть ли гильдии в кеше
            if (plugin.getGuildManager().getAllGuilds().isEmpty()) {
                plugin.getLogger().warning("В кеше нет гильдий при входе игрока " + player.getName());
                return;
            }

            // Проверяем, есть ли у игрока гильдия
            ru.snsocialmedia.common.models.guild.Guild playerGuild = plugin.getGuildManager()
                    .getPlayerGuild(player.getUniqueId());
            if (playerGuild == null) {
                plugin.getLogger().info(
                        "У игрока " + player.getName() + " нет гильдии. Пробуем добавить в существующую гильдию...");

                // Получаем первую гильдию из списка (или можно выбрать конкретную по имени/ID)
                ru.snsocialmedia.common.models.guild.Guild guild = plugin.getGuildManager().getAllGuilds().get(0);
                if (guild != null) {
                    // Добавляем игрока в гильдию
                    plugin.getLogger().info("Добавляем игрока в гильдию " + guild.getName() + " с ролью MEMBER");

                    // Добавление игрока будет произведено через сохранение ID гильдии
                    // в PersistentDataContainer игрока, которое обрабатывается в GUI

                    plugin.getLogger().info("Игрок " + player.getName() + " добавлен в гильдию " + guild.getName());

                    // Сохраняем ID гильдии в PersistentDataContainer игрока
                    player.getPersistentDataContainer().set(
                            ru.snsocialmedia.spigot.utils.PersistentDataKeys.CURRENT_GUILD,
                            org.bukkit.persistence.PersistentDataType.STRING,
                            guild.getId().toString());

                    // Дублируем ID в альтернативный ключ для совместимости
                    player.getPersistentDataContainer().set(
                            plugin.getGuildIdKey(),
                            org.bukkit.persistence.PersistentDataType.STRING,
                            guild.getId().toString());

                    plugin.getLogger().info("ID гильдии сохранен в данных игрока " + player.getName());
                    ru.snsocialmedia.spigot.utils.MessageUtil.sendMessage(player,
                            "§aВы были автоматически добавлены в гильдию: " + guild.getName());

                    // Проверяем, что ID гильдии сохранен
                    String savedGuildId = player.getPersistentDataContainer().get(
                            ru.snsocialmedia.spigot.utils.PersistentDataKeys.CURRENT_GUILD,
                            org.bukkit.persistence.PersistentDataType.STRING);

                    plugin.getLogger().info("Проверка: ID гильдии в данных игрока: " + savedGuildId);
                } else {
                    plugin.getLogger().warning("Не удалось получить гильдию из кеша");
                }
            } else {
                plugin.getLogger()
                        .info("Игрок " + player.getName() + " уже состоит в гильдии " + playerGuild.getName());

                // Обновляем ID гильдии в данных игрока для обеспечения согласованности
                player.getPersistentDataContainer().set(
                        ru.snsocialmedia.spigot.utils.PersistentDataKeys.CURRENT_GUILD,
                        org.bukkit.persistence.PersistentDataType.STRING,
                        playerGuild.getId().toString());

                player.getPersistentDataContainer().set(
                        plugin.getGuildIdKey(),
                        org.bukkit.persistence.PersistentDataType.STRING,
                        playerGuild.getId().toString());

                plugin.getLogger().info("ID гильдии обновлен в данных игрока " + player.getName());
            }

            // Выводим отладочную информацию о состоянии кеша гильдий
            plugin.getGuildManager().debugCacheState();
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при обработке входа игрока: " + e.getMessage());
            e.printStackTrace();
        }
    }
}