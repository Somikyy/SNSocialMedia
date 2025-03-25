package ru.snsocialmedia.spigot.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.snsocialmedia.spigot.SNSocialMediaSpigot;
import ru.snsocialmedia.spigot.gui.GuildMenuHandler;
import org.bukkit.Material;

/**
 * Обработчик событий GUI-меню гильдий
 */
public class GuildMenuListener implements Listener {

    private final SNSocialMediaSpigot plugin;
    private final GuildMenuHandler menuHandler;
    private static final int STORAGE_BUTTON_SLOT = 18; // Слот для кнопки хранилища гильдии

    public GuildMenuListener(SNSocialMediaSpigot plugin, GuildMenuHandler menuHandler) {
        this.plugin = plugin;
        this.menuHandler = menuHandler;
    }

    /**
     * Обрабатывает клики по элементам меню
     * 
     * @param event Событие клика
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        try {
            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getWhoClicked();

            // Проверяем, что инвентарь существует и имеет название
            if (event.getView() == null || event.getView().getTitle() == null) {
                return;
            }

            // Проверяем, что клик произошел в меню гильдии
            String title = event.getView().getTitle();
            if (!isGuildMenu(title)) {
                return;
            }

            // Предотвращаем взаимодействие с нашим GUI в любом случае
            event.setCancelled(true);

            // Подробное логирование для отладки
            String clickedInventoryType = (event.getClickedInventory() == event.getView().getTopInventory())
                    ? "верхний (GUI)"
                    : (event.getClickedInventory() == player.getInventory())
                            ? "нижний (инвентарь игрока)"
                            : "null или другой";

            plugin.getLogger().info("Игрок " + player.getName() +
                    " кликнул в меню '" + title +
                    "' по слоту " + event.getRawSlot() +
                    " (инвентарь: " + clickedInventoryType + ")");

            // Обрабатываем только клики по верхнему инвентарю (наше меню)
            if (event.getClickedInventory() != event.getView().getTopInventory()) {
                plugin.getLogger().info("Клик не обработан: был сделан не в верхнем инвентаре");
                return;
            }

            // Проверяем, доступен ли обработчик меню
            if (menuHandler == null) {
                plugin.getLogger().severe("GuildMenuHandler равен null! Не удалось обработать клик.");
                player.sendMessage("§cОшибка: Не удалось обработать клик. Пожалуйста, сообщите администрации.");
                return;
            }

            // Обрабатываем клик через GuildMenuHandler
            boolean handled = menuHandler.handleClick(player, event.getRawSlot());
            plugin.getLogger().info("Клик " + (handled ? "успешно обработан" : "не обработан") + " обработчиком меню");
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при обработке клика в инвентаре: " + e.getMessage());
            e.printStackTrace();
            event.setCancelled(true); // На всякий случай отменяем событие
        }
    }

    /**
     * Проверяет, является ли название инвентаря меню гильдии
     * 
     * @param title Название инвентаря
     * @return true, если это меню гильдии
     */
    private boolean isGuildMenu(String title) {
        if (title == null) {
            return false;
        }

        // Проверяем ключевые слова в названии меню
        return title.contains("гильди") ||
                title.contains("Гильди") ||
                title.contains("Действия:") ||
                title.contains("Приглашение");
    }

    /**
     * Обрабатывает перетаскивание предметов в инвентаре
     * 
     * @param event Событие перетаскивания
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        try {
            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }

            // Проверяем, что инвентарь существует и имеет название
            if (event.getView() == null || event.getView().getTitle() == null) {
                return;
            }

            // Проверяем, что перетаскивание произошло в меню гильдии
            String title = event.getView().getTitle();
            if (!isGuildMenu(title)) {
                return;
            }

            // Отменяем перетаскивание предметов в меню гильдии
            event.setCancelled(true);
            plugin.getLogger().info("Отменено перетаскивание предметов в меню гильдии для игрока " +
                    event.getWhoClicked().getName());
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при обработке перетаскивания в инвентаре: " + e.getMessage());
            e.printStackTrace();
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
        try {
            if (!(event.getPlayer() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getPlayer();

            // Проверяем, что инвентарь существует и имеет название
            if (event.getView() == null || event.getView().getTitle() == null) {
                return;
            }

            // Проверяем, что закрывается меню гильдии
            String title = event.getView().getTitle();
            if (!isGuildMenu(title)) {
                return;
            }

            // Логируем событие
            plugin.getLogger().info("Игрок " + player.getName() + " закрыл меню " + title);

            // Проверяем, доступен ли обработчик меню
            if (menuHandler == null) {
                plugin.getLogger().warning("GuildMenuHandler равен null при закрытии инвентаря!");
                return;
            }

            // Удаляем информацию о типе меню при закрытии
            menuHandler.handleInventoryClose(player);
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при обработке закрытия инвентаря: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Создает элемент GUI с заданными параметрами
     *
     * @param material Материал предмета
     * @param name     Название предмета
     * @param lore     Описание предмета (можно передать несколько строк)
     * @return Созданный предмет
     */
    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(java.util.Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Создает инвентарь основного меню гильдии
     *
     * @param player Игрок
     * @return Инвентарь
     */
    private Inventory createMainMenu(Player player) {
        Inventory inventory = org.bukkit.Bukkit.createInventory(null, 36, "§6Меню гильдии");

        // Добавляем кнопку хранилища гильдии
        inventory.setItem(STORAGE_BUTTON_SLOT, createGuiItem(Material.CHEST,
                "§e§lХранилище гильдии",
                "§7Нажмите, чтобы открыть",
                "§7хранилище вашей гильдии"));

        return inventory;
    }
}