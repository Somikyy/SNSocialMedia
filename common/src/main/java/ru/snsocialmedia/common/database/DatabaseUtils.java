package ru.snsocialmedia.common.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Утилитный класс для работы с базой данных
 */
public class DatabaseUtils {
    private static final Logger logger = Logger.getLogger(DatabaseUtils.class.getName());

    /**
     * Безопасно устанавливает UUID в PreparedStatement
     * 
     * @param statement PreparedStatement для установки параметра
     * @param index     индекс параметра
     * @param uuid      UUID для установки
     * @throws SQLException если произошла ошибка SQL
     */
    public static void setUUID(PreparedStatement statement, int index, UUID uuid) throws SQLException {
        if (uuid == null) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, uuid.toString());
        }
    }

    /**
     * Получает UUID из ResultSet по имени колонки
     * 
     * @param resultSet  ResultSet для получения UUID
     * @param columnName имя колонки
     * @return UUID или null, если значение в базе данных равно NULL
     * @throws SQLException если произошла ошибка SQL
     */
    public static UUID getUUID(ResultSet resultSet, String columnName) throws SQLException {
        String uuidString = resultSet.getString(columnName);
        if (uuidString == null || uuidString.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Не удалось преобразовать строку в UUID: " + uuidString, e);
            return null;
        }
    }

    /**
     * Получает UUID из ResultSet по индексу колонки
     * 
     * @param resultSet   ResultSet для получения UUID
     * @param columnIndex индекс колонки
     * @return UUID или null, если значение в базе данных равно NULL
     * @throws SQLException если произошла ошибка SQL
     */
    public static UUID getUUID(ResultSet resultSet, int columnIndex) throws SQLException {
        String uuidString = resultSet.getString(columnIndex);
        if (uuidString == null || uuidString.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Не удалось преобразовать строку в UUID: " + uuidString, e);
            return null;
        }
    }

    /**
     * Безопасно устанавливает дату в PreparedStatement
     * 
     * @param statement PreparedStatement для установки параметра
     * @param index     индекс параметра
     * @param date      дата для установки
     * @throws SQLException если произошла ошибка SQL
     */
    public static void setDate(PreparedStatement statement, int index, Date date) throws SQLException {
        if (date == null) {
            statement.setNull(index, java.sql.Types.TIMESTAMP);
        } else {
            statement.setTimestamp(index, new Timestamp(date.getTime()));
        }
    }

    /**
     * Получает дату из ResultSet по имени колонки
     * 
     * @param resultSet  ResultSet для получения даты
     * @param columnName имя колонки
     * @return Date или null, если значение в базе данных равно NULL
     * @throws SQLException если произошла ошибка SQL
     */
    public static Date getDate(ResultSet resultSet, String columnName) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(columnName);
        return timestamp != null ? new Date(timestamp.getTime()) : null;
    }

    /**
     * Логирует SQL-запрос и его параметры для отладки
     * 
     * @param query  строка SQL-запроса
     * @param params параметры запроса
     */
    public static void logQuery(String query, Object... params) {
        StringBuilder logMessage = new StringBuilder("Executing SQL: ").append(query);

        if (params != null && params.length > 0) {
            logMessage.append(" with params: [");
            for (int i = 0; i < params.length; i++) {
                if (i > 0) {
                    logMessage.append(", ");
                }
                logMessage.append(params[i] == null ? "NULL" : params[i].toString());
            }
            logMessage.append("]");
        }

        logger.info(logMessage.toString());
    }
}