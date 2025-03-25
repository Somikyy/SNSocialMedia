package ru.snsocialmedia.spigot.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import ru.snsocialmedia.common.models.guild.Guild;
import ru.snsocialmedia.common.models.guild.GuildMember;
import ru.snsocialmedia.common.models.guild.GuildRole;
import ru.snsocialmedia.common.models.guild.GuildStorage;
import ru.snsocialmedia.spigot.SNSocialMediaSpigot;
import ru.snsocialmedia.spigot.utils.MessageUtil;

/**
 * Меню для внесения предметов в хранилище гильдии
 */
public class DepositItemMenu {

    private final SNSocialMediaSpigot plugin;
    private static final String MENU_TITLE = "§a§lВнесение предметов";
    private static final int MENU_SIZE = 54; // 6 рядов по 9 слотов
    private static final int CONFIRM_SLOT = 49;
    private static final int CANCEL_SLOT = 45;
    private static final int INFO_SLOT = 4;

    // Слоты для размещения предметов
    private static final int DEPOSIT_START_SLOT = 9;
    private static final int DEPOSIT_END_SLOT = 44;

    /**
     * Конструктор
     *
     * @param plugin Экземпляр плагина
     */
    public DepositItemMenu(SNSocialMediaSpigot plugin) {
        this.plugin = plugin;
    }

    /**
     * Открывает меню внесения предметов для игрока
     *
     * @param player  Игрок
     * @param guildId ID гильдии
     */
    public void openMenu(Player player, UUID guildId) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Получаем гильдию
                Guild guild = plugin.getGuildManager().getGuild(guildId);
                if (guild == null) {
                    MessageUtil.sendErrorMessage(player, "Гильдия не найдена!");
                    return;
                }

                // Получаем хранилище гильдии
                GuildStorage storage = plugin.getGuildStorageManager().getStorage(guild);
                if (storage == null) {
                    MessageUtil.sendErrorMessage(player, "Хранилище гильдии не найдено!");
                    return;
                }

                // Проверяем наличие свободных слотов
                if (storage.isFull()) {
                    MessageUtil.sendErrorMessage(player, "Хранилище гильдии заполнено! Нет свободных слотов.");
                    return;
                }

                // Проверяем права игрока
                GuildMember member = guild.getMember(player.getUniqueId());
                if (member == null) {
                    MessageUtil.sendErrorMessage(player, "Вы не состоите в этой гильдии!");
                    return;
                }

                // Проверяем, имеет ли игрок право на внесение предметов
                if (!canDepositItems(member.getRole())) {
                    MessageUtil.sendErrorMessage(player, "У вас нет прав на внесение предметов в хранилище гильдии!");
                    return;
                }

                // Создаем и открываем инвентарь
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Inventory inventory = createInventory(guild, storage);
                    player.openInventory(inventory);
                });
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка при открытии меню внесения предметов", e);
                MessageUtil.sendErrorMessage(player, "Произошла ошибка при открытии меню внесения предметов!");
            }
        });
    }

    /**
     * Создает инвентарь меню внесения предметов
     *
     * @param guild   Гильдия
     * @param storage Хранилище гильдии
     * @return Инвентарь
     */
    private Inventory createInventory(Guild guild, GuildStorage storage) {
        Inventory inventory = Bukkit.createInventory(null, MENU_SIZE, MENU_TITLE);

        // Информация о хранилище
        ItemStack infoItem = createGuiItem(Material.PAPER, "§f§lИнформация",
                "§7Гильдия: §f" + guild.getName(),
                "§7Свободно слотов: §f" + storage.getFreeSlots() + "§7/§f" + storage.getMaxSlots(),
                "§7Разместите предметы на пустых слотах,",
                "§7затем нажмите кнопку подтверждения");
        inventory.setItem(INFO_SLOT, infoItem);

        // Кнопка отмены
        ItemStack cancelItem = createGuiItem(Material.BARRIER, "§c§lОтмена",
                "§7Нажмите, чтобы закрыть меню",
                "§7без внесения предметов");
        inventory.setItem(CANCEL_SLOT, cancelItem);

        // Кнопка подтверждения
        ItemStack confirmItem = createGuiItem(Material.LIME_STAINED_GLASS_PANE, "§a§lПодтвердить",
                "§7Нажмите, чтобы внести размещенные",
                "§7предметы в хранилище");
        inventory.setItem(CONFIRM_SLOT, confirmItem);

        // Заполняем слоты для предметов пустыми панелями с подсказкой
        for (int i = DEPOSIT_START_SLOT; i <= DEPOSIT_END_SLOT; i++) {
            ItemStack emptySlot = createGuiItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§7Разместите предмет",
                    "§7Положите сюда предмет,",
                    "§7который хотите внести",
                    "§7в хранилище гильдии");
            inventory.setItem(i, emptySlot);
        }

        return inventory;
    }

    /**
     * Обрабатывает клик по элементу меню
     *
     * @param player  Игрок
     * @param slot    Слот
     * @param guildId ID гильдии
     */
    public void handleClick(Player player, int slot, UUID guildId) {
        try {
            // Проверяем, что инвентарь открыт
            if (player.getOpenInventory() == null || !MENU_TITLE.equals(player.getOpenInventory().getTitle())) {
                return;
            }

            // Обрабатываем клик по кнопке отмены
            if (slot == CANCEL_SLOT) {
                player.closeInventory();
                return;
            }

            // Обрабатываем клик по кнопке подтверждения
            if (slot == CONFIRM_SLOT) {
                depositItems(player, guildId);
                return;
            }

            // Разрешаем размещать предметы в слотах для депозита
            if (slot >= DEPOSIT_START_SLOT && slot <= DEPOSIT_END_SLOT) {
                // Специальную обработку размещения не делаем - Minecraft сам разместит предметы
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при обработке клика в меню внесения предметов", e);
            MessageUtil.sendErrorMessage(player, "Произошла ошибка при обработке клика!");
        }
    }

    /**
     * Вносит предметы в хранилище
     *
     * @param player  Игрок
     * @param guildId ID гильдии
     */
    private void depositItems(Player player, UUID guildId) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Получаем гильдию
                Guild guild = plugin.getGuildManager().getGuild(guildId);
                if (guild == null) {
                    MessageUtil.sendErrorMessage(player, "Гильдия не найдена!");
                    return;
                }

                // Получаем хранилище гильдии
                GuildStorage storage = plugin.getGuildStorageManager().getStorage(guild);
                if (storage == null) {
                    MessageUtil.sendErrorMessage(player, "Хранилище гильдии не найдено!");
                    return;
                }

                // Проверяем инвентарь
                if (player.getOpenInventory() == null || !MENU_TITLE.equals(player.getOpenInventory().getTitle())) {
                    return;
                }

                Inventory inventory = player.getOpenInventory().getTopInventory();

                // Собираем предметы для внесения
                Map<String, Integer> itemsToDeposit = new HashMap<>();
                int totalItems = 0;

                for (int i = DEPOSIT_START_SLOT; i <= DEPOSIT_END_SLOT; i++) {
                    ItemStack item = inventory.getItem(i);
                    if (item != null && item.getType() != Material.AIR
                            && item.getType() != Material.LIGHT_GRAY_STAINED_GLASS_PANE) {
                        String itemType = item.getType().name();
                        int amount = item.getAmount();

                        if (itemsToDeposit.containsKey(itemType)) {
                            itemsToDeposit.put(itemType, itemsToDeposit.get(itemType) + amount);
                        } else {
                            itemsToDeposit.put(itemType, amount);
                        }

                        totalItems++;
                    }
                }

                if (itemsToDeposit.isEmpty()) {
                    MessageUtil.sendWarningMessage(player, "Вы не разместили ни одного предмета для внесения!");
                    return;
                }

                // Проверяем, хватит ли места в хранилище
                if (totalItems > storage.getFreeSlots()) {
                    MessageUtil.sendErrorMessage(player, "В хранилище недостаточно свободных слотов! Доступно "
                            + storage.getFreeSlots() + " слотов.");
                    return;
                }

                // Вносим предметы в хранилище
                boolean success = true;
                for (Map.Entry<String, Integer> entry : itemsToDeposit.entrySet()) {
                    String itemType = entry.getKey();
                    int amount = entry.getValue();

                    if (!storage.addItem(itemType, amount)) {
                        success = false;
                        break;
                    }
                }

                // Сохраняем изменения в хранилище
                if (success) {
                    plugin.getGuildStorageManager().saveStorage(storage);
                }

                // Очищаем инвентарь и закрываем его
                boolean finalSuccess = success;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // Очищаем слоты с предметами
                    if (finalSuccess) {
                        for (int i = DEPOSIT_START_SLOT; i <= DEPOSIT_END_SLOT; i++) {
                            ItemStack item = inventory.getItem(i);
                            if (item != null && item.getType() != Material.AIR
                                    && item.getType() != Material.LIGHT_GRAY_STAINED_GLASS_PANE) {
                                inventory.setItem(i,
                                        createGuiItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§7Разместите предмет",
                                                "§7Положите сюда предмет,",
                                                "§7который хотите внести",
                                                "§7в хранилище гильдии"));
                            }
                        }

                        MessageUtil.sendSuccessMessage(player, "Предметы успешно внесены в хранилище гильдии!");
                    } else {
                        MessageUtil.sendErrorMessage(player, "Не удалось внести предметы в хранилище гильдии!");
                    }

                    // Закрываем инвентарь с небольшой задержкой
                    Bukkit.getScheduler().runTaskLater(plugin, player::closeInventory, 20L);
                });
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка при внесении предметов в хранилище", e);
                MessageUtil.sendErrorMessage(player, "Произошла ошибка при внесении предметов в хранилище!");
            }
        });
    }

    /**
     * Проверяет, может ли игрок с указанной ролью вносить предметы
     *
     * @param role Роль игрока
     * @return true, если может вносить предметы
     */
    private boolean canDepositItems(GuildRole role) {
        return role != GuildRole.MEMBER;
    }

    /**
     * Создает предмет для интерфейса
     *
     * @param material Материал
     * @param name     Название
     * @param lore     Описание
     * @return Созданный предмет
     */
    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        List<String> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(line);
        }
        meta.setLore(loreList);

        item.setItemMeta(meta);
        return item;
    }
}