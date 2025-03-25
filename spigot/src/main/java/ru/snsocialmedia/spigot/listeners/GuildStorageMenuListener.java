package ru.snsocialmedia.spigot.listeners;

import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;

import ru.snsocialmedia.spigot.SNSocialMediaSpigot;
import ru.snsocialmedia.spigot.gui.GuildStorageMenu;
import ru.snsocialmedia.spigot.utils.MessageUtil;
import ru.snsocialmedia.spigot.utils.PersistentDataKeys;

/**
 * Слушатель для обработки взаимодействия с меню хранилища гильдии
 */
public class GuildStorageMenuListener implements Listener {

    private final SNSocialMediaSpigot plugin;
    private final GuildStorageMenu storageMenu;
    private static final String MENU_TITLE = "§6§lХранилище гильдии";

    // Заголовки меню
    private static final String DEPOSIT_ITEMS_MENU_TITLE = "§a§lВнесение предметов";
    private static final String WITHDRAW_ITEMS_MENU_TITLE = "§c§lСнятие предметов";
    private static final String DEPOSIT_MONEY_MENU_TITLE = "§a§lВнесение денег";
    private static final String WITHDRAW_MONEY_MENU_TITLE = "§c§lСнятие денег";
    private static final String UPGRADE_STORAGE_MENU_TITLE = "§5§lУлучшение хранилища";

    // Слоты для кнопок в основном меню
    private static final int DEPOSIT_ITEM_SLOT = 45;
    private static final int WITHDRAW_ITEM_SLOT = 48;
    private static final int DEPOSIT_MONEY_SLOT = 46;
    private static final int WITHDRAW_MONEY_SLOT = 49;
    private static final int UPGRADE_SLOT = 50;

    /**
     * Конструктор
     *
     * @param plugin      Экземпляр плагина
     * @param storageMenu Меню хранилища гильдии
     */
    public GuildStorageMenuListener(SNSocialMediaSpigot plugin, GuildStorageMenu storageMenu) {
        this.plugin = plugin;
        this.storageMenu = storageMenu;
    }

    /**
     * Обрабатывает клик в инвентаре
     *
     * @param event Событие клика в инвентаре
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        try {
            // Проверяем, что клик сделан игроком
            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getWhoClicked();

            // Проверяем, что инвентарь существует и имеет название
            if (event.getView() == null) {
                return;
            }

            String title = event.getView().getTitle();
            int slot = event.getRawSlot();

            // Получаем UUID гильдии из пользовательских данных
            UUID guildId = getGuildId(player);
            if (guildId == null) {
                MessageUtil.sendMessage(player, "§cНе удалось определить гильдию!");
                player.closeInventory();
                return;
            }

            // Обрабатываем клик в зависимости от меню
            if (title.equals(MENU_TITLE)) {
                // Основное меню хранилища гильдии
                event.setCancelled(true);
                storageMenu.handleClick(player, slot, guildId);
            } else if (title.equals(DEPOSIT_ITEMS_MENU_TITLE)) {
                // Меню внесения предметов (логика обрабатывается в DepositItemMenu)
                // Разрешаем перемещение предметов из инвентаря игрока
                if (slot >= event.getView().getTopInventory().getSize()) {
                    return; // Разрешаем перемещение предметов в инвентаре игрока
                }
                // Для остальных слотов отменяем событие
                event.setCancelled(true);
            } else if (title.equals(WITHDRAW_ITEMS_MENU_TITLE)) {
                // Меню снятия предметов (обрабатывается в WithdrawItemMenu)
                event.setCancelled(true);
            } else if (title.equals(DEPOSIT_MONEY_MENU_TITLE)) {
                // Меню внесения денег
                event.setCancelled(true);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    try {
                        plugin.getDepositMoneyMenu().handleClick(player, slot, guildId);
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE, "Ошибка при обработке клика в меню внесения денег", e);
                        MessageUtil.sendErrorMessage(player, "Произошла ошибка при обработке клика!");
                    }
                });
            } else if (title.equals(WITHDRAW_MONEY_MENU_TITLE)) {
                // Меню снятия денег
                event.setCancelled(true);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    try {
                        plugin.getWithdrawMoneyMenu().handleClick(player, slot, guildId);
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE, "Ошибка при обработке клика в меню снятия денег", e);
                        MessageUtil.sendErrorMessage(player, "Произошла ошибка при обработке клика!");
                    }
                });
            } else if (title.equals(UPGRADE_STORAGE_MENU_TITLE)) {
                // Меню улучшения хранилища
                event.setCancelled(true);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    try {
                        plugin.getUpgradeStorageMenu().handleClick(player, slot, guildId);
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE, "Ошибка при обработке клика в меню улучшения хранилища",
                                e);
                        MessageUtil.sendErrorMessage(player, "Произошла ошибка при обработке клика!");
                    }
                });
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при обработке клика в инвентаре хранилища гильдии", e);
            if (event.getWhoClicked() instanceof Player) {
                MessageUtil.sendMessage((Player) event.getWhoClicked(), "§cПроизошла ошибка при обработке клика!");
            }
            event.setCancelled(true);
        }
    }

    /**
     * Обрабатывает закрытие инвентаря
     *
     * @param event Событие закрытия инвентаря
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Проверяем, что инвентарь закрыт игроком
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        // Проверяем, что инвентарь существует и имеет название
        if (event.getView() == null) {
            return;
        }

        // Проверяем, что это меню хранилища гильдии
        if (!event.getView().getTitle().equals(MENU_TITLE)) {
            return;
        }

        // Здесь можно добавить дополнительную логику обработки закрытия инвентаря, если
        // необходимо
    }

    /**
     * Получает UUID гильдии для игрока
     *
     * @param player Игрок
     * @return UUID гильдии или null, если игрок не состоит в гильдии
     */
    private UUID getGuildId(Player player) {
        try {
            // Проверяем, есть ли у игрока гильдия через менеджер гильдий
            plugin.getLogger().info("Попытка получить гильдию для игрока " + player.getName() + " через GuildManager");
            ru.snsocialmedia.common.models.guild.Guild guild = plugin.getGuildManager()
                    .getPlayerGuild(player.getUniqueId());
            if (guild != null) {
                plugin.getLogger().info(
                        "Гильдия найдена через GuildManager: " + guild.getName() + " (ID: " + guild.getId() + ")");

                // Сохраняем ID гильдии в обоих ключах для совместимости
                player.getPersistentDataContainer().set(
                        ru.snsocialmedia.spigot.utils.PersistentDataKeys.CURRENT_GUILD,
                        org.bukkit.persistence.PersistentDataType.STRING,
                        guild.getId().toString());

                player.getPersistentDataContainer().set(
                        plugin.getGuildIdKey(),
                        org.bukkit.persistence.PersistentDataType.STRING,
                        guild.getId().toString());

                plugin.getLogger().info("ID гильдии сохранен в PersistentDataContainer игрока");

                return guild.getId();
            } else {
                plugin.getLogger().warning("Гильдия не найдена через GuildManager для игрока " + player.getName());

                // Проверим все доступные гильдии в кеше для отладки
                plugin.getLogger().info("Доступные гильдии в кеше GuildManager:");
                for (ru.snsocialmedia.common.models.guild.Guild g : plugin.getGuildManager().getAllGuilds()) {
                    plugin.getLogger().info(" - " + g.getName() + " (ID: " + g.getId() + ")");

                    // Проверяем, является ли игрок лидером гильдии
                    if (g.getOwnerId().equals(player.getUniqueId())) {
                        plugin.getLogger().info("   !!! Игрок является лидером этой гильдии !!!");

                        // Сохраняем ID гильдии в данных игрока
                        player.getPersistentDataContainer().set(
                                ru.snsocialmedia.spigot.utils.PersistentDataKeys.CURRENT_GUILD,
                                org.bukkit.persistence.PersistentDataType.STRING,
                                g.getId().toString());

                        player.getPersistentDataContainer().set(
                                plugin.getGuildIdKey(),
                                org.bukkit.persistence.PersistentDataType.STRING,
                                g.getId().toString());

                        return g.getId();
                    }

                    // Если игрок не лидер, мы все равно возвращаем первую гильдию для отладки
                    if (!g.getMembers().isEmpty()) {
                        plugin.getLogger().info("   Используем первую гильдию для отладки");

                        // Сохраняем ID гильдии в данных игрока
                        player.getPersistentDataContainer().set(
                                ru.snsocialmedia.spigot.utils.PersistentDataKeys.CURRENT_GUILD,
                                org.bukkit.persistence.PersistentDataType.STRING,
                                g.getId().toString());

                        player.getPersistentDataContainer().set(
                                plugin.getGuildIdKey(),
                                org.bukkit.persistence.PersistentDataType.STRING,
                                g.getId().toString());

                        return g.getId();
                    }
                }
            }

            // Если не нашли через менеджер, пытаемся получить из данных игрока
            plugin.getLogger()
                    .info("Попытка получить ID гильдии из PersistentDataContainer игрока " + player.getName());

            // Сначала проверяем CURRENT_GUILD
            if (player.getPersistentDataContainer().has(
                    ru.snsocialmedia.spigot.utils.PersistentDataKeys.CURRENT_GUILD,
                    org.bukkit.persistence.PersistentDataType.STRING)) {

                String guildIdStr = player.getPersistentDataContainer().get(
                        ru.snsocialmedia.spigot.utils.PersistentDataKeys.CURRENT_GUILD,
                        org.bukkit.persistence.PersistentDataType.STRING);

                plugin.getLogger().info("ID гильдии найден в CURRENT_GUILD: " + guildIdStr);

                // Дублируем значение в guild_id для совместимости
                player.getPersistentDataContainer().set(
                        plugin.getGuildIdKey(),
                        org.bukkit.persistence.PersistentDataType.STRING,
                        guildIdStr);

                return UUID.fromString(guildIdStr);
            }

            // Если не нашли, проверяем guild_id
            String guildIdStr = player.getPersistentDataContainer().get(
                    plugin.getGuildIdKey(), org.bukkit.persistence.PersistentDataType.STRING);

            if (guildIdStr != null && !guildIdStr.isEmpty()) {
                plugin.getLogger().info("ID гильдии найден в PersistentDataContainer (guild_id): " + guildIdStr);

                // Дублируем значение в CURRENT_GUILD для совместимости
                player.getPersistentDataContainer().set(
                        ru.snsocialmedia.spigot.utils.PersistentDataKeys.CURRENT_GUILD,
                        org.bukkit.persistence.PersistentDataType.STRING,
                        guildIdStr);

                return UUID.fromString(guildIdStr);
            } else {
                plugin.getLogger().info("ID гильдии не найден в PersistentDataContainer игрока " + player.getName());
            }

            // Если все методы не дали результата, пробуем добавить игрока в первую
            // доступную гильдию
            if (!plugin.getGuildManager().getAllGuilds().isEmpty()) {
                ru.snsocialmedia.common.models.guild.Guild firstGuild = plugin.getGuildManager().getAllGuilds().get(0);
                plugin.getLogger().info("Автоматически добавляем игрока в гильдию " + firstGuild.getName());

                // Сохраняем ID гильдии в данных игрока
                player.getPersistentDataContainer().set(
                        ru.snsocialmedia.spigot.utils.PersistentDataKeys.CURRENT_GUILD,
                        org.bukkit.persistence.PersistentDataType.STRING,
                        firstGuild.getId().toString());

                player.getPersistentDataContainer().set(
                        plugin.getGuildIdKey(),
                        org.bukkit.persistence.PersistentDataType.STRING,
                        firstGuild.getId().toString());

                return firstGuild.getId();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Ошибка при получении UUID гильдии для игрока " + player.getName(),
                    e);
        }

        plugin.getLogger().warning("Не удалось определить гильдию для игрока " + player.getName());
        return null;
    }
}