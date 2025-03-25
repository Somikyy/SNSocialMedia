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
 * Меню для внесения денег в хранилище гильдии
 */
public class DepositMoneyMenu {

    private final SNSocialMediaSpigot plugin;
    private static final String MENU_TITLE = "§a§lВнесение денег";
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
    public DepositMoneyMenu(SNSocialMediaSpigot plugin) {
        this.plugin = plugin;
    }

    /**
     * Открывает меню внесения денег для игрока
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

                // Проверяем, имеет ли игрок право на внесение денег
                if (!canDepositMoney(member.getRole())) {
                    MessageUtil.sendErrorMessage(player, "У вас нет прав на внесение денег в хранилище гильдии!");
                    return;
                }

                // Создаем и открываем инвентарь
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Inventory inventory = createInventory(guild, storage, player);
                    player.openInventory(inventory);
                });
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка при открытии меню внесения денег", e);
                MessageUtil.sendErrorMessage(player, "Произошла ошибка при открытии меню внесения денег!");
            }
        });
    }

    /**
     * Создает инвентарь меню внесения денег
     *
     * @param guild   Гильдия
     * @param storage Хранилище гильдии
     * @param player  Игрок
     * @return Инвентарь
     */
    private Inventory createInventory(Guild guild, GuildStorage storage, Player player) {
        Inventory inventory = Bukkit.createInventory(null, MENU_SIZE, MENU_TITLE);

        // Информация о хранилище
        double balance = getPlayerBalance(player);
        ItemStack infoItem = createGuiItem(Material.PAPER, "§f§lИнформация",
                "§7Гильдия: §f" + guild.getName(),
                "§7Деньги в хранилище: §f" + storage.getMoney() + " §7монет",
                "§7Ваш баланс: §f" + balance + " §7монет",
                "§7Выберите сумму для внесения");
        inventory.setItem(INFO_SLOT, infoItem);

        // Кнопка отмены
        ItemStack cancelItem = createGuiItem(Material.BARRIER, "§c§lОтмена",
                "§7Нажмите, чтобы закрыть меню",
                "§7без внесения денег");
        inventory.setItem(CANCEL_SLOT, cancelItem);

        // Кнопки с суммами
        inventory.setItem(AMOUNT_100_SLOT, createGuiItem(Material.GOLD_NUGGET, "§e§l100 монет",
                "§7Нажмите, чтобы внести",
                "§7100 монет в хранилище гильдии"));

        inventory.setItem(AMOUNT_500_SLOT, createGuiItem(Material.GOLD_INGOT, "§e§l500 монет",
                "§7Нажмите, чтобы внести",
                "§7500 монет в хранилище гильдии"));

        inventory.setItem(AMOUNT_1000_SLOT, createGuiItem(Material.GOLD_BLOCK, "§e§l1 000 монет",
                "§7Нажмите, чтобы внести",
                "§71 000 монет в хранилище гильдии"));

        inventory.setItem(AMOUNT_5000_SLOT, createGuiItem(Material.EMERALD, "§a§l5 000 монет",
                "§7Нажмите, чтобы внести",
                "§75 000 монет в хранилище гильдии"));

        inventory.setItem(AMOUNT_10000_SLOT, createGuiItem(Material.EMERALD_BLOCK, "§a§l10 000 монет",
                "§7Нажмите, чтобы внести",
                "§710 000 монет в хранилище гильдии"));

        inventory.setItem(AMOUNT_CUSTOM_SLOT, createGuiItem(Material.WRITABLE_BOOK, "§b§lСвоя сумма",
                "§7Нажмите, чтобы внести",
                "§7произвольную сумму монет",
                "§7(введете в чат)"));

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
                player.closeInventory();
                MessageUtil.sendInfoMessage(player, "Введите сумму для внесения в чат (только число):");
                // Предполагается, что есть система обработки следующего сообщения в чате
                // TODO: Реализовать обработку ввода произвольной суммы
            } else if (amount > 0) {
                depositMoney(player, guildId, amount);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при обработке клика в меню внесения денег", e);
            MessageUtil.sendErrorMessage(player, "Произошла ошибка при обработке клика!");
        }
    }

    /**
     * Вносит деньги в хранилище гильдии
     *
     * @param player  Игрок
     * @param guildId ID гильдии
     * @param amount  Сумма
     */
    private void depositMoney(Player player, UUID guildId, int amount) {
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

                // Проверяем баланс игрока
                double balance = getPlayerBalance(player);
                if (balance < amount) {
                    MessageUtil.sendErrorMessage(player, "У вас недостаточно денег! Необходимо: " + amount + " монет.");
                    player.closeInventory();
                    return;
                }

                // Снимаем деньги с игрока
                boolean success = withdrawPlayerMoney(player, amount);
                if (!success) {
                    MessageUtil.sendErrorMessage(player, "Не удалось снять деньги с вашего баланса!");
                    player.closeInventory();
                    return;
                }

                // Вносим деньги в хранилище
                storage.depositMoney(amount);
                plugin.getGuildStorageManager().saveStorage(storage);

                // Отправляем сообщение об успешном внесении денег
                MessageUtil.sendSuccessMessage(player, "Вы внесли " + amount + " монет в хранилище гильдии!");
                player.closeInventory();

                // Оповещаем всех членов гильдии о внесении денег
                notifyGuildMembers(guild, player, amount);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка при внесении денег в хранилище", e);
                MessageUtil.sendErrorMessage(player, "Произошла ошибка при внесении денег в хранилище!");
                player.closeInventory();
            }
        });
    }

    /**
     * Получает баланс игрока
     *
     * @param player Игрок
     * @return Баланс игрока
     */
    private double getPlayerBalance(Player player) {
        // TODO: Интеграция с экономикой сервера
        // Здесь должен быть вызов API экономики для получения баланса игрока
        // Например: return economy.getBalance(player);
        return 10000; // Заглушка для тестирования
    }

    /**
     * Снимает деньги с баланса игрока
     *
     * @param player Игрок
     * @param amount Сумма
     * @return true, если деньги успешно сняты
     */
    private boolean withdrawPlayerMoney(Player player, int amount) {
        // TODO: Интеграция с экономикой сервера
        // Здесь должен быть вызов API экономики для снятия денег с игрока
        // Например: return economy.withdrawPlayer(player, amount).transactionSuccess();
        return true; // Заглушка для тестирования
    }

    /**
     * Оповещает всех членов гильдии о внесении денег
     *
     * @param guild  Гильдия
     * @param player Игрок, внесший деньги
     * @param amount Сумма
     */
    private void notifyGuildMembers(Guild guild, Player player, int amount) {
        // Отправляем сообщение всем онлайн членам гильдии
        for (GuildMember member : guild.getMembers()) {
            Player memberPlayer = Bukkit.getPlayer(member.getPlayerId());
            if (memberPlayer != null && memberPlayer.isOnline() && !memberPlayer.equals(player)) {
                MessageUtil.sendInfoMessage(memberPlayer,
                        "Игрок " + player.getName() + " внес " + amount + " монет в хранилище гильдии!");
            }
        }
    }

    /**
     * Проверяет, может ли игрок с указанной ролью вносить деньги
     *
     * @param role Роль игрока
     * @return true, если может вносить деньги
     */
    private boolean canDepositMoney(GuildRole role) {
        return role != GuildRole.MEMBER; // Все, кроме обычных участников
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