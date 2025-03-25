package ru.snsocialmedia.common.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.snsocialmedia.common.database.DatabaseManager;
import ru.snsocialmedia.common.models.guild.Guild;
import ru.snsocialmedia.common.models.guild.GuildStorage;

/**
 * Менеджер для управления хранилищем гильдии
 */
public class GuildStorageManager {
    private final DatabaseManager databaseManager;
    private final Logger logger;
    private final Map<UUID, GuildStorage> storageCache = new HashMap<>();

    /**
     * Конструктор
     * 
     * @param databaseManager Менеджер базы данных
     * @param logger          Логгер
     */
    public GuildStorageManager(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    /**
     * Получает хранилище гильдии
     * 
     * @param guild Гильдия
     * @return Хранилище гильдии
     */
    public GuildStorage getStorage(Guild guild) {
        if (guild == null) {
            return null;
        }

        return getStorage(guild.getId());
    }

    /**
     * Получает хранилище гильдии по ID
     * 
     * @param guildId ID гильдии
     * @return Хранилище гильдии
     */
    public GuildStorage getStorage(UUID guildId) {
        if (guildId == null) {
            return null;
        }

        // Проверяем кеш
        if (storageCache.containsKey(guildId)) {
            return storageCache.get(guildId);
        }

        // Загружаем из базы данных
        GuildStorage storage = loadStorage(guildId);
        if (storage != null) {
            storageCache.put(guildId, storage);
        }

        return storage;
    }

    /**
     * Загружает хранилище гильдии из базы данных
     * 
     * @param guildId ID гильдии
     * @return Хранилище гильдии
     */
    private GuildStorage loadStorage(UUID guildId) {
        try {
            // Здесь должен быть код для загрузки хранилища из базы данных
            // Для примера возвращаем новое хранилище
            GuildStorage storage = new GuildStorage(guildId);

            // Добавляем тестовые данные
            storage.addItem("DIAMOND", 5);
            storage.addItem("GOLD_INGOT", 15);
            storage.addItem("IRON_INGOT", 32);
            storage.depositMoney(5000);

            return storage;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при загрузке хранилища гильдии", e);
            return new GuildStorage(guildId);
        }
    }

    /**
     * Сохраняет хранилище гильдии в базе данных
     * 
     * @param storage Хранилище гильдии
     * @return true, если сохранение прошло успешно
     */
    public boolean saveStorage(GuildStorage storage) {
        try {
            if (storage == null) {
                return false;
            }

            // Обновляем кеш
            storageCache.put(storage.getGuildId(), storage);

            // Здесь должен быть код для сохранения хранилища в базе данных

            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при сохранении хранилища гильдии", e);
            return false;
        }
    }

    /**
     * Добавляет предмет в хранилище гильдии
     * 
     * @param guild    Гильдия
     * @param itemType Тип предмета
     * @param amount   Количество
     * @return true, если предмет успешно добавлен
     */
    public boolean addItem(Guild guild, String itemType, int amount) {
        if (guild == null || itemType == null || amount <= 0) {
            return false;
        }

        GuildStorage storage = getStorage(guild);
        if (storage == null) {
            return false;
        }

        boolean result = storage.addItem(itemType, amount);
        if (result) {
            saveStorage(storage);
        }

        return result;
    }

    /**
     * Удаляет предмет из хранилища гильдии
     * 
     * @param guild    Гильдия
     * @param itemType Тип предмета
     * @param amount   Количество
     * @return true, если предмет успешно удален
     */
    public boolean removeItem(Guild guild, String itemType, int amount) {
        if (guild == null || itemType == null || amount <= 0) {
            return false;
        }

        GuildStorage storage = getStorage(guild);
        if (storage == null) {
            return false;
        }

        boolean result = storage.removeItem(itemType, amount);
        if (result) {
            saveStorage(storage);
        }

        return result;
    }

    /**
     * Добавляет деньги в хранилище гильдии
     * 
     * @param guild  Гильдия
     * @param amount Количество
     * @return true, если деньги успешно добавлены
     */
    public boolean depositMoney(Guild guild, int amount) {
        if (guild == null || amount <= 0) {
            return false;
        }

        GuildStorage storage = getStorage(guild);
        if (storage == null) {
            return false;
        }

        boolean result = storage.depositMoney(amount);
        if (result) {
            saveStorage(storage);
        }

        return result;
    }

    /**
     * Снимает деньги из хранилища гильдии
     * 
     * @param guild  Гильдия
     * @param amount Количество
     * @return true, если деньги успешно сняты
     */
    public boolean withdrawMoney(Guild guild, int amount) {
        if (guild == null || amount <= 0) {
            return false;
        }

        GuildStorage storage = getStorage(guild);
        if (storage == null) {
            return false;
        }

        boolean result = storage.withdrawMoney(amount);
        if (result) {
            saveStorage(storage);
        }

        return result;
    }

    /**
     * Улучшает хранилище гильдии
     * 
     * @param guild           Гильдия
     * @param additionalSlots Количество дополнительных слотов
     * @return true, если улучшение прошло успешно
     */
    public boolean upgradeStorage(Guild guild, int additionalSlots) {
        if (guild == null || additionalSlots <= 0) {
            return false;
        }

        GuildStorage storage = getStorage(guild);
        if (storage == null) {
            return false;
        }

        storage.increaseMaxSlots(additionalSlots);
        return saveStorage(storage);
    }

    /**
     * Очищает хранилище гильдии
     * 
     * @param guild Гильдия
     * @return true, если очистка прошла успешно
     */
    public boolean clearStorage(Guild guild) {
        if (guild == null) {
            return false;
        }

        GuildStorage storage = getStorage(guild);
        if (storage == null) {
            return false;
        }

        storage.clear();
        return saveStorage(storage);
    }

    /**
     * Создает хранилище для гильдии
     * 
     * @param guild Гильдия
     * @return Созданное хранилище
     */
    public GuildStorage createStorage(Guild guild) {
        if (guild == null) {
            return null;
        }

        GuildStorage storage = new GuildStorage(guild.getId());
        if (saveStorage(storage)) {
            return storage;
        }

        return null;
    }

    /**
     * Удаляет хранилище гильдии
     * 
     * @param guildId ID гильдии
     * @return true, если удаление прошло успешно
     */
    public boolean deleteStorage(UUID guildId) {
        try {
            if (guildId == null) {
                return false;
            }

            // Удаляем из кеша
            storageCache.remove(guildId);

            // Здесь должен быть код для удаления хранилища из базы данных

            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при удалении хранилища гильдии", e);
            return false;
        }
    }
}