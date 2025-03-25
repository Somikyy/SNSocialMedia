package ru.snsocialmedia.spigot.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
 * Меню хранилища гильдии
 */
public class GuildStorageMenu {

    private final SNSocialMediaSpigot plugin;
    private final DepositItemMenu depositItemMenu;
    private final WithdrawItemMenu withdrawItemMenu;
    private final DepositMoneyMenu depositMoneyMenu;
    private final WithdrawMoneyMenu withdrawMoneyMenu;
    private final UpgradeStorageMenu upgradeStorageMenu;

    private static final String MENU_TITLE = "§6§lХранилище гильдии";
    private static final int MENU_SIZE = 54; // 6 рядов по 9 слотов
    private static final int DEPOSIT_ITEM_SLOT = 45;
    private static final int DEPOSIT_MONEY_SLOT = 46;
    private static final int WITHDRAW_ITEM_SLOT = 48;
    private static final int WITHDRAW_MONEY_SLOT = 49;
    private static final int UPGRADE_SLOT = 50;
    private static final int INFO_SLOT = 53;

    // Слоты для предметов хранилища
    private static final int STORAGE_START_SLOT = 0;
    private static final int STORAGE_END_SLOT = 44;

    /**
     * Конструктор
     *
     * @param plugin Экземпляр плагина
     */
    public GuildStorageMenu(SNSocialMediaSpigot plugin) {
        this.plugin = plugin;
        this.depositItemMenu = new DepositItemMenu(plugin);
        this.withdrawItemMenu = new WithdrawItemMenu(plugin);
        this.depositMoneyMenu = new DepositMoneyMenu(plugin);
        this.withdrawMoneyMenu = new WithdrawMoneyMenu(plugin);
        this.upgradeStorageMenu = new UpgradeStorageMenu(plugin);
    }

    /**
     * Открывает меню хранилища гильдии для игрока
     *
     * @param player  Игрок
     * @param guildId ID гильдии
     */
    public void openMenu(Player player, UUID guildId) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Получаем гильдию и хранилище
                Guild guild = plugin.getGuildManager().getGuild(guildId);
                if (guild == null) {
                    MessageUtil.sendMessage(player, "§cГильдия не найдена!");
                    return;
                }

                GuildStorage storage = plugin.getGuildStorageManager().getStorage(guild);
                if (storage == null) {
                    MessageUtil.sendMessage(player, "§cХранилище гильдии не найдено!");
                    return;
                }

                // Проверяем, является ли игрок членом гильдии
                GuildMember member = guild.getMember(player.getUniqueId());
                if (member == null) {
                    MessageUtil.sendMessage(player, "§cВы не состоите в этой гильдии!");
                    return;
                }

                // Создаем инвентарь
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Inventory inventory = createInventory(guild, storage, member.getRole());
                    player.openInventory(inventory);
                });
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка при открытии меню хранилища гильдии", e);
                MessageUtil.sendMessage(player, "§cПроизошла ошибка при открытии меню хранилища гильдии!");
            }
        });
    }

    /**
     * Создает инвентарь хранилища гильдии
     *
     * @param guild   Гильдия
     * @param storage Хранилище гильдии
     * @param role    Роль игрока в гильдии
     * @return Инвентарь
     */
    private Inventory createInventory(Guild guild, GuildStorage storage, GuildRole role) {
        Inventory inventory = Bukkit.createInventory(null, MENU_SIZE, MENU_TITLE);

        // Добавляем предметы из хранилища
        Map<String, Integer> items = storage.getItems();
        int slot = STORAGE_START_SLOT;
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            if (slot > STORAGE_END_SLOT) {
                break;
            }

            String itemType = entry.getKey();
            int amount = entry.getValue();

            // Преобразуем строковый тип предмета в Material
            Material material;
            try {
                material = Material.valueOf(itemType);
            } catch (IllegalArgumentException e) {
                material = Material.STONE; // Значение по умолчанию, если тип предмета не найден
            }

            ItemStack itemStack = new ItemStack(material);
            ItemMeta meta = itemStack.getItemMeta();
            meta.setDisplayName("§e" + itemType);

            List<String> lore = new ArrayList<>();
            lore.add("§7Количество: §f" + amount);
            meta.setLore(lore);

            itemStack.setItemMeta(meta);
            inventory.setItem(slot, itemStack);

            slot++;
        }

        // Добавляем кнопки управления
        // Кнопка для внесения предметов
        if (canDepositItems(role)) {
            inventory.setItem(DEPOSIT_ITEM_SLOT, createGuiItem(Material.CHEST,
                    "§a§lВнести предметы",
                    "§7Нажмите, чтобы внести предметы",
                    "§7в хранилище гильдии"));
        }

        // Кнопка для внесения денег
        if (canDepositMoney(role)) {
            inventory.setItem(DEPOSIT_MONEY_SLOT, createGuiItem(Material.GOLD_INGOT,
                    "§a§lВнести деньги",
                    "§7Нажмите, чтобы внести деньги",
                    "§7в хранилище гильдии"));
        }

        // Кнопка для изъятия предметов
        if (canWithdrawItems(role)) {
            inventory.setItem(WITHDRAW_ITEM_SLOT, createGuiItem(Material.HOPPER,
                    "§c§lИзъять предметы",
                    "§7Нажмите, чтобы изъять предметы",
                    "§7из хранилища гильдии"));
        }

        // Кнопка для изъятия денег
        if (canWithdrawMoney(role)) {
            inventory.setItem(WITHDRAW_MONEY_SLOT, createGuiItem(Material.DIAMOND,
                    "§c§lИзъять деньги",
                    "§7Нажмите, чтобы изъять деньги",
                    "§7из хранилища гильдии"));
        }

        // Кнопка для улучшения хранилища
        if (canUpgradeStorage(role)) {
            inventory.setItem(UPGRADE_SLOT, createGuiItem(Material.ANVIL,
                    "§b§lУлучшить хранилище",
                    "§7Нажмите, чтобы улучшить хранилище",
                    "§7и увеличить количество слотов",
                    "§7Стоимость: §e10000 монет"));
        }

        // Информация о хранилище
        inventory.setItem(INFO_SLOT, createGuiItem(Material.PAPER,
                "§f§lИнформация о хранилище",
                "§7Гильдия: §f" + guild.getName(),
                "§7Слотов: §f" + storage.getItems().size() + "§7/§f" + storage.getMaxSlots(),
                "§7Свободно: §f" + storage.getFreeSlots() + " §7слотов",
                "§7Баланс: §f" + storage.getMoney() + " §7монет"));

        return inventory;
    }

    /**
     * Создает предмет для GUI
     *
     * @param material Материал
     * @param name     Название
     * @param lore     Описание
     * @return Предмет
     */
    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        if (lore != null && lore.length > 0) {
            meta.setLore(Arrays.asList(lore));
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Проверяет, может ли игрок с указанной ролью вносить предметы в хранилище
     *
     * @param role Роль
     * @return true, если может
     */
    private boolean canDepositItems(GuildRole role) {
        return role != GuildRole.MEMBER;
    }

    /**
     * Проверяет, может ли игрок с указанной ролью вносить деньги в хранилище
     *
     * @param role Роль
     * @return true, если может
     */
    private boolean canDepositMoney(GuildRole role) {
        return role != GuildRole.MEMBER;
    }

    /**
     * Проверяет, может ли игрок с указанной ролью изымать предметы из хранилища
     *
     * @param role Роль
     * @return true, если может
     */
    private boolean canWithdrawItems(GuildRole role) {
        return role == GuildRole.LEADER || role == GuildRole.OFFICER;
    }

    /**
     * Проверяет, может ли игрок с указанной ролью изымать деньги из хранилища
     *
     * @param role Роль
     * @return true, если может
     */
    private boolean canWithdrawMoney(GuildRole role) {
        return role == GuildRole.LEADER;
    }

    /**
     * Проверяет, может ли игрок с указанной ролью улучшать хранилище
     *
     * @param role Роль
     * @return true, если может
     */
    private boolean canUpgradeStorage(GuildRole role) {
        return role == GuildRole.LEADER;
    }

    /**
     * Обрабатывает клик по элементу меню
     *
     * @param player  Игрок
     * @param slot    Слот
     * @param guildId ID гильдии
     */
    public void handleClick(Player player, int slot, UUID guildId) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Получаем гильдию и хранилище
                Guild guild = plugin.getGuildManager().getGuild(guildId);
                if (guild == null) {
                    MessageUtil.sendMessage(player, "§cГильдия не найдена!");
                    return;
                }

                GuildStorage storage = plugin.getGuildStorageManager().getStorage(guild);
                if (storage == null) {
                    MessageUtil.sendMessage(player, "§cХранилище гильдии не найдено!");
                    return;
                }

                // Проверяем, является ли игрок членом гильдии
                GuildMember member = guild.getMember(player.getUniqueId());
                if (member == null) {
                    MessageUtil.sendMessage(player, "§cВы не состоите в этой гильдии!");
                    return;
                }

                GuildRole role = member.getRole();

                // Обрабатываем клик по слоту
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (slot == DEPOSIT_ITEM_SLOT && canDepositItems(role)) {
                        openDepositItemMenu(player, guildId);
                    } else if (slot == DEPOSIT_MONEY_SLOT && canDepositMoney(role)) {
                        openDepositMoneyMenu(player, guildId);
                    } else if (slot == WITHDRAW_ITEM_SLOT && canWithdrawItems(role)) {
                        openWithdrawItemMenu(player, guildId);
                    } else if (slot == WITHDRAW_MONEY_SLOT && canWithdrawMoney(role)) {
                        openWithdrawMoneyMenu(player, guildId);
                    } else if (slot == UPGRADE_SLOT && canUpgradeStorage(role)) {
                        upgradeStorage(player, guildId);
                    } else if (slot >= STORAGE_START_SLOT && slot <= STORAGE_END_SLOT) {
                        // Клик по предмету хранилища - пока ничего не делаем
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка при обработке клика в меню хранилища гильдии", e);
                MessageUtil.sendMessage(player, "§cПроизошла ошибка при обработке клика!");
            }
        });
    }

    /**
     * Открывает меню для внесения предметов
     *
     * @param player  Игрок
     * @param guildId ID гильдии
     */
    public void openDepositItemMenu(Player player, UUID guildId) {
        depositItemMenu.openMenu(player, guildId);
    }

    /**
     * Открывает меню для внесения денег
     *
     * @param player  Игрок
     * @param guildId ID гильдии
     */
    public void openDepositMoneyMenu(Player player, UUID guildId) {
        depositMoneyMenu.openMenu(player, guildId);
    }

    /**
     * Открывает меню для изъятия предметов
     *
     * @param player  Игрок
     * @param guildId ID гильдии
     */
    public void openWithdrawItemMenu(Player player, UUID guildId) {
        withdrawItemMenu.openMenu(player, guildId);
    }

    /**
     * Открывает меню для изъятия денег
     *
     * @param player  Игрок
     * @param guildId ID гильдии
     */
    private void openWithdrawMoneyMenu(Player player, UUID guildId) {
        withdrawMoneyMenu.openMenu(player, guildId);
    }

    /**
     * Улучшает хранилище гильдии
     *
     * @param player  Игрок
     * @param guildId ID гильдии
     */
    private void upgradeStorage(Player player, UUID guildId) {
        upgradeStorageMenu.openMenu(player, guildId);
    }
}