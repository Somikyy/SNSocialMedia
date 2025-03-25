package ru.snsocialmedia.common.models.friend;

/**
 * Перечисление возможных статусов запроса дружбы
 */
public enum FriendRequestStatus {
    /**
     * Запрос в ожидании ответа
     */
    PENDING,

    /**
     * Запрос принят
     */
    ACCEPTED,

    /**
     * Запрос отклонён
     */
    DECLINED
}