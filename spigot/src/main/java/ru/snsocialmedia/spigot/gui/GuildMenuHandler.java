package ru.snsocialmedia.spigot.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import ru.snsocialmedia.spigot.SNSocialMediaSpigot;
import ru.snsocialmedia.spigot.utils.MessageUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Класс для обработки GUI-меню гильдий на стороне Spigot
 */
public class GuildMenuHandler {

    private final SNSocialMediaSpigot plugin;

    // Карта для хранения типов открытых меню
    private final Map<UUID, String> openMenuTypes = new HashMap<>();

    // Карта для хранения данных об открытом меню (например, id гильдии)
    private final Map<UUID, String> menuData = new HashMap<>();

    // Константы для идентификации слотов меню
    private static final int SLOT_CREATE_GUILD = 11;
    private static final int SLOT_ACCEPT_INVITE = 13;
    private static final int SLOT_TOP_GUILDS = 15;

    private static final int SLOT_INFO = 10;
    private static final int SLOT_CHAT = 12;
    private static final int SLOT_INVITE = 14;
    private static final int SLOT_KICK = 16;
    private static final int SLOT_PROMOTE_DEMOTE = 20;
    private static final int SLOT_SETTINGS = 22;
    private static final int SLOT_LEAVE = 24;
    private static final int STORAGE_BUTTON_SLOT = 18; // Слот для кнопки открытия хранилища

    private final GuildStorageMenu storageMenu;

    public GuildMenuHandler(SNSocialMediaSpigot plugin) {
        this.plugin = plugin;
        this.storageMenu = new GuildStorageMenu(plugin);
    }

    /**
     * Открывает соответствующее меню для игрока
     * 
     * @param player   Игрок
     * @param menuType Тип меню
     * @param data     Дополнительные данные
     */
    public void openMenu(Player player, String menuType, String data) {
        try {
            if (player == null) {
                plugin.getLogger().warning("Попытка открыть меню для null игрока");
                return;
            }

            if (menuType == null) {
                plugin.getLogger().warning("Попытка открыть null тип меню для игрока " + player.getName());
                player.sendMessage("§cОшибка: некорректный тип меню");
                return;
            }

            plugin.getLogger()
                    .info("Открываем меню " + menuType + " для игрока " + player.getName() + " с данными: " +
                            (data != null ? data : "null"));

            switch (menuType) {
                case "no_guild":
                    openNoGuildMenu(player);
                    openMenuTypes.put(player.getUniqueId(), "no_guild");
                    break;
                case "guild_management":
                    openGuildManagementMenu(player, data);
                    openMenuTypes.put(player.getUniqueId(), "guild_management");
                    if (data != null) {
                        menuData.put(player.getUniqueId(), data);
                    }
                    break;
                case "create_guild":
                    openCreateGuildMenu(player);
                    openMenuTypes.put(player.getUniqueId(), "create_guild");
                    break;
                case "invite":
                    openInviteMenu(player, data);
                    openMenuTypes.put(player.getUniqueId(), "invite");
                    if (data != null) {
                        menuData.put(player.getUniqueId(), data);
                    }
                    break;
                case "members":
                    openMembersMenu(player, data);
                    openMenuTypes.put(player.getUniqueId(), "members");
                    if (data != null) {
                        menuData.put(player.getUniqueId(), data);
                    }
                    break;
                default:
                    player.sendMessage("§cНеизвестный тип меню: " + menuType);
                    plugin.getLogger().warning("Попытка открыть неизвестный тип меню: " + menuType);
                    break;
            }
        } catch (Exception e) {
            player.sendMessage("§cПроизошла ошибка при открытии меню: " + e.getMessage());
            plugin.getLogger().severe(
                    "Ошибка при открытии меню " + menuType + " для игрока " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Открывает меню для игрока, не состоящего в гильдии
     * 
     * @param player Игрок
     */
    private void openNoGuildMenu(Player player) {
        try {
            Inventory inventory = Bukkit.createInventory(player, 27, "§6Меню гильдий");

            // Создать гильдию
            ItemStack createItem = createItem(Material.EMERALD_BLOCK, "§a§lСоздать гильдию",
                    Arrays.asList("§7Создайте свою собственную гильдию", "§7и пригласите в неё друзей"));
            inventory.setItem(SLOT_CREATE_GUILD, createItem);

            // Принять приглашение
            ItemStack acceptItem = createItem(Material.PAPER, "§e§lПринять приглашение",
                    Arrays.asList("§7Если вас пригласили в гильдию,", "§7вы можете принять приглашение здесь"));
            inventory.setItem(SLOT_ACCEPT_INVITE, acceptItem);

            // Топ гильдий
            ItemStack topItem = createItem(Material.GOLD_BLOCK, "§6§lТоп гильдий",
                    Arrays.asList("§7Просмотр списка лучших гильдий", "§7на сервере"));
            inventory.setItem(SLOT_TOP_GUILDS, topItem);

            // Заполнение пустых слотов
            fillEmptySlots(inventory);

            player.openInventory(inventory);
        } catch (Exception e) {
            player.sendMessage("§cПроизошла ошибка при открытии меню: " + e.getMessage());
            plugin.getLogger().severe("Ошибка при открытии меню без гильдии для игрока " + player.getName());
            e.printStackTrace();
        }
    }

    /**
     * Открывает меню управления гильдией
     * 
     * @param player  Игрок
     * @param guildId Идентификатор гильдии
     */
    private void openGuildManagementMenu(Player player, String guildId) {
        try {
            Inventory inventory = Bukkit.createInventory(player, 36, "§6Меню управления гильдией");

            // Информация о гильдии
            ItemStack infoItem = createItem(Material.BOOK, "§e§lИнформация о гильдии",
                    Arrays.asList("§7Просмотр подробной информации", "§7о вашей гильдии"));
            inventory.setItem(SLOT_INFO, infoItem);

            // Чат гильдии
            ItemStack chatItem = createItem(Material.WRITABLE_BOOK, "§a§lЧат гильдии",
                    Arrays.asList("§7Отправить сообщение в", "§7чат вашей гильдии"));
            inventory.setItem(SLOT_CHAT, chatItem);

            // Пригласить игрока
            ItemStack inviteItem = createItem(Material.PAPER, "§b§lПригласить игрока",
                    Arrays.asList("§7Пригласить нового игрока", "§7в вашу гильдию"));
            inventory.setItem(SLOT_INVITE, inviteItem);

            // Исключить игрока
            ItemStack kickItem = createItem(Material.BARRIER, "§c§lИсключить игрока",
                    Arrays.asList("§7Исключить игрока", "§7из вашей гильдии"));
            inventory.setItem(SLOT_KICK, kickItem);

            // Повысить/Понизить
            ItemStack promoteItem = createItem(Material.GOLDEN_HELMET, "§6§lПовысить/Понизить",
                    Arrays.asList("§7Управление ролями", "§7участников гильдии"));
            inventory.setItem(SLOT_PROMOTE_DEMOTE, promoteItem);

            // Настройки гильдии
            ItemStack settingsItem = createItem(Material.REDSTONE_TORCH, "§d§lНастройки гильдии",
                    Arrays.asList("§7Настройка параметров", "§7вашей гильдии"));
            inventory.setItem(SLOT_SETTINGS, settingsItem);

            // Покинуть гильдию
            ItemStack leaveItem = createItem(Material.IRON_DOOR, "§c§lПокинуть гильдию",
                    Arrays.asList("§7Покинуть вашу", "§7текущую гильдию"));
            inventory.setItem(SLOT_LEAVE, leaveItem);

            // Добавить кнопку хранилища гильдии
            ItemStack storageItem = createItem(Material.CHEST, "§e§lХранилище гильдии",
                    Arrays.asList("§7Доступ к хранилищу", "§7предметов вашей гильдии"));
            inventory.setItem(STORAGE_BUTTON_SLOT, storageItem);

            // Заполнение пустых слотов
            fillEmptySlots(inventory);

            player.openInventory(inventory);
        } catch (Exception e) {
            player.sendMessage("§cПроизошла ошибка при открытии меню управления гильдией: " + e.getMessage());
            plugin.getLogger().severe("Ошибка при открытии меню управления гильдией для игрока " + player.getName()
                    + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Открывает меню создания гильдии
     * 
     * @param player Игрок
     */
    private void openCreateGuildMenu(Player player) {
        try {
            plugin.getLogger().info("Открытие меню создания гильдии для игрока " + player.getName());

            Inventory inventory = Bukkit.createInventory(player, 27, "§6Создание гильдии");

            // Инструкция по созданию
            ItemStack helpItem = createItem(Material.BOOK, "§e§lКак создать гильдию",
                    Arrays.asList("§7Введите в чат:", "§a/guild create <название> [тег]",
                            "§7Название: от 3 до 16 символов", "§7Тег: от 2 до 5 символов (необязательно)"));
            inventory.setItem(13, helpItem);

            // Заполнение пустых слотов
            fillEmptySlots(inventory);

            player.openInventory(inventory);
        } catch (Exception e) {
            player.sendMessage("§cПроизошла ошибка при открытии меню создания гильдии: " + e.getMessage());
            plugin.getLogger().severe("Ошибка при открытии меню создания гильдии для игрока " + player.getName());
            e.printStackTrace();
        }
    }

    /**
     * Открывает меню приглашения игроков
     * 
     * @param player  Игрок
     * @param guildId ID гильдии
     */
    private void openInviteMenu(Player player, String guildId) {
        try {
            plugin.getLogger().info(
                    "Открытие меню приглашения для игрока " + player.getName() + " (ID гильдии: " + guildId + ")");

            Inventory inventory = Bukkit.createInventory(player, 54, "§6Приглашение в гильдию");

            // Инструкция по приглашению
            ItemStack helpItem = createItem(Material.BOOK, "§e§lКак пригласить игрока",
                    Arrays.asList("§7Введите в чат:", "§a/guild invite <игрок>",
                            "§7или выберите игрока из списка ниже"));
            inventory.setItem(4, helpItem);

            // Получаем список онлайн игроков (в реальном коде)
            // Здесь для примера просто добавляем несколько плейсхолдеров
            List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
            int slot = 9;

            for (Player onlinePlayer : onlinePlayers) {
                if (!onlinePlayer.equals(player) && slot < 54) {
                    try {
                        ItemStack playerHead = createPlayerHead(onlinePlayer.getName(), "§a" + onlinePlayer.getName(),
                                Arrays.asList("§7Нажмите, чтобы пригласить", "§7этого игрока в гильдию"));
                        inventory.setItem(slot, playerHead);
                        slot++;
                    } catch (Exception e) {
                        plugin.getLogger().warning("Не удалось создать голову для игрока " + onlinePlayer.getName()
                                + ": " + e.getMessage());
                    }
                }
            }

            // Заполнение пустых слотов
            fillEmptySlots(inventory);

            player.openInventory(inventory);
        } catch (Exception e) {
            player.sendMessage("§cПроизошла ошибка при открытии меню приглашения: " + e.getMessage());
            plugin.getLogger().severe("Ошибка при открытии меню приглашения для игрока " + player.getName());
            e.printStackTrace();
        }
    }

    /**
     * Открывает меню управления участниками гильдии
     * 
     * @param player  Игрок
     * @param guildId ID гильдии
     */
    private void openMembersMenu(Player player, String guildId) {
        try {
            plugin.getLogger()
                    .info("Открытие меню участников для игрока " + player.getName() + " (ID гильдии: " + guildId + ")");

            Inventory inventory = Bukkit.createInventory(player, 54, "§6Участники гильдии");

            // В реальном коде здесь нужно будет получить список участников гильдии
            // Для примера просто добавляем плейсхолдеры

            // Заголовок
            ItemStack titleItem = createItem(Material.BOOK, "§e§lУчастники гильдии",
                    Arrays.asList("§7Список всех участников вашей гильдии", "§7с указанием их ролей"));
            inventory.setItem(4, titleItem);

            // Примеры участников (в реальном коде будут реальные игроки)
            ItemStack leaderItem = createItem(Material.GOLDEN_HELMET, "§6Лидер: Player1",
                    Arrays.asList("§7Роль: §6ЛИДЕР", "§7Онлайн: §aДа", "§7Нажмите для действий"));
            inventory.setItem(10, leaderItem);

            ItemStack officerItem = createItem(Material.IRON_HELMET, "§eОфицер: Player2",
                    Arrays.asList("§7Роль: §eОФИЦЕР", "§7Онлайн: §aДа", "§7Нажмите для действий"));
            inventory.setItem(11, officerItem);

            ItemStack memberItem = createItem(Material.LEATHER_HELMET, "§7Участник: Player3",
                    Arrays.asList("§7Роль: §7УЧАСТНИК", "§7Онлайн: §cНет", "§7Нажмите для действий"));
            inventory.setItem(12, memberItem);

            // Заполнение пустых слотов
            fillEmptySlots(inventory);

            player.openInventory(inventory);
        } catch (Exception e) {
            player.sendMessage("§cПроизошла ошибка при открытии меню участников: " + e.getMessage());
            plugin.getLogger().severe("Ошибка при открытии меню участников для игрока " + player.getName());
            e.printStackTrace();
        }
    }

    /**
     * Обрабатывает клик по элементу меню
     * 
     * @param player Игрок
     * @param slot   Слот
     * @return true, если клик обработан
     */
    public boolean handleClick(Player player, int slot) {
        try {
            // Базовые проверки
            if (player == null) {
                plugin.getLogger().warning("handleClick вызван с null player");
                return false;
            }

            UUID playerId = player.getUniqueId();
            plugin.getLogger()
                    .info("Обработка клика от игрока " + player.getName() + ", UUID: " + playerId + ", слот: " + slot);

            // Проверяем, есть ли у игрока открытое меню
            if (!openMenuTypes.containsKey(playerId)) {
                plugin.getLogger().info("У игрока " + player.getName() + " нет открытого меню гильдии");
                return false;
            }

            String menuType = openMenuTypes.get(playerId);
            if (menuType == null) {
                plugin.getLogger().warning("Тип меню для игрока " + player.getName() + " равен null");
                return false;
            }

            plugin.getLogger().info(
                    "Обработка клика от игрока " + player.getName() + " в меню типа: '" + menuType + "', слот: "
                            + slot);

            // Проверяем, что инвентарь открыт
            if (player.getOpenInventory() == null || player.getOpenInventory().getTopInventory() == null) {
                plugin.getLogger().warning("У игрока " + player.getName() + " нет открытого инвентаря");
                return false;
            }

            // Выводим название инвентаря для отладки
            String inventoryTitle = player.getOpenInventory().getTitle();
            plugin.getLogger().info("Название открытого инвентаря: '" + inventoryTitle + "'");

            // В зависимости от типа открытого меню обрабатываем клик
            boolean result = false;
            switch (menuType) {
                case "no_guild":
                    result = handleNoGuildMenuClick(player, slot);
                    break;
                case "guild_management":
                    result = handleGuildManagementMenuClick(player, slot);
                    break;
                case "create_guild":
                    result = handleCreateGuildMenuClick(player, slot);
                    break;
                case "invite":
                    result = handleInviteMenuClick(player, player.getOpenInventory().getTopInventory(), slot);
                    break;
                case "members":
                    result = handleMembersMenuClick(player, player.getOpenInventory().getTopInventory(), slot);
                    break;
                case "player_action":
                    result = handlePlayerActionMenuClick(player, slot);
                    break;
                case "storage":
                    result = handleStorageMenuClick(player, slot);
                    break;
                default:
                    plugin.getLogger().warning("Неизвестный тип меню: '" + menuType + "'");
                    result = false;
                    break;
            }

            // Логируем результат обработки
            plugin.getLogger().info("Результат обработки клика: " + (result ? "обработан" : "не обработан"));
            return result;

        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при обработке клика: " + e.getMessage());
            e.printStackTrace();

            // Сообщаем игроку об ошибке
            player.sendMessage("§cПроизошла ошибка при обработке действия. Пожалуйста, сообщите администрации.");

            // Логирование стека вызовов для отладки
            plugin.getLogger().severe("Стек вызовов:");
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().severe("  " + element.toString());
            }

            // Закрываем инвентарь, чтобы избежать дальнейших проблем
            player.closeInventory();

            // Возвращаем true, чтобы событие было отменено
            return true;
        }
    }

    /**
     * Обрабатывает закрытие инвентаря
     * 
     * @param player Игрок
     */
    public void handleInventoryClose(Player player) {
        try {
            UUID playerId = player.getUniqueId();

            // Логируем закрытие меню
            if (openMenuTypes.containsKey(playerId)) {
                String menuType = openMenuTypes.get(playerId);
                plugin.getLogger().info("Игрок " + player.getName() + " закрыл меню " + menuType);
            }

            // Удаляем информацию о меню
            openMenuTypes.remove(playerId);
            menuData.remove(playerId);
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при обработке закрытия инвентаря: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Обрабатывает клик в меню без гильдии
     * 
     * @param player Игрок
     * @param slot   Слот
     * @return true, если клик обработан
     */
    private boolean handleNoGuildMenuClick(Player player, int slot) {
        try {
            plugin.getLogger().info("Обработка клика в меню без гильдии, слот: " + slot);

            switch (slot) {
                case SLOT_CREATE_GUILD:
                    player.closeInventory();
                    openCreateGuildMenu(player);
                    return true;

                case SLOT_ACCEPT_INVITE:
                    player.closeInventory();
                    plugin.getLogger().info("Игрок " + player.getName() + " выполняет команду: guild accept");
                    player.performCommand("guild accept");
                    return true;

                case SLOT_TOP_GUILDS:
                    player.closeInventory();
                    plugin.getLogger().info("Игрок " + player.getName() + " выполняет команду: guild top");
                    player.performCommand("guild top");
                    return true;
            }

            return false;
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при обработке клика в меню без гильдии: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Обрабатывает клик в меню управления гильдией
     * 
     * @param player Игрок
     * @param slot   Слот
     * @return true, если клик обработан
     */
    private boolean handleGuildManagementMenuClick(Player player, int slot) {
        try {
            plugin.getLogger().info("Обработка клика в меню управления гильдией, слот: " + slot);

            switch (slot) {
                case SLOT_INFO:
                    player.closeInventory();
                    plugin.getLogger().info("Игрок " + player.getName() + " выполняет команду: guild info");
                    player.performCommand("guild info");
                    return true;

                case SLOT_CHAT:
                    player.closeInventory();
                    player.sendMessage("§aВведите сообщение для чата гильдии: §f/guild chat <сообщение>");
                    return true;

                case SLOT_INVITE:
                    player.closeInventory();
                    plugin.getLogger().info("Игрок " + player.getName() + " выполняет команду: guild invite");
                    player.performCommand("guild invite");
                    return true;

                case SLOT_KICK:
                    player.closeInventory();
                    // Открываем список участников для исключения
                    plugin.getLogger().info("Игрок " + player.getName() + " выполняет команду: guild members");
                    player.performCommand("guild members");
                    return true;

                case SLOT_PROMOTE_DEMOTE:
                    player.closeInventory();
                    // Открываем список участников для повышения/понижения
                    plugin.getLogger().info("Игрок " + player.getName() + " выполняет команду: guild members");
                    player.performCommand("guild members");
                    return true;

                case SLOT_SETTINGS:
                    player.closeInventory();
                    player.sendMessage("§aНастройки гильдии пока недоступны");
                    return true;

                case SLOT_LEAVE:
                    player.closeInventory();
                    plugin.getLogger().info("Игрок " + player.getName() + " выполняет команду: guild leave");
                    player.performCommand("guild leave");
                    return true;

                case STORAGE_BUTTON_SLOT:
                    // Открываем хранилище гильдии
                    player.closeInventory();
                    String guildId = menuData.get(player.getUniqueId());
                    if (guildId != null && !guildId.isEmpty()) {
                        openGuildStorage(player, UUID.fromString(guildId));
                    } else {
                        player.sendMessage("§cОшибка: Не удалось определить ID гильдии");
                    }
                    return true;
            }

            return false;
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при обработке клика в меню управления гильдией: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Обрабатывает клик в меню создания гильдии
     * 
     * @param player Игрок
     * @param slot   Слот
     * @return true, если клик обработан
     */
    private boolean handleCreateGuildMenuClick(Player player, int slot) {
        try {
            plugin.getLogger().info("Обработка клика в меню создания гильдии, слот: " + slot);

            // Возвращаемся в главное меню при любом клике
            player.closeInventory();
            player.sendMessage("§aДля создания гильдии введите: §f/guild create <название> [тег]");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при обработке клика в меню создания гильдии: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Обрабатывает клик в меню приглашения игроков
     * 
     * @param player    Игрок
     * @param inventory Инвентарь
     * @param slot      Слот
     * @return true, если клик обработан
     */
    private boolean handleInviteMenuClick(Player player, Inventory inventory, int slot) {
        try {
            plugin.getLogger().info("Обработка клика в меню приглашения игроков, слот: " + slot);

            ItemStack item = inventory.getItem(slot);

            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                String displayName = item.getItemMeta().getDisplayName();

                if (displayName.startsWith("§a")) {
                    String playerName = displayName.substring(2); // Убираем префикс §a
                    player.closeInventory();
                    plugin.getLogger()
                            .info("Игрок " + player.getName() + " выполняет команду: guild invite " + playerName);
                    player.performCommand("guild invite " + playerName);
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при обработке клика в меню приглашения игроков: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Обрабатывает клик в меню участников гильдии
     * 
     * @param player    Игрок
     * @param inventory Инвентарь
     * @param slot      Слот
     * @return true, если клик обработан
     */
    private boolean handleMembersMenuClick(Player player, Inventory inventory, int slot) {
        try {
            plugin.getLogger().info("Обработка клика в меню участников гильдии, слот: " + slot);

            ItemStack item = inventory.getItem(slot);

            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                String displayName = item.getItemMeta().getDisplayName();
                plugin.getLogger().info("Клик на предмет с именем: '" + displayName + "'");

                if (displayName.contains(": ")) {
                    String[] parts = displayName.split(": ");
                    if (parts.length > 1) {
                        String playerName = parts[1];
                        plugin.getLogger().info("Найдено имя игрока в дисплее: '" + playerName + "'");
                        player.closeInventory();

                        // Открываем меню действий с игроком
                        openPlayerActionMenu(player, playerName);
                        return true;
                    } else {
                        plugin.getLogger().warning("Разделение строки не дало верного результата: " + displayName);
                    }
                } else {
                    plugin.getLogger().info("Имя предмета не содержит двоеточие: " + displayName);
                }
            } else {
                plugin.getLogger().info("Предмет в слоте " + slot + " не содержит метаданных или пустой");
            }

            return false;
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при обработке клика в меню участников гильдии: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Открывает меню действий с игроком
     * 
     * @param player     Игрок, открывающий меню
     * @param targetName Имя целевого игрока
     */
    private void openPlayerActionMenu(Player player, String targetName) {
        try {
            plugin.getLogger()
                    .info("Открытие меню действий с игроком '" + targetName + "' для игрока " + player.getName());

            Inventory inventory = Bukkit.createInventory(player, 27, "§6Действия: " + targetName);

            // Исключить
            ItemStack kickItem = createItem(Material.BARRIER, "§c§lИсключить игрока",
                    Arrays.asList("§7Исключить игрока " + targetName, "§7из вашей гильдии"));
            inventory.setItem(11, kickItem);

            // Повысить
            ItemStack promoteItem = createItem(Material.GOLDEN_HELMET, "§6§lПовысить игрока",
                    Arrays.asList("§7Повысить игрока " + targetName, "§7до следующей роли"));
            inventory.setItem(13, promoteItem);

            // Понизить
            ItemStack demoteItem = createItem(Material.LEATHER_HELMET, "§e§lПонизить игрока",
                    Arrays.asList("§7Понизить игрока " + targetName, "§7до предыдущей роли"));
            inventory.setItem(15, demoteItem);

            // Заполнение пустых слотов
            fillEmptySlots(inventory);

            // Сохраняем имя целевого игрока в данных меню для последующего использования
            openMenuTypes.put(player.getUniqueId(), "player_action");
            menuData.put(player.getUniqueId(), targetName);

            plugin.getLogger().info("Тип меню для игрока " + player.getName() + " установлен на 'player_action'");
            plugin.getLogger()
                    .info("Данные меню для игрока " + player.getName() + " установлены на '" + targetName + "'");

            player.openInventory(inventory);
        } catch (Exception e) {
            player.sendMessage("§cПроизошла ошибка при открытии меню действий: " + e.getMessage());
            plugin.getLogger().severe("Ошибка при открытии меню действий с игроком для игрока " + player.getName()
                    + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Обрабатывает клик в меню действий с игроком
     * 
     * @param player Игрок
     * @param slot   Слот
     * @return true, если клик обработан
     */
    private boolean handlePlayerActionMenuClick(Player player, int slot) {
        try {
            String targetName = menuData.get(player.getUniqueId());
            plugin.getLogger().info("handlePlayerActionMenuClick: Получено имя целевого игрока: " +
                    (targetName != null ? "'" + targetName + "'" : "null"));

            if (targetName == null || targetName.isEmpty()) {
                plugin.getLogger().warning("Не удалось получить имя целевого игрока для действий");
                player.sendMessage("§cОшибка: Не удалось определить игрока для выполнения действия");
                player.closeInventory();
                return false;
            }

            plugin.getLogger().info("Обработка клика в меню действий с игроком '" + targetName +
                    "', слот: " + slot);

            switch (slot) {
                case 11: // Исключить
                    player.closeInventory();
                    plugin.getLogger()
                            .info("Игрок " + player.getName() + " выполняет команду: guild kick " + targetName);
                    player.performCommand("guild kick " + targetName);
                    return true;
                case 13: // Повысить
                    player.closeInventory();
                    plugin.getLogger()
                            .info("Игрок " + player.getName() + " выполняет команду: guild promote " + targetName);
                    player.performCommand("guild promote " + targetName);
                    return true;
                case 15: // Понизить
                    player.closeInventory();
                    plugin.getLogger()
                            .info("Игрок " + player.getName() + " выполняет команду: guild demote " + targetName);
                    player.performCommand("guild demote " + targetName);
                    return true;
                default:
                    plugin.getLogger().info("Клик в меню действий в неизвестный слот: " + slot);
                    return false;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при обработке клика в меню действий с игроком: " + e.getMessage());
            e.printStackTrace();
            // Выводим дополнительную информацию о StackTrace для отладки
            StringBuilder stackInfo = new StringBuilder("Стек вызовов:\n");
            for (StackTraceElement element : e.getStackTrace()) {
                stackInfo.append("  ").append(element.toString()).append("\n");
            }
            plugin.getLogger().severe(stackInfo.toString());

            return false;
        }
    }

    /**
     * Создает предмет для меню с дополнительными проверками
     * 
     * @param material Материал
     * @param name     Название
     * @param lore     Описание
     * @return Предмет
     */
    private ItemStack createItem(Material material, String name, List<String> lore) {
        try {
            if (material == null) {
                plugin.getLogger().warning("Попытка создать предмет с null материалом");
                return new ItemStack(Material.STONE);
            }

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                if (name != null) {
                    meta.setDisplayName(name);
                }
                if (lore != null) {
                    meta.setLore(lore);
                }
                item.setItemMeta(meta);
            }

            return item;
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при создании предмета: " + e.getMessage());
            e.printStackTrace();
            return new ItemStack(Material.STONE);
        }
    }

    /**
     * Создает предмет-голову игрока
     * 
     * @param playerName  Имя игрока
     * @param displayName Отображаемое имя
     * @param lore        Описание
     * @return Предмет-голова
     */
    private ItemStack createPlayerHead(String playerName, String displayName, List<String> lore) {
        try {
            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();

            if (meta != null) {
                Player onlinePlayer = Bukkit.getPlayer(playerName);
                if (onlinePlayer != null) {
                    // Если игрок онлайн, используем его как владельца головы
                    meta.setOwningPlayer(onlinePlayer);
                } else {
                    // Если игрок не в сети, используем UUID и профиль
                    try {
                        // В более новых версиях Bukkit/Spigot можно использовать UUID напрямую
                        // Но для обратной совместимости создаем голову без установки владельца
                        // Можно было бы использовать UUIDFetcher для асинхронного получения UUID по
                        // имени,
                        // но этот API может быть нестабильным
                        plugin.getLogger().info("Создание головы для оффлайн игрока: " + playerName);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Ошибка при получении головы игрока: " + e.getMessage());
                        // Просто продолжаем без установки владельца головы
                    }
                }

                meta.setDisplayName(displayName);
                if (lore != null) {
                    meta.setLore(lore);
                }
                item.setItemMeta(meta);
            }

            return item;
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при создании головы игрока: " + e.getMessage());
            e.printStackTrace();
            return createItem(Material.PLAYER_HEAD, displayName, lore);
        }
    }

    /**
     * Заполняет пустые слоты в инвентаре
     * 
     * @param inventory Инвентарь
     */
    private void fillEmptySlots(Inventory inventory) {
        try {
            ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);

            for (int i = 0; i < inventory.getSize(); i++) {
                if (inventory.getItem(i) == null) {
                    inventory.setItem(i, filler);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при заполнении пустых слотов в инвентаре: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Обрабатывает клик в меню хранилища гильдии
     * 
     * @param player Игрок
     * @param slot   Слот
     * @return true, если клик обработан
     */
    private boolean handleStorageMenuClick(Player player, int slot) {
        try {
            String guildId = getGuildIdString(player);
            if (guildId == null || guildId.isEmpty()) {
                MessageUtil.sendMessage(player, "§cВы не состоите в гильдии!");
                return false;
            }

            openGuildStorage(player, UUID.fromString(guildId));
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при обработке клика в меню хранилища: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Открывает хранилище гильдии для игрока
     * 
     * @param player  Игрок
     * @param guildId ID гильдии
     */
    private void openGuildStorage(Player player, UUID guildId) {
        try {
            plugin.getLogger().info("Открытие хранилища гильдии для игрока " + player.getName());
            if (guildId == null) {
                player.sendMessage("§cОшибка: ID гильдии не может быть null");
                return;
            }

            // Сохраняем ID гильдии в контейнере данных игрока
            player.getPersistentDataContainer().set(
                    ru.snsocialmedia.spigot.utils.PersistentDataKeys.CURRENT_GUILD,
                    org.bukkit.persistence.PersistentDataType.STRING,
                    guildId.toString());

            plugin.getGuildStorageMenu().openMenu(player, guildId);
        } catch (Exception e) {
            player.sendMessage("§cПроизошла ошибка при открытии хранилища гильдии: " + e.getMessage());
            plugin.getLogger().severe(
                    "Ошибка при открытии хранилища гильдии для игрока " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Получает идентификатор гильдии игрока в виде строки
     * 
     * @param player Игрок
     * @return Идентификатор гильдии или null, если не найден
     */
    private String getGuildIdString(Player player) {
        try {
            // Логируем начало попытки получения ID гильдии
            plugin.getLogger().info("Попытка получить ID гильдии для игрока " + player.getName());

            // Сначала пытаемся получить через менеджер гильдий
            ru.snsocialmedia.common.models.guild.Guild guild = plugin.getGuildManager()
                    .getPlayerGuild(player.getUniqueId());
            if (guild != null) {
                String guildId = guild.getId().toString();
                plugin.getLogger().info("Найдена гильдия через GuildManager: " + guild.getName() + ", ID: " + guildId);

                // Сохраняем ID гильдии в ключах игрока для будущего использования
                player.getPersistentDataContainer().set(
                        ru.snsocialmedia.spigot.utils.PersistentDataKeys.CURRENT_GUILD,
                        org.bukkit.persistence.PersistentDataType.STRING,
                        guildId);

                plugin.getLogger().info("ID гильдии сохранен в PersistentDataContainer игрока");
                return guildId;
            } else {
                plugin.getLogger().info("Гильдия через GuildManager не найдена для игрока " + player.getName());
            }

            // Если не нашли через менеджер, пытаемся получить из данных игрока
            if (player.getPersistentDataContainer().has(
                    ru.snsocialmedia.spigot.utils.PersistentDataKeys.CURRENT_GUILD,
                    org.bukkit.persistence.PersistentDataType.STRING)) {

                String guildId = player.getPersistentDataContainer().get(
                        ru.snsocialmedia.spigot.utils.PersistentDataKeys.CURRENT_GUILD,
                        org.bukkit.persistence.PersistentDataType.STRING);

                plugin.getLogger().info("Найден ID гильдии в CURRENT_GUILD: " + guildId);
                return guildId;
            } else {
                plugin.getLogger().info("ID гильдии не найден в CURRENT_GUILD для игрока " + player.getName());
            }

            // Если не нашли гильдию, пробуем использовать alternateGuildIdKey
            if (player.getPersistentDataContainer().has(
                    plugin.getGuildIdKey(),
                    org.bukkit.persistence.PersistentDataType.STRING)) {

                String guildId = player.getPersistentDataContainer().get(
                        plugin.getGuildIdKey(),
                        org.bukkit.persistence.PersistentDataType.STRING);

                plugin.getLogger().info("Найден ID гильдии в guild_id: " + guildId);

                // Дублируем значение в CURRENT_GUILD для совместимости
                player.getPersistentDataContainer().set(
                        ru.snsocialmedia.spigot.utils.PersistentDataKeys.CURRENT_GUILD,
                        org.bukkit.persistence.PersistentDataType.STRING,
                        guildId);

                return guildId;
            } else {
                plugin.getLogger().info("ID гильдии не найден в guild_id для игрока " + player.getName());
            }

            plugin.getLogger()
                    .warning("Не удалось найти ID гильдии для игрока " + player.getName() + " ни одним из способов");
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при получении ID гильдии: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
}