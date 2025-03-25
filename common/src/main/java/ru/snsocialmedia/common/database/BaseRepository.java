package ru.snsocialmedia.common.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Базовый класс для всех репозиториев, работающих с базой данных
 */
public abstract class BaseRepository {
    protected final Logger logger;

    public BaseRepository(Logger logger) {
        this.logger = logger;
    }

    /**
     * Получает соединение с базой данных
     * 
     * @return Connection объект соединения
     * @throws SQLException если произошла ошибка при получении соединения
     */
    protected Connection getConnection() throws SQLException {
        DatabaseManager dbManager = DatabaseManager.getInstance();
        if (dbManager == null) {
            throw new SQLException("DatabaseManager не инициализирован");
        }
        if (!dbManager.isConnected()) {
            throw new SQLException("Нет активного подключения к базе данных");
        }

        Connection connection = dbManager.getConnection();
        if (connection == null) {
            throw new SQLException("Не удалось получить соединение с базой данных");
        }
        return connection;
    }

    /**
     * Безопасно закрывает ресурсы базы данных
     * 
     * @param connection соединение для закрытия
     * @param statement  подготовленное выражение для закрытия
     * @param resultSet  результат запроса для закрытия
     */
    protected void closeResources(Connection connection, PreparedStatement statement, ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Ошибка при закрытии ResultSet", e);
            }
        }

        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Ошибка при закрытии PreparedStatement", e);
            }
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Ошибка при закрытии Connection", e);
            }
        }
    }

    /**
     * Выполняет SQL-запрос без возврата результатов
     * 
     * @param query  строка SQL-запроса
     * @param params параметры запроса
     * @return true, если запрос выполнен успешно, false в противном случае
     */
    protected boolean executeUpdate(String query, Object... params) {
        Connection connection = null;
        PreparedStatement statement = null;

        try {
            connection = getConnection();
            statement = connection.prepareStatement(query);

            for (int i = 0; i < params.length; i++) {
                if (params[i] == null) {
                    statement.setNull(i + 1, java.sql.Types.VARCHAR);
                } else {
                    statement.setObject(i + 1, params[i]);
                }
            }

            DatabaseUtils.logQuery(query, params);

            int affectedRows = statement.executeUpdate();
            logger.info("SQL запрос выполнен, затронуто строк: " + affectedRows);

            return affectedRows > 0;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при выполнении SQL-запроса: " + query, e);
            return false;
        } finally {
            closeResources(connection, statement, null);
        }
    }

    /**
     * Проверяет существование записи в базе данных
     * 
     * @param query  строка SQL-запроса для проверки
     * @param params параметры запроса
     * @return true, если запись существует, false в противном случае
     */
    protected boolean exists(String query, Object... params) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            connection = getConnection();
            statement = connection.prepareStatement(query);

            for (int i = 0; i < params.length; i++) {
                if (params[i] == null) {
                    statement.setNull(i + 1, java.sql.Types.VARCHAR);
                } else {
                    statement.setObject(i + 1, params[i]);
                }
            }

            DatabaseUtils.logQuery(query, params);

            resultSet = statement.executeQuery();
            boolean exists = resultSet.next();

            logger.info("Проверка существования записи: " + exists);

            return exists;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при проверке существования записи: " + query, e);
            return false;
        } finally {
            closeResources(connection, statement, resultSet);
        }
    }

    /**
     * Получает количество записей в результате запроса
     * 
     * @param query  строка SQL-запроса для подсчета
     * @param params параметры запроса
     * @return количество записей или -1, если произошла ошибка
     */
    protected int count(String query, Object... params) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            connection = getConnection();
            statement = connection.prepareStatement(query);

            for (int i = 0; i < params.length; i++) {
                if (params[i] == null) {
                    statement.setNull(i + 1, java.sql.Types.VARCHAR);
                } else {
                    statement.setObject(i + 1, params[i]);
                }
            }

            DatabaseUtils.logQuery(query, params);

            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                logger.info("Подсчет записей: " + count);
                return count;
            }

            return 0;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при подсчете записей: " + query, e);
            return -1;
        } finally {
            closeResources(connection, statement, resultSet);
        }
    }

    /**
     * Проверяет, работает ли соединение с базой данных
     * 
     * @return true, если соединение работает, false в противном случае
     */
    protected boolean testConnection() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            connection = getConnection();
            statement = connection.prepareStatement("SELECT 1");
            resultSet = statement.executeQuery();

            boolean connected = resultSet.next() && resultSet.getInt(1) == 1;

            if (connected) {
                logger.info("Тест соединения с базой данных пройден успешно");
            } else {
                logger.warning("Тест соединения с базой данных не пройден");
            }

            return connected;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при тестировании соединения с базой данных", e);
            return false;
        } finally {
            closeResources(connection, statement, resultSet);
        }
    }
}