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
 * Меню для снятия предметов из хранилища гильдии
 */
public class WithdrawItemMenu {

    private final SNSocialMediaSpigot plugin;
    private static final String MENU_TITLE = "§c§lСнятие предметов";
    private static final int MENU_SIZE = 54; // 6 рядов по 9 слотов
    private static final int CONFIRM_SLOT = 49;
    private static final int CANCEL_SLOT = 45;
    private static final int INFO_SLOT = 4;

    // Слоты для отображения предметов гильдии
    private static final int ITEM_START_SLOT = 9;
    private static final int ITEM_END_SLOT = 44;

    // Карта выбранных предметов для снятия: слот -> количество
    private final Map<Player, Map<Integer, Integer>> selectedItems = new HashMap<>();

    /**
     * Конструктор
     *
     * @param plugin Экземпляр плагина
     */
    public WithdrawItemMenu(SNSocialMediaSpigot plugin) {
        this.plugin = plugin;
    }

    /**
     * Открывает меню снятия предметов для игрока
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

                // Проверяем права игрока
                GuildMember member = guild.getMember(player.getUniqueId());
                if (member == null) {
                    MessageUtil.sendErrorMessage(player, "Вы не состоите в этой гильдии!");
                    return;
                }

                // Проверяем, имеет ли игрок право на снятие предметов
                if (!canWithdrawItems(member.getRole())) {
                    MessageUtil.sendErrorMessage(player, "У вас нет прав на снятие предметов из хранилища гильдии!");
                    return;
                }

                // Создаем и открываем инвентарь
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // Инициализируем карту выбранных предметов
                    selectedItems.put(player, new HashMap<>());

                    // Создаем и открываем инвентарь
                    Inventory inventory = createInventory(guild, storage);
                    player.openInventory(inventory);
                });
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка при открытии меню снятия предметов", e);
                MessageUtil.sendErrorMessage(player, "Произошла ошибка при открытии меню снятия предметов!");
            }
        });
    }

    /**
     * Создает инвентарь меню снятия предметов
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
                "§7Предметов в хранилище: §f" + storage.getItems().size(),
                "§7Нажмите на предмет, чтобы выбрать его",
                "§7для снятия из хранилища гильдии");
        inventory.setItem(INFO_SLOT, infoItem);

        // Кнопка отмены
        ItemStack cancelItem = createGuiItem(Material.BARRIER, "§c§lОтмена",
                "§7Нажмите, чтобы закрыть меню",
                "§7без снятия предметов");
        inventory.setItem(CANCEL_SLOT, cancelItem);

        // Кнопка подтверждения
        ItemStack confirmItem = createGuiItem(Material.LIME_STAINED_GLASS_PANE, "§a§lПодтвердить",
                "§7Нажмите, чтобы снять выбранные",
                "§7предметы из хранилища гильдии");
        inventory.setItem(CONFIRM_SLOT, confirmItem);

        // Отображаем предметы из хранилища
        int slot = ITEM_START_SLOT;
        for (Map.Entry<String, Integer> entry : storage.getItems().entrySet()) {
            if (slot > ITEM_END_SLOT)
                break; // Если слоты закончились

            String itemType = entry.getKey();
            int amount = entry.getValue();

            try {
                Material material = Material.valueOf(itemType);
                ItemStack item = new ItemStack(material, 1);
                ItemMeta meta = item.getItemMeta();

                meta.setDisplayName("§f" + formatMaterialName(material.name()));

                List<String> lore = new ArrayList<>();
                lore.add("§7Количество: §f" + amount);
                lore.add("§7Левый клик - выбрать 1 шт.");
                lore.add("§7Правый клик - выбрать 8 шт.");
                lore.add("§7Шифт + клик - выбрать 64 шт.");
                meta.setLore(lore);

                item.setItemMeta(meta);
                inventory.setItem(slot, item);
                slot++;
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Неизвестный материал в хранилище: " + itemType);
            }
        }

        return inventory;
    }

    /**
     * Обрабатывает клик по элементу меню
     *
     * @param player       Игрок
     * @param slot         Слот
     * @param guildId      ID гильдии
     * @param isRightClick Правый ли клик
     * @param isShiftClick Зажат ли шифт
     */
    public void handleClick(Player player, int slot, UUID guildId, boolean isRightClick, boolean isShiftClick) {
        try {
            // Проверяем, что инвентарь открыт
            if (player.getOpenInventory() == null || !MENU_TITLE.equals(player.getOpenInventory().getTitle())) {
                return;
            }

            // Обрабатываем клик по кнопке отмены
            if (slot == CANCEL_SLOT) {
                player.closeInventory();
                selectedItems.remove(player);
                return;
            }

            // Обрабатываем клик по кнопке подтверждения
            if (slot == CONFIRM_SLOT) {
                withdrawItems(player, guildId);
                return;
            }

            // Обрабатываем клик по предмету
            if (slot >= ITEM_START_SLOT && slot <= ITEM_END_SLOT) {
                toggleItemSelection(player, slot, isRightClick, isShiftClick);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при обработке клика в меню снятия предметов", e);
            MessageUtil.sendErrorMessage(player, "Произошла ошибка при обработке клика!");
        }
    }

    /**
     * Переключает выбор предмета в слоте
     *
     * @param player       Игрок
     * @param slot         Слот
     * @param isRightClick Правый ли клик
     * @param isShiftClick Зажат ли шифт
     */
    private void toggleItemSelection(Player player, int slot, boolean isRightClick, boolean isShiftClick) {
        try {
            // Проверяем инвентарь
            if (player.getOpenInventory() == null || !MENU_TITLE.equals(player.getOpenInventory().getTitle())) {
                return;
            }

            Inventory inventory = player.getOpenInventory().getTopInventory();
            ItemStack item = inventory.getItem(slot);

            // Проверяем, что в слоте есть предмет
            if (item == null || item.getType() == Material.AIR) {
                return;
            }

            // Получаем тип предмета
            Material material = item.getType();
            String itemType = material.name();

            // Получаем хранилище и проверяем наличие предмета
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    Guild guild = plugin.getGuildManager().getGuild(player.getUniqueId());
                    if (guild == null) {
                        MessageUtil.sendErrorMessage(player, "Гильдия не найдена!");
                        return;
                    }

                    GuildStorage storage = plugin.getGuildStorageManager().getStorage(guild);
                    if (storage == null) {
                        MessageUtil.sendErrorMessage(player, "Хранилище гильдии не найдено!");
                        return;
                    }

                    // Проверяем наличие предмета в хранилище
                    int availableAmount = storage.getItemAmount(itemType);
                    if (availableAmount <= 0) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            MessageUtil.sendErrorMessage(player, "Этого предмета больше нет в хранилище!");
                            player.closeInventory();
                        });
                        return;
                    }

                    // Определяем количество для выбора в зависимости от типа клика
                    int amountToToggle = 1; // левый клик
                    if (isRightClick) {
                        amountToToggle = 8; // правый клик
                    }
                    if (isShiftClick) {
                        amountToToggle = 64; // шифт + клик
                    }

                    // Проверяем, выбран ли уже этот предмет
                    Map<Integer, Integer> playerSelectedItems = selectedItems.getOrDefault(player, new HashMap<>());
                    int currentSelectedAmount = playerSelectedItems.getOrDefault(slot, 0);

                    // Если предмет уже выбран, снимаем выбор
                    if (currentSelectedAmount > 0) {
                        playerSelectedItems.remove(slot);
                    } else {
                        // Иначе выбираем предмет, учитывая доступное количество
                        amountToToggle = Math.min(amountToToggle, availableAmount);
                        playerSelectedItems.put(slot, amountToToggle);
                    }

                    // Обновляем карту выбранных предметов
                    selectedItems.put(player, playerSelectedItems);

                    // Обновляем отображение в инвентаре
                    int finalAmountToToggle = amountToToggle;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        // Создаем новый ItemStack с обновленной информацией
                        ItemStack updatedItem = item.clone();
                        ItemMeta meta = updatedItem.getItemMeta();

                        List<String> lore = new ArrayList<>();
                        lore.add("§7Количество: §f" + availableAmount);

                        if (currentSelectedAmount > 0) {
                            // Снимаем выбор
                            lore.add("§7Левый клик - выбрать 1 шт.");
                            lore.add("§7Правый клик - выбрать 8 шт.");
                            lore.add("§7Шифт + клик - выбрать 64 шт.");
                        } else {
                            // Выбираем предмет
                            lore.add("§a✓ Выбрано: §f" + finalAmountToToggle + " шт.");
                            lore.add("§7Нажмите снова, чтобы отменить выбор");
                        }

                        meta.setLore(lore);
                        updatedItem.setItemMeta(meta);

                        // Обновляем предмет в инвентаре
                        inventory.setItem(slot, updatedItem);
                    });
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Ошибка при обработке выбора предмета", e);
                }
            });
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при переключении выбора предмета", e);
        }
    }

    /**
     * Снимает предметы из хранилища гильдии
     *
     * @param player  Игрок
     * @param guildId ID гильдии
     */
    private void withdrawItems(Player player, UUID guildId) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Проверяем инвентарь
                if (player.getOpenInventory() == null || !MENU_TITLE.equals(player.getOpenInventory().getTitle())) {
                    return;
                }

                Inventory inventory = player.getOpenInventory().getTopInventory();

                // Получаем выбранные предметы
                Map<Integer, Integer> playerSelectedItems = selectedItems.getOrDefault(player, new HashMap<>());
                if (playerSelectedItems.isEmpty()) {
                    MessageUtil.sendWarningMessage(player, "Вы не выбрали ни одного предмета для снятия!");
                    return;
                }

                // Получаем гильдию и хранилище
                Guild guild = plugin.getGuildManager().getGuild(guildId);
                if (guild == null) {
                    MessageUtil.sendErrorMessage(player, "Гильдия не найдена!");
                    return;
                }

                GuildStorage storage = plugin.getGuildStorageManager().getStorage(guild);
                if (storage == null) {
                    MessageUtil.sendErrorMessage(player, "Хранилище гильдии не найдено!");
                    return;
                }

                // Проверяем наличие места в инвентаре игрока
                int requiredSlots = 0;
                Map<String, Integer> itemsToWithdraw = new HashMap<>();

                for (Map.Entry<Integer, Integer> entry : playerSelectedItems.entrySet()) {
                    int slot = entry.getKey();
                    int amount = entry.getValue();

                    ItemStack item = inventory.getItem(slot);
                    if (item == null || item.getType() == Material.AIR) {
                        continue;
                    }

                    String itemType = item.getType().name();

                    // Проверяем, достаточно ли предметов в хранилище
                    int availableAmount = storage.getItemAmount(itemType);
                    if (availableAmount < amount) {
                        MessageUtil.sendErrorMessage(player, "Недостаточно предметов в хранилище: " +
                                formatMaterialName(itemType) + " (доступно: " + availableAmount + ", требуется: "
                                + amount + ")");
                        return;
                    }

                    itemsToWithdraw.put(itemType, amount);
                    requiredSlots++;
                }

                if (itemsToWithdraw.isEmpty()) {
                    MessageUtil.sendWarningMessage(player, "Вы не выбрали ни одного предмета для снятия!");
                    return;
                }

                // Проверяем наличие свободных слотов в инвентаре игрока
                int freeSlots = 0;
                for (ItemStack item : player.getInventory().getStorageContents()) {
                    if (item == null || item.getType() == Material.AIR) {
                        freeSlots++;
                    }
                }

                if (freeSlots < requiredSlots) {
                    MessageUtil.sendErrorMessage(player,
                            "В вашем инвентаре недостаточно места! Требуется слотов: " + requiredSlots);
                    return;
                }

                // Снимаем предметы из хранилища и добавляем их в инвентарь игрока
                boolean success = true;
                for (Map.Entry<String, Integer> entry : itemsToWithdraw.entrySet()) {
                    String itemType = entry.getKey();
                    int amount = entry.getValue();

                    // Снимаем предмет из хранилища
                    if (!storage.removeItem(itemType, amount)) {
                        success = false;
                        break;
                    }

                    // Добавляем предмет в инвентарь игрока
                    Material material = Material.valueOf(itemType);
                    ItemStack itemToAdd = new ItemStack(material, amount);

                    // Добавление предмета в инвентарь игрока должно выполняться в основном потоке
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.getInventory().addItem(itemToAdd);
                    });
                }

                // Сохраняем изменения в хранилище
                if (success) {
                    plugin.getGuildStorageManager().saveStorage(storage);

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        selectedItems.remove(player);
                        MessageUtil.sendSuccessMessage(player, "Предметы успешно сняты из хранилища гильдии!");
                        player.closeInventory();
                    });
                } else {
                    MessageUtil.sendErrorMessage(player, "Не удалось снять предметы из хранилища гильдии!");
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка при снятии предметов из хранилища", e);
                MessageUtil.sendErrorMessage(player, "Произошла ошибка при снятии предметов из хранилища!");
            }
        });
    }

    /**
     * Обрабатывает закрытие инвентаря
     *
     * @param player Игрок
     */
    public void handleClose(Player player) {
        selectedItems.remove(player);
    }

    /**
     * Проверяет, может ли игрок с указанной ролью снимать предметы
     *
     * @param role Роль игрока
     * @return true, если может снимать предметы
     */
    private boolean canWithdrawItems(GuildRole role) {
        return role == GuildRole.LEADER || role == GuildRole.OFFICER;
    }

    /**
     * Форматирует название материала
     *
     * @param materialName Название материала
     * @return Отформатированное название
     */
    private String formatMaterialName(String materialName) {
        String[] words = materialName.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return result.toString().trim();
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