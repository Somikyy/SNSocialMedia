package ru.snsocialmedia.common.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Репозиторий для работы с друзьями и запросами на дружбу
 */
public class FriendRepository extends BaseRepository {
    private static FriendRepository instance;

    private FriendRepository(Logger logger) {
        super(logger);
    }

    public static synchronized FriendRepository getInstance(Logger logger) {
        if (instance == null) {
            instance = new FriendRepository(logger);
        }
        return instance;
    }

    /**
     * Создает запрос на дружбу
     * 
     * @param requestId   ID запроса
     * @param senderId    ID отправителя
     * @param receiverId  ID получателя
     * @param requestDate дата запроса
     * @param status      статус запроса
     * @return true, если запрос создан успешно
     */
    public boolean createFriendRequest(UUID requestId, UUID senderId, UUID receiverId, Date requestDate,
            String status) {
        Connection connection = null;
        PreparedStatement statement = null;

        try {
            connection = getConnection();
            statement = connection.prepareStatement(SQLQueries.INSERT_FRIEND_REQUEST);

            DatabaseUtils.setUUID(statement, 1, requestId);
            DatabaseUtils.setUUID(statement, 2, senderId);
            DatabaseUtils.setUUID(statement, 3, receiverId);
            DatabaseUtils.setDate(statement, 4, requestDate);
            statement.setString(5, status);

            DatabaseUtils.logQuery(SQLQueries.INSERT_FRIEND_REQUEST, requestId, senderId, receiverId, requestDate,
                    status);

            int affectedRows = statement.executeUpdate();
            logger.info("Запрос на дружбу создан, затронуто строк: " + affectedRows);

            return affectedRows > 0;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при создании запроса на дружбу", e);
            return false;
        } finally {
            closeResources(connection, statement, null);
        }
    }

    /**
     * Обновляет статус запроса на дружбу
     * 
     * @param requestId ID запроса
     * @param status    новый статус
     * @return true, если статус обновлен успешно
     */
    public boolean updateFriendRequestStatus(UUID requestId, String status) {
        Connection connection = null;
        PreparedStatement statement = null;

        try {
            connection = getConnection();
            statement = connection.prepareStatement(SQLQueries.UPDATE_FRIEND_REQUEST);

            statement.setString(1, status);
            DatabaseUtils.setUUID(statement, 2, requestId);

            DatabaseUtils.logQuery(SQLQueries.UPDATE_FRIEND_REQUEST, status, requestId);

            int affectedRows = statement.executeUpdate();
            logger.info("Статус запроса на дружбу обновлен, затронуто строк: " + affectedRows);

            return affectedRows > 0;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при обновлении статуса запроса на дружбу", e);
            return false;
        } finally {
            closeResources(connection, statement, null);
        }
    }

    /**
     * Создает дружбу между двумя игроками
     * 
     * @param friendshipId   ID дружбы
     * @param player1Id      ID первого игрока
     * @param player2Id      ID второго игрока
     * @param friendshipDate дата создания дружбы
     * @param favorite       отмечена ли дружба как избранная
     * @return true, если дружба создана успешно
     */
    public boolean createFriendship(UUID friendshipId, UUID player1Id, UUID player2Id, Date friendshipDate,
            boolean favorite) {
        Connection connection = null;
        PreparedStatement statement = null;

        try {
            // Проверяем, существует ли уже дружба между этими игроками
            if (existsFriendship(player1Id, player2Id)) {
                logger.info("Дружба между игроками " + player1Id + " и " + player2Id + " уже существует");
                return false;
            }

            connection = getConnection();
            statement = connection.prepareStatement(SQLQueries.INSERT_FRIENDSHIP);

            DatabaseUtils.setUUID(statement, 1, friendshipId);
            DatabaseUtils.setUUID(statement, 2, player1Id);
            DatabaseUtils.setUUID(statement, 3, player2Id);
            DatabaseUtils.setDate(statement, 4, friendshipDate);
            statement.setBoolean(5, favorite);

            DatabaseUtils.logQuery(SQLQueries.INSERT_FRIENDSHIP, friendshipId, player1Id, player2Id, friendshipDate,
                    favorite);

            int affectedRows = statement.executeUpdate();
            logger.info("Дружба создана, затронуто строк: " + affectedRows);

            return affectedRows > 0;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при создании дружбы", e);
            return false;
        } finally {
            closeResources(connection, statement, null);
        }
    }

    /**
     * Проверяет, существует ли дружба между двумя игроками
     * 
     * @param player1Id ID первого игрока
     * @param player2Id ID второго игрока
     * @return true, если дружба существует
     */
    public boolean existsFriendship(UUID player1Id, UUID player2Id) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            connection = getConnection();
            statement = connection.prepareStatement(SQLQueries.SELECT_FRIENDSHIP_BY_PLAYERS);

            DatabaseUtils.setUUID(statement, 1, player1Id);
            DatabaseUtils.setUUID(statement, 2, player2Id);
            DatabaseUtils.setUUID(statement, 3, player2Id);
            DatabaseUtils.setUUID(statement, 4, player1Id);

            DatabaseUtils.logQuery(SQLQueries.SELECT_FRIENDSHIP_BY_PLAYERS, player1Id, player2Id, player2Id, player1Id);

            resultSet = statement.executeQuery();
            boolean exists = resultSet.next();

            logger.info("Проверка существования дружбы между " + player1Id + " и " + player2Id + ": " + exists);

            return exists;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при проверке существования дружбы", e);
            return false;
        } finally {
            closeResources(connection, statement, resultSet);
        }
    }

    /**
     * Удаляет дружбу между двумя игроками
     * 
     * @param player1Id ID первого игрока
     * @param player2Id ID второго игрока
     * @return true, если дружба удалена успешно
     */
    public boolean deleteFriendship(UUID player1Id, UUID player2Id) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            connection = getConnection();

            // Сначала находим ID дружбы
            PreparedStatement findStatement = connection.prepareStatement(SQLQueries.SELECT_FRIENDSHIP_BY_PLAYERS);
            DatabaseUtils.setUUID(findStatement, 1, player1Id);
            DatabaseUtils.setUUID(findStatement, 2, player2Id);
            DatabaseUtils.setUUID(findStatement, 3, player2Id);
            DatabaseUtils.setUUID(findStatement, 4, player1Id);

            resultSet = findStatement.executeQuery();

            if (!resultSet.next()) {
                logger.info("Дружба между игроками " + player1Id + " и " + player2Id + " не найдена");
                return false;
            }

            UUID friendshipId = DatabaseUtils.getUUID(resultSet, "id");
            resultSet.close();
            findStatement.close();

            // Теперь удаляем дружбу
            statement = connection.prepareStatement(SQLQueries.DELETE_FRIENDSHIP);
            DatabaseUtils.setUUID(statement, 1, friendshipId);

            DatabaseUtils.logQuery(SQLQueries.DELETE_FRIENDSHIP, friendshipId);

            int affectedRows = statement.executeUpdate();
            logger.info("Дружба удалена, затронуто строк: " + affectedRows);

            return affectedRows > 0;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при удалении дружбы", e);
            return false;
        } finally {
            closeResources(connection, statement, resultSet);
        }
    }

    /**
     * Получает список друзей игрока
     * 
     * @param playerId ID игрока
     * @return список ID друзей
     */
    public List<UUID> getPlayerFriends(UUID playerId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<UUID> friends = new ArrayList<>();

        try {
            connection = getConnection();
            statement = connection.prepareStatement(SQLQueries.SELECT_PLAYER_FRIENDSHIPS);

            DatabaseUtils.setUUID(statement, 1, playerId);
            DatabaseUtils.setUUID(statement, 2, playerId);

            DatabaseUtils.logQuery(SQLQueries.SELECT_PLAYER_FRIENDSHIPS, playerId, playerId);

            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                UUID player1Id = DatabaseUtils.getUUID(resultSet, "player1_id");
                UUID player2Id = DatabaseUtils.getUUID(resultSet, "player2_id");

                if (player1Id.equals(playerId)) {
                    friends.add(player2Id);
                } else {
                    friends.add(player1Id);
                }
            }

            logger.info("Получен список друзей игрока " + playerId + ", количество: " + friends.size());

            return friends;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при получении списка друзей", e);
            return friends;
        } finally {
            closeResources(connection, statement, resultSet);
        }
    }

    /**
     * Получает список входящих запросов на дружбу
     * 
     * @param playerId ID игрока
     * @return список ID отправителей запросов
     */
    public List<UUID> getPendingFriendRequests(UUID playerId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<UUID> requests = new ArrayList<>();

        try {
            connection = getConnection();
            statement = connection.prepareStatement(SQLQueries.SELECT_PENDING_FRIEND_REQUESTS_BY_RECEIVER);

            DatabaseUtils.setUUID(statement, 1, playerId);

            DatabaseUtils.logQuery(SQLQueries.SELECT_PENDING_FRIEND_REQUESTS_BY_RECEIVER, playerId);

            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                UUID senderId = DatabaseUtils.getUUID(resultSet, "sender_id");
                requests.add(senderId);
            }

            logger.info("Получен список входящих запросов на дружбу для игрока " + playerId + ", количество: "
                    + requests.size());

            return requests;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при получении списка входящих запросов на дружбу", e);
            return requests;
        } finally {
            closeResources(connection, statement, resultSet);
        }
    }

    /**
     * Проверяет наличие запроса на дружбу
     * 
     * @param senderId   ID отправителя
     * @param receiverId ID получателя
     * @return true, если запрос существует
     */
    public boolean existsFriendRequest(UUID senderId, UUID receiverId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            connection = getConnection();
            String query = "SELECT * FROM friend_requests WHERE sender_id = ? AND receiver_id = ? AND status = 'PENDING'";
            statement = connection.prepareStatement(query);

            DatabaseUtils.setUUID(statement, 1, senderId);
            DatabaseUtils.setUUID(statement, 2, receiverId);

            DatabaseUtils.logQuery(query, senderId, receiverId);

            resultSet = statement.executeQuery();
            boolean exists = resultSet.next();

            logger.info("Проверка существования запроса на дружбу от " + senderId + " к " + receiverId + ": " + exists);

            return exists;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при проверке существования запроса на дружбу", e);
            return false;
        } finally {
            closeResources(connection, statement, resultSet);
        }
    }
}