package ru.snsocialmedia.common.models.guild;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Класс для хранения предметов гильдии
 */
public class GuildStorage {

    private final UUID guildId;
    private final Map<String, Integer> items = new HashMap<>();
    private double money;
    private int maxSlots;

    /**
     * Создает новое хранилище гильдии
     * 
     * @param guildId  ID гильдии
     * @param maxSlots Максимальное количество слотов
     */
    public GuildStorage(UUID guildId, int maxSlots) {
        this.guildId = guildId;
        this.maxSlots = maxSlots;
        this.money = 0;
    }

    /**
     * Конструктор с параметрами по умолчанию
     * 
     * @param guildId ID гильдии
     */
    public GuildStorage(UUID guildId) {
        this(guildId, 20); // По умолчанию 20 слотов
    }

    /**
     * Возвращает ID гильдии
     * 
     * @return ID гильдии
     */
    public UUID getGuildId() {
        return guildId;
    }

    /**
     * Возвращает карту предметов в хранилище
     * 
     * @return Карта предметов (Тип предмета -> Количество)
     */
    public Map<String, Integer> getItems() {
        return new HashMap<>(items);
    }

    /**
     * Возвращает количество денег в хранилище
     * 
     * @return Количество денег
     */
    public double getMoney() {
        return money;
    }

    /**
     * Устанавливает количество денег в хранилище
     * 
     * @param money Новое количество денег
     */
    public void setMoney(double money) {
        this.money = money;
    }

    /**
     * Возвращает максимальное количество слотов в хранилище
     * 
     * @return Максимальное количество слотов
     */
    public int getMaxSlots() {
        return maxSlots;
    }

    /**
     * Устанавливает максимальное количество слотов в хранилище
     * 
     * @param maxSlots Максимальное количество слотов
     */
    public void setMaxSlots(int maxSlots) {
        this.maxSlots = maxSlots;
    }

    /**
     * Увеличивает максимальное количество слотов в хранилище
     * 
     * @param slots Количество добавляемых слотов
     */
    public void increaseMaxSlots(int slots) {
        if (slots > 0) {
            this.maxSlots += slots;
        }
    }

    /**
     * Проверяет, есть ли свободные слоты в хранилище
     * 
     * @return true, если есть свободные слоты
     */
    public boolean hasFreeSlots() {
        return items.size() < maxSlots;
    }

    /**
     * Проверяет количество свободных слотов в хранилище
     * 
     * @return Количество свободных слотов
     */
    public int getFreeSlots() {
        return maxSlots - items.size();
    }

    /**
     * Добавляет предмет в хранилище
     * 
     * @param itemType Тип предмета
     * @param amount   Количество
     * @return true, если предмет успешно добавлен
     */
    public boolean addItem(String itemType, int amount) {
        if (amount <= 0) {
            return false;
        }

        // Если предмета еще нет в хранилище и нет свободных слотов
        if (!items.containsKey(itemType) && !hasFreeSlots()) {
            return false;
        }

        int current = items.getOrDefault(itemType, 0);
        items.put(itemType, current + amount);
        return true;
    }

    /**
     * Извлекает предмет из хранилища
     * 
     * @param itemType Тип предмета
     * @param amount   Количество
     * @return true, если предмет успешно извлечен
     */
    public boolean removeItem(String itemType, int amount) {
        if (amount <= 0) {
            return false;
        }

        int current = items.getOrDefault(itemType, 0);
        if (current < amount) {
            return false;
        }

        if (current == amount) {
            items.remove(itemType);
        } else {
            items.put(itemType, current - amount);
        }
        return true;
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
     * Пополняет баланс хранилища
     * 
     * @param amount Сумма пополнения
     * @return true, если операция успешна
     */
    public boolean depositMoney(double amount) {
        if (amount <= 0) {
            return false;
        }
        money += amount;
        return true;
    }

    /**
     * Снимает деньги с баланса хранилища
     * 
     * @param amount Сумма снятия
     * @return true, если операция успешна
     */
    public boolean withdrawMoney(double amount) {
        if (amount <= 0 || money < amount) {
            return false;
        }
        money -= amount;
        return true;
    }

    /**
     * Проверяет, содержит ли хранилище предмет
     * 
     * @param itemType Тип предмета
     * @return true, если предмет найден
     */
    public boolean hasItem(String itemType) {
        return items.containsKey(itemType);
    }

    /**
     * Проверяет, содержит ли хранилище предмет в нужном количестве
     * 
     * @param itemType Тип предмета
     * @param amount   Количество
     * @return true, если предмет найден в нужном количестве
     */
    public boolean hasItem(String itemType, int amount) {
        return getItemAmount(itemType) >= amount;
    }

    /**
     * Очищает хранилище
     */
    public void clear() {
        items.clear();
        money = 0;
    }
}