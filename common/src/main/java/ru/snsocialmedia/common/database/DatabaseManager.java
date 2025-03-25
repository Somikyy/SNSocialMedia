package ru.snsocialmedia.common.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {

    private static DatabaseManager instance;
    private final Logger logger;
    private HikariDataSource dataSource;

    @Getter
    private boolean connected = false;

    private DatabaseManager(String host, int port, String database, String username, String password, int poolSize,
            Logger logger) {
        this.logger = logger;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=false&useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&allowPublicKeyRetrieval=true");
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(poolSize);
            config.setMinimumIdle(2); // Минимальное количество соединений в пуле
            config.setPoolName("SNSocialMedia-HikariPool");
            config.setConnectionTimeout(10000); // 10 секунд
            config.setIdleTimeout(600000); // 10 минут
            config.setMaxLifetime(1800000); // 30 минут
            config.setAutoCommit(true); // Важно! Автоматический коммит транзакций

            // Параметры для повышения производительности и стабильности
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
            config.addDataSourceProperty("connectTimeout", "10000");
            config.addDataSourceProperty("socketTimeout", "30000");

            dataSource = new HikariDataSource(config);

            // Тестовое подключение для проверки
            try (Connection connection = dataSource.getConnection()) {
                if (connection.isValid(5)) {
                    connected = true;
                    logger.info("Тестовое подключение к базе данных успешно!");
                } else {
                    logger.severe("Тестовое подключение к базе данных не прошло проверку valid()");
                    connected = false;
                }
            }

            if (connected) {
                logger.info("Успешное подключение к базе данных MySQL!");
            } else {
                logger.severe("Не удалось установить соединение с базой данных MySQL!");
            }
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "MySQL драйвер не найден!", e);
            dataSource = null;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка подключения к базе данных!", e);
            dataSource = null;
        }
    }

    public static DatabaseManager getInstance() {
        return instance;
    }

    public static void initialize(String host, int port, String database, String username, String password,
            int poolSize, Logger logger) {
        if (instance == null) {
            instance = new DatabaseManager(host, port, database, username, password, poolSize, logger);
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource не инициализирован.");
        }
        Connection connection = dataSource.getConnection();
        if (connection == null) {
            throw new SQLException("Не удалось получить соединение из пула.");
        }
        return connection;
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            connected = false;
            logger.info("Соединение с базой данных закрыто.");
        }
    }

    public void createTables() {
        try (Connection connection = getConnection();
                Statement statement = connection.createStatement()) {

            logger.info("Начинаю создание таблиц базы данных...");

            // Создаем таблицы для гильдий
            statement.executeUpdate(SQLQueries.CREATE_GUILDS_TABLE);
            logger.info("Таблица guilds создана.");

            statement.executeUpdate(SQLQueries.CREATE_GUILD_MEMBERS_TABLE);
            logger.info("Таблица guild_members создана.");

            statement.executeUpdate(SQLQueries.CREATE_GUILD_INVITES_TABLE);
            logger.info("Таблица guild_invites создана.");

            // Создаем таблицы для друзей
            statement.executeUpdate(SQLQueries.CREATE_FRIEND_REQUESTS_TABLE);
            logger.info("Таблица friend_requests создана.");

            statement.executeUpdate(SQLQueries.CREATE_FRIENDSHIPS_TABLE);
            logger.info("Таблица friendships создана.");

            // Создаем таблицы для пати
            statement.executeUpdate(SQLQueries.CREATE_PARTIES_TABLE);
            logger.info("Таблица parties создана.");

            statement.executeUpdate(SQLQueries.CREATE_PARTY_MEMBERS_TABLE);
            logger.info("Таблица party_members создана.");

            statement.executeUpdate(SQLQueries.CREATE_PARTY_INVITES_TABLE);
            logger.info("Таблица party_invites создана.");

            statement.executeUpdate(SQLQueries.CREATE_PARTY_SETTINGS_TABLE);
            logger.info("Таблица party_settings создана.");

            logger.info("Все таблицы базы данных успешно созданы.");

            // Проверка существования таблиц
            checkTablesExist(connection);

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при создании таблиц базы данных!", e);
        }
    }

    private void checkTablesExist(Connection connection) {
        try {
            Statement statement = connection.createStatement();
            statement.executeQuery("SHOW TABLES");
            java.sql.ResultSet rs = statement.getResultSet();

            logger.info("Проверка созданных таблиц:");
            StringBuilder tables = new StringBuilder();
            while (rs.next()) {
                tables.append(rs.getString(1)).append(", ");
            }
            if (tables.length() > 0) {
                tables.setLength(tables.length() - 2); // Убираем последнюю запятую и пробел
            }
            logger.info("Существующие таблицы: " + tables.toString());
            rs.close();
            statement.close();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Не удалось проверить существующие таблицы", e);
        }
    }
}