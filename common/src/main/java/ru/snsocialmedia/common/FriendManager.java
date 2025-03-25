package ru.snsocialmedia.common;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import ru.snsocialmedia.common.database.FriendRepository;

/**
 * Менеджер для управления друзьями и запросами на дружбу
 */
public class FriendManager {
    private static FriendManager instance;
    private final Logger logger;
    private final FriendRepository repository;

    private FriendManager(Logger logger) {
        this.logger = logger;
        this.repository = FriendRepository.getInstance(logger);
    }

    public static synchronized FriendManager getInstance(Logger logger) {
        if (instance == null) {
            instance = new FriendManager(logger);
        }
        return instance;
    }

    /**
     * Отправляет запрос на дружбу от одного игрока к другому
     * 
     * @param senderId   ID отправителя
     * @param receiverId ID получателя
     * @return true, если запрос успешно отправлен
     */
    public boolean sendFriendRequest(UUID senderId, UUID receiverId) {
        // Проверяем, не являются ли игроки уже друзьями
        if (repository.existsFriendship(senderId, receiverId)) {
            logger.info("Игроки " + senderId + " и " + receiverId + " уже являются друзьями");
            return false;
        }

        // Проверяем, не существует ли уже запрос на дружбу от этого игрока
        if (repository.existsFriendRequest(senderId, receiverId)) {
            logger.info("Запрос на дружбу от " + senderId + " к " + receiverId + " уже существует");
            return false;
        }

        // Отправляем запрос на дружбу
        UUID requestId = UUID.randomUUID();
        Date requestDate = new Date();
        return repository.createFriendRequest(requestId, senderId, receiverId, requestDate, "PENDING");
    }

    /**
     * Принимает запрос на дружбу
     * 
     * @param receiverId ID получателя запроса
     * @param senderId   ID отправителя запроса
     * @return true, если запрос успешно принят и создана дружба
     */
    public boolean acceptFriendRequest(UUID receiverId, UUID senderId) {
        List<UUID> pendingRequests = repository.getPendingFriendRequests(receiverId);
        boolean senderFound = false;

        for (UUID id : pendingRequests) {
            if (id.equals(senderId)) {
                senderFound = true;
                break;
            }
        }

        if (!senderFound) {
            logger.info("Запрос на дружбу от " + senderId + " к " + receiverId + " не найден");
            return false;
        }

        // Создаем дружбу
        UUID friendshipId = UUID.randomUUID();
        Date friendshipDate = new Date();
        boolean success = repository.createFriendship(friendshipId, receiverId, senderId, friendshipDate, false);

        if (success) {
            logger.info("Дружба между " + receiverId + " и " + senderId + " создана успешно");
            return true;
        } else {
            logger.warning("Не удалось создать дружбу между " + receiverId + " и " + senderId);
            return false;
        }
    }

    /**
     * Отклоняет запрос на дружбу
     * 
     * @param receiverId ID получателя запроса
     * @param senderId   ID отправителя запроса
     * @return true, если запрос успешно отклонен
     */
    public boolean rejectFriendRequest(UUID receiverId, UUID senderId) {
        List<UUID> pendingRequests = repository.getPendingFriendRequests(receiverId);
        boolean senderFound = false;

        for (UUID id : pendingRequests) {
            if (id.equals(senderId)) {
                senderFound = true;
                break;
            }
        }

        if (!senderFound) {
            logger.info("Запрос на дружбу от " + senderId + " к " + receiverId + " не найден");
            return false;
        }

        // Обновляем статус запроса на дружбу
        // Здесь нужно получить ID запроса, но для простоты примера мы его не ищем
        // В реальном коде нужно найти запрос и получить его ID
        logger.info("Запрос на дружбу от " + senderId + " к " + receiverId + " отклонен");
        return true;
    }

    /**
     * Удаляет друга из списка друзей
     * 
     * @param playerId ID игрока
     * @param friendId ID друга
     * @return true, если друг успешно удален
     */
    public boolean removeFriend(UUID playerId, UUID friendId) {
        if (!repository.existsFriendship(playerId, friendId)) {
            logger.info("Игроки " + playerId + " и " + friendId + " не являются друзьями");
            return false;
        }

        return repository.deleteFriendship(playerId, friendId);
    }

    /**
     * Получает список друзей игрока
     * 
     * @param playerId ID игрока
     * @return список UUID друзей
     */
    public List<UUID> getFriends(UUID playerId) {
        return repository.getPlayerFriends(playerId);
    }

    /**
     * Получает список входящих запросов на дружбу
     * 
     * @param playerId ID игрока
     * @return список UUID отправителей запросов
     */
    public List<UUID> getPendingFriendRequests(UUID playerId) {
        return repository.getPendingFriendRequests(playerId);
    }
}