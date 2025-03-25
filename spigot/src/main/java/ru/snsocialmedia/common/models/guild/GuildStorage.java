package ru.snsocialmedia.common.models.guild;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Класс хранилища гильдии
 */
public class GuildStorage {
    private final UUID guildId;
    private Map<String, Integer> items;
    private int money;
    private int maxSlots;

    /**
     * Конструктор по умолчанию
     * 
     * @param guildId ID гильдии
     */
    public GuildStorage(UUID guildId) {
        this.guildId = guildId;
        this.items = new HashMap<>();
        this.money = 0;
        this.maxSlots = 45; // По умолчанию 45 слотов (5 рядов)
    }

    /**
     * Полный конструктор
     * 
     * @param guildId  ID гильдии
     * @param items    Карта предметов (тип -> количество)
     * @param money    Деньги
     * @param maxSlots Максимальное количество слотов
     */
    public GuildStorage(UUID guildId, Map<String, Integer> items, int money, int maxSlots) {
        this.guildId = guildId;
        this.items = items;
        this.money = money;
        this.maxSlots = maxSlots;
    }

    /**
     * Получает ID гильдии
     * 
     * @return ID гильдии
     */
    public UUID getGuildId() {
        return guildId;
    }

    /**
     * Получает карту предметов
     * 
     * @return Карта предметов (тип -> количество)
     */
    public Map<String, Integer> getItems() {
        return items;
    }

    /**
     * Устанавливает карту предметов
     * 
     * @param items Карта предметов (тип -> количество)
     */
    public void setItems(Map<String, Integer> items) {
        this.items = items;
    }

    /**
     * Добавляет предмет в хранилище
     * 
     * @param itemType Тип предмета
     * @param amount   Количество
     * @return true, если предмет успешно добавлен
     */
    public boolean addItem(String itemType, int amount) {
        if (isFull() && !items.containsKey(itemType)) {
            return false;
        }

        int current = items.getOrDefault(itemType, 0);
        items.put(itemType, current + amount);
        return true;
    }

    /**
     * Удаляет предмет из хранилища
     * 
     * @param itemType Тип предмета
     * @param amount   Количество
     * @return true, если предмет успешно удален
     */
    public boolean removeItem(String itemType, int amount) {
        if (!items.containsKey(itemType)) {
            return false;
        }

        int current = items.get(itemType);
        if (current < amount) {
            return false;
        }

        int newAmount = current - amount;
        if (newAmount > 0) {
            items.put(itemType, newAmount);
        } else {
            items.remove(itemType);
        }

        return true;
    }

    /**
     * Проверяет, есть ли предмет в хранилище
     * 
     * @param itemType Тип предмета
     * @return true, если предмет есть в хранилище
     */
    public boolean hasItem(String itemType) {
        return items.containsKey(itemType);
    }

    /**
     * Получает количество предмета в хранилище
     * 
     * @param itemType Тип предмета
     * @return Количество предмета
     */
    public int getItemAmount(String itemType) {
        return items.getOrDefault(itemType, 0);
    }

    /**
     * Получает деньги гильдии
     * 
     * @return Деньги гильдии
     */
    public int getMoney() {
        return money;
    }

    /**
     * Устанавливает деньги гильдии
     * 
     * @param money Деньги гильдии
     */
    public void setMoney(int money) {
        this.money = money;
    }

    /**
     * Добавляет деньги в хранилище
     * 
     * @param amount Количество
     * @return true, если деньги успешно добавлены
     */
    public boolean depositMoney(int amount) {
        if (amount <= 0) {
            return false;
        }

        money += amount;
        return true;
    }

    /**
     * Снимает деньги из хранилища
     * 
     * @param amount Количество
     * @return true, если деньги успешно сняты
     */
    public boolean withdrawMoney(int amount) {
        if (amount <= 0 || money < amount) {
            return false;
        }

        money -= amount;
        return true;
    }

    /**
     * Получает максимальное количество слотов
     * 
     * @return Максимальное количество слотов
     */
    public int getMaxSlots() {
        return maxSlots;
    }

    /**
     * Устанавливает максимальное количество слотов
     * 
     * @param maxSlots Максимальное количество слотов
     */
    public void setMaxSlots(int maxSlots) {
        this.maxSlots = maxSlots;
    }

    /**
     * Проверяет, заполнено ли хранилище
     * 
     * @return true, если хранилище заполнено
     */
    public boolean isFull() {
        return items.size() >= maxSlots;
    }

    /**
     * Получает количество свободных слотов
     * 
     * @return Количество свободных слотов
     */
    public int getFreeSlots() {
        return Math.max(0, maxSlots - items.size());
    }

    /**
     * Увеличивает максимальное количество слотов
     * 
     * @param additionalSlots Количество дополнительных слотов
     */
    public void increaseMaxSlots(int additionalSlots) {
        if (additionalSlots > 0) {
            maxSlots += additionalSlots;
        }
    }

    /**
     * Очищает хранилище
     */
    public void clear() {
        items.clear();
        money = 0;
    }
}