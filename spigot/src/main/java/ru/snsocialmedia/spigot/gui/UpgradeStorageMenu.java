package ru.snsocialmedia.spigot.gui;

import java.util.ArrayList;
import java.util.List;
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
 * Меню улучшения хранилища гильдии
 */
public class UpgradeStorageMenu {

    private final SNSocialMediaSpigot plugin;
    private static final String MENU_TITLE = "§5§lУлучшение хранилища";
    private static final int MENU_SIZE = 36; // 4 ряда по 9 слотов
    private static final int CANCEL_SLOT = 31;
    private static final int INFO_SLOT = 4;

    // Типы улучшений
    public enum UpgradeType {
        SLOTS_SMALL(10, "Маленькое улучшение", 5, 5000, Material.CHEST),
        SLOTS_MEDIUM(11, "Среднее улучшение", 10, 10000, Material.IRON_BLOCK),
        SLOTS_LARGE(12, "Большое улучшение", 20, 20000, Material.GOLD_BLOCK),
        SLOTS_HUGE(13, "Огромное улучшение", 30, 30000, Material.DIAMOND_BLOCK),
        SLOTS_ULTIMATE(14, "Максимальное улучшение", 50, 50000, Material.NETHERITE_BLOCK),

        TAX_REDUCTION(19, "Снижение налогов", 0, 15000, Material.GOLD_INGOT),
        INTEREST_INCREASE(20, "Увеличение процентов", 0, 25000, Material.EMERALD),
        SECURITY_UPGRADE(21, "Улучшение безопасности", 0, 20000, Material.IRON_DOOR),
        REMOTE_ACCESS(22, "Удаленный доступ", 0, 35000, Material.ENDER_CHEST);

        private final int slot;
        private final String name;
        private final int slots;
        private final int cost;
        private final Material material;

        UpgradeType(int slot, String name, int slots, int cost, Material material) {
            this.slot = slot;
            this.name = name;
            this.slots = slots;
            this.cost = cost;
            this.material = material;
        }

        public int getSlot() {
            return slot;
        }

        public String getName() {
            return name;
        }

        public int getSlots() {
            return slots;
        }

        public int getCost() {
            return cost;
        }

        public Material getMaterial() {
            return material;
        }
    }

    /**
     * Конструктор
     *
     * @param plugin Экземпляр плагина
     */
    public UpgradeStorageMenu(SNSocialMediaSpigot plugin) {
        this.plugin = plugin;
    }

    /**
     * Открывает меню улучшения хранилища для игрока
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

                // Проверяем, имеет ли игрок право на улучшение хранилища
                if (!canUpgradeStorage(member.getRole())) {
                    MessageUtil.sendErrorMessage(player, "У вас нет прав на улучшение хранилища гильдии!");
                    return;
                }

                // Создаем и открываем инвентарь
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Inventory inventory = createInventory(guild, storage);
                    player.openInventory(inventory);
                });
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка при открытии меню улучшения хранилища", e);
                MessageUtil.sendErrorMessage(player, "Произошла ошибка при открытии меню улучшения хранилища!");
            }
        });
    }

    /**
     * Создает инвентарь меню улучшения хранилища
     *
     * @param guild   Гильдия
     * @param storage Хранилище гильдии
     * @return Инвентарь
     */
    private Inventory createInventory(Guild guild, GuildStorage storage) {
        Inventory inventory = Bukkit.createInventory(null, MENU_SIZE, MENU_TITLE);

        // Информация о хранилище
        ItemStack infoItem = createGuiItem(Material.PAPER, "§f§lИнформация о хранилище",
                "§7Гильдия: §f" + guild.getName(),
                "§7Слотов: §f" + storage.getItems().size() + "§7/§f" + storage.getMaxSlots(),
                "§7Свободно: §f" + storage.getFreeSlots() + " §7слотов",
                "§7Баланс: §f" + storage.getMoney() + " §7монет",
                "",
                "§7Выберите улучшение для покупки");
        inventory.setItem(INFO_SLOT, infoItem);

        // Кнопка отмены
        ItemStack cancelItem = createGuiItem(Material.BARRIER, "§c§lНазад",
                "§7Нажмите, чтобы вернуться",
                "§7в меню хранилища гильдии");
        inventory.setItem(CANCEL_SLOT, cancelItem);

        // Добавляем все доступные улучшения
        for (UpgradeType upgradeType : UpgradeType.values()) {
            boolean canAfford = storage.getMoney() >= upgradeType.getCost();
            boolean isFeatureEnabled = isFeatureEnabled(upgradeType);
            boolean isSlotUpgrade = upgradeType.getSlots() > 0;

            // Пропускаем недоступные функции
            if (!isFeatureEnabled && !isSlotUpgrade) {
                // Создаем заблокированный предмет
                ItemStack item = createGuiItem(Material.BARRIER, "§c§l" + upgradeType.getName() + " §8(скоро)",
                        "§7Эта функция пока недоступна",
                        "§7и появится в следующих обновлениях");
                inventory.setItem(upgradeType.getSlot(), item);
                continue;
            }

            // Создаем предмет улучшения
            String itemName = canAfford ? "§a§l" + upgradeType.getName() : "§c§l" + upgradeType.getName();
            List<String> lore = new ArrayList<>();

            if (isSlotUpgrade) {
                lore.add("§7Увеличивает вместимость хранилища");
                lore.add("§7на §f" + upgradeType.getSlots() + " §7слотов");
            } else {
                switch (upgradeType) {
                    case TAX_REDUCTION:
                        lore.add("§7Снижает налоги на транзакции");
                        lore.add("§7в хранилище гильдии на §f10%");
                        break;
                    case INTEREST_INCREASE:
                        lore.add("§7Увеличивает проценты по вкладам");
                        lore.add("§7в хранилище гильдии на §f5%");
                        break;
                    case SECURITY_UPGRADE:
                        lore.add("§7Улучшает безопасность хранилища");
                        lore.add("§7и добавляет логирование действий");
                        break;
                    case REMOTE_ACCESS:
                        lore.add("§7Позволяет получить доступ к хранилищу");
                        lore.add("§7удаленно через команду §f/guild storage");
                        break;
                }
            }

            lore.add("");
            lore.add("§7Стоимость: §f" + upgradeType.getCost() + " §7монет");

            if (!canAfford) {
                lore.add("§cНедостаточно средств в хранилище!");
                lore.add("§cНеобходимо еще §f" + (upgradeType.getCost() - storage.getMoney()) + " §cмонет");
            } else {
                lore.add("§aНажмите, чтобы приобрести улучшение");
            }

            Material material = canAfford ? upgradeType.getMaterial() : Material.RED_STAINED_GLASS_PANE;
            ItemStack item = createGuiItem(material, itemName, lore.toArray(new String[0]));
            inventory.setItem(upgradeType.getSlot(), item);
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
                plugin.getGuildStorageMenu().openMenu(player, guildId);
                return;
            }

            // Проверяем, был ли клик по улучшению
            UpgradeType upgradeType = getUpgradeTypeBySlot(slot);
            if (upgradeType == null) {
                return;
            }

            // Если это не улучшение слотов и функция недоступна, ничего не делаем
            if (upgradeType.getSlots() == 0 && !isFeatureEnabled(upgradeType)) {
                MessageUtil.sendInfoMessage(player, "Эта функция будет доступна в будущих обновлениях!");
                return;
            }

            // Покупаем улучшение
            purchaseUpgrade(player, guildId, upgradeType);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при обработке клика в меню улучшения хранилища", e);
            MessageUtil.sendErrorMessage(player, "Произошла ошибка при обработке клика!");
        }
    }

    /**
     * Покупает улучшение для хранилища гильдии
     *
     * @param player      Игрок
     * @param guildId     ID гильдии
     * @param upgradeType Тип улучшения
     */
    private void purchaseUpgrade(Player player, UUID guildId, UpgradeType upgradeType) {
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

                // Проверяем наличие денег в хранилище
                if (storage.getMoney() < upgradeType.getCost()) {
                    MessageUtil.sendErrorMessage(player, "Недостаточно средств в хранилище гильдии! Необходимо: "
                            + upgradeType.getCost() + " монет.");
                    player.closeInventory();
                    return;
                }

                // Снимаем деньги из хранилища
                if (!storage.withdrawMoney(upgradeType.getCost())) {
                    MessageUtil.sendErrorMessage(player, "Не удалось снять деньги из хранилища гильдии!");
                    player.closeInventory();
                    return;
                }

                // Применяем улучшение
                boolean success = false;
                if (upgradeType.getSlots() > 0) {
                    // Увеличиваем количество слотов
                    success = plugin.getGuildStorageManager().upgradeStorage(guild, upgradeType.getSlots());
                } else {
                    // Применяем другие улучшения
                    success = applyFeatureUpgrade(guild, storage, upgradeType);
                }

                if (success) {
                    plugin.getGuildStorageManager().saveStorage(storage);
                    MessageUtil.sendSuccessMessage(player, "Вы успешно приобрели улучшение "
                            + upgradeType.getName() + " для хранилища гильдии!");

                    // Повторно открываем меню
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.closeInventory();
                        openMenu(player, guildId);
                    });

                    // Оповещаем членов гильдии
                    notifyGuildMembers(guild, player, upgradeType);
                } else {
                    // Возвращаем деньги, если улучшение не удалось
                    storage.depositMoney(upgradeType.getCost());
                    plugin.getGuildStorageManager().saveStorage(storage);
                    MessageUtil.sendErrorMessage(player, "Не удалось применить улучшение!");
                    player.closeInventory();
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка при покупке улучшения", e);
                MessageUtil.sendErrorMessage(player, "Произошла ошибка при покупке улучшения!");
                player.closeInventory();
            }
        });
    }

    /**
     * Применяет функциональное улучшение к хранилищу гильдии
     *
     * @param guild       Гильдия
     * @param storage     Хранилище гильдии
     * @param upgradeType Тип улучшения
     * @return true, если улучшение успешно применено
     */
    private boolean applyFeatureUpgrade(Guild guild, GuildStorage storage, UpgradeType upgradeType) {
        // Здесь должна быть логика применения различных улучшений
        // В настоящее время все улучшения, кроме слотов, недоступны
        return false;
    }

    /**
     * Оповещает всех членов гильдии о покупке улучшения
     *
     * @param guild       Гильдия
     * @param player      Игрок, купивший улучшение
     * @param upgradeType Тип улучшения
     */
    private void notifyGuildMembers(Guild guild, Player player, UpgradeType upgradeType) {
        // Отправляем сообщение всем онлайн членам гильдии
        for (GuildMember member : guild.getMembers()) {
            Player memberPlayer = Bukkit.getPlayer(member.getPlayerId());
            if (memberPlayer != null && memberPlayer.isOnline() && !memberPlayer.equals(player)) {
                MessageUtil.sendInfoMessage(memberPlayer,
                        "Игрок " + player.getName() + " приобрел улучшение " + upgradeType.getName()
                                + " для хранилища гильдии!");
            }
        }
    }

    /**
     * Проверяет, может ли игрок с указанной ролью улучшать хранилище
     *
     * @param role Роль игрока
     * @return true, если может улучшать хранилище
     */
    private boolean canUpgradeStorage(GuildRole role) {
        return role == GuildRole.LEADER;
    }

    /**
     * Получает тип улучшения по номеру слота
     *
     * @param slot Номер слота
     * @return Тип улучшения или null, если не найден
     */
    private UpgradeType getUpgradeTypeBySlot(int slot) {
        for (UpgradeType upgradeType : UpgradeType.values()) {
            if (upgradeType.getSlot() == slot) {
                return upgradeType;
            }
        }
        return null;
    }

    /**
     * Проверяет, доступна ли функция
     *
     * @param upgradeType Тип улучшения
     * @return true, если функция доступна
     */
    private boolean isFeatureEnabled(UpgradeType upgradeType) {
        // В текущей версии доступны только улучшения слотов
        return upgradeType.getSlots() > 0;
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