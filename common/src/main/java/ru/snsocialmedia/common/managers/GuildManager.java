package ru.snsocialmedia.common.managers;

import com.zaxxer.hikari.HikariDataSource;
import ru.snsocialmedia.common.database.DatabaseManager;
import ru.snsocialmedia.common.database.SQLQueries;
import ru.snsocialmedia.common.models.guild.Guild;
import ru.snsocialmedia.common.models.guild.GuildMember;
import ru.snsocialmedia.common.models.guild.GuildRole;
import ru.snsocialmedia.common.models.guild.GuildStorage;
import ru.snsocialmedia.common.managers.GuildStorageManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Менеджер для управления гильдиями
 */
public class GuildManager {

    private static GuildManager instance;
    private final Logger logger;
    private final Map<UUID, Guild> guilds = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerGuilds = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> guildChatMode = new ConcurrentHashMap<>();
    private final DatabaseManager dbManager;
    private final GuildStorageManager guildStorageManager;

    private GuildManager(DatabaseManager databaseManager, Logger logger, GuildStorageManager guildStorageManager) {
        this.dbManager = databaseManager;
        this.logger = logger;
        this.guildStorageManager = guildStorageManager;
        createTablesIfNotExist();
    }

    /**
     * Получает экземпляр менеджера гильдий
     *
     * @return Экземпляр менеджера гильдий
     */
    public static GuildManager getInstance() {
        if (instance == null) {
            Logger logger = Logger.getLogger("GuildManager");
            instance = new GuildManager(DatabaseManager.getInstance(), logger, GuildStorageManager.getInstance());
        }
        return instance;
    }

    /**
     * Инициализирует менеджер гильдий
     *
     * @param logger Логгер для вывода сообщений
     */
    public static void initialize(Logger logger) {
        if (instance == null) {
            GuildStorageManager.initialize(logger);
            instance = new GuildManager(DatabaseManager.getInstance(), logger, GuildStorageManager.getInstance());
            instance.loadGuilds();
        }
    }

    /**
     * Загружает все гильдии из базы данных
     */
    private void loadGuilds() {
        logger.info("Загрузка гильдий из базы данных...");

        try {
            // Получаем соединение с базой данных
            var connection = dbManager.getConnection();
            if (connection == null) {
                logger.severe("Не удалось получить соединение с базой данных для загрузки гильдий");
                return;
            }

            // SQL запрос для получения всех гильдий
            String sql = "SELECT id, name, tag, leader, description, creation_date, level, experience " +
                    "FROM guilds";

            try (var statement = connection.prepareStatement(sql);
                    var resultSet = statement.executeQuery()) {

                int count = 0;
                while (resultSet.next()) {
                    UUID guildId = UUID.fromString(resultSet.getString("id"));
                    String name = resultSet.getString("name");
                    String tag = resultSet.getString("tag");
                    UUID leaderId = UUID.fromString(resultSet.getString("leader"));
                    String description = resultSet.getString("description");
                    Date creationDate = new Date(resultSet.getTimestamp("creation_date").getTime());
                    int level = resultSet.getInt("level");
                    int experience = resultSet.getInt("experience");

                    // Создаем объект гильдии
                    Guild guild = new Guild();
                    guild.setId(guildId);
                    guild.setName(name);
                    guild.setTag(tag);
                    guild.setLeader(leaderId);
                    guild.setDescription(description);
                    guild.setCreationDate(creationDate);
                    guild.setLevel(level);
                    guild.setExperience(experience);

                    // Загружаем участников гильдии
                    loadGuildMembers(guild);

                    // Добавляем гильдию в кеш
                    guilds.put(guildId, guild);

                    // Добавляем лидера в мапу связей игрок-гильдия
                    playerGuilds.put(leaderId, guildId);

                    count++;
                }

                logger.info("Загружено " + count + " гильдий из базы данных");
            }
        } catch (Exception e) {
            logger.severe("Ошибка при загрузке гильдий из базы данных: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Загружает участников гильдии из базы данных
     * 
     * @param guild Гильдия, для которой загружаются участники
     */
    private void loadGuildMembers(Guild guild) {
        try {
            // Получаем соединение с базой данных
            var connection = dbManager.getConnection();
            if (connection == null) {
                logger.severe("Не удалось получить соединение с базой данных для загрузки участников гильдии");
                return;
            }

            // SQL запрос для получения всех участников гильдии
            String sql = "SELECT player_id, player_name, role FROM guild_members WHERE guild_id = ?";

            try (var statement = connection.prepareStatement(sql)) {
                statement.setString(1, guild.getId().toString());

                try (var resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        UUID playerId = UUID.fromString(resultSet.getString("player_id"));
                        String playerName = resultSet.getString("player_name");
                        String roleStr = resultSet.getString("role");

                        // Преобразуем строку роли в перечисление
                        GuildRole role = GuildRole.valueOf(roleStr.toUpperCase());

                        // Добавляем участника в гильдию
                        guild.getMembers().put(playerId, role);

                        // Добавляем связь игрок-гильдия в кеш
                        playerGuilds.put(playerId, guild.getId());
                    }

                    logger.info(
                            "Загружено " + guild.getMembers().size() + " участников для гильдии " + guild.getName());
                }
            }
        } catch (Exception e) {
            logger.severe("Ошибка при загрузке участников гильдии " + guild.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Создает новую гильдию
     *
     * @param name        Название гильдии
     * @param tag         Тег гильдии
     * @param leaderId    UUID лидера гильдии
     * @param description Описание гильдии
     * @return Созданная гильдия или null, если создание не удалось
     */
    public Guild createGuild(String name, String tag, UUID leaderId, String description) {
        if (playerGuilds.containsKey(leaderId)) {
            return null; // Игрок уже состоит в гильдии
        }

        for (Guild guild : guilds.values()) {
            if (guild.getName().equalsIgnoreCase(name) || guild.getTag().equalsIgnoreCase(tag)) {
                return null; // Гильдия с таким названием или тегом уже существует
            }
        }

        Guild guild = new Guild(name, tag, leaderId);
        guild.setDescription(description);
        guilds.put(guild.getId(), guild);
        playerGuilds.put(leaderId, guild.getId());

        // Создаем хранилище гильдии
        GuildStorage storage = guildStorageManager.createStorage(guild, 10); // Начальное количество слотов - 10
        if (storage == null) {
            logger.warning("Не удалось создать хранилище для гильдии " + name);
        } else {
            logger.info("Создано хранилище для гильдии " + name + " с 10 слотами");
        }

        return guild;
    }

    /**
     * Сохраняет гильдию в базу данных
     *
     * @param guild Гильдия для сохранения
     * @return true, если сохранение успешно
     */
    public boolean saveGuild(Guild guild) {
        // TODO: Реализовать сохранение гильдии в базу данных
        return true;
    }

    /**
     * Удаляет гильдию и все связанные данные
     *
     * @param guildId UUID гильдии
     * @return true, если удаление успешно
     */
    public boolean deleteGuild(UUID guildId) {
        Guild guild = guilds.remove(guildId);
        if (guild == null) {
            return false;
        }

        for (UUID playerId : guild.getMembers().keySet()) {
            playerGuilds.remove(playerId);
        }

        // TODO: Реализовать удаление гильдии из базы данных

        return true;
    }

    /**
     * Получает гильдию по её UUID
     *
     * @param guildId UUID гильдии
     * @return Гильдия или null, если не найдена
     */
    public Guild getGuild(UUID guildId) {
        return guilds.get(guildId);
    }

    /**
     * Получает гильдию по имени или тегу
     *
     * @param nameOrTag Имя или тег гильдии
     * @return Гильдия или null, если не найдена
     */
    public Guild getGuildByNameOrTag(String nameOrTag) {
        for (Guild guild : guilds.values()) {
            if (guild.getName().equalsIgnoreCase(nameOrTag) || guild.getTag().equalsIgnoreCase(nameOrTag)) {
                return guild;
            }
        }
        return null;
    }

    /**
     * Получает гильдию игрока
     *
     * @param playerId UUID игрока
     * @return Гильдия или null, если игрок не состоит в гильдии
     */
    public Guild getPlayerGuild(UUID playerId) {
        UUID guildId = playerGuilds.get(playerId);
        return guildId != null ? guilds.get(guildId) : null;
    }

    /**
     * Добавляет игрока в гильдию
     *
     * @param guildId  UUID гильдии
     * @param playerId UUID игрока
     * @param role     Роль игрока в гильдии
     * @return true, если игрок успешно добавлен
     */
    public boolean addPlayerToGuild(UUID guildId, UUID playerId, GuildRole role) {
        Guild guild = guilds.get(guildId);
        if (guild == null || playerGuilds.containsKey(playerId)) {
            return false;
        }

        guild.addMember(playerId, role);
        playerGuilds.put(playerId, guildId);
        return true;
    }

    /**
     * Удаляет игрока из гильдии
     *
     * @param guildId  UUID гильдии
     * @param playerId UUID игрока
     * @return true, если игрок успешно удален
     */
    public boolean removePlayerFromGuild(UUID guildId, UUID playerId) {
        Guild guild = guilds.get(guildId);
        if (guild == null || !guild.removeMember(playerId)) {
            return false;
        }

        playerGuilds.remove(playerId);
        return true;
    }

    /**
     * Получает список всех гильдий
     *
     * @return Список всех гильдий
     */
    public List<Guild> getAllGuilds() {
        return new ArrayList<>(guilds.values());
    }

    /**
     * Получает топ гильдий по выбранному критерию
     *
     * @param limit  Количество гильдий в топе
     * @param sortBy Критерий сортировки: "level", "experience", "members", "age"
     * @return Список гильдий, отсортированный по выбранному критерию
     */
    public List<Guild> getTopGuilds(int limit, String sortBy) {
        return guilds.values().stream()
                .sorted((g1, g2) -> {
                    switch (sortBy.toLowerCase()) {
                        case "level":
                            int levelCompare = Integer.compare(g2.getLevel(), g1.getLevel());
                            if (levelCompare != 0)
                                return levelCompare;
                            return Integer.compare(g2.getExperience(), g1.getExperience());

                        case "experience":
                            return Integer.compare(g2.getExperience(), g1.getExperience());

                        case "members":
                            return Integer.compare(g2.getMembers().size(), g1.getMembers().size());

                        case "age":
                            return g1.getCreationDate().compareTo(g2.getCreationDate());

                        default:
                            return Integer.compare(g2.getLevel(), g1.getLevel());
                    }
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Получает топ гильдий по уровню (упрощенный метод)
     *
     * @param limit Количество гильдий в топе
     * @return Список гильдий, отсортированный по уровню
     */
    public List<Guild> getTopGuilds(int limit) {
        return getTopGuilds(limit, "level");
    }

    /**
     * Вычисляет и применяет награды за повышение уровня гильдии
     * 
     * @param guild Гильдия, повысившая уровень
     */
    private void calculateLevelUpRewards(Guild guild) {
        int level = guild.getLevel();

        // Дополнительные слоты хранилища на определенных уровнях
        if (level == 5 || level == 10 || level == 15 || level == 20 || level == 25) {
            // Увеличиваем размер хранилища
            int additionalSlots = 10; // 10 дополнительных слотов за каждый указанный уровень

            // Безопасное получение и обновление хранилища с проверкой на null
            try {
                // Обновим хранилище, если гильдия имеет его
                guildStorageManager.expandGuildStorage(guild.getId(), additionalSlots);

                // Логируем это действие
                logger.info("Увеличено хранилище гильдии " + guild.getName() + " на " + additionalSlots +
                        " слотов за достижение " + level + " уровня.");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Не удалось обновить хранилище гильдии: " + guild.getName(), e);
            }
        }

        // Увеличение максимального количества участников на определенных уровнях
        if (level == 3) {
            guild.setMaxMembers(15); // 15 максимум участников на 3 уровне
        } else if (level == 7) {
            guild.setMaxMembers(20); // 20 максимум участников на 7 уровне
        } else if (level == 12) {
            guild.setMaxMembers(25); // 25 максимум участников на 12 уровне
        } else if (level == 18) {
            guild.setMaxMembers(30); // 30 максимум участников на 18 уровне
        } else if (level == 25) {
            guild.setMaxMembers(40); // 40 максимум участников на 25 уровне
        }
    }

    /**
     * Добавляет опыт гильдии с учетом наград за повышение уровня
     * 
     * @param guildId UUID гильдии
     * @param amount  Количество опыта
     * @param source  Источник опыта (для логирования)
     * @return true, если добавление успешно
     */
    public boolean addGuildExperienceWithRewards(UUID guildId, int amount, String source) {
        if (amount <= 0) {
            return false;
        }

        Guild guild = guilds.get(guildId);
        if (guild == null) {
            return false;
        }

        int oldLevel = guild.getLevel();
        guild.addExperience(amount);
        int newLevel = guild.getLevel();

        // Логируем получение опыта
        logger.info("Гильдия " + guild.getName() + " получила " + amount + " опыта из источника: " + source);

        // Если уровень повысился, применяем награды
        if (newLevel > oldLevel) {
            // Логируем повышение уровня
            logger.info("Гильдия " + guild.getName() + " повысила уровень с " + oldLevel + " до " + newLevel + "!");

            // Рассчитываем и применяем награды за новый уровень
            calculateLevelUpRewards(guild);
        }

        // Сохраняем изменения в гильдии
        saveGuild(guild);

        return true;
    }

    // Метод для установки режима чата гильдии для игрока
    public void setGuildChatMode(UUID playerId, boolean enabled) {
        guildChatMode.put(playerId, enabled);
    }

    // Метод для проверки, включен ли режим чата гильдии для игрока
    public boolean isGuildChatMode(UUID playerId) {
        return guildChatMode.getOrDefault(playerId, false);
    }

    /**
     * Проверяет, есть ли у игрока приглашение в гильдию
     *
     * @param playerId UUID игрока
     * @param guildId  UUID гильдии
     * @return true, если приглашение существует
     */
    public boolean hasGuildInvitation(UUID playerId, UUID guildId) {
        Guild guild = guilds.get(guildId);
        return guild != null && guild.getInvites().contains(playerId);
    }

    /**
     * Удаляет приглашение игрока в гильдию
     *
     * @param playerId UUID игрока
     * @param guildId  UUID гильдии
     * @return true, если приглашение было удалено
     */
    public boolean removeGuildInvitation(UUID playerId, UUID guildId) {
        Guild guild = guilds.get(guildId);
        if (guild == null) {
            return false;
        }

        boolean removed = guild.removeInvite(playerId);

        if (removed) {
            logger.info("Удалено приглашение для игрока " + playerId + " из гильдии " + guild.getName());
            // Сохраняем изменения в БД
            saveGuild(guild);
        }

        return removed;
    }

    /**
     * Устанавливает роль игрока в гильдии
     *
     * @param guildId  UUID гильдии
     * @param playerId UUID игрока
     * @param role     Новая роль игрока
     * @return true, если роль успешно изменена
     */
    public boolean setPlayerRole(UUID guildId, UUID playerId, GuildRole role) {
        Guild guild = guilds.get(guildId);
        if (guild == null || !guild.getMembers().containsKey(playerId)) {
            logger.warning("Попытка установить роль для игрока, который не состоит в гильдии");
            return false;
        }

        // Проверяем, не пытаемся ли мы изменить роль лидера
        if (playerId.equals(guild.getLeader()) && role != GuildRole.LEADER) {
            logger.warning("Попытка изменить роль текущего лидера гильдии на не-лидерскую роль");
            // Если устанавливаем роль для текущего лидера, но не LEADER
            return false;
        }

        // Сохраняем старую роль для логгирования
        GuildRole oldRole = guild.getMembers().get(playerId);

        // Если мы хотим назначить нового лидера
        if (role == GuildRole.LEADER) {
            // Используем метод changeLeader для правильной смены лидера
            boolean result = guild.changeLeader(playerId);
            if (result) {
                logger.info("Игрок " + playerId + " стал новым лидером гильдии " + guild.getName() +
                        " (предыдущая роль: " + oldRole + ")");
                saveGuild(guild);
            }
            return result;
        }

        // Для обычных ролей просто устанавливаем
        guild.getMembers().put(playerId, role);
        logger.info("Игроку " + playerId + " установлена роль " + role +
                " в гильдии " + guild.getName() +
                " (предыдущая роль: " + oldRole + ")");

        // Сохраняем изменения
        saveGuild(guild);
        return true;
    }

    /**
     * Добавляет связь игрок-гильдия в кеш
     * 
     * @param playerId UUID игрока
     * @param guildId  UUID гильдии
     */
    public void addPlayerGuild(UUID playerId, UUID guildId) {
        if (playerId == null || guildId == null) {
            logger.warning("Попытка добавить связь игрок-гильдия с null значениями");
            return;
        }

        playerGuilds.put(playerId, guildId);
        logger.info("Добавлена связь игрок-гильдия: " + playerId + " -> " + guildId);
    }

    private void createTablesIfNotExist() {
        // Реализуем создание таблиц в базе данных, если они не существуют
        try {
            var connection = dbManager.getConnection();
            if (connection == null) {
                logger.severe("Не удалось получить соединение с базой данных для создания таблиц");
                return;
            }

            try (var statement = connection.createStatement()) {
                // Создаем таблицу гильдий
                statement.executeUpdate(SQLQueries.CREATE_GUILDS_TABLE);

                // Создаем таблицу участников гильдий
                statement.executeUpdate(SQLQueries.CREATE_GUILD_MEMBERS_TABLE);

                // Создаем таблицу приглашений в гильдии
                statement.executeUpdate(SQLQueries.CREATE_GUILD_INVITES_TABLE);

                logger.info("Таблицы гильдий успешно созданы или уже существуют");
            }
        } catch (Exception e) {
            logger.severe("Ошибка при создании таблиц гильдий: " + e.getMessage());
            e.printStackTrace();
        }
    }
}