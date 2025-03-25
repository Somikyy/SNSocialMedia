package ru.snsocialmedia.common.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    private static GuildStorageManager instance;
    private final DatabaseManager databaseManager;
    private final Logger logger;
    private final Map<UUID, GuildStorage> guildStorageCache = new HashMap<>();

    /**
     * Конструктор
     *
     * @param databaseManager Менеджер базы данных
     * @param logger          Логгер
     */
    public GuildStorageManager(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
        createTablesIfNotExist();
    }

    /**
     * Получает экземпляр менеджера хранилищ гильдий
     *
     * @return Экземпляр менеджера хранилищ гильдий
     */
    public static GuildStorageManager getInstance() {
        if (instance == null) {
            Logger logger = Logger.getLogger("GuildStorageManager");
            instance = new GuildStorageManager(DatabaseManager.getInstance(), logger);
        }
        return instance;
    }

    /**
     * Инициализирует менеджер хранилищ гильдий
     *
     * @param logger Логгер для вывода сообщений
     */
    public static void initialize(Logger logger) {
        if (instance == null) {
            instance = new GuildStorageManager(DatabaseManager.getInstance(), logger);
        }
    }

    /**
     * Создает таблицы для хранения данных о хранилищах гильдий
     */
    private void createTablesIfNotExist() {
        try (Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS guild_storage (" +
                                "guild_id CHAR(36) PRIMARY KEY, " +
                                "max_slots INT NOT NULL DEFAULT 10, " +
                                "money DOUBLE NOT NULL DEFAULT 0.0" +
                                ")")) {
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Не удалось создать таблицу guild_storage", e);
        }

        try (Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS guild_storage_items (" +
                                "guild_id CHAR(36) NOT NULL, " +
                                "item_type VARCHAR(100) NOT NULL, " +
                                "amount INT NOT NULL, " +
                                "PRIMARY KEY (guild_id, item_type), " +
                                "FOREIGN KEY (guild_id) REFERENCES guild_storage(guild_id) ON DELETE CASCADE" +
                                ")")) {
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Не удалось создать таблицу guild_storage_items", e);
        }
    }

    /**
     * Создает хранилище для гильдии
     *
     * @param guild    Гильдия
     * @param maxSlots Максимальное количество слотов
     * @return Созданное хранилище
     */
    public GuildStorage createStorage(Guild guild, int maxSlots) {
        UUID guildId = guild.getId();
        GuildStorage storage = new GuildStorage(guildId, maxSlots);

        try (Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO guild_storage (guild_id, max_slots, money) VALUES (?, ?, ?)")) {
            statement.setString(1, guildId.toString());
            statement.setInt(2, maxSlots);
            statement.setDouble(3, 0.0);
            statement.executeUpdate();

            guildStorageCache.put(guildId, storage);
            logger.info("Создано хранилище для гильдии " + guild.getName());
            return storage;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Не удалось создать хранилище для гильдии " + guild.getName(), e);
            return null;
        }
    }

    /**
     * Загружает хранилище гильдии из базы данных
     *
     * @param guildId ID гильдии
     * @return Хранилище гильдии или null, если не найдено
     */
    public GuildStorage loadStorage(UUID guildId) {
        // Проверяем кэш
        if (guildStorageCache.containsKey(guildId)) {
            return guildStorageCache.get(guildId);
        }

        try (Connection connection = databaseManager.getConnection();
                PreparedStatement storageStatement = connection.prepareStatement(
                        "SELECT max_slots, money FROM guild_storage WHERE guild_id = ?")) {
            storageStatement.setString(1, guildId.toString());
            ResultSet storageResult = storageStatement.executeQuery();

            if (storageResult.next()) {
                int maxSlots = storageResult.getInt("max_slots");
                double money = storageResult.getDouble("money");

                GuildStorage storage = new GuildStorage(guildId, maxSlots);
                storage.setMoney(money);

                // Загружаем предметы
                try (PreparedStatement itemsStatement = connection.prepareStatement(
                        "SELECT item_type, amount FROM guild_storage_items WHERE guild_id = ?")) {
                    itemsStatement.setString(1, guildId.toString());
                    ResultSet itemsResult = itemsStatement.executeQuery();

                    while (itemsResult.next()) {
                        String itemType = itemsResult.getString("item_type");
                        int amount = itemsResult.getInt("amount");
                        storage.addItem(itemType, amount);
                    }
                }

                guildStorageCache.put(guildId, storage);
                return storage;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Не удалось загрузить хранилище для гильдии " + guildId, e);
        }

        return null;
    }

    /**
     * Сохраняет хранилище гильдии в базу данных
     *
     * @param storage Хранилище гильдии
     * @return true, если операция успешна
     */
    public boolean saveStorage(GuildStorage storage) {
        UUID guildId = storage.getGuildId();
        try (Connection connection = databaseManager.getConnection()) {
            // Обновляем основную информацию о хранилище
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE guild_storage SET max_slots = ?, money = ? WHERE guild_id = ?")) {
                statement.setInt(1, storage.getMaxSlots());
                statement.setDouble(2, storage.getMoney());
                statement.setString(3, guildId.toString());
                statement.executeUpdate();
            }

            // Удаляем все предметы и вставляем заново
            try (PreparedStatement deleteStatement = connection.prepareStatement(
                    "DELETE FROM guild_storage_items WHERE guild_id = ?")) {
                deleteStatement.setString(1, guildId.toString());
                deleteStatement.executeUpdate();
            }

            // Вставляем предметы
            Map<String, Integer> items = storage.getItems();
            if (!items.isEmpty()) {
                try (PreparedStatement insertStatement = connection.prepareStatement(
                        "INSERT INTO guild_storage_items (guild_id, item_type, amount) VALUES (?, ?, ?)")) {
                    for (Map.Entry<String, Integer> entry : items.entrySet()) {
                        insertStatement.setString(1, guildId.toString());
                        insertStatement.setString(2, entry.getKey());
                        insertStatement.setInt(3, entry.getValue());
                        insertStatement.addBatch();
                    }
                    insertStatement.executeBatch();
                }
            }

            logger.info("Сохранено хранилище для гильдии " + guildId);
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Не удалось сохранить хранилище для гильдии " + guildId, e);
            return false;
        }
    }

    /**
     * Удаляет хранилище гильдии
     *
     * @param guildId ID гильдии
     * @return true, если операция успешна
     */
    public boolean deleteStorage(UUID guildId) {
        try (Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM guild_storage WHERE guild_id = ?")) {
            statement.setString(1, guildId.toString());
            int result = statement.executeUpdate();

            if (result > 0) {
                guildStorageCache.remove(guildId);
                logger.info("Удалено хранилище для гильдии " + guildId);
                return true;
            }
            return false;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Не удалось удалить хранилище для гильдии " + guildId, e);
            return false;
        }
    }

    /**
     * Увеличивает размер хранилища гильдии
     *
     * @param guild    Гильдия
     * @param addSlots Количество добавляемых слотов
     * @return true, если операция успешна
     */
    public boolean upgradeStorage(Guild guild, int addSlots) {
        if (addSlots <= 0) {
            return false;
        }

        UUID guildId = guild.getId();
        GuildStorage storage = loadStorage(guildId);
        if (storage == null) {
            return false;
        }

        int newMaxSlots = storage.getMaxSlots() + addSlots;
        storage.setMaxSlots(newMaxSlots);

        try (Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE guild_storage SET max_slots = ? WHERE guild_id = ?")) {
            statement.setInt(1, newMaxSlots);
            statement.setString(2, guildId.toString());
            int result = statement.executeUpdate();

            if (result > 0) {
                logger.info(
                        "Увеличен размер хранилища для гильдии " + guild.getName() + " до " + newMaxSlots + " слотов");
                return true;
            }
            return false;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Не удалось увеличить размер хранилища для гильдии " + guild.getName(), e);
            return false;
        }
    }

    /**
     * Получает хранилище гильдии
     *
     * @param guild Гильдия
     * @return Хранилище гильдии или null, если не найдено
     */
    public GuildStorage getStorage(Guild guild) {
        return loadStorage(guild.getId());
    }

    /**
     * Обновляет кэш хранилища гильдии
     *
     * @param storage Хранилище гильдии
     */
    public void updateCache(GuildStorage storage) {
        guildStorageCache.put(storage.getGuildId(), storage);
    }

    /**
     * Очищает кэш хранилища гильдии
     *
     * @param guildId ID гильдии
     */
    public void clearCache(UUID guildId) {
        guildStorageCache.remove(guildId);
    }

    /**
     * Обновляет количество денег в хранилище гильдии
     *
     * @param guild Гильдия
     * @param money Новое количество денег
     * @return true, если операция успешна
     */
    public boolean updateMoney(Guild guild, double money) {
        if (money < 0) {
            return false;
        }

        UUID guildId = guild.getId();
        GuildStorage storage = loadStorage(guildId);
        if (storage == null) {
            return false;
        }

        storage.setMoney(money);

        try (Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE guild_storage SET money = ? WHERE guild_id = ?")) {
            statement.setDouble(1, money);
            statement.setString(2, guildId.toString());
            int result = statement.executeUpdate();

            if (result > 0) {
                logger.info("Обновлен баланс хранилища для гильдии " + guild.getName() + ": " + money);
                updateCache(storage);
                return true;
            }
            return false;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Не удалось обновить баланс хранилища для гильдии " + guild.getName(), e);
            return false;
        }
    }

    /**
     * Начисляет проценты на баланс всех гильдий
     * 
     * @param interestRate Процентная ставка (например, 0.05 для 5%)
     * @return Количество обновленных гильдий
     */
    public int applyInterest(double interestRate) {
        if (interestRate <= 0) {
            return 0;
        }

        int updatedGuilds = 0;

        try (Connection connection = databaseManager.getConnection();
                PreparedStatement selectStatement = connection.prepareStatement(
                        "SELECT guild_id, money FROM guild_storage WHERE money > 0");
                PreparedStatement updateStatement = connection.prepareStatement(
                        "UPDATE guild_storage SET money = ? WHERE guild_id = ?")) {

            ResultSet resultSet = selectStatement.executeQuery();

            while (resultSet.next()) {
                String guildIdStr = resultSet.getString("guild_id");
                double money = resultSet.getDouble("money");

                // Рассчитываем проценты
                double interest = money * interestRate;
                double newMoney = money + interest;

                // Обновляем баланс в базе данных
                updateStatement.setDouble(1, newMoney);
                updateStatement.setString(2, guildIdStr);
                int result = updateStatement.executeUpdate();

                if (result > 0) {
                    updatedGuilds++;

                    // Обновляем кеш, если гильдия находится в нем
                    UUID guildId = UUID.fromString(guildIdStr);
                    if (guildStorageCache.containsKey(guildId)) {
                        GuildStorage storage = guildStorageCache.get(guildId);
                        storage.setMoney(newMoney);
                    }
                }
            }

            logger.info("Начислены проценты (" + (interestRate * 100) + "%) для " + updatedGuilds + " гильдий");
            return updatedGuilds;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при начислении процентов на баланс гильдий", e);
            return 0;
        }
    }

    /**
     * Увеличивает размер хранилища гильдии по ID гильдии
     *
     * @param guildId  ID гильдии
     * @param addSlots Количество добавляемых слотов
     * @return true, если операция успешна
     */
    public boolean expandGuildStorage(UUID guildId, int addSlots) {
        if (addSlots <= 0) {
            return false;
        }

        GuildStorage storage = loadStorage(guildId);
        if (storage == null) {
            logger.warning("Не удалось найти хранилище для гильдии " + guildId + " для расширения");
            return false;
        }

        int newMaxSlots = storage.getMaxSlots() + addSlots;
        storage.setMaxSlots(newMaxSlots);

        try (Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE guild_storage SET max_slots = ? WHERE guild_id = ?")) {
            statement.setInt(1, newMaxSlots);
            statement.setString(2, guildId.toString());
            int result = statement.executeUpdate();

            if (result > 0) {
                logger.info("Увеличен размер хранилища для гильдии " + guildId + " до " + newMaxSlots + " слотов");
                updateCache(storage);
                return true;
            }
            return false;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Не удалось увеличить размер хранилища для гильдии " + guildId, e);
            return false;
        }
    }
}