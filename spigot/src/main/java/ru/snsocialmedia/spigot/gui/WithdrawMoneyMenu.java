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
 * Меню для снятия денег из хранилища гильдии
 */
public class WithdrawMoneyMenu {

    private final SNSocialMediaSpigot plugin;
    private static final String MENU_TITLE = "§c§lСнятие денег";
    private static final int MENU_SIZE = 27; // 3 ряда по 9 слотов
    private static final int CANCEL_SLOT = 18;
    private static final int INFO_SLOT = 4;

    // Слоты для кнопок с суммами
    private static final int AMOUNT_100_SLOT = 10;
    private static final int AMOUNT_500_SLOT = 11;
    private static final int AMOUNT_1000_SLOT = 12;
    private static final int AMOUNT_5000_SLOT = 14;
    private static final int AMOUNT_10000_SLOT = 15;
    private static final int AMOUNT_CUSTOM_SLOT = 16;

    /**
     * Конструктор
     *
     * @param plugin Экземпляр плагина
     */
    public WithdrawMoneyMenu(SNSocialMediaSpigot plugin) {
        this.plugin = plugin;
    }

    /**
     * Открывает меню снятия денег для игрока
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

                // Проверяем, имеет ли игрок право на снятие денег
                if (!canWithdrawMoney(member.getRole())) {
                    MessageUtil.sendErrorMessage(player, "У вас нет прав на снятие денег из хранилища гильдии!");
                    return;
                }

                // Создаем и открываем инвентарь
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Inventory inventory = createInventory(guild, storage);
                    player.openInventory(inventory);
                });
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка при открытии меню снятия денег", e);
                MessageUtil.sendErrorMessage(player, "Произошла ошибка при открытии меню снятия денег!");
            }
        });
    }

    /**
     * Создает инвентарь меню снятия денег
     *
     * @param guild   Гильдия
     * @param storage Хранилище гильдии
     * @return Инвентарь
     */
    private Inventory createInventory(Guild guild, GuildStorage storage) {
        Inventory inventory = Bukkit.createInventory(null, MENU_SIZE, MENU_TITLE);

        // Информация о хранилище
        double money = storage.getMoney();
        ItemStack infoItem = createGuiItem(Material.PAPER, "§f§lИнформация",
                "§7Гильдия: §f" + guild.getName(),
                "§7Деньги в хранилище: §f" + money + " §7монет",
                "§7Выберите сумму для снятия");
        inventory.setItem(INFO_SLOT, infoItem);

        // Кнопка отмены
        ItemStack cancelItem = createGuiItem(Material.BARRIER, "§c§lОтмена",
                "§7Нажмите, чтобы закрыть меню",
                "§7без снятия денег");
        inventory.setItem(CANCEL_SLOT, cancelItem);

        // Кнопки с суммами
        addAmountButton(inventory, AMOUNT_100_SLOT, Material.GOLD_NUGGET, 100, money);
        addAmountButton(inventory, AMOUNT_500_SLOT, Material.GOLD_INGOT, 500, money);
        addAmountButton(inventory, AMOUNT_1000_SLOT, Material.GOLD_BLOCK, 1000, money);
        addAmountButton(inventory, AMOUNT_5000_SLOT, Material.EMERALD, 5000, money);
        addAmountButton(inventory, AMOUNT_10000_SLOT, Material.EMERALD_BLOCK, 10000, money);

        // Кнопка для своей суммы
        boolean canCustom = money > 0;
        Material customMaterial = canCustom ? Material.WRITABLE_BOOK : Material.BARRIER;
        String customName = canCustom ? "§b§lСвоя сумма" : "§c§lНедоступно";
        List<String> customLore = new ArrayList<>();
        if (canCustom) {
            customLore.add("§7Нажмите, чтобы снять");
            customLore.add("§7произвольную сумму монет");
            customLore.add("§7(введете в чат)");
        } else {
            customLore.add("§cНедостаточно средств");
            customLore.add("§cв хранилище гильдии!");
        }

        ItemStack customItem = createGuiItem(customMaterial, customName, customLore.toArray(new String[0]));
        inventory.setItem(AMOUNT_CUSTOM_SLOT, customItem);

        return inventory;
    }

    /**
     * Добавляет кнопку с суммой в инвентарь
     *
     * @param inventory Инвентарь
     * @param slot      Слот
     * @param material  Материал
     * @param amount    Сумма
     * @param available Доступная сумма в хранилище
     */
    private void addAmountButton(Inventory inventory, int slot, Material material, int amount, double available) {
        boolean canWithdraw = available >= amount;
        Material buttonMaterial = canWithdraw ? material : Material.BARRIER;
        String buttonName = canWithdraw ? "§e§l" + amount + " монет" : "§c§lНедоступно";
        List<String> lore = new ArrayList<>();

        if (canWithdraw) {
            lore.add("§7Нажмите, чтобы снять");
            lore.add("§7" + amount + " монет из хранилища");
            lore.add("§7гильдии");
        } else {
            lore.add("§cНедостаточно средств");
            lore.add("§cв хранилище гильдии!");
            lore.add("§cТребуется: " + amount + " монет");
            lore.add("§cДоступно: " + available + " монет");
        }

        ItemStack item = createGuiItem(buttonMaterial, buttonName, lore.toArray(new String[0]));
        inventory.setItem(slot, item);
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

            // Обрабатываем клик по кнопкам с суммами
            int amount = 0;
            boolean customAmount = false;

            switch (slot) {
                case AMOUNT_100_SLOT:
                    amount = 100;
                    break;
                case AMOUNT_500_SLOT:
                    amount = 500;
                    break;
                case AMOUNT_1000_SLOT:
                    amount = 1000;
                    break;
                case AMOUNT_5000_SLOT:
                    amount = 5000;
                    break;
                case AMOUNT_10000_SLOT:
                    amount = 10000;
                    break;
                case AMOUNT_CUSTOM_SLOT:
                    customAmount = true;
                    break;
                default:
                    return; // Клик по другому слоту
            }

            if (customAmount) {
                withdrawCustomAmount(player, guildId);
            } else if (amount > 0) {
                withdrawMoney(player, guildId, amount);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при обработке клика в меню снятия денег", e);
            MessageUtil.sendErrorMessage(player, "Произошла ошибка при обработке клика!");
        }
    }

    /**
     * Начинает процесс снятия произвольной суммы денег
     *
     * @param player  Игрок
     * @param guildId ID гильдии
     */
    private void withdrawCustomAmount(Player player, UUID guildId) {
        player.closeInventory();
        MessageUtil.sendInfoMessage(player, "Введите сумму для снятия в чат (только число):");
        // TODO: Реализовать обработку ввода произвольной суммы
    }

    /**
     * Снимает деньги из хранилища гильдии
     *
     * @param player  Игрок
     * @param guildId ID гильдии
     * @param amount  Сумма
     */
    private void withdrawMoney(Player player, UUID guildId, int amount) {
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
                if (storage.getMoney() < amount) {
                    MessageUtil.sendErrorMessage(player, "В хранилище гильдии недостаточно денег!");
                    player.closeInventory();
                    return;
                }

                // Снимаем деньги из хранилища
                storage.withdrawMoney(amount);
                plugin.getGuildStorageManager().saveStorage(storage);

                // Добавляем деньги игроку
                boolean success = depositPlayerMoney(player, amount);
                if (!success) {
                    // Возвращаем деньги в хранилище в случае ошибки
                    storage.depositMoney(amount);
                    plugin.getGuildStorageManager().saveStorage(storage);

                    MessageUtil.sendErrorMessage(player, "Не удалось зачислить деньги на ваш баланс!");
                    player.closeInventory();
                    return;
                }

                // Отправляем сообщение об успешном снятии денег
                MessageUtil.sendSuccessMessage(player, "Вы сняли " + amount + " монет из хранилища гильдии!");
                player.closeInventory();

                // Оповещаем всех членов гильдии о снятии денег
                notifyGuildMembers(guild, player, amount);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка при снятии денег из хранилища", e);
                MessageUtil.sendErrorMessage(player, "Произошла ошибка при снятии денег из хранилища!");
                player.closeInventory();
            }
        });
    }

    /**
     * Добавляет деньги на баланс игрока
     *
     * @param player Игрок
     * @param amount Сумма
     * @return true, если деньги успешно добавлены
     */
    private boolean depositPlayerMoney(Player player, int amount) {
        // TODO: Интеграция с экономикой сервера
        // Здесь должен быть вызов API экономики для добавления денег игроку
        // Например: return economy.depositPlayer(player, amount).transactionSuccess();
        return true; // Заглушка для тестирования
    }

    /**
     * Оповещает всех членов гильдии о снятии денег
     *
     * @param guild  Гильдия
     * @param player Игрок, снявший деньги
     * @param amount Сумма
     */
    private void notifyGuildMembers(Guild guild, Player player, int amount) {
        // Отправляем сообщение всем онлайн членам гильдии
        for (GuildMember member : guild.getMembers()) {
            Player memberPlayer = Bukkit.getPlayer(member.getPlayerId());
            if (memberPlayer != null && memberPlayer.isOnline() && !memberPlayer.equals(player)) {
                MessageUtil.sendInfoMessage(memberPlayer,
                        "Игрок " + player.getName() + " снял " + amount + " монет из хранилища гильдии!");
            }
        }
    }

    /**
     * Проверяет, может ли игрок с указанной ролью снимать деньги
     *
     * @param role Роль игрока
     * @return true, если может снимать деньги
     */
    private boolean canWithdrawMoney(GuildRole role) {
        return role == GuildRole.LEADER || role == GuildRole.OFFICER; // Только лидер и офицеры
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