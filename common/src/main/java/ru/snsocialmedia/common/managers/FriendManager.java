package ru.snsocialmedia.common.managers;

import ru.snsocialmedia.common.database.DatabaseManager;
import ru.snsocialmedia.common.models.friend.FriendRequest;
import ru.snsocialmedia.common.models.friend.FriendRequestStatus;
import ru.snsocialmedia.common.models.friend.Friendship;

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
 * Менеджер для управления данными друзей
 */
public class FriendManager {

    private static FriendManager instance;
    private final Logger logger;
    private final Map<UUID, Set<Friendship>> playerFriendships = new ConcurrentHashMap<>();
    private final Map<UUID, Set<FriendRequest>> playerRequests = new ConcurrentHashMap<>();
    private final Map<UUID, FriendRequest> requests = new ConcurrentHashMap<>();
    private final Map<UUID, Friendship> friendships = new ConcurrentHashMap<>();

    private FriendManager(Logger logger) {
        this.logger = logger;
    }

    /**
     * Получает экземпляр менеджера друзей
     *
     * @return Экземпляр менеджера друзей
     */
    public static FriendManager getInstance() {
        return instance;
    }

    /**
     * Инициализирует менеджер друзей
     *
     * @param logger Логгер для вывода сообщений
     */
    public static void initialize(Logger logger) {
        if (instance == null) {
            instance = new FriendManager(logger);
            instance.loadFriendData();
        }
    }

    /**
     * Загружает данные о друзьях из базы данных
     */
    private void loadFriendData() {
        logger.info("Загрузка данных о друзьях из базы данных...");

        // TODO: Реализовать загрузку данных о друзьях из базы данных
    }

    /**
     * Отправляет запрос дружбы
     *
     * @param senderId   UUID отправителя
     * @param receiverId UUID получателя
     * @return Созданный запрос дружбы или null, если запрос не удалось отправить
     */
    public FriendRequest sendFriendRequest(UUID senderId, UUID receiverId) {
        // Проверяем, не являются ли игроки уже друзьями
        if (areFriends(senderId, receiverId)) {
            return null;
        }

        // Проверяем, не существует ли уже запрос от этого игрока
        for (FriendRequest request : getPlayerIncomingRequests(receiverId)) {
            if (request.getSenderId().equals(senderId) && request.getStatus() == FriendRequestStatus.PENDING) {
                return null;
            }
        }

        // Создаем новый запрос дружбы
        FriendRequest request = new FriendRequest(senderId, receiverId);

        // Сохраняем запрос в базу данных
        if (saveFriendRequest(request)) {
            requests.put(request.getId(), request);

            // Добавляем запрос в списки игроков
            playerRequests.computeIfAbsent(senderId, k -> new HashSet<>()).add(request);
            playerRequests.computeIfAbsent(receiverId, k -> new HashSet<>()).add(request);

            return request;
        }

        return null;
    }

    /**
     * Сохраняет запрос дружбы в базу данных
     *
     * @param request Запрос дружбы для сохранения
     * @return true, если сохранение успешно
     */
    public boolean saveFriendRequest(FriendRequest request) {
        // TODO: Реализовать сохранение запроса дружбы в базу данных
        return true;
    }

    /**
     * Принимает запрос дружбы
     *
     * @param requestId UUID запроса дружбы
     * @return Созданная дружба или null, если запрос не удалось принять
     */
    public Friendship acceptFriendRequest(UUID requestId) {
        FriendRequest request = requests.get(requestId);
        if (request == null || request.getStatus() != FriendRequestStatus.PENDING) {
            return null;
        }

        // Принимаем запрос
        request.accept();
        saveFriendRequest(request);

        // Создаем дружбу
        Friendship friendship = new Friendship(request.getSenderId(), request.getReceiverId());

        // Сохраняем дружбу в базу данных
        if (saveFriendship(friendship)) {
            friendships.put(friendship.getId(), friendship);

            // Добавляем дружбу в списки игроков
            playerFriendships.computeIfAbsent(request.getSenderId(), k -> new HashSet<>()).add(friendship);
            playerFriendships.computeIfAbsent(request.getReceiverId(), k -> new HashSet<>()).add(friendship);

            return friendship;
        }

        return null;
    }

    /**
     * Сохраняет дружбу в базу данных
     *
     * @param friendship Дружба для сохранения
     * @return true, если сохранение успешно
     */
    public boolean saveFriendship(Friendship friendship) {
        // TODO: Реализовать сохранение дружбы в базу данных
        return true;
    }

    /**
     * Отклоняет запрос дружбы
     *
     * @param requestId UUID запроса дружбы
     * @return true, если запрос успешно отклонен
     */
    public boolean declineFriendRequest(UUID requestId) {
        FriendRequest request = requests.get(requestId);
        if (request == null || request.getStatus() != FriendRequestStatus.PENDING) {
            return false;
        }

        // Отклоняем запрос
        request.decline();
        saveFriendRequest(request);

        return true;
    }

    /**
     * Удаляет дружбу
     *
     * @param friendshipId UUID дружбы
     * @return true, если дружба успешно удалена
     */
    public boolean removeFriendship(UUID friendshipId) {
        Friendship friendship = friendships.get(friendshipId);
        if (friendship == null) {
            return false;
        }

        // Удаляем дружбу из списков игроков
        Set<Friendship> player1Friendships = playerFriendships.get(friendship.getPlayer1Id());
        if (player1Friendships != null) {
            player1Friendships.remove(friendship);
        }

        Set<Friendship> player2Friendships = playerFriendships.get(friendship.getPlayer2Id());
        if (player2Friendships != null) {
            player2Friendships.remove(friendship);
        }

        // Удаляем дружбу из кэша
        friendships.remove(friendshipId);

        // TODO: Удаление дружбы из базы данных

        return true;
    }

    /**
     * Удаляет дружбу между игроками
     *
     * @param player1Id UUID первого игрока
     * @param player2Id UUID второго игрока
     * @return true, если дружба успешно удалена
     */
    public boolean removeFriendship(UUID player1Id, UUID player2Id) {
        Friendship friendship = getFriendship(player1Id, player2Id);
        if (friendship == null) {
            return false;
        }

        return removeFriendship(friendship.getId());
    }

    /**
     * Получает дружбу между игроками
     *
     * @param player1Id UUID первого игрока
     * @param player2Id UUID второго игрока
     * @return Дружба или null, если игроки не являются друзьями
     */
    public Friendship getFriendship(UUID player1Id, UUID player2Id) {
        Set<Friendship> player1Friendships = playerFriendships.get(player1Id);
        if (player1Friendships == null) {
            return null;
        }

        for (Friendship friendship : player1Friendships) {
            if (friendship.involvesPlayer(player2Id)) {
                return friendship;
            }
        }

        return null;
    }

    /**
     * Проверяет, являются ли игроки друзьями
     *
     * @param player1Id UUID первого игрока
     * @param player2Id UUID второго игрока
     * @return true, если игроки являются друзьями
     */
    public boolean areFriends(UUID player1Id, UUID player2Id) {
        return getFriendship(player1Id, player2Id) != null;
    }

    /**
     * Получает список друзей игрока
     *
     * @param playerId UUID игрока
     * @return Список UUID друзей игрока
     */
    public List<UUID> getPlayerFriends(UUID playerId) {
        Set<Friendship> friendships = playerFriendships.get(playerId);
        if (friendships == null) {
            return new ArrayList<>();
        }

        return friendships.stream()
                .map(friendship -> friendship.getOtherPlayer(playerId))
                .collect(Collectors.toList());
    }

    /**
     * Получает список входящих запросов дружбы игрока
     *
     * @param playerId UUID игрока
     * @return Список входящих запросов дружбы
     */
    public List<FriendRequest> getPlayerIncomingRequests(UUID playerId) {
        Set<FriendRequest> requests = playerRequests.get(playerId);
        if (requests == null) {
            return new ArrayList<>();
        }

        return requests.stream()
                .filter(request -> request.getReceiverId().equals(playerId)
                        && request.getStatus() == FriendRequestStatus.PENDING)
                .collect(Collectors.toList());
    }

    /**
     * Получает список исходящих запросов дружбы игрока
     *
     * @param playerId UUID игрока
     * @return Список исходящих запросов дружбы
     */
    public List<FriendRequest> getPlayerOutgoingRequests(UUID playerId) {
        Set<FriendRequest> requests = playerRequests.get(playerId);
        if (requests == null) {
            return new ArrayList<>();
        }

        return requests.stream()
                .filter(request -> request.getSenderId().equals(playerId)
                        && request.getStatus() == FriendRequestStatus.PENDING)
                .collect(Collectors.toList());
    }

    /**
     * Помечает дружбу как избранную
     *
     * @param friendshipId UUID дружбы
     * @param favorite     true, чтобы пометить как избранное
     * @return true, если статус успешно изменен
     */
    public boolean setFriendshipFavorite(UUID friendshipId, boolean favorite) {
        Friendship friendship = friendships.get(friendshipId);
        if (friendship == null) {
            return false;
        }

        friendship.setFavorite(favorite);
        saveFriendship(friendship);

        return true;
    }
}